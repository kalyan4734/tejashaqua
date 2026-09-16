package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.tejashaqua.app.R
import com.tejashaqua.app.data.model.AquaRate
import com.tejashaqua.app.data.model.CustomerInfo
import com.tejashaqua.app.data.model.RateTrend
import com.tejashaqua.app.data.repository.CustomerRepository
import com.tejashaqua.app.ui.components.CustomerFoundDialog
import com.tejashaqua.app.ui.components.MarketItem
import com.tejashaqua.app.ui.components.RateGraphBottomSheet
import com.tejashaqua.app.ui.components.SellerPostsDialog
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.AquaLightBlue
import com.tejashaqua.app.ui.theme.DarkBlueText
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.ui.theme.LiveGreen
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.utils.CurrencyUtils
import com.tejashaqua.app.utils.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUserId: String,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLocationClick: () -> Unit,
    onPrawnsClick: () -> Unit,
    onFishRatesClick: () -> Unit,
    onItemClick: (Map<String, Any>) -> Unit,
    onChatListClick: (Map<String, Any>) -> Unit,
    onFilterClick: () -> Unit,
    onLocationFetched: (String, String) -> Unit,
    showNameSheetInitial: Boolean,
    onNameSave: (String) -> Unit,
    onNameSkip: () -> Unit,
    initialTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    locationViewModel: LocationSearchViewModel = viewModel(),
    marketplaceViewModel: com.tejashaqua.app.ui.viewmodel.MarketplaceViewModel = viewModel(),
    chatViewModel: com.tejashaqua.app.ui.viewmodel.ChatViewModel = viewModel(),
    marketplaceListState: LazyListState = rememberLazyListState()
) {
    var selectedItem by remember { mutableIntStateOf(initialTab) }

    // Sync internal state with initialTab when it changes from outside
    LaunchedEffect(initialTab) {
        selectedItem = initialTab
    }

    // Call onTabChange whenever internal selection changes
    LaunchedEffect(selectedItem) {
        onTabChange(selectedItem)
    }

    val productSearchText by marketplaceViewModel.searchText.collectAsState()
    val selectedCategoryFilter by marketplaceViewModel.selectedCategory.collectAsState()

    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"

    LaunchedEffect(currentLang) {
        locationViewModel.updateLocationForLanguage(currentLang)
    }

    var showGraphSheet by remember { mutableStateOf(false) }
    var selectedRateForGraph by remember { mutableStateOf<AquaRate?>(null) }

    var selectedCustomer by remember { mutableStateOf<CustomerInfo?>(null) }

    var showSellerPostsDialog by remember { mutableStateOf(false) }
    var selectedSellerId by remember { mutableStateOf("") }
    var selectedSellerName by remember { mutableStateOf("") }

    // Logic to detect special search
    LaunchedEffect(productSearchText) {
        if (productSearchText.lowercase().trim() == "sowmya") {
            selectedCustomer = CustomerRepository.getCustomerBySearch(productSearchText)
        }
    }

    val fetchedName by locationViewModel.currentLocationName.collectAsState()
    val fetchedSub by locationViewModel.currentSubLocation.collectAsState()
    val userLatLng by locationViewModel.currentLatLng.collectAsState()
    val isFetchingLocation by locationViewModel.isFetchingLocation.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(fetchedName, fetchedSub) {
        if (fetchedName.isNotBlank()) {
            onLocationFetched(fetchedName, fetchedSub)
        }
    }

    var showWelcomeSheet by remember { mutableStateOf(showNameSheetInitial) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    // Marketplace State from ViewModel
    val db = remember { FirebaseFirestore.getInstance() }
    val listings by marketplaceViewModel.listings.collectAsState()
    val isLoadingListings by marketplaceViewModel.isLoading.collectAsState()
    val isPaginating by marketplaceViewModel.isPaginating.collectAsState()
    val isLastPage by marketplaceViewModel.isLastPage.collectAsState()
    val loadingCategory by marketplaceViewModel.loadingCategory.collectAsState()

    val isScrollInProgress = marketplaceListState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (isScrollInProgress) {
            keyboardController?.hide()
        }
    }

    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastCheckedNotifications by remember { mutableLongStateOf(0L) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    val blockedUsers by marketplaceViewModel.blockedUsers.collectAsState()

    val radiusKm by marketplaceViewModel.radiusKm.collectAsState()
    val priceRange by marketplaceViewModel.priceRange.collectAsState()
    val sortBy by marketplaceViewModel.sortBy.collectAsState()

    // Logic to reload listings when location, category or fetching status changes
    LaunchedEffect(userLatLng, selectedCategoryFilter, fetchedName, isFetchingLocation, radiusKm, priceRange, sortBy) {
        if (!isFetchingLocation) {
            marketplaceViewModel.loadListings(
                lat = userLatLng?.latitude,
                lng = userLatLng?.longitude,
                locationName = fetchedName,
                category = selectedCategoryFilter,
                isFirstPage = true
            )
        }
    }

    // Real-time listener for badge count and user updates
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            marketplaceViewModel.startUserMetadataListener(currentUserId)
            
            db.collection("users").document(currentUserId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    lastCheckedNotifications = snapshot.getLong("lastCheckedNotifications") ?: 0L
                }
            }
        }
    }

    // Update unread count whenever listings or lastCheckedNotifications change
    LaunchedEffect(listings, lastCheckedNotifications) {
        unreadNotificationCount = if (lastCheckedNotifications > 0) {
            listings.count { data ->
                val ts = (data["timestamp"] as? Number)?.toLong() ?: 0L
                val userId = data["userId"]?.toString() ?: ""
                ts > lastCheckedNotifications && userId != currentUserId
            }
        } else {
            0
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId).collection("favorites")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        favoriteIds = snapshot.documents.map { it.id }.toSet()
                    }
                }
        }
    }

    val filteredListings =
        remember(listings, productSearchText, selectedCategoryFilter, blockedUsers) {
            listings.filter { data ->
                val title = data["title"]?.toString()?.lowercase() ?: ""
                val location = data["location"]?.toString()?.lowercase() ?: ""
                val category = data["category"]?.toString() ?: ""
                val userId = data["userId"]?.toString() ?: ""

                if (blockedUsers.contains(userId)) return@filter false

                val matchesSearch =
                    productSearchText.isBlank() || title.contains(productSearchText.lowercase()) || location.contains(
                        productSearchText.lowercase()
                    ) || category.lowercase()
                        .contains(productSearchText.lowercase()) || (data["businessSubCategory"]?.toString()
                        ?.lowercase()?.contains(productSearchText.lowercase())
                        ?: false) || (data["serviceType"]?.toString()?.lowercase()
                        ?.contains(productSearchText.lowercase()) ?: false)

                // Note: Cloud Function already filters by selectedCategoryFilter,
                // but we keep this local filter for consistency and immediate UI updates if needed.
                val matchesCategory = when (selectedCategoryFilter) {
                    "All" -> true
                    "VEHICLES" -> {
                        val serviceType = (data["serviceType"]?.toString() ?: "").lowercase().trim()
                        category.uppercase() == "VEHICLES" || category.uppercase() == "VEHICLE" || (category.uppercase() == "SERVICES" && 
                            (serviceType.contains("vehicle") || 
                             serviceType.contains("వెహికల్") ||
                             serviceType.contains("bore well") ||
                             serviceType.contains("బోర్ వెల్") ||
                             serviceType.contains("earth mover") ||
                             serviceType.contains("ఎర్త్ మూవర్")))
                    }

                    "FEED" -> {
                        val subCat = (data["businessSubCategory"]?.toString() ?: "").lowercase().trim()
                        category.uppercase() == "FEED" || (category.uppercase() == "BUSINESS" && (subCat == "feed" || subCat == "మేత"))
                    }

                    "MEDICINE" -> {
                        val subCat = (data["businessSubCategory"]?.toString() ?: "").lowercase().trim()
                        category.uppercase() == "MEDICINE" || (category.uppercase() == "BUSINESS" && (subCat == "medicine" || subCat == "మెడిసిన్" || subCat == "మందులు"))
                    }

                    "BUSINESS" -> {
                        val subCat = (data["businessSubCategory"]?.toString() ?: "").lowercase().trim()
                        (category.uppercase() == "BUSINESS" || category.uppercase() == "BIZ") && 
                        (subCat != "feed" && subCat != "మేత" && subCat != "medicine" && subCat != "మెడిసిన్" && subCat != "మందులు")
                    }

                    "SERVICES" -> {
                        val serviceType = (data["serviceType"]?.toString() ?: "").lowercase().trim()
                        category.uppercase() == "SERVICES" && 
                            !(serviceType.contains("vehicle") || 
                              serviceType.contains("వెహికల్") ||
                              serviceType.contains("bore well") ||
                              serviceType.contains("బోర్ వెల్") ||
                              serviceType.contains("earth mover") ||
                              serviceType.contains("ఎర్త్ మూవర్"))
                    }

                    "PRAWNS" -> category.uppercase() == "PRAWNS" || category.uppercase() == "PRAWN" || category.uppercase() == "HATCHERY"
                    "FISH" -> category.uppercase() == "FISH" || category.uppercase() == "SEED"
                    "TANKS" -> category.uppercase() == "TANKS" || category.uppercase() == "TANK" || category.uppercase() == "POND" || category.uppercase() == "LAND"

                    else -> category.uppercase() == selectedCategoryFilter
                }

                matchesSearch && matchesCategory
            }
        }

    val chunkedListings = remember(filteredListings) {
        filteredListings.chunked(2)
    }

    // Chat State from ViewModel
    val chats by chatViewModel.chats.collectAsState()
    val isLoadingChats by chatViewModel.isLoading.collectAsState()
    var chatSearchText by remember { mutableStateOf("") }
    var chatSelectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUserId) {
        chatViewModel.startChatsListener(currentUserId)
    }

    val listingStatusMap = chatViewModel.listingStatusMap

    val totalUnreadCount by remember(chats) {
        derivedStateOf { chats.sumOf { it.unreadCount } }
    }

    val filteredChats = remember(chats, chatSearchText, chatSelectedTabIndex) {
        chats.filter {
            (it.name.contains(chatSearchText, ignoreCase = true) || it.listingInfo.contains(
                chatSearchText, ignoreCase = true
            )) && when (chatSelectedTabIndex) {
                1 -> it.type == "Buying"
                2 -> it.type == "Selling"
                else -> true
            }
        }
    }

    val sortedChats = remember(filteredChats, listingStatusMap.toMap()) {
        filteredChats.sortedWith(compareByDescending<ChatListItemData> {
            listingStatusMap[it.listingId] ?: true
        }.thenByDescending { it.time })
    }

    val selectedSellerPosts = remember(selectedSellerId, listings) {
        if (selectedSellerId.isEmpty()) emptyList()
        else listings.filter { item -> item["userId"]?.toString() == selectedSellerId }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
        if (selectedItem == 0 || selectedItem == 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AquaBlue, AquaLightBlue
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            keyboardController?.hide()
                            onLocationClick()
                        }) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isGpsDisabled = fetchedName == stringResource(R.string.enable_gps_message)
                            Text(
                                text = fetchedName,
                                color = if (isGpsDisabled) Color(0xFFFFEB3B) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isGpsDisabled) Color(0xFFFFEB3B) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (fetchedSub.isNotEmpty()) {
                            Text(
                                text = fetchedSub,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = {
                        keyboardController?.hide()
                        showNotificationsSheet = true

                        // Mark as read by updating timestamp to current time
                        if (currentUserId.isNotEmpty()) {
                            db.collection("users").document(currentUserId)
                                .update("lastCheckedNotifications", System.currentTimeMillis())
                        }
                    }) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationCount > 0) {
                                    Badge(
                                        containerColor = Color.Red, contentColor = Color.White
                                    ) {
                                        Text(text = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString())
                                    }
                                }
                            }) {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        } else if (selectedItem == 2) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AquaBlue, AquaLightBlue
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.chats),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }

                TextField(
                    value = chatSearchText,
                    onValueChange = { chatSearchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .heightIn(min = 48.dp),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_conversations),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search, contentDescription = null, tint = GrayText
                        )
                    },
                    trailingIcon = {
                        if (chatSearchText.isNotEmpty()) {
                            IconButton(onClick = { chatSearchText = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = GrayText
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = AquaBlue
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )

                TabRow(
                    selectedTabIndex = chatSelectedTabIndex,
                    containerColor = Color.White,
                    contentColor = AquaBlue,
                    indicator = { tabPositions ->
                        if (chatSelectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[chatSelectedTabIndex]),
                                color = AquaBlue,
                                height = 3.dp
                            )
                        }
                    },
                    divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }) {
                    Tab(
                        selected = chatSelectedTabIndex == 0,
                        onClick = { chatSelectedTabIndex = 0 }) {
                        Text(
                            stringResource(R.string.all),
                            modifier = Modifier.padding(14.dp),
                            fontWeight = if (chatSelectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Tab(
                        selected = chatSelectedTabIndex == 1,
                        onClick = { chatSelectedTabIndex = 1 }) {
                        Text(
                            stringResource(R.string.buying),
                            modifier = Modifier.padding(14.dp),
                            fontWeight = if (chatSelectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Tab(
                        selected = chatSelectedTabIndex == 2,
                        onClick = { chatSelectedTabIndex = 2 }) {
                        Text(
                            stringResource(R.string.selling),
                            modifier = Modifier.padding(14.dp),
                            fontWeight = if (chatSelectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }, bottomBar = {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = if (selectedItem == 0) R.drawable.home_selected else R.drawable.home),
                        contentDescription = stringResource(R.string.home),
                        modifier = Modifier.size(24.dp)
                    )
                },
                selected = selectedItem == 0,
                onClick = {
                    keyboardController?.hide()
                    selectedItem = 0
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaBlue,
                    unselectedIconColor = GrayText,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = if (selectedItem == 1) R.drawable.shopping_bag_selected else R.drawable.iv_shopping),
                        contentDescription = stringResource(R.string.search_tab),
                        modifier = Modifier.size(24.dp)
                    )
                },
                selected = selectedItem == 1,
                onClick = {
                    keyboardController?.hide()
                    selectedItem = 1
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaBlue,
                    unselectedIconColor = GrayText,
                    indicatorColor = Color.Transparent
                )
            )
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick = {
                        keyboardController?.hide()
                        onAddClick()
                    },
                    containerColor = AquaBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(54.dp)
                        .offset(y = (-12).dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_post),
                        contentDescription = stringResource(R.string.add),
                        modifier = Modifier.size(54.dp),
                        tint = Color.Unspecified
                    )
                }
            }
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (totalUnreadCount > 0) {
                                Badge(
                                    containerColor = Color.Red, contentColor = Color.White
                                ) {
                                    Text(text = if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString())
                                }
                            }
                        }) {
                        Icon(
                            painter = painterResource(id = if (selectedItem == 2) R.drawable.message_selected else R.drawable.iv_conversation),
                            contentDescription = stringResource(R.string.chats),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }, selected = selectedItem == 2, onClick = {
                    keyboardController?.hide()
                    selectedItem = 2
                }, colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaBlue,
                    unselectedIconColor = GrayText,
                    indicatorColor = Color.Transparent
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = if (selectedItem == 3) R.drawable.profile_selected else R.drawable.profile),
                        contentDescription = stringResource(R.string.profile),
                        modifier = Modifier.size(24.dp)
                    )
                },
                selected = selectedItem == 3,
                onClick = {
                    keyboardController?.hide()
                    selectedItem = 3
                    onProfileClick()
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaBlue,
                    unselectedIconColor = GrayText,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                state = marketplaceListState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (selectedItem == 0 || selectedItem == 1) {
                    item {
                        SearchHeader(
                            productSearchText = productSearchText,
                            onProductSearchChange = { marketplaceViewModel.setSearchText(it) })
                    }

                    if (selectedItem == 0 && productSearchText.isBlank()) {
                        item {
                            AquaRatesSection(
                                onRateClick = { rate ->
                                    if (rate.isPrawn) {
                                        onPrawnsClick()
                                    } else {
                                        onFishRatesClick()
                                    }
                                })
                        }
                    }

                    item {
                        CategoryFilterRow(
                            selected = selectedCategoryFilter,
                            onSelect = { marketplaceViewModel.setSelectedCategory(it) },
                            onFilterClick = onFilterClick
                        )
                    }

                    // Marketplace Section flattened
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.fresh_marketplace),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = DarkBlueText
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.items_count, filteredListings.size),
                                color = GrayText,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val showInitialLoading = (isLoadingListings || isFetchingLocation || loadingCategory != null) && filteredListings.isEmpty()

                    if (showInitialLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = AquaBlue, modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        if (isFetchingLocation) stringResource(R.string.fetching_location) else stringResource(R.string.loading_marketplace),
                                        color = GrayText,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else if (filteredListings.isEmpty() && !isLoadingListings && !isFetchingLocation) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val message =
                                    if (productSearchText.isEmpty() && selectedCategoryFilter == "All") stringResource(
                                        R.string.no_listings
                                    ) else stringResource(R.string.no_match_search)
                                Text(message, color = GrayText)
                            }
                        }
                    } else {
                        items(
                            count = chunkedListings.size, key = { index ->
                                val row = if (index < chunkedListings.size) chunkedListings[index] else emptyList()
                                row.joinToString("-") { it["id"]?.toString() ?: "" }
                            }) { index ->
                            val rowItems = chunkedListings[index]

                            // Load more when reaching near the end
                            if (index >= chunkedListings.size - 2 && !isLastPage && !isPaginating) {
                                SideEffect {
                                    marketplaceViewModel.loadListings(
                                        lat = userLatLng?.latitude,
                                        lng = userLatLng?.longitude,
                                        locationName = fetchedName,
                                        category = selectedCategoryFilter,
                                        isFirstPage = false
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { data ->
                                    val listingId = data["id"]?.toString() ?: ""
                                    val images =
                                        (data["images"] as? List<*>)?.filterIsInstance<String>()
                                    val isFavorited = favoriteIds.contains(listingId)

                                    val categoryStr = data["category"]?.toString() ?: "Other"
                                    val displayCategory = when (categoryStr.uppercase()) {
                                        "FISH" -> stringResource(R.string.cat_fish_seed)
                                        "PRAWNS" -> stringResource(R.string.cat_prawns)
                                        "EQUIPMENTS" -> stringResource(R.string.cat_equipments)
                                        "VEHICLES" -> stringResource(R.string.cat_vehicles)
                                        "FEED" -> stringResource(R.string.cat_feed)
                                        "SERVICES" -> stringResource(R.string.cat_services)
                                        "TANKS" -> stringResource(R.string.cat_tanks)
                                        "BUSINESS" -> stringResource(R.string.cat_business)
                                        "JOBS" -> stringResource(R.string.cat_jobs)
                                        else -> categoryStr
                                    }
                                    val naText = stringResource(R.string.not_available_short)
                                    val tonText = stringResource(R.string.unit_ton)
                                    val acreText = stringResource(R.string.unit_acre)
                                    val priceLabel = when (categoryStr.uppercase()) {
                                        "PRAWNS" -> {
                                            val rateVal = data["rateValue"]?.toString()
                                                ?.takeIf { it.isNotBlank() } ?: naText
                                            if (rateVal == naText) naText else {
                                                val formattedRate =
                                                    CurrencyUtils.formatPrice(rateVal)
                                                val type = data["rateType"]?.toString() ?: "Paise"
                                                val isPaise = type.contains("Paise", ignoreCase = true) || 
                                                             type.contains("పైసలు") || 
                                                             type.contains("paisa", ignoreCase = true)
                                                
                                                if (isPaise) {
                                                    stringResource(R.string.paise_per_seed_label, formattedRate, stringResource(R.string.unit_paise), stringResource(R.string.seed_suffix))
                                                } else {
                                                    stringResource(R.string.rupees_per_seed_label, formattedRate, stringResource(R.string.seed_suffix))
                                                }
                                            }
                                        }

                                        "FEED" -> "₹${CurrencyUtils.formatPrice(data["ratePerTon"] ?: naText)}/$tonText"
                                        "BUSINESS" -> {
                                            if (data["businessSubCategory"] == "Feed") {
                                                "₹${
                                                    CurrencyUtils.formatPrice(
                                                        data["ratePerTon"]?.toString()
                                                        ?.takeIf { it.isNotBlank() } ?: naText)
                                                }/$tonText"
                                            } else {
                                                val displayVal = data["price"]?.toString()
                                                    ?.takeIf { it.isNotBlank() }
                                                    ?: data["rateValue"]?.toString()
                                                        ?.takeIf { it.isNotBlank() }
                                                    ?: data["ratePerTon"]?.toString()
                                                        ?.takeIf { it.isNotBlank() } ?: naText
                                                "₹${CurrencyUtils.formatPrice(displayVal)}"
                                            }
                                        }

                                        "JOBS" -> "₹${CurrencyUtils.formatPrice(data["salary"] ?: naText)}"
                                        "TANKS" -> "₹${CurrencyUtils.formatPrice(data["estPricePerAcre"] ?: naText)}/$acreText"
                                        else -> "₹${CurrencyUtils.formatPrice(data["price"] ?: data["rateValue"] ?: naText)}"
                                    }

                                    MarketItem(
                                        title = data["title"]?.toString()
                                        ?.takeIf { it.isNotBlank() }
                                        ?: stringResource(R.string.no_title),
                                        price = priceLabel,
                                        category = displayCategory,
                                        location = data["location"]?.toString() ?: "Unknown",
                                        posterName = data["posterName"]?.toString() ?: "User",
                                        imageUrl = images?.firstOrNull(),
                                        isFavorited = isFavorited,
                                        timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                            ?: (data["timestamp"] as? Long) ?: 0L,
                                        onFavoriteClick = {
                                            if (currentUserId.isNotEmpty() && listingId.isNotEmpty()) {
                                                val favRef =
                                                    db.collection("users").document(currentUserId)
                                                        .collection("favorites").document(listingId)
                                                if (isFavorited) {
                                                    favRef.delete()
                                                } else {
                                                    favRef.set(data)
                                                }
                                            }
                                        },
                                        onClick = { onItemClick(data) },
                                        onPosterClick = {
                                            selectedSellerId = data["userId"]?.toString() ?: ""
                                            selectedSellerName =
                                                data["posterName"]?.toString() ?: "User"
                                            if (selectedSellerId.isNotEmpty()) {
                                                showSellerPostsDialog = true
                                            }
                                        },
                                        rawCategory = categoryStr,
                                        lat = (data["lat"] as? Number)?.toDouble(),
                                        lng = (data["lng"] as? Number)?.toDouble(),
                                        viewCount = 0, // Don't show in Dashboard
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        if (isPaginating) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = AquaBlue, modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedItem == 0) {
                        item { FooterSection() }
                    }
                } else if (selectedItem == 2) {
                    if (isLoadingChats) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AquaBlue)
                            }
                        }
                    } else if (sortedChats.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.no_chats), color = GrayText)
                            }
                        }
                    } else {
                        items(sortedChats) { chat ->
                            ChatListItem(
                                chat = chat,
                                onClick = { onChatListClick(chat.fullData) },
                                initialListingExists = listingStatusMap[chat.listingId]
                            )
                            HorizontalDivider(
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (selectedItem == 2) stringResource(R.string.news) else stringResource(
                                    R.string.coming_soon
                                ), color = GrayText
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.welcome_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            IconButton(onClick = {
                                showWelcomeSheet = false
                                onNameSkip()
                            }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.welcome_desc),
                            fontSize = 12.sp,
                            color = GrayText,
                            lineHeight = 21.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    stringResource(R.string.enter_name), color = Color.Gray
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PersonOutline,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showWelcomeSheet = false
                                    onNameSkip()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFFE0E0E0)
                                )
                            ) {
                                Text(
                                    stringResource(R.string.skip_now),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            Button(
                                onClick = {
                                    if (tempName.isNotBlank()) {
                                        onNameSave(tempName)
                                        showWelcomeSheet = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                            ) {
                                Text(
                                    stringResource(R.string.save),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showNotificationsSheet) {
                val notifications = remember(listings, lastCheckedNotifications) {
                    listings.filter { data ->
                        val userId = data["userId"] as? String ?: ""
                        userId != currentUserId
                    }.sortedByDescending { it["timestamp"] as? Long ?: 0L }
                }

                ModalBottomSheet(
                    onDismissRequest = { showNotificationsSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.notifications),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            IconButton(onClick = {
                                showNotificationsSheet = false
                            }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (notifications.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(R.string.no_notifications), color = GrayText)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 500.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(notifications) { data ->
                                    val timestamp = data["timestamp"] as? Long ?: 0L
                                    val isNew = timestamp > lastCheckedNotifications
                                    val title = data["title"]?.toString() ?: "New Post"
                                    val category = data["category"]?.toString() ?: "Post"
                                    val posterName = data["posterName"]?.toString() ?: "User"
                                    val fullLocation = data["location"]?.toString() ?: ""
                                    val shortLoc = fullLocation.split(",").firstOrNull()?.trim() ?: "Local"
                                    
                                    var localizedLoc by remember(fullLocation, currentLang) { mutableStateOf(shortLoc) }
                                    val lat = (data["lat"] as? Number)?.toDouble()
                                    val lng = (data["lng"] as? Number)?.toDouble()
                                    
                                    LaunchedEffect(lat, lng, currentLang) {
                                        if (lat != null && lng != null) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val locale = java.util.Locale.forLanguageTag(currentLang)
                                                    val geocoder = android.location.Geocoder(context, locale)
                                                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                                                    if (!addresses.isNullOrEmpty()) {
                                                        val address = addresses[0]
                                                        localizedLoc = address.locality ?: address.subAdminArea ?: shortLoc
                                                    }
                                                } catch (e: Exception) {}
                                            }
                                        }
                                    }

                                    Card(
                                        onClick = {
                                            showNotificationsSheet = false
                                            onItemClick(data)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isNew) Color(0xFFF0F7FF) else Color(
                                                0xFFFAFAFA
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, if (isNew) AquaBlue.copy(alpha = 0.3f) else Color(
                                                0xFFEEEEEE
                                            )
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        if (isNew) AquaBlue else Color.LightGray,
                                                        CircleShape
                                                    ), contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.PostAdd,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isNew) "NEW: $title" else title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.Black
                                                )
                                                Text(
                                                    text = "$category posted by $posterName from $localizedLoc",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = SimpleDateFormat(
                                                        "dd MMM, hh:mm a", Locale.getDefault()
                                                    ).format(Date(timestamp)),
                                                    fontSize = 11.sp,
                                                    color = GrayText
                                                )
                                            }
                                            if (isNew) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color.Red, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showGraphSheet && selectedRateForGraph != null) {
                RateGraphBottomSheet(
                    rate = selectedRateForGraph!!, onDismiss = { showGraphSheet = false })
            }

            if (selectedCustomer != null) {
                CustomerFoundDialog(
                    customer = selectedCustomer!!, onDismiss = { selectedCustomer = null })
            }

            if (showSellerPostsDialog) {
                SellerPostsDialog(
                    sellerName = selectedSellerName,
                    sellerPosts = selectedSellerPosts,
                    onDismiss = { showSellerPostsDialog = false },
                    onItemClick = { onItemClick(it) })
            }
        }
    }
}

@Composable
fun CategoryFilterRow(selected: String, onSelect: (String) -> Unit, onFilterClick: () -> Unit = {}) {
    val categories = listOf(
        "All",
        "FISH",
        "PRAWNS",
        "EQUIPMENTS",
        "VEHICLES",
        "FEED",
        "MEDICINE",
        "SERVICES",
        "TANKS",
        "BUSINESS",
        "JOBS"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val label = when (category) {
                    "All" -> stringResource(R.string.all)
                    "FISH" -> stringResource(R.string.cat_fish_seed)
                    "PRAWNS" -> stringResource(R.string.cat_prawns)
                    "EQUIPMENTS" -> stringResource(R.string.cat_equipments)
                    "VEHICLES" -> stringResource(R.string.cat_vehicles)
                    "FEED" -> stringResource(R.string.cat_feed)
                    "MEDICINE" -> stringResource(R.string.cat_medicine)
                    "SERVICES" -> stringResource(R.string.cat_services)
                    "TANKS" -> stringResource(R.string.cat_tanks)
                    "BUSINESS" -> stringResource(R.string.cat_business)
                    "JOBS" -> stringResource(R.string.cat_jobs)
                    else -> category
                }

                val selectedColor = when (category) {
                    "FISH" -> Color(0xFF009688)
                    "PRAWNS" -> Color(0xFF3F51B5)
                    "EQUIPMENTS" -> Color(0xFF1976D2)
                    "VEHICLES" -> Color(0xFF1976D2)
                    "FEED" -> Color(0xFFE65100)
                    "MEDICINE" -> Color(0xFFD81B60)
                    "SERVICES" -> Color(0xFFF57C00)
                    "TANKS" -> Color(0xFF388E3C)
                    "BUSINESS" -> Color(0xFFB71C1C)
                    "JOBS" -> Color(0xFF673AB7)
                    else -> AquaBlue
                }

                FilterChip(
                    selected = selected == category,
                    onClick = { onSelect(category) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedColor, selectedLabelColor = Color.White
                    )
                )
            }
        }
        
        IconButton(
            onClick = onFilterClick,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = AquaBlue
            )
        }
    }
}

@Composable
fun SearchHeader(
    productSearchText: String, onProductSearchChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Search,
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AquaLightBlue, MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextField(
            value = productSearchText,
            onValueChange = { onProductSearchChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 14.sp, color = GrayText) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, contentDescription = null, tint = GrayText
                )
            },
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
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = AquaBlue
            ),
            shape = RoundedCornerShape(25.dp),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
    }
}

@Composable
fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.tejas_aqua_watermark_logo),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            alpha = 0.12f // Increased transparency
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(R.string.footer_text),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD1D9E6),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AquaRatesSection(onRateClick: (AquaRate) -> Unit) {
    val currentDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    
    // Initialize with placeholders to ensure section is always visible
    var rates by remember { mutableStateOf<List<AquaRate>>(
        listOf(
            AquaRate("Prawns", "--", "", RateTrend.FLAT, isPrawn = true),
            AquaRate("Rohu", "--", "", RateTrend.FLAT, isPrawn = false)
        )
    ) }
    var isLoading by remember { mutableStateOf(true) }

    val fishTypes = listOf(
        "Prawns", "Rohu"
    )

    LaunchedEffect(Unit) {
        db.collection("aqua_rates").addSnapshotListener { value, error ->
            if (error != null) {
                isLoading = false
                return@addSnapshotListener
            }

            if (value != null && !value.isEmpty) {
                val fetchedMap =
                    value.documents.associateBy({ it.id.lowercase(Locale.ROOT).trim() }, { doc ->
                        val price = doc.getString("price") ?: "--"
                        val change = doc.getString("change") ?: ""
                        val trendStr = doc.getString("trend") ?: "FLAT"
                        val trend = try {
                            RateTrend.valueOf(trendStr)
                        } catch (_: Exception) {
                            RateTrend.FLAT
                        }
                        val isPrawn =
                            doc.getBoolean("isPrawn") ?: (doc.id.lowercase(Locale.ROOT).trim() == "prawns")

                        AquaRate(doc.id, price, change, trend, isPrawn)
                    })

                // Merge with the fixed list of fish types
                rates = fishTypes.map { fish ->
                    fetchedMap[fish.lowercase(Locale.ROOT).trim()] ?: AquaRate(
                        fish,
                        "--",
                        "",
                        RateTrend.FLAT,
                        isPrawn = fish.lowercase(Locale.ROOT).trim() == "prawns"
                    )
                }
            }

            // Fallback if still no valid data after merging
            if (rates.all { it.price == "--" }) {
                rates = listOf(
                    AquaRate(
                        "Prawns",
                        context.getString(R.string.no_data_available),
                        context.getString(R.string.view_all_prices),
                        RateTrend.FLAT,
                        isPrawn = true
                    ), AquaRate(
                        "Rohu", context.getString(R.string.no_data_available), "", RateTrend.FLAT
                    )
                )
            }
            isLoading = false
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = stringResource(R.string.today_aqua_rates),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = DarkBlueText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = GrayText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(currentDate, color = GrayText, fontSize = 13.sp)
                }
            }
            Surface(
                color = Color(0xFFE8F5E9), shape = CircleShape
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(LiveGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.live),
                        color = LiveGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rates.forEach { rate ->
                RateCard(
                    rate = rate, onClick = { onRateClick(rate) }, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RateCard(
    rate: AquaRate, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val isNoData = rate.price == "--" || rate.price == "N/A" || rate.price.lowercase(Locale.ROOT)
        .contains("no change") || rate.price.contains("మార్పు లేదు") || rate.price == "No data available for today" || rate.price == "ఈ రోజు డేటా అందుబాటులో లేదు" || rate.price == stringResource(
        R.string.no_data_available
    )

    val displayPrice = if (isNoData) stringResource(R.string.no_data_available) else rate.price

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Category Badge
            Surface(
                color = if (rate.isPrawn) Color(0xFFE0F2F1) else Color(0xFFFFF3E0),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (rate.isPrawn) stringResource(R.string.cat_prawns) else stringResource(
                        R.string.cat_fish
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (rate.isPrawn) Color(0xFF00796B) else Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayPrice,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                    Text(
                        text = if (rate.isPrawn) stringResource(
                            R.string.count_label, "100"
                        ) else rate.getDisplayName(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rate.isPrawn) Color(0xFF3F51B5) else Color(0xFF009688)
                    )
                }

                Icon(
                    painter = painterResource(id = rate.icon),
                    contentDescription = null,
                    tint = if (rate.isPrawn) Color(0xFF3F51B5) else Color(0xFF009688),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
