package com.tejashaqua.app.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedPageScreen(
    listingData: Map<String, Any>,
    currentUserId: String,
    onBackClick: () -> Unit,
    onChatClick: (Map<String, Any>) -> Unit
) {
    val title = listingData["title"]?.toString() ?: "No Title"
    val priceValue = listingData["price"] ?: listingData["rateValue"] ?: "N/A"
    val price = "₹$priceValue"
    val category = listingData["category"]?.toString() ?: "Other"
    val fullLocation = listingData["location"]?.toString() ?: "Unknown"
    // Use the first part of the address (Locality) as the main location
    val location = fullLocation.split(",").firstOrNull()?.trim() ?: fullLocation
    val description = listingData["description"]?.toString() ?: "No description provided."
    val posterName = listingData["posterName"]?.toString() ?: "User"
    val images = listingData["images"] as? List<*> ?: emptyList<String>()
    val timestamp = listingData["timestamp"] as? Long ?: System.currentTimeMillis()
    val listingUserId = listingData["userId"]?.toString() ?: ""
    val isOwnListing = currentUserId == listingUserId

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detailed Page", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Save logic */ }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Save", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        },
        bottomBar = {
            if (!isOwnListing) {
                Surface(
                    tonalElevation = 8.dp, 
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Button(
                        onClick = { onChatClick(listingData) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat with Seller", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // Main Image
            item {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color(0xFFF5F5F5))) {
                    if (images.isNotEmpty()) {
                        val firstImage = images[0]?.toString() ?: ""
                        val bitmap = remember(firstImage) {
                            if (firstImage.isNotEmpty()) {
                                try {
                                    val decodedString = Base64.decode(firstImage, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                } catch (_: Exception) { null }
                            } else null
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).align(Alignment.Center),
                            alpha = 0.3f
                        )
                    }
                    
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "1/${if(images.isEmpty()) 1 else images.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Header Info
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(color = Color(0xFFE8EAF6), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = AquaBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(text = "$price/kg", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AquaBlue)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Posted ${getRelativeTime(timestamp)}", fontSize = 13.sp, color = GrayText)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = location, fontSize = 13.sp, color = GrayText)
                    }
                }
            }

            // Seller Info
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Seller", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(50.dp).background(Color(0xFFE0F7FA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = posterName.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF0097A7))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = posterName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Member since May 2026 • ⚡ Responds in 1 hr", fontSize = 12.sp, color = GrayText)
                        }
                    }
                }
            }

            // Description
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Description", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = description, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
                }
            }

            // Location Section with Map Tile
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Location", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val context = LocalContext.current
                    // Use coordinates if available in listingData, else default to Rajahmundry
                    val lat = listingData["lat"] as? Double ?: 17.0005
                    val lng = listingData["lng"] as? Double ?: 81.7729
                    val position = LatLng(lat, lng)
                    val cameraPositionState = rememberCameraPositionState {
                        this.position = CameraPosition.fromLatLngZoom(position, 13f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
                            uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
                        ) {
                            Marker(
                                state = MarkerState(position = position),
                                title = location
                            )
                        }
                        
                        // Transparent clickable overlay to open in Google Maps app
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    val gmmIntentUri = "geo:$lat,$lng?q=${android.net.Uri.encode(fullLocation)}".toUri()
                                    val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(mapIntent)
                                }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fullLocation, 
                                fontSize = 14.sp, 
                                color = Color.Black, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = {
                            val gmmIntentUri = "geo:$lat,$lng?q=${android.net.Uri.encode(fullLocation)}".toUri()
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        }) {
                            Text("View on Map", color = AquaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Details section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRowItem("Hatchery Name", listingData["hatcheryName"]?.toString() ?: posterName)
                    DetailRowItem("Type of Prawn", listingData["prawnType"]?.toString() ?: "Growth Line")
                    DetailRowItem("PL Days", "PL10")
                    DetailRowItem("Rate", "$price/kg")
                    DetailRowItem("Location", location)
                }
            }

            // Similar Listings
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Similar Listings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(3) {
                            MarketItem(
                                title = "Vannamei PL10",
                                price = "₹180/kg",
                                category = "Prawn Hatchery",
                                location = "Nellore",
                                posterName = "Sri Lakshmi",
                                imageBase64 = null,
                                modifier = Modifier.width(160.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DetailRowItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = GrayText, fontSize = 14.sp)
        Text(text = value, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} mins ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> "${diff / 86400_000} days ago"
    }
}
