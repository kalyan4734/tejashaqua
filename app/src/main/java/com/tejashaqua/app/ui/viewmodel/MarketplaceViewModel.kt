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

    private var currentPage = 0
    private var lastParams: FetchParams? = null
    
    private var myListingsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var savedItemsListener: com.google.firebase.firestore.ListenerRegistration? = null

    data class FetchParams(
        val lat: Double?,
        val lng: Double?,
        val locationName: String,
        val category: String
    )

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchText(query: String) {
        _searchText.value = query
    }

    fun loadListings(
        lat: Double?,
        lng: Double?,
        locationName: String,
        category: String,
        isFirstPage: Boolean = false
    ) {
        val currentParams = FetchParams(lat, lng, locationName, category)
        
        if (isFirstPage) {
            // Prevent redundant refreshes if params are identical and we already have data
            if (currentParams == lastParams && _listings.value.isNotEmpty()) {
                return
            }

            // Prevent overlapping loads for the same page
            if (_isLoading.value) return
            
            _isLoading.value = true
            _isLastPage.value = false
            _listings.value = emptyList() // Clear old listings immediately for new filter
            currentPage = 0
            lastParams = currentParams
        } else {
            if (_isPaginating.value || _isLastPage.value || _isLoading.value) return
            _isPaginating.value = true
        }

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
                val response = result.data as? Map<*, *>
                val newItems = (response?.get("listings") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()

                if (isFirstPage) {
                    _listings.value = newItems
                    currentPage = 1
                } else {
                    _listings.value = (_listings.value + newItems).distinctBy { it["id"] }
                    currentPage++
                }

                _isLastPage.value = response?.get("isLastPage") as? Boolean ?: true
                _isLoading.value = false
                _isPaginating.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
                _isPaginating.value = false
            }
    }

    fun startMyListingsListener(userId: String) {
        if (myListingsListener != null) return
        
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
                    }
                }
                _isLoadingMyListings.value = false
            }
    }

    fun startSavedItemsListener(userId: String) {
        if (savedItemsListener != null) return
        
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
                    }
                }
                _isLoadingSavedItems.value = false
            }
    }

    override fun onCleared() {
        super.onCleared()
        myListingsListener?.remove()
        savedItemsListener?.remove()
    }
}
