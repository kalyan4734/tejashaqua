package com.tejashaqua.app.ui.viewmodel

import android.app.Activity
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var verificationId: String = ""

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

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phoneNumber")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Verification Failed")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@AuthViewModel.verificationId = verificationId
                    _authState.value = AuthState.OtpSent(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(otp: String) {
        _authState.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserExists()
                } else {
                    _authState.value = AuthState.Error(task.exception?.localizedMessage ?: "Sign-in Failed")
                }
            }
    }

    private fun checkUserExists() {
        val userId = auth.currentUser?.uid ?: return
        val phoneNumber = auth.currentUser?.phoneNumber?.removePrefix("+91") ?: ""
        
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val isAdmin = document.getBoolean("isAdmin") ?: false
                
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val joinedAt = document.getLong("joinedAt") ?: System.currentTimeMillis()
                    val onboardingComplete = document.getBoolean("onboardingComplete") ?: false
                    
                    if (onboardingComplete) {
                        _authState.value = AuthState.Success(userId, name, phoneNumber, joinedAt, isAdmin)
                    } else {
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
                        "isAdmin" to false // Default to false for new users
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
                    _authState.value = AuthState.Success(userId, name, phoneNumber, joinedAt, isAdmin)
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to save user info")
                }
        }
    }

    fun updateProfile(name: String, profileBitmap: Bitmap?, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>("name" to name)
                
                if (profileBitmap != null) {
                    val url = uploadProfileImage(userId, profileBitmap)
                    updates["profilePic"] = url
                }

                db.collection("users").document(userId).update(updates).await()
                
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

    private suspend fun uploadProfileImage(userId: String, bitmap: Bitmap): String {
        val fileName = "profile_pics/$userId.jpg"
        val ref = storage.reference.child(fileName)
        
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val data = baos.toByteArray()
        
        ref.putBytes(data).await()
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
