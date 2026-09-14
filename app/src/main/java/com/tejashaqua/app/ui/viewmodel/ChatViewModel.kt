package com.tejashaqua.app.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.tejashaqua.app.ui.screens.ChatListItemData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    
    private val _chats = MutableStateFlow<List<ChatListItemData>>(emptyList())
    val chats: StateFlow<List<ChatListItemData>> = _chats

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val listingStatusMap = mutableStateMapOf<String, Boolean>()
    
    private var chatsListener: ListenerRegistration? = null
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    fun startChatsListener(currentUserId: String) {
        if (currentUserId.isEmpty() || chatsListener != null) return

        _isLoading.value = true
        chatsListener = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null || snapshot == null) return@addSnapshotListener

                val chatList = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val sellerId = data["sellerId"]?.toString() ?: ""
                    val buyerId = data["buyerId"]?.toString() ?: ""
                    val isBuying = if (sellerId.isNotEmpty()) sellerId != currentUserId else buyerId == currentUserId

                    val unreadCounts = data["unreadCounts"] as? Map<*, *>
                    val unreadCount = (unreadCounts?.get(currentUserId) as? Number)?.toInt()
                        ?: (data["unreadCounts.$currentUserId"] as? Number)?.toInt() ?: 0

                    val lid = data["listingId"]?.toString() ?: ""

                    ChatListItemData(
                        chatId = doc.id,
                        name = if (isBuying) data["sellerName"]?.toString() ?: "Seller" 
                               else data["buyerName"]?.toString() ?: "Buyer",
                        otherUserId = if (isBuying) data["sellerId"]?.toString() ?: "" 
                                     else data["buyerId"]?.toString() ?: "",
                        type = if (isBuying) "Buying" else "Selling",
                        listingId = lid,
                        listingInfo = data["listingTitle"]?.toString() ?: "Listing",
                        lastMessage = data["lastMessage"]?.toString() ?: "",
                        time = when (val ts = data["lastMessageTimestamp"]) {
                            is com.google.firebase.Timestamp -> ts.toDate().time
                            is Number -> ts.toLong()
                            else -> 0L
                        },
                        unreadCount = unreadCount,
                        listingImage = data["listingImage"]?.toString(),
                        fullData = data + mapOf("id" to lid)
                    )
                }.sortedByDescending { it.time }
                
                _chats.value = chatList
                fetchListingStatuses(chatList)
            }
    }

    private fun fetchListingStatuses(chats: List<ChatListItemData>) {
        val uniqueListingIds = chats.map { it.listingId }
            .filter { it.isNotEmpty() && !listingStatusMap.containsKey(it) }
            .distinct()

        if (uniqueListingIds.isNotEmpty()) {
            uniqueListingIds.chunked(10).forEach { chunk ->
                db.collection("listings")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val foundIds = snapshot.documents.map { it.id }.toSet()
                        chunk.forEach { id ->
                            listingStatusMap[id] = foundIds.contains(id)
                        }
                    }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
        activeListeners.values.forEach { it.remove() }
    }
}
