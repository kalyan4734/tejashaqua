package com.tejashaqua.app.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OtpSent(val verificationId: String) : AuthState()
    data class Success(val userName: String) : AuthState()
    object RequireName : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var verificationId: String = ""

    init {
        // Check for existing session on initialization
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        if (auth.currentUser != null) {
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
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")
                    val onboardingComplete = document.getBoolean("onboardingComplete") ?: false
                    
                    if (name != null && onboardingComplete) {
                        _authState.value = AuthState.Success(name)
                    } else {
                        _authState.value = AuthState.RequireName
                    }
                } else {
                    val dummyName = "User_${userId.takeLast(4)}"
                    val user = hashMapOf(
                        "uid" to userId,
                        "name" to dummyName,
                        "phone" to (auth.currentUser?.phoneNumber ?: ""),
                        "onboardingComplete" to false
                    )
                    db.collection("users").document(userId).set(user)
                    _authState.value = AuthState.RequireName
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to check user info")
            }
    }

    fun saveUserName(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>(
            "name" to name,
            "onboardingComplete" to true
        )

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener {
                _authState.value = AuthState.Success(name)
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to save user info")
            }
    }

    fun skipOnboarding() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).update("onboardingComplete", true)
            .addOnSuccessListener {
                db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                    val currentName = doc.getString("name") ?: "User"
                    _authState.value = AuthState.Success(currentName)
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
