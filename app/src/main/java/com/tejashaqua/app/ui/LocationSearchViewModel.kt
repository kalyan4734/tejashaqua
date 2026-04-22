package com.tejashaqua.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.location.Geocoder
import java.util.Locale

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val placesClient = if (Places.isInitialized()) Places.createClient(application) else null
    private var token = AutocompleteSessionToken.newInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _searchResults = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val searchResults: StateFlow<List<AutocompletePrediction>> = _searchResults

    private val _currentLocationName = MutableStateFlow("Fetching location...")
    val currentLocationName: StateFlow<String> = _currentLocationName

    private val _currentSubLocation = MutableStateFlow("")
    val currentSubLocation: StateFlow<String> = _currentSubLocation

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(getApplication(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        _currentLocationName.value = address.locality ?: address.subAdminArea ?: "Unknown Location"
                        _currentSubLocation.value = address.getAddressLine(0) ?: ""
                    }
                } else {
                    _currentLocationName.value = "Location not found"
                }
            }.addOnFailureListener {
                _currentLocationName.value = "Failed to get location"
            }
        } catch (e: SecurityException) {
            _currentLocationName.value = "Permission denied"
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _error.value = null
            return
        }

        if (placesClient == null) {
            _error.value = "Places SDK not initialized"
            return
        }

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                _searchResults.value = response.autocompletePredictions
                _error.value = null
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
                _searchResults.value = emptyList()
            }
    }
}
