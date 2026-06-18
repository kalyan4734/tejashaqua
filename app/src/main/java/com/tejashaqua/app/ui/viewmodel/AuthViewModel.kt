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
    data class Success(val userId: String, val userName: String, val mobileNumber: String, val joinedAt: Long, val isAdmin: Boolean = false) : AuthState()
    data class RequireName(val phoneNumber: String, val isAdmin: Boolean = false) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: android.app.Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val analytics = FirebaseAnalytics.getInstance(application)
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var verificationId: String = ""
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var loadingTimeoutJob: Job? = null

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Loading
            checkUserExists()
        }
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

    fun sendOtp(phoneNumber: String, activity: Activity) {
        if (phoneNumber.length != 10) {
            _authState.value = AuthState.Error("Please enter a valid 10-digit phone number.")
            return
        }
        
        // If already loading, don't trigger another one
        if (_authState.value is AuthState.Loading) return

        _authState.value = AuthState.Loading
        startLoadingTimeout()
        
        // Clear previous verification data for a fresh attempt from login screen
        if (activity.localClassName.contains("MainActivity") && resendToken == null) {
             verificationId = ""
        }

        try {
            val bundle = Bundle()
            bundle.putString("phone_number", "+91$phoneNumber")
            analytics.logEvent("otp_request", bundle)

            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+91$phoneNumber")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        android.util.Log.d("AuthViewModel", "onVerificationCompleted")
                        stopLoadingTimeout()
                        signInWithPhoneAuthCredential(credential)
                    }

                    override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                        stopLoadingTimeout()
                        android.util.Log.e("AuthViewModel", "onVerificationFailed: ${e.message}", e)
                        
                        val errorBundle = Bundle()
                        errorBundle.putString(FirebaseAnalytics.Param.METHOD, "phone")
                        errorBundle.putString("error_message", e.localizedMessage)
                        analytics.logEvent("auth_failure", errorBundle)

                        val message = when(e) {
                            is FirebaseAuthInvalidCredentialsException -> "Invalid phone number."
                            is FirebaseAuthException -> {
                                if (e.errorCode == "ERROR_TOO_MANY_REQUESTS") "Too many requests. Please try again later."
                                else e.localizedMessage ?: "Verification Failed"
                            }
                            else -> e.localizedMessage ?: "Verification Failed"
                        }
                        _authState.value = AuthState.Error(message)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        android.util.Log.d("AuthViewModel", "onCodeSent: $verificationId")
                        stopLoadingTimeout()
                        this@AuthViewModel.verificationId = verificationId
                        this@AuthViewModel.resendToken = token
                        _authState.value = AuthState.OtpSent(verificationId)
                    }
                })
            
            resendToken?.let {
                builder.setForceResendingToken(it)
            }
                
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Exception) {
            stopLoadingTimeout()
            android.util.Log.e("AuthViewModel", "verifyPhoneNumber exception", e)
            _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to start verification")
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.length != 6) {
            _authState.value = AuthState.Error("Please enter a 6-digit OTP.")
            return
        }
        if (verificationId.isEmpty()) {
            _authState.value = AuthState.Error("Session expired. Please resend OTP.")
            return
        }
        
        _authState.value = AuthState.Loading
        startLoadingTimeout()
        
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            signInWithPhoneAuthCredential(credential)
        } catch (e: Exception) {
            stopLoadingTimeout()
            _authState.value = AuthState.Error(e.localizedMessage ?: "Invalid OTP attempt")
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                stopLoadingTimeout()
                if (task.isSuccessful) {
                    analytics.logEvent("otp_verify_success", null)
                    checkUserExists()
                } else {
                    val bundle = Bundle()
                    bundle.putString("error_message", task.exception?.localizedMessage)
                    analytics.logEvent("otp_verify_failure", bundle)
                    _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Sign-in Failed")
                }
            }
    }

    private fun checkUserExists() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _authState.value = AuthState.Error("Session lost. Please try again.")
            return
        }
        val phoneNumber = auth.currentUser?.phoneNumber?.removePrefix("+91") ?: ""
        
        _authState.value = AuthState.Loading
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val isAdmin = document.getBoolean("isAdmin") ?: false
                
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val joinedAt = document.getLong("joinedAt") ?: System.currentTimeMillis()
                    val onboardingComplete = document.getBoolean("onboardingComplete") ?: false
                    
                    if (onboardingComplete) {
                        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, null)
                        _authState.value = AuthState.Success(userId, name, phoneNumber, joinedAt, isAdmin)
                    } else {
                        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, null)
                        _authState.value = AuthState.RequireName(phoneNumber, isAdmin)
                    }
                } else {
                    val now = System.currentTimeMillis()
                    val dummyName = "User_${userId.takeLast(4)}"
                    val user = hashMapOf(
                        "uid" to userId,
                        "name" to dummyName,
                        "phone" to phoneNumber,
                        "joinedAt" to now,
                        "onboardingComplete" to false,
                        "isAdmin" to false
                    )
                    db.collection("users").document(userId).set(user)
                    _authState.value = AuthState.RequireName(phoneNumber, false)
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to check user info")
            }
    }

    fun saveUserName(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val phoneNumber = auth.currentUser?.phoneNumber?.removePrefix("+91") ?: ""
        val now = System.currentTimeMillis()
        
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val joinedAt = doc.getLong("joinedAt") ?: now
            val isAdmin = doc.getBoolean("isAdmin") ?: false
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "onboardingComplete" to true
            )
            
            db.collection("users").document(userId).update(updates)
                .addOnSuccessListener {
                    analytics.logEvent("profile_onboarding_complete", null)
                    _authState.value = AuthState.Success(userId, name, phoneNumber, joinedAt, isAdmin)
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to save user info")
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
                val isAdmin = doc.getBoolean("isAdmin") ?: false
                
                _authState.value = AuthState.Success(userId, name, phoneNumber, joinedAt, isAdmin)
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
        db.collection("users").document(userId).update("onboardingComplete", true)
            .addOnSuccessListener {
                db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                    val currentName = doc.getString("name") ?: "User"
                    val joinedAt = doc.getLong("joinedAt") ?: System.currentTimeMillis()
                    val isAdmin = doc.getBoolean("isAdmin") ?: false
                    _authState.value = AuthState.Success(userId, currentName, phoneNumber, joinedAt, isAdmin)
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
