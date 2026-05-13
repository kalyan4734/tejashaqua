package com.tejashaqua.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tejashaqua.app.data.model.ListingCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ListingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _postState = MutableStateFlow<PostState>(PostState.Idle)
    val postState: StateFlow<PostState> = _postState

    fun saveListing(data: Map<String, Any>) {
        _postState.value = PostState.Loading
        val listingId = data["id"] as? String ?: db.collection("listings").document().id
        val finalData = data.toMutableMap()
        finalData["id"] = listingId
        finalData["timestamp"] = System.currentTimeMillis()

        db.collection("listings").document(listingId).set(finalData)
            .addOnSuccessListener {
                _postState.value = PostState.Success
            }
            .addOnFailureListener { e ->
                _postState.value = PostState.Error(e.localizedMessage ?: "Failed to post listing")
            }
    }

    fun deleteListing(listingId: String) {
        db.collection("listings").document(listingId).delete()
    }

    fun resetState() {
        _postState.value = PostState.Idle
    }

    sealed class PostState {
        object Idle : PostState()
        object Loading : PostState()
        object Success : PostState()
        data class Error(val message: String) : PostState()
    }
}
