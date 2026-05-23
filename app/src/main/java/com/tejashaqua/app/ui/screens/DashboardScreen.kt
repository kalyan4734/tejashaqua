package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tejashaqua.app.R
import com.tejashaqua.app.data.model.AquaRate
import com.tejashaqua.app.data.model.RateTrend
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.ui.theme.*
import com.tejashaqua.app.ui.components.MarketItem
import com.tejashaqua.app.ui.components.RateGraphBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUserId: String,
    locationName: String,
    subLocation: String,
    onSeeAllRatesClick: () -> Unit,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLocationClick: () -> Unit,
    onPrawnsClick: () -> Unit,
    onItemClick: (Map<String, Any>) -> Unit,
    onChatListClick: (Map<String, Any>) -> Unit,
    onLocationFetched: (String, String) -> Unit,
    showNameSheetInitial: Boolean,
    onNameSave: (String) -> Unit,
    onNameSkip: () -> Unit,
    locationViewModel: LocationSearchViewModel = viewModel()
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var productSearchText by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    
    var showGraphSheet by remember { mutableStateOf(false) }
    var selectedRateForGraph by remember { mutableStateOf<AquaRate?>(null) }

    val fetchedName by locationViewModel.currentLocationName.collectAsState()
    val fetchedSub by locationViewModel.currentSubLocation.collectAsState()

    val fetchingLocText = stringResource(R.string.fetching_location)
    
    LaunchedEffect(Unit) {
        locationViewModel.fetchCurrentLocation()
    }

    LaunchedEffect(fetchedName, fetchedSub) {
        if (fetchedName != fetchingLocText) {
            onLocationFetched(fetchedName, fetchedSub)
        }
    }

    var showWelcomeSheet by remember { mutableStateOf(showNameSheetInitial) } 
    var tempName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = listOf(AquaBlue, AquaLightBlue)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLocationClick() }
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = locationName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        if (subLocation.isNotEmpty()) {
                            Text(
                                text = subLocation,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, stringResource(R.string.home)) },
                    selected = selectedItem == 0,
                    onClick = { selectedItem = 0 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AquaBlue, unselectedIconColor = GrayText)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, stringResource(R.string.search_tab)) },
                    selected = selectedItem == 1,
                    onClick = { selectedItem = 1 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AquaBlue, unselectedIconColor = GrayText)
                )
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = AquaBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp).offset(y = (-10).dp)
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.add), modifier = Modifier.size(30.dp))
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, stringResource(R.string.chats)) },
                    selected = selectedItem == 2,
                    onClick = { selectedItem = 2 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AquaBlue, unselectedIconColor = GrayText)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, stringResource(R.string.profile)) },
                    selected = selectedItem == 3,
                    onClick = { 
                        selectedItem = 3
                        onProfileClick()
                    },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AquaBlue, unselectedIconColor = GrayText)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (selectedItem == 0) {
                    item { 
                        SearchHeader(
                            productSearchText = productSearchText,
                            onProductSearchChange = { productSearchText = it }
                        ) 
                    }
                    item { 
                        AquaRatesSection(
                            onSeeAllClick = onSeeAllRatesClick,
                            onRateClick = { rate ->
                                if (rate.isPrawn) {
                                    onPrawnsClick()
                                } else {
                                    selectedRateForGraph = rate
                                    showGraphSheet = true
                                }
                            }
                        ) 
                    }
                    item {
                        CategoryFilterRow(
                            selected = selectedCategoryFilter,
                            onSelect = { selectedCategoryFilter = it }
                        )
                    }
                    item { 
                        MarketplaceSection(
                            currentUserId = currentUserId,
                            searchText = productSearchText, 
                            categoryFilter = selectedCategoryFilter, 
                            onItemClick = onItemClick
                        ) 
                    }
                    item { FooterSection() }
                } else if (selectedItem == 1) {
                    item { 
                        SearchHeader(
                            productSearchText = productSearchText,
                            onProductSearchChange = { productSearchText = it }
                        ) 
                    }
                    item {
                        CategoryFilterRow(
                            selected = selectedCategoryFilter,
                            onSelect = { selectedCategoryFilter = it }
                        )
                    }
                    item { 
                        MarketplaceSection(
                            currentUserId = currentUserId,
                            searchText = productSearchText, 
                            categoryFilter = selectedCategoryFilter, 
                            onItemClick = onItemClick
                        ) 
                    }
                } else if (selectedItem == 2) {
                    item {
                        DashboardChatList(
                            currentUserId = currentUserId,
                            onChatClick = onChatListClick
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (selectedItem == 2) stringResource(R.string.news) else "Coming Soon",
                                color = GrayText
                            )
                        }
                    }
                }
            }

            if (showWelcomeSheet) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        showWelcomeSheet = false
                        onNameSkip()
                    },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.welcome_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            IconButton(onClick = { 
                                showWelcomeSheet = false
                                onNameSkip()
                            }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.welcome_desc), fontSize = 14.sp, color = GrayText, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(value = tempName, onValueChange = { tempName = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.enter_name), color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color.Black) }, shape = RoundedCornerShape(12.dp), singleLine = true)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { 
                                    showWelcomeSheet = false
                                    onNameSkip()
                                }, 
                                modifier = Modifier.weight(1f).height(56.dp), 
                                shape = RoundedCornerShape(12.dp), 
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Text(stringResource(R.string.skip_now), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Button(
                                onClick = { 
                                    if (tempName.isNotBlank()) {
                                        onNameSave(tempName)
                                        showWelcomeSheet = false
                                    } 
                                }, 
                                modifier = Modifier.weight(1f).height(56.dp), 
                                shape = RoundedCornerShape(12.dp), 
                                colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                            ) {
                                Text(stringResource(R.string.save), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showGraphSheet && selectedRateForGraph != null) {
                RateGraphBottomSheet(
                    rate = selectedRateForGraph!!,
                    onDismiss = { showGraphSheet = false }
                )
            }
        }
    }
}

@Composable
fun CategoryFilterRow(selected: String, onSelect: (String) -> Unit) {
    val categories = listOf("All", "FISH", "PRAWNS", "EQUIPMENTS", "VEHICLES", "FEED", "SERVICES", "TANKS", "BUSINESS")
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val label = when(category) {
                "All" -> stringResource(R.string.all)
                "FISH" -> stringResource(R.string.cat_fish)
                "PRAWNS" -> stringResource(R.string.cat_prawns)
                "EQUIPMENTS" -> stringResource(R.string.cat_equipments)
                "VEHICLES" -> stringResource(R.string.cat_vehicles)
                "FEED" -> stringResource(R.string.cat_feed)
                "SERVICES" -> stringResource(R.string.cat_services)
                "TANKS" -> stringResource(R.string.cat_tanks)
                "BUSINESS" -> stringResource(R.string.cat_business)
                else -> category
            }
            
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AquaBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun SearchHeader(
    productSearchText: String,
    onProductSearchChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.verticalGradient(colors = listOf(AquaLightBlue, MaterialTheme.colorScheme.background)))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextField(
            value = productSearchText,
            onValueChange = { onProductSearchChange(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
            trailingIcon = {
                if (productSearchText.isNotEmpty()) {
                    IconButton(onClick = { 
                        onProductSearchChange("") 
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = GrayText)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(25.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
    }
}

@Composable
fun MarketplaceSection(currentUserId: String, searchText: String, categoryFilter: String, onItemClick: (Map<String, Any>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var listings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("listings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    listings = value.documents.map { 
                        val data = it.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = it.id
                        data
                    }
                }
                isLoading = false
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

    val filteredListings = remember(listings, searchText, categoryFilter) {
        listings.filter { data ->
            val title = (data["title"] as? String)?.lowercase() ?: ""
            val location = (data["location"] as? String)?.lowercase() ?: ""
            val category = (data["category"] as? String) ?: ""
            
            val matchesSearch = searchText.isBlank() || 
                title.contains(searchText.lowercase()) || 
                location.contains(searchText.lowercase()) ||
                category.lowercase().contains(searchText.lowercase())
            
            val matchesCategory = categoryFilter == "All" || category.uppercase() == categoryFilter
            
            matchesSearch && matchesCategory
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.fresh_marketplace), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkBlueText)
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(R.string.items_count, filteredListings.size), color = GrayText, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AquaBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading marketplace...", color = GrayText, fontSize = 14.sp)
                    }
                }
            }
        } else if (filteredListings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                val message = if (searchText.isEmpty() && categoryFilter == "All") stringResource(R.string.no_listings) else stringResource(R.string.no_match_search)
                Text(message, color = GrayText)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredListings.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { data ->
                            val listingId = data["id"]?.toString() ?: ""
                            val images = (data["images"] as? List<*>)?.filterIsInstance<String>()
                            val isFavorited = favoriteIds.contains(listingId)

                            val categoryStr = data["category"]?.toString() ?: "Other"
                            val priceLabel = when (categoryStr.uppercase()) {
                                "PRAWNS" -> "₹${data["rateValue"] ?: "N/A"}/${data["rateType"]?.toString()?.lowercase() ?: "paise"}"
                                "FEED" -> "₹${data["ratePerTon"] ?: "N/A"}/ton"
                                "BUSINESS" -> if (data["businessSubCategory"] == "Feed") "₹${data["ratePerTon"] ?: "N/A"}/ton" else "₹${data["price"] ?: data["rateValue"] ?: "N/A"}"
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
                                imageUrl = images?.firstOrNull(),
                                isFavorited = isFavorited,
                                onFavoriteClick = {
                                    if (currentUserId.isNotEmpty() && listingId.isNotEmpty()) {
                                        val favRef = db.collection("users").document(currentUserId)
                                            .collection("favorites").document(listingId)
                                        if (isFavorited) {
                                            favRef.delete()
                                        } else {
                                            favRef.set(data)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).clickable { onItemClick(data) }
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardChatList(currentUserId: String, onChatClick: (Map<String, Any>) -> Unit) {
    var searchText by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val db = FirebaseFirestore.getInstance()
    var chats by remember { mutableStateOf(listOf<ChatListItemData>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null || snapshot == null) return@addSnapshotListener
                
                chats = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val buyerId = data["buyerId"] as? String ?: ""
                    val isBuying = buyerId == currentUserId
                    
                    val unreadCounts = data["unreadCounts"] as? Map<*, *>
                    val unreadCount = (unreadCounts?.get(currentUserId) as? Long)?.toInt() ?: 
                                     (data["unreadCounts.$currentUserId"] as? Long)?.toInt() ?: 0

                    ChatListItemData(
                        chatId = doc.id,
                        name = if (isBuying) data["sellerName"] as? String ?: "Seller" else data["buyerName"] as? String ?: "Buyer",
                        otherUserId = if (isBuying) data["sellerId"] as? String ?: "" else data["buyerId"] as? String ?: "",
                        type = if (isBuying) "Buying" else "Selling",
                        listingInfo = data["listingTitle"] as? String ?: "Listing",
                        lastMessage = data["lastMessage"] as? String ?: "",
                        time = (data["lastMessageTimestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 
                               (data["lastMessageTimestamp"] as? Long) ?: 0L,
                        unreadCount = unreadCount,
                        listingImage = data["listingImage"] as? String,
                        fullData = data + mapOf("id" to (data["listingId"] ?: ""))
                    )
                }.sortedByDescending { it.time }
            }
    }

    val filteredChats = chats.filter {
        (it.name.contains(searchText, ignoreCase = true) || it.listingInfo.contains(searchText, ignoreCase = true)) &&
        when (selectedTabIndex) {
            1 -> it.type == "Buying"
            2 -> it.type == "Selling"
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Text(
            stringResource(R.string.chats), 
            fontWeight = FontWeight.Bold, 
            fontSize = 18.sp, 
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = DarkBlueText
        )

        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = 50.dp),
            placeholder = { Text(stringResource(R.string.search_conversations), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = AquaBlue,
            divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
        ) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                Text(stringResource(R.string.all), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                Text(stringResource(R.string.buying), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }) {
                Text(stringResource(R.string.selling), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AquaBlue)
            }
        } else if (filteredChats.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_chats), color = GrayText)
            }
        } else {
            filteredChats.forEach { chat ->
                ChatListItem(chat, onClick = { onChatClick(chat.fullData) })
                HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun FooterSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.footer_text), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D9E6), lineHeight = 38.sp)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AquaRatesSection(onSeeAllClick: () -> Unit, onRateClick: (AquaRate) -> Unit) {
    val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var rates by remember { mutableStateOf<List<AquaRate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val fishTypes = listOf(
        "Prawns", "Rohu", "Pangasius", "Roopchand"
    )

    LaunchedEffect(Unit) {
        db.collection("aqua_rates")
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    val fetchedMap = value.documents.associateBy({ it.id }, { doc ->
                        val price = doc.getString("price") ?: "--"
                        val change = doc.getString("change") ?: context.getString(R.string.no_change)
                        val trendStr = doc.getString("trend") ?: "FLAT"
                        val trend = try { RateTrend.valueOf(trendStr) } catch (_: Exception) { RateTrend.FLAT }
                        val isPrawn = doc.getBoolean("isPrawn") ?: (doc.id == "Prawns")
                        
                        AquaRate(doc.id, price, change, trend, isPrawn)
                    })

                    // Merge with the fixed list of fish types
                    rates = fishTypes.map { fish ->
                        fetchedMap[fish] ?: AquaRate(fish, "--", context.getString(R.string.no_change), RateTrend.FLAT, isPrawn = fish == "Prawns")
                    }
                }
                
                if (rates.isEmpty()) {
                    rates = listOf(
                        AquaRate("Prawns", "₹280-1000", context.getString(R.string.view_all_prices), RateTrend.FLAT, isPrawn = true),
                        AquaRate("Rohu", "₹160/kg", "+₹5 (3%)", RateTrend.UP),
                        AquaRate("Katla", "₹180/kg", "-₹10 (5.9%)", RateTrend.DOWN),
                        AquaRate("Tilapia", "₹120/kg", context.getString(R.string.no_change), RateTrend.FLAT),
                        AquaRate("Pangasius", "₹95/kg", "+₹2 (2%)", RateTrend.UP)
                    )
                }
                isLoading = false
            }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.today_aqua_rates), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkBlueText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = GrayText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(currentDate, color = GrayText, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(LiveGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.live), color = LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(rates) { rate ->
                RateCard(
                    rate = rate,
                    onClick = { onRateClick(rate) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSeeAllClick, 
            modifier = Modifier.fillMaxWidth().height(52.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = AquaBlue), 
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.see_all_rates), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RateCard(
    rate: AquaRate,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }, 
        shape = RoundedCornerShape(16.dp), 
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)), 
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(44.dp).background(rate.iconBgColor, CircleShape), contentAlignment = Alignment.Center) { 
                    Icon(painterResource(id = rate.icon), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) 
                }
                if (rate.isPrawn) {
                    Surface(modifier = Modifier.size(20.dp), shape = CircleShape, color = AquaBlue) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(rate.getDisplayName(), fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Text(rate.price, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (rate.trend != RateTrend.FLAT) {
                    Icon(
                        imageVector = if (rate.trend == RateTrend.UP) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (rate.trend == RateTrend.UP) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = rate.change,
                    fontSize = 12.sp,
                    color = when {
                        rate.isPrawn -> AquaBlue
                        rate.trend == RateTrend.UP -> Color(0xFF4CAF50)
                        rate.trend == RateTrend.DOWN -> Color(0xFFF44336)
                        else -> GrayText
                    }
                )
            }
        }
    }
}
