package com.tejashaqua.app.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Map State
    val rajahmundry = LatLng(17.0005, 81.7729)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(rajahmundry, 12f)
    }

    fun updateLocationFromLatLng(latLng: LatLng) {
        selectedLatLng = latLng
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val loc = address.locality ?: address.subAdminArea ?: "Unknown Location"
                val sub = address.getAddressLine(0) ?: ""
                selectedLocation = loc to sub
            }
        } catch (e: Exception) {
            selectedLocation = "Selected Point" to "${latLng.latitude}, ${latLng.longitude}"
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AquaBlue)) {
                CenterAlignedTopAppBar(
                    title = { Text("Select Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                            .height(54.dp),
                        placeholder = { Text("Search city, area, or district...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
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
                    Text("Confirm Location", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Search", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Map", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                // In a real app, we'd fetch the LatLng for this prediction here
                                selectedLatLng = null 
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            updateLocationFromLatLng(latLng)
                        }
                    ) {
                        selectedLatLng?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = selectedLocation?.first ?: "Selected Location",
                                snippet = selectedLocation?.second ?: ""
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
                                Column {
                                    Text(text = selectedLocation!!.first, fontWeight = FontWeight.Bold)
                                    Text(text = selectedLocation!!.second, fontSize = 12.sp, color = GrayText, maxLines = 1)
                                }
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
                    fontSize = 13.sp, 
                    color = GrayText
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}
