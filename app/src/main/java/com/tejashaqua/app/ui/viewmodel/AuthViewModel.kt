package com.tejashaqua.app.ui.viewmodel

import android.app.Activity
import android.os.Bundle
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.tejashaqua.app.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OtpSent(val verificationId: String) : AuthState()
    data class Success(val userId: String, val userName: String, val mobileNumber: String, val joinedAt: Long, val isAdmin: Boolean = false, val showMobileNumber: Boolean = false) : AuthState()
    data class RequireName(val phoneNumber: String, val isAdmin: Boolean = false) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val analytics = FirebaseAnalytics.getInstance(application)
    private val functions = FirebaseFunctions.getInstance("asia-south1")
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _autoOtp = MutableStateFlow<String?>(null)
    val autoOtp: StateFlow<String?> = _autoOtp

    private var pendingPhoneNumber: String = ""
    private var loadingTimeoutJob: Job? = null
    private var userDocListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startUserDocListener(currentUser.uid)
        }
    }

    private fun startUserDocListener(userId: String) {
        userDocListener?.remove()
        // If we don't already have a Success state, show loading
        if (_authState.value !is AuthState.Success) {
            _authState.value = AuthState.Loading
        }

        userDocListener = db.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    _authState.value = AuthState.Error(error.localizedMessage ?: "Connection error")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val phone = document.getString("phone") ?: auth.currentUser?.phoneNumber?.removePrefix("+91") ?: ""
                    val joinedAt = document.getLong("joinedAt") ?: System.currentTimeMillis()
                    val onboardingComplete = document.getBoolean("onboardingComplete") ?: false
                    val showMobile = document.getBoolean("showMobileNumber") ?: false
                    val isAdmin = document.getBoolean("isAdmin") ?: false

                    if (onboardingComplete) {
                        _authState.value = AuthState.Success(userId, name, phone, joinedAt, isAdmin, showMobile)
                    } else {
                        _authState.value = AuthState.RequireName(phone, isAdmin)
                    }
                } else if (document != null && !document.exists()) {
                    // Create user document if missing
                    val phone = auth.currentUser?.phoneNumber?.removePrefix("+91") ?: ""
                    val adminNumbers = listOf("9359599599", "8014311143")
                    val isSuperAdmin = adminNumbers.contains(phone)

                    val now = System.currentTimeMillis()
                    val user = hashMapOf(
                        "uid" to userId,
                        "name" to "User",
                        "phone" to phone,
                        "joinedAt" to now,
                        "onboardingComplete" to false,
                        "isAdmin" to isSuperAdmin,
                        "showMobileNumber" to false,
                        "lastCheckedNotifications" to now
                    )
                    db.collection("users").document(userId).set(user)
                }
            }
    }

    fun updatePrivacyPreference(enabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        // Optimistically update to avoid local flicker
        val current = _authState.value
        if (current is AuthState.Success) {
            _authState.value = current.copy(showMobileNumber = enabled)
        }

        db.collection("users").document(userId).update("showMobileNumber", enabled)
    }

    override fun onCleared() {
        super.onCleared()
        userDocListener?.remove()
    }

    private fun startLoadingTimeout() {
        loadingTimeoutJob?.cancel()
        loadingTimeoutJob = viewModelScope.launch {
            delay(45000L) // 45 seconds safety timeout
            if (_authState.value is AuthState.Loading) {
                _authState.value = AuthState.Error("Request timed out. Please check your network and try again.")
            }
        }
    }

    private fun stopLoadingTimeout() {
        loadingTimeoutJob?.cancel()
        loadingTimeoutJob = null
    }

    fun clearVerificationData() {
        pendingPhoneNumber = ""
        _authState.value = AuthState.Idle
    }

    fun startOtpRetriever() {
        // 1. Try SMS Retriever (Silent, needs Hash)
        val client = SmsRetriever.getClient(getApplication())
        client.startSmsRetriever()
            .addOnSuccessListener {
                android.util.Log.d("AuthViewModel", "SMS Retriever started")
            }
            .addOnFailureListener {
                android.util.Log.e("AuthViewModel", "SMS Retriever failed", it)
            }
            
        // 2. Also start SMS User Consent (Needs "Allow" click, works WITHOUT Hash)
        client.startSmsUserConsent(null) // null listens to any OTP-like message from non-contacts
            .addOnSuccessListener {
                android.util.Log.d("AuthViewModel", "SMS User Consent started")
            }
    }

    fun setAutoOtp(otp: String) {
        if (otp.length == 6 && otp.all { it.isDigit() }) {
            _autoOtp.value = otp
        }
    }

    fun clearAutoOtp() {
        _autoOtp.value = null
    }

    fun sendOtp(phoneNumber: String, activity: Activity? = null) {
        if (phoneNumber.length != 10) {
            _authState.value = AuthState.Error(getApplication<android.app.Application>().getString(R.string.invalid_phone_error))
            return
        }
        
        // If already loading, don't trigger another one
        if (_authState.value is AuthState.Loading) return

        _authState.value = AuthState.Loading
        startLoadingTimeout()
        
        this.pendingPhoneNumber = phoneNumber

        try {
            val bundle = Bundle()
            bundle.putString("phone_number", "+91$phoneNumber")
            analytics.logEvent("otp_request", bundle)

            val data = hashMapOf("phoneNumber" to phoneNumber)
            functions.getHttpsCallable("sendOtp")
                .call(data)
                .addOnSuccessListener { result ->
                    stopLoadingTimeout()
                    val response = result.data as? Map<*, *>
                    if (response?.get("success") == true) {
                        android.util.Log.d("AuthViewModel", "OTP Sent successfully via MSG91")
                        _authState.value = AuthState.OtpSent("msg91_session")
                    } else {
                        val message = response?.get("message") as? String ?: "Failed to send OTP"
                        _authState.value = AuthState.Error(message)
                    }
                }
                .addOnFailureListener { e ->
                    stopLoadingTimeout()
                    android.util.Log.e("AuthViewModel", "sendOtp failure", e)
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to send OTP")
                }
        } catch (e: Exception) {
            stopLoadingTimeout()
            android.util.Log.e("AuthViewModel", "sendOtp exception", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to start verification")
        }
    }

    fun resendOtp(phoneNumber: String) {
        if (_authState.value is AuthState.Loading) return

        _authState.value = AuthState.Loading
        startLoadingTimeout()

        val data = hashMapOf("phoneNumber" to phoneNumber)
        functions.getHttpsCallable("resendOtp")
            .call(data)
            .addOnSuccessListener { result ->
                stopLoadingTimeout()
                val response = result.data as? Map<*, *>
                if (response?.get("success") == true) {
                    android.util.Log.d("AuthViewModel", "OTP Resent successfully")
                    _authState.value = AuthState.OtpSent("msg91_session_resend")
                } else {
                    val message = response?.get("message") as? String ?: "Failed to resend OTP"
                    _authState.value = AuthState.Error(message)
                }
            }
            .addOnFailureListener { e ->
                stopLoadingTimeout()
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to resend OTP")
            }
    }

    fun verifyOtp(otp: String) {
        if (otp.length != 6) {
            _authState.value = AuthState.Error(getApplication<android.app.Application>().getString(R.string.otp_6_digit_error))
            return
        }
        if (pendingPhoneNumber.isEmpty()) {
            _authState.value = AuthState.Error(getApplication<android.app.Application>().getString(R.string.session_expired_error))
            return
        }
        
        _authState.value = AuthState.Loading
        startLoadingTimeout()
        
        try {
            val data = hashMapOf(
                "phoneNumber" to pendingPhoneNumber,
                "otp" to otp
            )
            functions.getHttpsCallable("verifyOtp")
                .call(data)
                .addOnSuccessListener { result ->
                    val response = result.data as? Map<*, *>
                    if (response?.get("success") == true) {
                        val customToken = response["customToken"] as? String
                        if (customToken != null) {
                            signInWithCustomToken(customToken)
                        } else {
                            stopLoadingTimeout()
                            _authState.value = AuthState.Error("Verification successful but token missing")
                        }
                    } else {
                        stopLoadingTimeout()
                        val message = response?.get("message") as? String ?: "Invalid OTP"
                        _authState.value = AuthState.Error(message)
                    }
                }
                .addOnFailureListener { e ->
                    stopLoadingTimeout()
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Verification failed")
                }
        } catch (e: Exception) {
            stopLoadingTimeout()
            _authState.value = AuthState.Error(e.localizedMessage ?: "Invalid OTP attempt")
        }
    }

    private fun signInWithCustomToken(token: String) {
        auth.signInWithCustomToken(token)
            .addOnCompleteListener { task ->
                stopLoadingTimeout()
                if (task.isSuccessful) {
                    analytics.logEvent("otp_verify_success", null)
                    val userId = auth.currentUser?.uid
                    if (userId != null) startUserDocListener(userId)
                } else {
                    val bundle = Bundle()
                    bundle.putString("error_message", task.exception?.localizedMessage)
                    analytics.logEvent("otp_verify_failure", bundle)
                    _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Sign-in Failed")
                }
            }
    }

    fun saveUserName(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val phoneNumber = auth.currentUser?.phoneNumber?.removePrefix("+91") ?: ""
        val now = System.currentTimeMillis()
        
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val joinedAt = doc.getLong("joinedAt") ?: now
            val adminNumbers = listOf("9359599599", "8014311143")
            val isSuperAdmin = adminNumbers.contains(phoneNumber)
            var isAdmin = doc.getBoolean("isAdmin") ?: false
            if (isSuperAdmin) isAdmin = true

            val updates = hashMapOf<String, Any>(
                "name" to name,
                "onboardingComplete" to true
            )
            
            // Also save token here
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                updates["fcmToken"] = token
                db.collection("users").document(userId).update(updates)
                    .addOnSuccessListener {
                        analytics.logEvent("profile_onboarding_complete", null)
                        _authState.value = AuthState.Success(userId, name, phoneNumber, joinedAt, isAdmin, false)
                    }
                    .addOnFailureListener { e ->
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to save user info")
                    }
            }
        }
    }

    fun updateProfile(name: String, profileImage: Any?, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>("name" to name)
                
                if (profileImage != null) {
                    val url = uploadProfileImage(userId, profileImage)
                    updates["profilePic"] = url
                }

                db.collection("users").document(userId).update(updates).await()
                analytics.logEvent("profile_updated", null)
                
                val doc = db.collection("users").document(userId).get().await()
                val phoneNumber = doc.getString("phone") ?: ""
                val joinedAt = doc.getLong("joinedAt") ?: System.currentTimeMillis()
                val showMobile = doc.getBoolean("showMobileNumber") ?: false
                
                val adminNumbers = listOf("9359599599", "8014311143")
                var isAdmin = doc.getBoolean("isAdmin") ?: false
                if (adminNumbers.contains(phoneNumber)) isAdmin = true
                
                _authState.value = AuthState.Success(userId, name, phoneNumber, joinedAt, isAdmin, showMobile)
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    private suspend fun uploadProfileImage(userId: String, image: Any): String {
        val fileName = "profile_pics/$userId.jpg"
        val ref = storage.reference.child(fileName)
        
        when (image) {
            is Bitmap -> {
                val baos = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                val data = baos.toByteArray()
                ref.putBytes(data).await()
            }
            is String -> { // Local URI string
                ref.putFile(image.toUri()).await()
            }
            is Uri -> {
                ref.putFile(image).await()
            }
            else -> throw IllegalArgumentException("Unsupported image type")
        }
        
        return ref.downloadUrl.await().toString()
    }

    fun skipOnboarding() {
        val userId = auth.currentUser?.uid ?: return
        val phoneNumber = auth.currentUser?.phoneNumber?.removePrefix("+91") ?: ""
        
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val updates = mapOf("onboardingComplete" to true, "fcmToken" to token)
            db.collection("users").document(userId).update(updates)
                .addOnSuccessListener {
                    db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                        val currentName = doc.getString("name") ?: "User"
                        val joinedAt = doc.getLong("joinedAt") ?: System.currentTimeMillis()
                        val showMobile = doc.getBoolean("showMobileNumber") ?: false
                        
                        val adminNumbers = listOf("9359599599", "8014311143")
                        var isAdmin = doc.getBoolean("isAdmin") ?: false
                        if (adminNumbers.contains(phoneNumber)) isAdmin = true

                        _authState.value = AuthState.Success(userId, currentName, phoneNumber, joinedAt, isAdmin, showMobile)
                    }
                }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
