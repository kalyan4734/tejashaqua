package com.tejashaqua.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tejashaqua.app.utils.NotificationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

class ListingViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _postState = MutableStateFlow<PostState>(PostState.Idle)
    val postState: StateFlow<PostState> = _postState

    fun saveListing(data: Map<String, Any>, newBitmaps: List<Bitmap>, existingUrls: List<String>) {
        viewModelScope.launch {
            _postState.value = PostState.Loading
            try {
                val uploadedUrls = uploadImages(newBitmaps)
                val finalUrls = existingUrls + uploadedUrls
                
                val listingId = data["id"] as? String ?: db.collection("listings").document().id
                val title = data["title"]?.toString() ?: "Listing"
                val finalData = data.toMutableMap()
                finalData["id"] = listingId
                finalData["timestamp"] = System.currentTimeMillis()
                finalData["images"] = finalUrls

                db.collection("listings").document(listingId).set(finalData).await()
                
                // Show local notification for feedback
                NotificationUtils.showLocalNotification(
                    getApplication(),
                    "Ad Posted Successfully!",
                    "Your listing '$title' is now live."
                )

                _postState.value = PostState.Success
            } catch (e: Exception) {
                _postState.value = PostState.Error(e.localizedMessage ?: "Failed to post listing")
            }
        }
    }

    private suspend fun uploadImages(bitmaps: List<Bitmap>): List<String> {
        val urls = mutableListOf<String>()
        for (bitmap in bitmaps) {
            val fileName = "listings/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            val data = baos.toByteArray()
            
            ref.putBytes(data).await()
            val url = ref.downloadUrl.await().toString()
            urls.add(url)
        }
        return urls
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
