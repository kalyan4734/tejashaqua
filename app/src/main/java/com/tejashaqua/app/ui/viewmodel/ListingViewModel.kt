package com.tejashaqua.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tejashaqua.app.R
import com.tejashaqua.app.utils.ImageUtils
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
    private val analytics = FirebaseAnalytics.getInstance(application)

    private val _postState = MutableStateFlow<PostState>(PostState.Idle)
    val postState: StateFlow<PostState> = _postState

    fun saveListing(data: Map<String, Any>, photos: List<Any>) {
        viewModelScope.launch {
            _postState.value = PostState.Loading
            try {
                val userId = data["userId"]?.toString() ?: ""
                val existingListingId = data["id"] as? String
                
                // Enforce 2-listing limit for new posts
                if (existingListingId == null && userId.isNotEmpty()) {
                    val userListings = db.collection("listings")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    
                    if (userListings.size() >= 2) {
                        val message = getApplication<Application>().getString(R.string.limit_reached_error)
                        _postState.value = PostState.Error(message)
                        return@launch
                    }
                }

                // Separate existing URLs from new content (Bitmaps or local URIs)
                val existingUrls = mutableListOf<String>()
                val toUpload = mutableListOf<Any>()
                
                for (photo in photos) {
                    if (photo is String && photo.startsWith("http")) {
                        existingUrls.add(photo)
                    } else {
                        toUpload.add(photo)
                    }
                }

                val uploadedUrls = uploadImages(toUpload)
                val finalUrls = existingUrls + uploadedUrls
                
                val listingId = existingListingId ?: db.collection("listings").document().id
                val title = data["title"]?.toString() ?: "Listing"
                val finalData = data.toMutableMap()
                finalData["id"] = listingId
                finalData["timestamp"] = System.currentTimeMillis()
                finalData["images"] = finalUrls

                db.collection("listings").document(listingId).set(finalData).await()
                
                val bundle = Bundle()
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, listingId)
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, title)
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, data["category"]?.toString() ?: "unknown")
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

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

    private suspend fun uploadImages(toUpload: List<Any>): List<String> {
        val urls = mutableListOf<String>()
        for (item in toUpload) {
            val fileName = "listings/${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(fileName)
            
            try {
                when (item) {
                    is Bitmap -> {
                        val baos = ByteArrayOutputStream()
                        item.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val data = baos.toByteArray()
                        ref.putBytes(data).await()
                    }
                    is String -> { // Local URI string
                        val uri = item.toUri()
                        val rotatedBitmap = ImageUtils.getCorrectlyOrientedBitmap(getApplication(), uri)
                        if (rotatedBitmap != null) {
                            val baos = ByteArrayOutputStream()
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                            val data = baos.toByteArray()
                            ref.putBytes(data).await()
                            rotatedBitmap.recycle()
                        } else {
                            ref.putFile(uri).await()
                        }
                    }
                    is Uri -> {
                        val rotatedBitmap = ImageUtils.getCorrectlyOrientedBitmap(getApplication(), item)
                        if (rotatedBitmap != null) {
                            val baos = ByteArrayOutputStream()
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                            val data = baos.toByteArray()
                            ref.putBytes(data).await()
                            rotatedBitmap.recycle()
                        } else {
                            ref.putFile(item).await()
                        }
                    }
                }
                val url = ref.downloadUrl.await().toString()
                urls.add(url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return urls
    }

    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("listings").document(listingId).get().await()
                if (doc.exists()) {
                    val images = doc.get("images") as? List<*>
                    images?.forEach { imageUrl ->
                        try {
                            storage.getReferenceFromUrl(imageUrl.toString()).delete().await()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    db.collection("listings").document(listingId).delete().await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
