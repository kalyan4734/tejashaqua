package com.tejashaqua.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.tejashaqua.app.R
import com.tejashaqua.app.utils.LocaleHelper
import java.util.Locale

class LocationSearchViewModel(application: Application) : AndroidViewModel(application) {
    private fun getPlacesClient() = if (Places.isInitialized()) Places.createClient(getApplication()) else null
    private var token = AutocompleteSessionToken.newInstance()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _searchResults = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val searchResults: StateFlow<List<AutocompletePrediction>> = _searchResults

    private val _currentLocationName = MutableStateFlow(application.getString(R.string.fetching_location))
    val currentLocationName: StateFlow<String> = _currentLocationName

    private val _currentSubLocation = MutableStateFlow("")
    val currentSubLocation: StateFlow<String> = _currentSubLocation

    private val _currentLatLng = MutableStateFlow<LatLng?>(null)
    val currentLatLng: StateFlow<LatLng?> = _currentLatLng

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isGpsEnabled = MutableStateFlow(true)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled

    private val _isFetchingLocation = MutableStateFlow(false)
    val isFetchingLocation: StateFlow<Boolean> = _isFetchingLocation

    private val _isFetchingPlaceDetails = MutableStateFlow(false)
    val isFetchingPlaceDetails: StateFlow<Boolean> = _isFetchingPlaceDetails

    var isManualSelection = false
        private set

    fun onPermissionDenied() {
        if (isManualSelection) return
        _currentLocationName.value = getApplication<Application>().getString(R.string.location_permission_denied)
    }

    fun setManualLocation(name: String, sub: String, latLng: LatLng?) {
        isManualSelection = true
        _currentLocationName.value = name
        _currentSubLocation.value = sub
        _currentLatLng.value = latLng
    }

    fun updateLocationForLanguage(lang: String) {
        val latLng = _currentLatLng.value
        if (latLng != null) {
            updateLocationData(latLng.latitude, latLng.longitude, lang)
        }
    }

    fun refreshGpsStatus() {
        val locationManager = getApplication<Application>().getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val isLocationEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        }
        if (_isGpsEnabled.value != isLocationEnabled) {
            _isGpsEnabled.value = isLocationEnabled
            if (isLocationEnabled) {
                fetchCurrentLocation(force = true)
            }
        }
    }

    fun fetchCurrentLocation(force: Boolean = false) {
        if (force) isManualSelection = false

        Log.d("LocationVM", "fetchCurrentLocation called")
        
        // Use current language context for localized strings
        val context = LocaleHelper.wrapContext(getApplication())
        
        if (!isManualSelection) {
            _currentLocationName.value = context.getString(R.string.fetching_location)
            _isFetchingLocation.value = true
        }

        // Check if GPS is enabled
        val locationManager = getApplication<Application>().getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val isLocationEnabled = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        }
        
        _isGpsEnabled.value = isLocationEnabled

        if (!isLocationEnabled) {
            Log.d("LocationVM", "Location services are disabled")
            if (!isManualSelection) {
                _currentLocationName.value = context.getString(R.string.enable_gps_message)
                _isFetchingLocation.value = false
            }
            return
        }

        try {
            val priority = if (ContextCompat.checkSelfPermission(
                    getApplication(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(priority, cts.token)
                .addOnSuccessListener { currentLoc ->
                    Log.d("LocationVM", "getCurrentLocation success: $currentLoc")
                    _isFetchingLocation.value = false
                    if (currentLoc != null) {
                        if (!isManualSelection) {
                            _currentLatLng.value = LatLng(currentLoc.latitude, currentLoc.longitude)
                            updateLocationData(currentLoc.latitude, currentLoc.longitude)
                        }
                    } else {
                        Log.d("LocationVM", "getCurrentLocation was null, trying lastLocation")
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                if (!isManualSelection) {
                                    _currentLatLng.value = LatLng(location.latitude, location.longitude)
                                    updateLocationData(location.latitude, location.longitude)
                                }
                            } else if (!isManualSelection) {
                                _currentLocationName.value = context.getString(R.string.location_not_found)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("LocationVM", "getCurrentLocation failure", e)
                    _isFetchingLocation.value = false
                    if (!isManualSelection) {
                        _currentLocationName.value = context.getString(R.string.failed_get_location)
                    }
                }
        } catch (e: SecurityException) {
            Log.e("LocationVM", "SecurityException: permission denied", e)
            _isFetchingLocation.value = false
            if (!isManualSelection) {
                _currentLocationName.value = context.getString(R.string.location_permission_denied)
            }
        }
    }

    private fun updateLocationData(latitude: Double, longitude: Double, langOverride: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val lang = langOverride ?: LocaleHelper.getSelectedLanguage(getApplication()) ?: "en"
            val locale = Locale.forLanguageTag(lang)
            val geocoder = Geocoder(getApplication(), locale)
            val wrappedContext = LocaleHelper.wrapContext(getApplication(), lang)
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    
                    // Localize locality - Prioritize specific village/area over larger city
                    val locality = address.subLocality ?: address.locality ?: address.subAdminArea ?: wrappedContext.getString(R.string.unknown_location)
                    _currentLocationName.value = locality
                    
                    // Construct a localized sub-location if possible
                    val district = address.locality ?: ""
                    val adminArea = address.adminArea ?: ""
                    val displaySub = if (address.subLocality != null && district.isNotEmpty()) "$district, $adminArea" else if (adminArea.isNotEmpty()) adminArea else address.getAddressLine(0) ?: ""
                    
                    _currentSubLocation.value = displaySub
                    _currentLatLng.value = LatLng(latitude, longitude)
                } else {
                    _currentLocationName.value = wrappedContext.getString(R.string.unknown_location)
                    _currentSubLocation.value = "$latitude, $longitude"
                }
            } catch (e: Exception) {
                Log.e("LocationVM", "Geocoder error", e)
                _currentLocationName.value = wrappedContext.getString(R.string.unknown_location)
                _currentSubLocation.value = "$latitude, $longitude"
            }
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _error.value = null
            return
        }

        val client = getPlacesClient()
        if (client == null) {
            _error.value = "Places SDK not initialized"
            return
        }

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        client.findAutocompletePredictions(request)
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
        val client = getPlacesClient()
        if (client == null) return

        _isFetchingPlaceDetails.value = true
        val placeFields = listOf(Place.Field.LAT_LNG)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                _isFetchingPlaceDetails.value = false
                response.place.latLng?.let { latLng ->
                    val modelLatLng = LatLng(latLng.latitude, latLng.longitude)
                    callback(modelLatLng)
                }
            }
            .addOnFailureListener { exception ->
                _isFetchingPlaceDetails.value = false
                _error.value = exception.message
            }
    }
}
