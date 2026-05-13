package com.tejashaqua.app.ui.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.tejashaqua.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val placesClient = if (Places.isInitialized()) Places.createClient(application) else null
    private var token = AutocompleteSessionToken.newInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _searchResults = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val searchResults: StateFlow<List<AutocompletePrediction>> = _searchResults

    private val _currentLocationName = MutableStateFlow(application.getString(R.string.fetching_location))
    val currentLocationName: StateFlow<String> = _currentLocationName

    private val _currentSubLocation = MutableStateFlow("")
    val currentSubLocation: StateFlow<String> = _currentSubLocation

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchCurrentLocation() {
        _currentLocationName.value = getApplication<Application>().getString(R.string.fetching_location)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    processLocation(location.latitude, location.longitude)
                } else {
                    // If last location is null, request current location
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { currentLoc ->
                            if (currentLoc != null) {
                                processLocation(currentLoc.latitude, currentLoc.longitude)
                            } else {
                                _currentLocationName.value = getApplication<Application>().getString(R.string.location_not_found)
                            }
                        }
                        .addOnFailureListener {
                            _currentLocationName.value = getApplication<Application>().getString(R.string.failed_get_location)
                        }
                }
            }.addOnFailureListener {
                _currentLocationName.value = getApplication<Application>().getString(R.string.failed_get_location)
            }
        } catch (e: SecurityException) {
            _currentLocationName.value = "Permission denied"
        }
    }

    private fun processLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                _currentLocationName.value = address.locality ?: address.subAdminArea ?: "Unknown Location"
                _currentSubLocation.value = address.getAddressLine(0) ?: ""
            } else {
                _currentLocationName.value = "Unknown Location"
            }
        } catch (e: Exception) {
            _currentLocationName.value = "Unknown Location"
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

    fun getPlaceLatLng(placeId: String, onSuccess: (LatLng) -> Unit) {
        if (placesClient == null) return

        val placeFields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                response.place.latLng?.let { onSuccess(it) }
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
            }
    }
}
