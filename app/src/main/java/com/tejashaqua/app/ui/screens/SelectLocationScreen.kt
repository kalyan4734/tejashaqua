package com.tejashaqua.app.ui.screens

import android.app.Activity
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.foundation.text.KeyboardOptions
import com.tejashaqua.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.utils.LocaleHelper
import com.tejashaqua.app.utils.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectLocationScreen(
    onBackClick: () -> Unit,
    onLocationConfirm: (String, String, LatLng?) -> Unit,
    locationViewModel: LocationSearchViewModel = viewModel()
) {
    var searchText by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedLocation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    
    val searchResults by locationViewModel.searchResults.collectAsState()
    val deviceLatLng by locationViewModel.currentLatLng.collectAsState()
    val currentLocationName by locationViewModel.currentLocationName.collectAsState()
    val currentSubLocation by locationViewModel.currentSubLocation.collectAsState()
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val isGpsEnabled by locationViewModel.isGpsEnabled.collectAsState()
    val isFetchingLocation by locationViewModel.isFetchingLocation.collectAsState()

    val gpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        coroutineScope.launch {
            delay(1000) // Give system time to update
            locationViewModel.refreshGpsStatus()
            if (result.resultCode == Activity.RESULT_OK) {
                locationViewModel.fetchCurrentLocation(force = true)
            }
        }
    }

    // Refresh status when returning to screen
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                locationViewModel.refreshGpsStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    // Map State
    val defaultLocation = LatLng(17.0005, 81.7729) // Rajahmundry
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    LaunchedEffect(Unit) {
        locationViewModel.fetchCurrentLocation()
    }

    LaunchedEffect(deviceLatLng) {
        deviceLatLng?.let { latLng ->
            if (selectedLatLng == null) {
                selectedLatLng = latLng
                selectedLocation = currentLocationName to currentSubLocation
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
            }
        }
    }

    fun updateLocationFromLatLng(latLng: LatLng) {
        selectedLatLng = latLng
        coroutineScope.launch {
            try {
                val lang = LocaleHelper.getSelectedLanguage(context) ?: "en"
                val locale = Locale.forLanguageTag(lang)
                val geocoder = Geocoder(context, locale)
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                }
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val loc = address.locality ?: address.subAdminArea ?: context.getString(R.string.unknown_location)
                    val sub = address.getAddressLine(0) ?: ""
                    selectedLocation = loc to sub
                }
            } catch (e: Exception) {
                selectedLocation = context.getString(R.string.selected_point) to "${latLng.latitude}, ${latLng.longitude}"
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AquaBlue)) {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.select_location_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            onBackClick()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
                )
                
                if (selectedTabIndex == 0) {
                    TextField(
                        value = searchText,
                        onValueChange = { 
                            searchText = it
                            locationViewModel.searchLocation(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .heightIn(min = 54.dp),
                        placeholder = { Text(stringResource(R.string.search_location_placeholder), fontSize = 16.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = keyboardOptions
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { 
                        keyboardController?.hide()
                        selectedLocation?.let { (loc, sub) ->
                            onLocationConfirm(loc, sub, selectedLatLng)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
                    enabled = selectedLocation != null
                ) {
                    Text(stringResource(R.string.confirm_location), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = AquaBlue,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .height(3.dp)
                                .background(color = AquaBlue)
                        )
                    }
                },
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { 
                        keyboardController?.hide()
                        selectedTabIndex = 0 
                    },
                    text = { Text(stringResource(R.string.search_tab), fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { 
                        keyboardController?.hide()
                        selectedTabIndex = 1 
                    },
                    text = { Text(stringResource(R.string.map_tab), fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0) {
                if (!isGpsEnabled && searchText.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(AquaBlue.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AquaBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.enable_gps_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = stringResource(R.string.enable_gps_desc),
                                    fontSize = 14.sp,
                                    color = GrayText
                                )
                            }
                            TextButton(
                                onClick = {
                                    val activity = context.findActivity()
                                    if (activity != null) {
                                        // Force refresh status first
                                        locationViewModel.refreshGpsStatus()
                                        
                                        com.tejashaqua.app.utils.LocationUtils.checkLocationSettings(
                                            activity,
                                            onEnabled = { 
                                                locationViewModel.refreshGpsStatus()
                                                locationViewModel.fetchCurrentLocation(force = true) 
                                            },
                                            onError = { exception ->
                                                try {
                                                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                                                    gpsLauncher.launch(intentSenderRequest)
                                                } catch (e: Exception) {
                                                    // Fallback to settings
                                                    val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            },
                                            onFailure = {
                                                // If dialog fails to even check, open settings
                                                try {
                                                    val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("SelectLocation", "Error opening settings", e)
                                                }
                                            }
                                        )
                                    } else {
                                        // Fallback if activity is null
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
{
                                Text(
                                    text = stringResource(R.string.turn_on_location),
                                    color = AquaBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (isFetchingLocation && searchResults.isEmpty()) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = AquaBlue
                            )
                        }
                    }

                    items(searchResults) { prediction ->
                        val primaryText = prediction.getPrimaryText(null).toString()
                        val secondaryText = prediction.getSecondaryText(null).toString()
                        val isSelected = selectedLocation?.first == primaryText
                        
                        LocationSearchItem(
                            location = primaryText,
                            subLocation = secondaryText,
                            isSelected = isSelected,
                            onClick = {
                                selectedLocation = primaryText to secondaryText
                                keyboardController?.hide()
                                // Fetch LatLng for the selected place
                                locationViewModel.getPlaceLatLng(prediction.placeId) { latLng ->
                                    selectedLatLng = latLng
                                    // Optionally move camera to the selected location if they switch to map tab
                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                                }
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    val hasLocationPermission = remember {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                        onMapClick = { latLng ->
                            updateLocationFromLatLng(latLng)
                        }
                    ) {
                        selectedLatLng?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = selectedLocation?.first ?: stringResource(R.string.selected_location),
                                snippet = selectedLocation?.second ?: ""
                            )
                        }
                    }

                    if (!hasLocationPermission) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.location_permission_denied),
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // Floating card showing selected address from map
                    if (selectedLocation != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AquaBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = selectedLocation!!.first, fontWeight = FontWeight.Bold)
                                    Text(text = selectedLocation!!.second, fontSize = 14.sp, color = GrayText, maxLines = 1)
                                }
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null, 
                                    tint = GrayText.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationSearchItem(
    location: String,
    subLocation: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isSelected) Color(0xFFF0F7FF) else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn, 
                contentDescription = null, 
                tint = if (isSelected) AquaBlue else Color(0xFF00639B), 
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = location, 
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 16.sp, 
                    color = Color.Black
                )
                Text(
                    text = subLocation, 
                    fontSize = 14.sp,
                    color = GrayText
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}
