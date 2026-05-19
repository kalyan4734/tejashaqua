package com.tejashaqua.app.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.res.stringResource
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.components.MarketItem
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import java.util.Locale

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
    val categoryString = listingData["category"]?.toString() ?: "Other"
    val category = try { ListingCategory.valueOf(categoryString.uppercase()) } catch (e: Exception) { null }
    val fullLocation = listingData["location"]?.toString() ?: "Unknown"
    // Use the first part of the address (Locality) as the main location
    val location = fullLocation.split(",").firstOrNull()?.trim() ?: fullLocation
    val description = listingData["description"]?.toString() ?: "No description provided."
    val posterName = listingData["posterName"]?.toString() ?: "User"
    val images = (listingData["images"] as? List<*>) ?: emptyList<String>()
    val timestamp = (listingData["timestamp"] as? Long) ?: System.currentTimeMillis()
    val listingUserId = listingData["userId"]?.toString() ?: ""
    val isOwnListing = currentUserId == listingUserId
    val listingId = listingData["id"]?.toString() ?: ""

    var isFavorited by remember { mutableStateOf(false) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(listingId, currentUserId) {
        if (currentUserId.isNotEmpty() && listingId.isNotEmpty()) {
            db.collection("users").document(currentUserId)
                .collection("favorites").document(listingId)
                .addSnapshotListener { snapshot, _ ->
                    isFavorited = snapshot != null && snapshot.exists()
                }
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId)
                .collection("favorites")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        favoriteIds = snapshot.documents.map { it.id }.toSet()
                    }
                }
        }
    }

    val toggleFavorite = { listing: Map<String, Any>, isFav: Boolean ->
        val id = listing["id"]?.toString() ?: ""
        if (currentUserId.isNotEmpty() && id.isNotEmpty()) {
            val favRef = db.collection("users").document(currentUserId)
                .collection("favorites").document(id)
            if (isFav) {
                favRef.delete()
            } else {
                favRef.set(listing)
            }
        }
    }

    var similarListings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(categoryString, listingData["id"]) {
        db.collection("listings")
            .whereEqualTo("category", categoryString)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshot ->
                similarListings = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    if (doc.id != listingData["id"]) data else null
                }.take(5)
            }
    }

    val pagerState = rememberPagerState { if (images.isEmpty()) 1 else images.size }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.detailed_page_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { toggleFavorite(listingData, isFavorited) }) {
                        Icon(
                            if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (isFavorited) Color.Red else Color.White
                        )
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
                        Text(stringResource(R.string.chat_with_seller), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val imageUrl = images[page]?.toString() ?: ""
                            if (imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Listing Image ${page + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(id = R.drawable.app_logo)
                                )
                            }
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).align(Alignment.Center),
                            alpha = 0.3f
                        )
                    }
                    
                    if (images.size > 1) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${images.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Header Info
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(color = Color(0xFFE8EAF6), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = categoryString,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = AquaBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    
                    val priceLabel = when(category) {
                        ListingCategory.PRAWNS -> {
                            val rate = listingData["rateValue"] ?: "N/A"
                            val unit = listingData["rateType"]?.toString()?.lowercase() ?: "kg"
                            "₹$rate/$unit"
                        }
                        ListingCategory.FEED -> "₹${listingData["ratePerTon"] ?: "N/A"}/ton"
                        ListingCategory.JOBS -> "₹${listingData["salary"] ?: "N/A"}"
                        else -> price
                    }
                    Text(text = priceLabel, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AquaBlue)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.posted_ago, getRelativeTime(timestamp)), fontSize = 13.sp, color = GrayText)
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
                    Text(text = stringResource(R.string.seller_label), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
                            Text(text = stringResource(R.string.member_since, "May 2026") + " • ⚡ Responds in 1 hr", fontSize = 12.sp, color = GrayText)
                        }
                    }
                }
            }

            // Description
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.description_label), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = description, fontSize = 14.sp, color = Color.DarkGray, lineHeight = 20.sp)
                }
            }

            // Location Section with Map Tile
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.posted_location), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val context = LocalContext.current
                    // Safely extract coordinates as Numbers to handle Double/Float variations from Firestore
                    var lat by remember { mutableStateOf((listingData["lat"] as? Number)?.toDouble()) }
                    var lng by remember { mutableStateOf((listingData["lng"] as? Number)?.toDouble()) }
                    
                    // Default to Rajahmundry if absolutely no coordinates are found
                    val finalLat = lat ?: 17.0005
                    val finalLng = lng ?: 81.7729
                    val position = LatLng(finalLat, finalLng)
                    
                    val cameraPositionState = rememberCameraPositionState {
                        this.position = CameraPosition.fromLatLngZoom(position, 13f)
                    }

                    // Fallback: If coordinates are missing, try to find them using the location name
                    LaunchedEffect(fullLocation) {
                        if (lat == null || lng == null) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocationName(fullLocation, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    lat = address.latitude
                                    lng = address.longitude
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    // Force camera update when coordinates change (e.g. navigating to a different listing or Geocoder result)
                    LaunchedEffect(lat, lng) {
                        if (lat != null && lng != null) {
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(LatLng(lat!!, lng!!), 13f))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                mapToolbarEnabled = true,
                                myLocationButtonEnabled = false,
                                compassEnabled = false
                            )
                        ) {
                            Marker(
                                state = MarkerState(position = LatLng(lat ?: finalLat, lng ?: finalLng)),
                                title = location,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
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
                                text = location, 
                                fontSize = 14.sp, 
                                color = Color.Black, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = {
                            val targetLat = lat ?: finalLat
                            val targetLng = lng ?: finalLng
                            val gmmIntentUri = "geo:$targetLat,$targetLng?q=${android.net.Uri.encode(fullLocation)}".toUri()
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        }) {
                            Text(stringResource(R.string.view_on_map), color = AquaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Details section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.details_label), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    when(category) {
                        ListingCategory.FISH -> {
                            DetailRowItem(stringResource(R.string.fish_type_label), listingData["fishType"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.size_label), "${listingData["sizeValue"] ?: ""} ${listingData["sizeType"] ?: ""}")
                            DetailRowItem(stringResource(R.string.fish_age_label), "${listingData["fishAge"] ?: ""} Months")
                            DetailRowItem(stringResource(R.string.quantity_label), "${listingData["quantity"] ?: ""} ${listingData["unitType"] ?: ""}")
                            DetailRowItem(stringResource(R.string.price_label), price)
                        }
                        ListingCategory.PRAWNS -> {
                            DetailRowItem(stringResource(R.string.hatchery_name_label), listingData["hatcheryName"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.prawn_type_label), listingData["prawnType"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.pl_days_label), listingData["plDays"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.quantity_label), "${listingData["quantity"] ?: ""} ${listingData["unitType"] ?: ""}")
                            DetailRowItem(stringResource(R.string.rate_label), "₹${listingData["rateValue"] ?: "N/A"}/${listingData["rateType"] ?: ""}")
                        }
                        ListingCategory.EQUIPMENTS -> {
                            DetailRowItem(stringResource(R.string.equipment_type_label), listingData["equipmentType"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.price_label), price)
                        }
                        ListingCategory.VEHICLES -> {
                            DetailRowItem(stringResource(R.string.vehicle_name_label), listingData["vehicleName"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.capacity_label), listingData["vehicleCapacity"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.service_type_label), listingData["serviceType"]?.toString() ?: "N/A")
                        }
                        ListingCategory.FEED -> {
                            DetailRowItem(stringResource(R.string.feed_name_label), listingData["feedName"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.business_type_label), listingData["businessType"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.rate_per_ton_label), "₹${listingData["ratePerTon"] ?: "N/A"}")
                        }
                        ListingCategory.SERVICES -> {
                            DetailRowItem(stringResource(R.string.service_type_label), listingData["serviceType"]?.toString() ?: "N/A")
                            when(listingData["serviceType"]?.toString()) {
                                "Bore Well" -> DetailRowItem(stringResource(R.string.bore_type_label), listingData["boreWellType"]?.toString() ?: "N/A")
                                "Live Fish Vehicles" -> {
                                    DetailRowItem(stringResource(R.string.vehicle_name_label), listingData["vehicleName"]?.toString() ?: "N/A")
                                    DetailRowItem(stringResource(R.string.capacity_label), listingData["vehicleCapacity"]?.toString() ?: "N/A")
                                }
                                "Nets" -> DetailRowItem(stringResource(R.string.net_type_label), listingData["netType"]?.toString() ?: "N/A")
                            }
                        }
                        ListingCategory.TANKS -> {
                            DetailRowItem(stringResource(R.string.tank_acres_label), "${listingData["tankAcres"] ?: "N/A"} Acres")
                            DetailRowItem(stringResource(R.string.est_price_per_acre_label), "₹${listingData["estPricePerAcre"] ?: "N/A"}")
                            DetailRowItem(stringResource(R.string.tank_location_label), listingData["tankLocation"]?.toString() ?: "N/A")
                        }
                        ListingCategory.JOBS -> {
                            DetailRowItem(stringResource(R.string.job_type_label), listingData["jobType"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.salary_label), "₹${listingData["salary"] ?: "N/A"}")
                            DetailRowItem(stringResource(R.string.tank_acres_label), listingData["tankAcres"]?.toString() ?: "N/A")
                            DetailRowItem(stringResource(R.string.work_location_label), listingData["tankLocation"]?.toString() ?: "N/A")
                        }
                        else -> {
                            DetailRowItem("Category", categoryString)
                            DetailRowItem(stringResource(R.string.price_label), price)
                        }
                    }
                    DetailRowItem(stringResource(R.string.posted_location), location)
                }
            }

            // Similar Listings
            if (similarListings.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.similar_listings), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = similarListings) { data ->
                                val simId = data["id"]?.toString() ?: ""
                                val simImages = (data["images"] as? List<*>)?.filterIsInstance<String>()
                                val isSimFav = favoriteIds.contains(simId)

                                val categoryStr = data["category"]?.toString() ?: "Other"
                                val priceLabel = when (categoryStr.uppercase()) {
                                    "PRAWNS" -> "₹${data["rateValue"] ?: "N/A"}/${data["rateType"]?.toString()?.lowercase() ?: "paise"}"
                                    "FEED" -> "₹${data["ratePerTon"] ?: "N/A"}/ton"
                                    "JOBS" -> "₹${data["salary"] ?: "N/A"}"
                                    "TANKS" -> "₹${data["estPricePerAcre"] ?: "N/A"}/acre"
                                    else -> "₹${data["price"] ?: data["rateValue"] ?: "N/A"}"
                                }

                                MarketItem(
                                    title = data["title"]?.toString()?.takeIf { it.isNotBlank() } ?: "No Title",
                                    price = priceLabel,
                                    category = categoryStr,
                                    location = data["location"]?.toString() ?: "Unknown",
                                    posterName = data["posterName"]?.toString() ?: "User",
                                    imageUrl = simImages?.firstOrNull(),
                                    isFavorited = isSimFav,
                                    onFavoriteClick = { toggleFavorite(data, isSimFav) },
                                    modifier = Modifier.width(160.dp)
                                )
                            }
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
