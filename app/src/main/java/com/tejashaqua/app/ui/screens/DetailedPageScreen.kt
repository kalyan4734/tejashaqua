package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import android.location.Geocoder
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.R
import com.tejashaqua.app.utils.CurrencyUtils
import com.tejashaqua.app.utils.LocaleHelper
import com.tejashaqua.app.ui.components.MarketItem
import com.tejashaqua.app.ui.components.SellerPostsDialog
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext

import androidx.lifecycle.viewmodel.compose.viewModel
import com.tejashaqua.app.ui.viewmodel.UserActionViewModel
import android.widget.Toast
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedPageScreen(
    listingData: Map<String, Any>,
    currentUserId: String,
    onBackClick: () -> Unit,
    onChatClick: (Map<String, Any>) -> Unit,
    onItemClick: (Map<String, Any>) -> Unit,
    userActionViewModel: UserActionViewModel = viewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    val reportReasons = listOf(
        stringResource(R.string.report_inappropriate),
        stringResource(R.string.report_scam),
        stringResource(R.string.report_spam),
        stringResource(R.string.report_other)
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }
    
    val naText = stringResource(R.string.not_available_short)
    val tonText = stringResource(R.string.unit_ton)
    val acreText = stringResource(R.string.unit_acre)
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"

    val title = listingData["title"]?.toString() ?: stringResource(R.string.no_title)
    val categoryStr = listingData["category"]?.toString() ?: "Other"
    val displayCategory = remember(categoryStr, currentLang) {
        when(categoryStr.uppercase()) {
            "FISH" -> context.getString(R.string.cat_fish_seed)
            "PRAWNS" -> context.getString(R.string.cat_prawns)
            "EQUIPMENTS" -> context.getString(R.string.cat_equipments)
            "VEHICLES" -> context.getString(R.string.cat_vehicles)
            "FEED" -> context.getString(R.string.cat_feed)
            "SERVICES" -> context.getString(R.string.cat_services)
            "TANKS" -> context.getString(R.string.cat_tanks)
            "BUSINESS" -> context.getString(R.string.cat_business)
            "JOBS" -> context.getString(R.string.cat_jobs)
            else -> categoryStr
        }
    }
    
    val priceLabel = remember(listingData, currentLang) {
        when (categoryStr.uppercase()) {
            "PRAWNS" -> {
                val rateVal = listingData["rateValue"]?.toString()?.takeIf { it.isNotBlank() } ?: naText
                if (rateVal == naText) naText else {
                    val formattedRate = CurrencyUtils.formatPrice(rateVal)
                    val type = listingData["rateType"]?.toString() ?: "Paise"
                    if (type.contains("Paise", ignoreCase = true)) "$formattedRate ${context.getString(R.string.unit_paise)}/Seed" else "₹$formattedRate/Seed"
                }
            }
            "FEED" -> "₹${CurrencyUtils.formatPrice(listingData["ratePerTon"] ?: naText)}/$tonText"
            "BUSINESS" -> {
                if (listingData["businessSubCategory"] == "Feed") {
                    "₹${CurrencyUtils.formatPrice(listingData["ratePerTon"]?.toString()?.takeIf { it.isNotBlank() } ?: naText)}/$tonText"
                } else {
                    val displayVal = listingData["price"]?.toString()?.takeIf { it.isNotBlank() }
                        ?: listingData["rateValue"]?.toString()?.takeIf { it.isNotBlank() }
                        ?: listingData["ratePerTon"]?.toString()?.takeIf { it.isNotBlank() }
                        ?: naText
                    "₹${CurrencyUtils.formatPrice(displayVal)}"
                }
            }
            "JOBS" -> "₹${CurrencyUtils.formatPrice(listingData["salary"] ?: naText)}"
            "TANKS" -> "₹${CurrencyUtils.formatPrice(listingData["estPricePerAcre"] ?: naText)}/$acreText"
            else -> "₹${CurrencyUtils.formatPrice(listingData["price"] ?: listingData["rateValue"] ?: naText)}"
        }
    }
    val category = try { ListingCategory.valueOf(categoryStr.uppercase()) } catch (e: Exception) { null }
    val fullLocation = listingData["location"]?.toString() ?: stringResource(R.string.unknown_location)
    // Use the first part of the address (Locality) as the main location
    val location = fullLocation.split(",").firstOrNull()?.trim() ?: fullLocation
    
    var localizedLocation by remember(fullLocation, currentLang) { mutableStateOf(location) }

    LaunchedEffect(listingData["lat"], listingData["lng"], currentLang) {
        val latVal = (listingData["lat"] as? Number)?.toDouble()
        val lngVal = (listingData["lng"] as? Number)?.toDouble()
        if (latVal != null && lngVal != null) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val locale = java.util.Locale.forLanguageTag(currentLang)
                    val geocoder = android.location.Geocoder(context, locale)
                    val addresses = geocoder.getFromLocation(latVal, lngVal, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val newLoc = address.locality ?: address.subAdminArea ?: location
                        localizedLocation = newLoc
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    val description = listingData["description"]?.toString() ?: stringResource(R.string.no_description)
    val posterName = listingData["posterName"]?.toString() ?: stringResource(R.string.user_label)
    val images = (listingData["images"] as? List<*>) ?: emptyList<String>()
    val timestamp = (listingData["timestamp"] as? Long) ?: System.currentTimeMillis()
    val listingUserId = listingData["userId"]?.toString() ?: listingData["posterId"]?.toString() ?: ""
    val isOwnListing = currentUserId.isNotEmpty() && listingUserId.isNotEmpty() && currentUserId == listingUserId
    val listingId = listingData["id"]?.toString() ?: ""

    // --- MAP STATE OPTIMIZATION (Hoisted for scroll performance) ---
    var lat by remember(listingId) { mutableStateOf((listingData["lat"] as? Number)?.toDouble()) }
    var lng by remember(listingId) { mutableStateOf((listingData["lng"] as? Number)?.toDouble()) }
    
    val finalLat = lat ?: 17.0005
    val finalLng = lng ?: 81.7729
    val mapPosition = remember(finalLat, finalLng) { LatLng(finalLat, finalLng) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapPosition, 13f)
    }

    // Sync camera if lat/lng changes (e.g. from Geocoder)
    LaunchedEffect(lat, lng) {
        if (lat != null && lng != null) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat!!, lng!!), 13f)
        }
    }

    LaunchedEffect(fullLocation) {
        if (lat == null || lng == null) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
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
    }

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
    // -------------------------------------------------------------
    
    // Pass from Home Page to avoid flicker for Joined Date and Privacy Toggle
    var sellerJoinedAt by remember(listingId) { 
        mutableLongStateOf((listingData["sellerJoinedAt"] as? Number)?.toLong() ?: 0L) 
    }
    var sellerShowMobile by remember(listingId) { 
        mutableStateOf(listingData["sellerShowMobile"] as? Boolean ?: false) 
    }
    val db = remember { FirebaseFirestore.getInstance() }
    
    var showSellerPostsDialog by remember { mutableStateOf(false) }
    var sellerListings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    DisposableEffect(listingUserId, listingId) {
        var userListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        if (listingUserId.isNotEmpty()) {
            userListener = db.collection("users").document(listingUserId)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        val remoteJoinedAt = doc.getLong("joinedAt") ?: 0L
                        if (remoteJoinedAt > 0) {
                            sellerJoinedAt = remoteJoinedAt
                        }

                        val remoteShowMobile = doc.get("showMobileNumber")
                        if (remoteShowMobile is Boolean) {
                            // Only update if it's different from what we started with
                            // to avoid a frame where it resets to default.
                            if (sellerShowMobile != remoteShowMobile) {
                                sellerShowMobile = remoteShowMobile
                            }
                        }
                    }
                }
            
            db.collection("listings")
                .whereEqualTo("userId", listingUserId)
                .get()
                .addOnSuccessListener { snapshot ->
                    sellerListings = snapshot.documents.map { doc ->
                        val d = doc.data?.toMutableMap() ?: mutableMapOf()
                        d["id"] = doc.id
                        d
                    }
                }
        }
        
        onDispose {
            userListener?.remove()
        }
    }

    key(listingId) {
        var isFavorited by remember { mutableStateOf(false) }
        var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        val listState = rememberLazyListState()

        LaunchedEffect(listingId) {
            listState.scrollToItem(0)
        }

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

        LaunchedEffect(categoryStr, listingId) {
            db.collection("listings")
                .whereEqualTo("category", categoryStr)
                .limit(10)
                .get()
                .addOnSuccessListener { snapshot ->
                    similarListings = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        if (doc.id != listingId) data else null
                    }.take(5)
                }
        }

    val pagerState = rememberPagerState { if (images.isEmpty()) 1 else images.size }
    var showFullScreenPager by remember { mutableStateOf(false) }

    if (showFullScreenPager && images.isNotEmpty()) {
        FullScreenImageDialog(
            images = images,
            initialPage = pagerState.currentPage,
            onDismiss = { showFullScreenPager = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.detailed_page_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        keyboardController?.hide()
                        toggleFavorite(listingData, isFavorited) 
                    }) {
                        Icon(
                            if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.saved_items),
                            tint = if (isFavorited) Color.Red else Color.White
                        )
                    }
                    if (!isOwnListing) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.report_listing)) },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.block_user)) },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showBlockDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        },
        bottomBar = {
            if (currentUserId.isNotEmpty() && !isOwnListing) {
                Surface(
                    tonalElevation = 8.dp, 
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (sellerShowMobile) {
                                val contactNumber = listingData["contactNumber"]?.toString() ?: ""
                                OutlinedButton(
                                    onClick = { 
                                        keyboardController?.hide()
                                        if (contactNumber.isNotEmpty()) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                data = android.net.Uri.parse("tel:$contactNumber")
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.phone_not_available), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AquaBlue),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AquaBlue)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.contact_us), 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Button(
                                onClick = { 
                                    keyboardController?.hide()
                                    onChatClick(listingData) 
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.chat_with_seller), 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (showReportDialog) {
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text(stringResource(R.string.report_listing)) },
                text = {
                    Column {
                        Text(stringResource(R.string.select_reason), modifier = Modifier.padding(bottom = 8.dp))
                        reportReasons.forEach { reason ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (selectedReason == reason), onClick = { selectedReason = reason })
                                Text(reason, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        userActionViewModel.reportListing(listingId, currentUserId, selectedReason) {
                            Toast.makeText(context, context.getString(R.string.report_submitted), Toast.LENGTH_SHORT).show()
                            showReportDialog = false
                        }
                    }) { Text(stringResource(R.string.report)) }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        if (showBlockDialog) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text(stringResource(R.string.block_user)) },
                text = { Text(stringResource(R.string.block_user_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        userActionViewModel.blockUser(listingUserId) {
                            Toast.makeText(context, context.getString(R.string.user_blocked), Toast.LENGTH_SHORT).show()
                            showBlockDialog = false
                            onBackClick()
                        }
                    }) { Text(stringResource(R.string.block)) }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // 1. Main Image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Square container like OLX
                        .background(Color(0xFFF5F5F5))
                        .clickable { if (images.isNotEmpty()) showFullScreenPager = true }
                ) {
                    if (images.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val imageUrl = images[page]?.toString() ?: ""
                            if (imageUrl.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Blurred background filling the container
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(50.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Slight dark overlay to make the main image pop
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))

                                    // Actual image centered and fit
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Listing Image ${page + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        error = painterResource(id = R.drawable.app_logo)
                                    )
                                }
                            }
                        }
                    } else if (categoryStr.uppercase() == "JOBS") {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF3E5F5)), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Color(0xFF673AB7).copy(alpha = 0.5f)
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
                    
                    if (images.size > 1) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${images.size}",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 2. Header Info
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(color = Color(0xFFE8EAF6), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = displayCategory,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = AquaBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    
                    Text(text = priceLabel, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AquaBlue)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = getRelativeTime(context, timestamp), fontSize = 14.sp, color = GrayText)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = localizedLocation, fontSize = 14.sp, color = GrayText)
                    }
                }
            }

            // 3. Description
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.description_label), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = description, fontSize = 15.sp, color = Color.DarkGray, lineHeight = 21.sp)
                }
            }

            // 5. Details section
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.details_label), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    when(category) {
                        ListingCategory.FISH -> {
                            DetailRowItem(stringResource(R.string.fish_type_label), listingData["fishType"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.size_label), "${listingData["sizeValue"]?.toString() ?: ""} ${listingData["sizeType"]?.toString() ?: ""}")
                            DetailRowItem(stringResource(R.string.fish_age_label), stringResource(R.string.months_suffix, listingData["fishAge"]?.toString() ?: ""))
                            DetailRowItem(stringResource(R.string.quantity_label), "${CurrencyUtils.formatPrice(listingData["quantity"])} ${listingData["unitType"]?.toString() ?: ""}")
                            DetailRowItem(stringResource(R.string.price_label), priceLabel)
                        }
                        ListingCategory.PRAWNS -> {
                            DetailRowItem(stringResource(R.string.hatchery_name_label), listingData["hatcheryName"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.prawn_type_label), listingData["prawnType"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.quantity_label), "${listingData["quantity"]?.toString() ?: ""} ${listingData["unitType"]?.toString() ?: ""}")
                            DetailRowItem(stringResource(R.string.rate_label), priceLabel)
                        }
                        ListingCategory.EQUIPMENTS -> {
                            DetailRowItem(stringResource(R.string.equipment_type_label), listingData["equipmentType"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.price_label), priceLabel)
                        }
                        ListingCategory.VEHICLES -> {
                            val capacity = listingData["vehicleCapacity"]?.toString() ?: stringResource(R.string.not_available_short)
                            val unit = listingData["vehicleCapacityUnit"]?.toString() ?: ""
                            DetailRowItem(stringResource(R.string.vehicle_name_label), listingData["vehicleName"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.capacity_label), if (unit.isNotEmpty()) "$capacity $unit" else capacity)
                            DetailRowItem(stringResource(R.string.service_type_label), listingData["serviceType"]?.toString() ?: stringResource(R.string.not_available_short))
                        }
                        ListingCategory.FEED -> {
                            DetailRowItem(stringResource(R.string.feed_name_label), listingData["feedName"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.business_type_label), listingData["businessType"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.rate_per_ton_label), priceLabel)
                        }
                        ListingCategory.BUSINESS -> {
                            DetailRowItem(stringResource(R.string.business_category), listingData["businessSubCategory"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.type_label), listingData["businessType"]?.toString() ?: stringResource(R.string.not_available_short))
                            if (listingData["businessSubCategory"] == "Feed") {
                                DetailRowItem(stringResource(R.string.feed_name_label), listingData["feedName"]?.toString() ?: stringResource(R.string.not_available_short))
                                DetailRowItem(stringResource(R.string.rate_per_ton_label), priceLabel)
                            } else if (listingData["businessSubCategory"] == "Medicine") {
                                DetailRowItem(stringResource(R.string.medicine_name_label), listingData["medicineName"]?.toString() ?: stringResource(R.string.not_available_short))
                                DetailRowItem(stringResource(R.string.medicine_rate_label), priceLabel)
                            } else if (listingData["businessSubCategory"] == "Others") {
                                DetailRowItem(stringResource(R.string.price_label), priceLabel)
                            }
                        }
                        ListingCategory.SERVICES -> {
                            val serviceTypeStr = listingData["serviceType"]?.toString() ?: ""
                            val boreWell = stringResource(R.string.service_bore_well)
                            val fishVehicles = stringResource(R.string.service_live_fish_vehicles)
                            val nets = stringResource(R.string.service_nets)

                            DetailRowItem(stringResource(R.string.service_type_label), serviceTypeStr.ifEmpty { stringResource(R.string.not_available_short) })
                            
                            when {
                                serviceTypeStr == "Bore Well" || serviceTypeStr == boreWell -> {
                                    DetailRowItem(stringResource(R.string.bore_type_label), listingData["boreWellType"]?.toString() ?: stringResource(R.string.not_available_short))
                                }
                                serviceTypeStr == "Live Fish Vehicles" || serviceTypeStr == fishVehicles -> {
                                    val capacity = listingData["vehicleCapacity"]?.toString() ?: stringResource(R.string.not_available_short)
                                    val unit = listingData["vehicleCapacityUnit"]?.toString() ?: ""
                                    DetailRowItem(stringResource(R.string.vehicle_name_label), listingData["vehicleName"]?.toString() ?: stringResource(R.string.not_available_short))
                                    DetailRowItem(stringResource(R.string.capacity_label), if (unit.isNotEmpty()) "$capacity $unit" else capacity)
                                }
                                serviceTypeStr == "Nets" || serviceTypeStr == nets -> {
                                    DetailRowItem(stringResource(R.string.net_type_label), listingData["netType"]?.toString() ?: stringResource(R.string.not_available_short))
                                }
                            }
                            DetailRowItem(stringResource(R.string.price_label), priceLabel)
                        }
                        ListingCategory.TANKS -> {
                            DetailRowItem(stringResource(R.string.tank_acres_label), stringResource(R.string.acres_suffix, listingData["tankAcres"]?.toString() ?: stringResource(R.string.not_available_short)))
                            DetailRowItem(stringResource(R.string.est_price_per_acre_label), priceLabel)
                            DetailRowItem(stringResource(R.string.tank_location_label), listingData["tankLocation"]?.toString() ?: stringResource(R.string.not_available_short))
                        }
                        ListingCategory.JOBS -> {
                            DetailRowItem(stringResource(R.string.job_type_label), listingData["jobType"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.salary_label), priceLabel)
                            DetailRowItem(stringResource(R.string.tank_acres_label), listingData["tankAcres"]?.toString() ?: stringResource(R.string.not_available_short))
                            DetailRowItem(stringResource(R.string.work_location_label), listingData["tankLocation"]?.toString() ?: stringResource(R.string.not_available_short))
                        }
                        else -> {
                            DetailRowItem(stringResource(R.string.category_label), displayCategory)
                            DetailRowItem(stringResource(R.string.price_label), priceLabel)
                        }
                    }
                    DetailRowItem(stringResource(R.string.posted_location), localizedLocation)
                }
            }

            // 6. Location Section with Map Tile
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Seller Details Header
                    Text(text = stringResource(R.string.seller_label), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // User details above map
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clickable { 
                                showSellerPostsDialog = true
                            }
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(Color(0xFFE0F7FA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = posterName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF0097A7), fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = posterName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            val joinedDate = if (sellerJoinedAt > 0) {
                                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(sellerJoinedAt))
                            } else {
                                stringResource(R.string.may_2024)
                            }
                            Text(text = stringResource(R.string.member_since_label, joinedDate), fontSize = 13.sp, color = GrayText)
                        }
                    }

                    Text(text = stringResource(R.string.posted_location), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    
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
                            googleMapOptionsFactory = {
                                com.google.android.gms.maps.GoogleMapOptions().liteMode(true)
                            },
                            properties = MapProperties(
                                isMyLocationEnabled = hasLocationPermission,
                                mapType = MapType.NORMAL
                            ),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                mapToolbarEnabled = true,
                                myLocationButtonEnabled = hasLocationPermission,
                                compassEnabled = false,
                                scrollGesturesEnabled = false,
                                zoomGesturesEnabled = false
                            )
                        ) {
                            val markerPos = LatLng(lat ?: finalLat, lng ?: finalLng)
                            Marker(
                                state = MarkerState(position = markerPos),
                                title = localizedLocation,
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
                                text = localizedLocation,
                                fontSize = 12.sp,
                                color = Color.Black, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = {
                            val targetLat = lat ?: finalLat
                            val targetLng = lng ?: finalLng
                            val gmmIntentUri = "geo:$targetLat,$targetLng?q=${android.net.Uri.encode(fullLocation)}".toUri()
                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        }) {
                            Text(stringResource(R.string.view_on_map), color = AquaBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 7. Similar Listings
            if (similarListings.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(R.string.similar_listings), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(items = similarListings) { data ->
                                val simId = data["id"]?.toString() ?: ""
                                val simImages = (data["images"] as? List<*>)?.filterIsInstance<String>()
                                val isSimFav = favoriteIds.contains(simId)

                                val categoryStrSim = data["category"]?.toString() ?: "Other"
                                val displayCategorySim = when(categoryStrSim.uppercase()) {
                                    "FISH" -> stringResource(R.string.cat_fish_seed)
                                    "PRAWNS" -> stringResource(R.string.cat_prawns)
                                    "EQUIPMENTS" -> stringResource(R.string.cat_equipments)
                                    "VEHICLES" -> stringResource(R.string.cat_vehicles)
                                    "FEED" -> stringResource(R.string.cat_feed)
                                    "SERVICES" -> stringResource(R.string.cat_services)
                                    "TANKS" -> stringResource(R.string.cat_tanks)
                                    "BUSINESS" -> stringResource(R.string.cat_business)
                                    "JOBS" -> stringResource(R.string.cat_jobs)
                                    else -> categoryStrSim
                                }
                                
                                val simPriceLabel = when (categoryStrSim.uppercase()) {
                                    "PRAWNS" -> {
                                        val rateVal = data["rateValue"]?.toString()?.takeIf { it.isNotBlank() } ?: naText
                                        if (rateVal == naText) naText else {
                                            val formattedRate = CurrencyUtils.formatPrice(rateVal)
                                            val type = data["rateType"]?.toString() ?: "Paise"
                                            if (type.contains("Paise", ignoreCase = true)) "$formattedRate Paise/Seed" else "₹$formattedRate/Seed"
                                        }
                                    }
                                    "FEED" -> "₹${CurrencyUtils.formatPrice(data["ratePerTon"] ?: naText)}/$tonText"
                                    "BUSINESS" -> {
                                        if (data["businessSubCategory"] == "Feed") {
                                            "₹${CurrencyUtils.formatPrice(data["ratePerTon"] ?: naText)}/$tonText"
                                        } else {
                                            "₹${CurrencyUtils.formatPrice(data["price"] ?: data["rateValue"] ?: naText)}"
                                        }
                                    }
                                    "JOBS" -> "₹${CurrencyUtils.formatPrice(data["salary"] ?: naText)}"
                                    "TANKS" -> "₹${CurrencyUtils.formatPrice(data["estPricePerAcre"] ?: naText)}/$acreText"
                                    else -> "₹${CurrencyUtils.formatPrice(data["price"] ?: data["rateValue"] ?: naText)}"
                                }

                                MarketItem(
                                    title = data["title"]?.toString()?.takeIf { it.isNotBlank() } ?: "No Title",
                                    price = simPriceLabel,
                                    category = displayCategorySim,
                                    location = data["location"]?.toString() ?: "Unknown",
                                    posterName = data["posterName"]?.toString() ?: "User",
                                    imageUrl = simImages?.firstOrNull(),
                                    isFavorited = isSimFav,
                                    onFavoriteClick = { toggleFavorite(data, isSimFav) },
                                    onClick = { onItemClick(data) },
                                    rawCategory = categoryStrSim,
                                    modifier = Modifier.width(160.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        
        if (showSellerPostsDialog) {
            SellerPostsDialog(
                sellerName = posterName,
                sellerPosts = sellerListings,
                onDismiss = { showSellerPostsDialog = false },
                onItemClick = { onItemClick(it) }
            )
        }
    }
  }
}



@Composable
fun FullScreenImageDialog(
    images: List<*>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val fullScreenPagerState = rememberPagerState(initialPage = initialPage) { images.size }
    val scaleStates = remember { mutableStateMapOf<Int, Float>() }
    val isZoomed = (scaleStates[fullScreenPagerState.currentPage] ?: 1f) > 1f

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = fullScreenPagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isZoomed
            ) { page ->
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                LaunchedEffect(scale) {
                    scaleStates[page] = scale
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
                        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                        
                        if (newScale > 1f) {
                            val extraWidth = (newScale - 1) * constraints.maxWidth
                            val extraHeight = (newScale - 1) * constraints.maxHeight
                            val maxX = extraWidth / 2
                            val maxY = extraHeight / 2
                            
                            val newOffset = offset + offsetChange
                            offset = androidx.compose.ui.geometry.Offset(
                                x = newOffset.x.coerceIn(-maxX, maxX),
                                y = newOffset.y.coerceIn(-maxY, maxY)
                            )
                        } else {
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        }
                        scale = newScale
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = state)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = androidx.compose.ui.geometry.Offset.Zero
                                        } else {
                                            scale = 2f
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = images[page]?.toString() ?: "",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            
            if (images.size > 1) {
                Text(
                    text = "${fullScreenPagerState.currentPage + 1}/${images.size}",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                )
            }
        }
    }
}

@Composable
fun DetailRowItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = GrayText, fontSize = 15.sp)
        Text(text = value, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

private fun getRelativeTime(context: android.content.Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> context.getString(R.string.just_now)
        diff < 3600_000 -> context.getString(R.string.mins_ago, diff / 60_000)
        diff < 86400_000 -> context.getString(R.string.hours_ago, diff / 3600_000)
        else -> context.getString(R.string.days_ago, diff / 86400_000)
    }
}
