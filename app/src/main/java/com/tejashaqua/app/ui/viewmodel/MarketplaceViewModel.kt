package com.tejashaqua.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {
    private val functions = FirebaseFunctions.getInstance("asia-south1")
    
    private val _listings = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val listings: StateFlow<List<Map<String, Any>>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating: StateFlow<Boolean> = _isPaginating

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage

    private val _loadingCategory = MutableStateFlow<String?>(null)
    val loadingCategory: StateFlow<String?> = _loadingCategory

    // Filter State
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    // My Listings State
    private val _myListings = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val myListings: StateFlow<List<Map<String, Any>>> = _myListings
    
    private val _isLoadingMyListings = MutableStateFlow(false)
    val isLoadingMyListings: StateFlow<Boolean> = _isLoadingMyListings

    // Saved Items State
    private val _savedItems = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val savedItems: StateFlow<List<Map<String, Any>>> = _savedItems

    private val _isLoadingSavedItems = MutableStateFlow(false)
    val isLoadingSavedItems: StateFlow<Boolean> = _isLoadingSavedItems

    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers

    private var currentPage = 0
    private var lastParams: FetchParams? = null
    private var latestRequestId = 0L
    
    private var myListingsUserId: String? = null
    private var myListingsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var savedItemsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userMetadataListener: com.google.firebase.firestore.ListenerRegistration? = null

    data class FetchParams(
        val lat: Double?,
        val lng: Double?,
        val locationName: String,
        val category: String
    )

    fun startUserMetadataListener(userId: String) {
        if (userId.isEmpty() || userMetadataListener != null) return
        
        userMetadataListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val blocked = snapshot.get("blockedUsers") as? List<*>
                    _blockedUsers.value = blocked?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                }
            }
    }

    fun setSearchText(query: String) {
        _searchText.value = query
    }

    fun setSelectedCategory(category: String) {
        if (_selectedCategory.value != category) {
            _selectedCategory.value = category
            // Clear listings immediately when category changes to avoid showing stale 
            // data from the previous category while the new one is loading.
            _listings.value = emptyList()
            _isLastPage.value = false
            currentPage = 0
            _loadingCategory.value = category
        }
    }

    fun loadListings(
        lat: Double?,
        lng: Double?,
        locationName: String,
        category: String,
        isFirstPage: Boolean = false,
        forceRefresh: Boolean = false
    ) {
        val currentParams = FetchParams(lat, lng, locationName, category)
        
        if (isFirstPage) {
            // Prevent redundant refreshes if params are identical and we already have data
            val isSameLocation = if (lat != null && lng != null && lastParams?.lat != null && lastParams?.lng != null) {
                val distance = android.location.Location("").apply {
                    latitude = lat
                    longitude = lng
                }.distanceTo(android.location.Location("").apply {
                    latitude = lastParams?.lat!!
                    longitude = lastParams?.lng!!
                })
                distance < 500 
            } else {
                lat == lastParams?.lat && lng == lastParams?.lng
            }

            val isSameCategory = category == lastParams?.category
            val isSameName = if (isSameLocation && lat != null) true else locationName.trim().lowercase() == lastParams?.locationName?.trim()?.lowercase()

            // If we are already loading something for the SAME params, skip.
            if (!forceRefresh && _isLoading.value && isSameLocation && isSameCategory && isSameName) return

            // If not currently loading, but params are identical and we have data, skip.
            if (!forceRefresh && !_isLoading.value && isSameLocation && isSameCategory && isSameName && _listings.value.isNotEmpty()) {
                return
            }

            _isLoading.value = true
            _isLastPage.value = false
            
            // Ensure UI shows loading for this specific category
            _loadingCategory.value = category
            
            if (forceRefresh || category != lastParams?.category) {
                _listings.value = emptyList()
            }

            currentPage = 0
            lastParams = currentParams
        } else {
            if (_isPaginating.value || _isLastPage.value || _isLoading.value) return
            _isPaginating.value = true
            lastParams = currentParams
        }

        val requestId = System.currentTimeMillis()
        latestRequestId = requestId

        val data = hashMapOf(
            "lat" to lat,
            "lng" to lng,
            "locationName" to locationName,
            "category" to category,
            "page" to if (isFirstPage) 0 else currentPage,
            "pageSize" to 10
        )

        functions.getHttpsCallable("getListingsByLocation").call(data)
            .addOnSuccessListener { result ->
                if (latestRequestId == requestId) {
                    val response = result.data as? Map<*, *>
                    val newItems = (response?.get("listings") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()

                    if (isFirstPage) {
                        _listings.value = newItems
                        currentPage = 1
                        _loadingCategory.value = null
                    } else {
                        // Merge and ensure no duplicates
                        val currentList = _listings.value
                        _listings.value = (currentList + newItems).distinctBy { it["id"] }
                        currentPage++
                    }

                    _isLastPage.value = response?.get("isLastPage") as? Boolean ?: true
                    _isLoading.value = false
                    _isPaginating.value = false
                }
            }
            .addOnFailureListener {
                if (latestRequestId == requestId) {
                    _isLoading.value = false
                    _isPaginating.value = false
                    _loadingCategory.value = null
                }
            }
    }

    fun refreshCurrent() {
        val params = lastParams ?: return
        loadListings(
            lat = params.lat,
            lng = params.lng,
            locationName = params.locationName,
            category = params.category,
            isFirstPage = true,
            forceRefresh = true
        )
    }

    fun startMyListingsListener(userId: String) {
        if (myListingsListener != null && myListingsUserId == userId) return
        
        myListingsListener?.remove()
        myListingsUserId = userId
        _isLoadingMyListings.value = true
        myListingsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("listings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _myListings.value = snapshot.documents.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
                    }.sortedByDescending { (it["timestamp"] as? Number)?.toLong() ?: 0L }
                }
                _isLoadingMyListings.value = false
            }
    }

    private var savedItemsUserId: String? = null
    fun startSavedItemsListener(userId: String) {
        if (savedItemsListener != null && savedItemsUserId == userId) return
        
        savedItemsListener?.remove()
        savedItemsUserId = userId
        _isLoadingSavedItems.value = true
        savedItemsListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("favorites")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _savedItems.value = snapshot.documents.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
                    }.sortedByDescending { (it["timestamp"] as? Number)?.toLong() ?: 0L }
                }
                _isLoadingSavedItems.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        myListingsListener?.remove()
        savedItemsListener?.remove()
        userMetadataListener?.remove()
    }
}
