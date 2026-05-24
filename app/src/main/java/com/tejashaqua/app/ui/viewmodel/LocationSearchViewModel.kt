package com.tejashaqua.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.model.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.tejashaqua.app.R
import com.tejashaqua.app.utils.LocaleHelper
import java.util.Locale

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val placesClient = if (Places.isInitialized()) Places.createClient(application) else null
    private var token = AutocompleteSessionToken.newInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _searchResults = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val searchResults: StateFlow<List<AutocompletePrediction>> = _searchResults

    private val _currentLocationName = MutableStateFlow("")
    val currentLocationName: StateFlow<String> = _currentLocationName

    private val _currentSubLocation = MutableStateFlow("")
    val currentSubLocation: StateFlow<String> = _currentSubLocation

    private val _currentLatLng = MutableStateFlow<LatLng?>(null)
    val currentLatLng: StateFlow<LatLng?> = _currentLatLng

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchCurrentLocation() {
        Log.d("LocationVM", "fetchCurrentLocation called")
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { currentLoc ->
                    Log.d("LocationVM", "getCurrentLocation success: $currentLoc")
                    if (currentLoc != null) {
                        updateLocationData(currentLoc.latitude, currentLoc.longitude)
                    } else {
                        Log.d("LocationVM", "getCurrentLocation was null, trying lastLocation")
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                updateLocationData(location.latitude, location.longitude)
                            } else {
                                _currentLocationName.value = getApplication<Application>().getString(R.string.location_not_found)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LocationVM", "getCurrentLocation failure", e)
                    _currentLocationName.value = getApplication<Application>().getString(R.string.failed_get_location)
                }
        } catch (e: SecurityException) {
            Log.e("LocationVM", "SecurityException: permission denied", e)
            _currentLocationName.value = "Permission denied"
        }
    }

    private fun updateLocationData(latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val lang = LocaleHelper.getSelectedLanguage(getApplication()) ?: "en"
            val locale = Locale.forLanguageTag(lang)
            val geocoder = Geocoder(getApplication(), locale)
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    _currentLocationName.value = address.locality ?: address.subAdminArea ?: getApplication<Application>().getString(R.string.unknown_location)
                    _currentSubLocation.value = address.getAddressLine(0) ?: ""
                    _currentLatLng.value = LatLng(latitude, longitude)
                }
            } catch (e: Exception) {
                _currentLocationName.value = getApplication<Application>().getString(R.string.unknown_location)
            }
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

    fun getPlaceLatLng(placeId: String, callback: (LatLng) -> Unit) {
        if (placesClient == null) return

        val placeFields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                response.place.latLng?.let { latLng ->
                    val modelLatLng = LatLng(latLng.latitude, latLng.longitude)
                    callback(modelLatLng)
                }
            }
            .addOnFailureListener { exception ->
                _error.value = exception.message
            }
    }
}
