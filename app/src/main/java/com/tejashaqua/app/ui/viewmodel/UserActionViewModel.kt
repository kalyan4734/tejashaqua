package com.tejashaqua.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserActionViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun blockUser(blockedUserId: String, onSuccess: () -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Add to current user's blocked list
        db.collection("users").document(currentUserId)
            .update("blockedUsers", FieldValue.arrayUnion(blockedUserId))
            .addOnSuccessListener { onSuccess() }
    }

    fun reportListing(listingId: String, reporterId: String, reason: String, onSuccess: () -> Unit) {
        val report = hashMapOf(
            "listingId" to listingId,
            "reporterId" to reporterId,
            "reason" to reason,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "pending"
        )

        db.collection("reports").add(report)
            .addOnSuccessListener { onSuccess() }
    }

    fun reportUser(reportedUserId: String, reporterId: String, reason: String, onSuccess: () -> Unit) {
        val report = hashMapOf(
            "reportedUserId" to reportedUserId,
            "reporterId" to reporterId,
            "reason" to reason,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "pending"
        )

        db.collection("user_reports").add(report)
            .addOnSuccessListener { onSuccess() }
    }
}
