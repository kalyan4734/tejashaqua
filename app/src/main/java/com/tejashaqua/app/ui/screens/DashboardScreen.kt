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
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.ui.theme.*
import com.tejashaqua.app.ui.components.MarketItem
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
    onItemClick: (Map<String, Any>) -> Unit,
    onPrawnsClick: () -> Unit,
    onLocationFetched: (String, String) -> Unit,
    showNameSheetInitial: Boolean,
    onNameSave: (String) -> Unit,
    onNameSkip: () -> Unit,
    locationViewModel: LocationSearchViewModel = viewModel()
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var productSearchText by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val fetchedName by locationViewModel.currentLocationName.collectAsState()
    val fetchedSub by locationViewModel.currentSubLocation.collectAsState()

    val fetchingLocText = stringResource(R.string.fetching_location)
    val notFoundText = stringResource(R.string.location_not_found)
    val failedText = stringResource(R.string.failed_get_location)
    
    LaunchedEffect(Unit) {
        if (locationName == fetchingLocText) {
            locationViewModel.fetchCurrentLocation()
        }
    }

    LaunchedEffect(fetchedName, fetchedSub) {
        if (locationName == fetchingLocText && 
            fetchedName != fetchingLocText && 
            fetchedName != notFoundText &&
            fetchedName != failedText) {
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
                    Spacer(modifier = Modifier.width(16.dp))
                    Box {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape).align(Alignment.TopEnd).offset(x = 1.dp, y = (-1).dp))
                    }
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
                    icon = { Icon(Icons.Default.Work, stringResource(R.string.jobs)) },
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
                    icon = { Icon(Icons.Default.Article, stringResource(R.string.news)) },
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
                item { 
                    SearchHeader(
                        productSearchText = productSearchText,
                        onProductSearchChange = { productSearchText = it }
                    ) 
                }
                item { 
                    AquaRatesSection(
                        onSeeAllClick = onSeeAllRatesClick,
                        onPrawnsClick = onPrawnsClick
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
        }
    }
}

@Composable
fun CategoryFilterRow(selected: String, onSelect: (String) -> Unit) {
    val categories = listOf("All", "FISH", "PRAWNS", "EQUIPMENTS", "VEHICLES", "FEED", "BOREWELL", "TANKS")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category) },
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
            modifier = Modifier.fillMaxWidth().height(50.dp),
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

                            MarketItem(
                                title = data["title"]?.toString() ?: "No Title",
                                price = "₹${data["price"] ?: data["rateValue"] ?: "N/A"}",
                                category = data["category"]?.toString() ?: "Other",
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
fun FooterSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.footer_text), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D9E6), lineHeight = 38.sp)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AquaRatesSection(onSeeAllClick: () -> Unit, onPrawnsClick: () -> Unit) {
    val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
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
            item { 
                RateCard(
                    name = "Prawns", 
                    price = "₹280-1000", 
                    status = "View All Prices", 
                    bgColor = Color(0xFFE0F2F1), 
                    icon = painterResource(id = R.drawable.prawn),
                    isPrawn = true,
                    onClick = onPrawnsClick
                ) 
            }
            item { 
                RateCard(
                    name = "Rohu", 
                    price = "₹160/kg", 
                    status = "+₹5 (3%)", 
                    bgColor = Color(0xFFFFF3E0), 
                    icon = painterResource(id = R.drawable.fish),
                    onClick = onSeeAllClick
                ) 
            }
            item { 
                RateCard(
                    name = "Tilapia", 
                    price = "₹120/kg", 
                    status = "No Change", 
                    bgColor = Color(0xFFE8EAF6), 
                    icon = painterResource(id = R.drawable.fish),
                    onClick = onSeeAllClick
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
                Text("See All Rates", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RateCard(
    name: String, 
    price: String, 
    status: String, 
    bgColor: Color, 
    icon: androidx.compose.ui.graphics.painter.Painter,
    isPrawn: Boolean = false,
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
                Box(modifier = Modifier.size(44.dp).background(bgColor, CircleShape), contentAlignment = Alignment.Center) { 
                    Icon(icon, null, tint = Color.Unspecified, modifier = Modifier.size(24.dp)) 
                }
                if (isPrawn) {
                    Surface(modifier = Modifier.size(20.dp), shape = CircleShape, color = AquaBlue) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(name, fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Medium)
            Text(price, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            
            if (!isPrawn && status.contains("+")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(status, fontSize = 12.sp, color = Color.Red)
                }
            } else {
                Text(status, fontSize = 12.sp, color = if (isPrawn) AquaBlue else GrayText)
            }
        }
    }
}
