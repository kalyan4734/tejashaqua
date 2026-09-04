package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    onLocationFetched: (String, String) -> Unit,
    showNameSheetInitial: Boolean,
    onNameSave: (String) -> Unit,
    onNameSkip: () -> Unit,
    initialTab: Int = 0,
    onTabChange: (Int) -> Unit = {},
    locationViewModel: LocationSearchViewModel = viewModel()
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
    var productSearchText by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
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

    // Marketplace State
    val db = remember { FirebaseFirestore.getInstance() }
    var listings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastCheckedNotifications by remember { mutableLongStateOf(0L) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    var blockedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingListings by remember { mutableStateOf(true) }
    var isLastPage by remember { mutableStateOf(false) }
    var isPaginating by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    val functions = remember { FirebaseFunctions.getInstance("asia-south1") }

    fun loadListings(isFirstPage: Boolean = false) {
        if (isPaginating || (isLastPage && !isFirstPage)) return

        val currentPos = userLatLng

        if (isFirstPage) {
            isLastPage = false
            currentPage = 0
        }

        isPaginating = true

        // Sorting via Cloud Functions (Distance if location exists, else Latest First)
        val data = hashMapOf(
            "lat" to currentPos?.latitude,
            "lng" to currentPos?.longitude,
            "locationName" to fetchedName,
            "category" to selectedCategoryFilter,
            "page" to if (isFirstPage) 0 else currentPage,
            "pageSize" to 10
        )

        functions.getHttpsCallable("getListingsByLocation").call(data)
            .addOnSuccessListener { result ->
                val response = result.data as? Map<*, *>
                val newItems =
                    (response?.get("listings") as? List<*>)?.mapNotNull { it as? Map<String, Any> }
                        ?: emptyList()

                if (isFirstPage) {
                    listings = newItems
                    currentPage = 1
                } else {
                    listings = (listings + newItems).distinctBy { it["id"] }
                    currentPage++
                }

                isLastPage = response?.get("isLastPage") as? Boolean ?: true
                isPaginating = false
                isLoadingListings = false
            }.addOnFailureListener {
                isPaginating = false
                isLoadingListings = false
            }
    }

    // Logic to reload listings when location, category or fetching status changes
    LaunchedEffect(userLatLng, selectedCategoryFilter, isFetchingLocation) {
        if (!isFetchingLocation) {
            isLoadingListings = true
            loadListings(isFirstPage = true)
        }
    }

    // Real-time listener for badge count and user updates
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId).addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val blocked = snapshot.get("blockedUsers") as? List<*>
                    blockedUsers = blocked?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()

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
                        val serviceType = data["serviceType"]?.toString() ?: ""
                        val fishVehiclesEn = "Live Fish Vehicles"
                        val fishVehiclesTe = context.getString(R.string.service_live_fish_vehicles)
                        val boreWellEn = "Bore Well"
                        val boreWellTe = context.getString(R.string.service_bore_well)
                        val earthMoversEn = "Earth Movers"
                        val earthMoversTe = context.getString(R.string.service_earth_movers)

                        category.uppercase() == "VEHICLES" || (category.uppercase() == "SERVICES" && (serviceType == fishVehiclesEn || serviceType == fishVehiclesTe || serviceType == boreWellEn || serviceType == boreWellTe || serviceType == earthMoversEn || serviceType == earthMoversTe))
                    }

                    "FEED" -> {
                        val businessSubCategory = data["businessSubCategory"]?.toString() ?: ""
                        category.uppercase() == "FEED" || (category.uppercase() == "BUSINESS" && businessSubCategory == "Feed")
                    }

                    "BUSINESS" -> {
                        val businessSubCategory = data["businessSubCategory"]?.toString() ?: ""
                        category.uppercase() == "BUSINESS" && businessSubCategory != "Feed"
                    }

                    "SERVICES" -> {
                        val serviceType = data["serviceType"]?.toString() ?: ""
                        val fishVehiclesEn = "Live Fish Vehicles"
                        val fishVehiclesTe = context.getString(R.string.service_live_fish_vehicles)
                        val boreWellEn = "Bore Well"
                        val boreWellTe = context.getString(R.string.service_bore_well)
                        val earthMoversEn = "Earth Movers"
                        val earthMoversTe = context.getString(R.string.service_earth_movers)

                        category.uppercase() == "SERVICES" && (serviceType != fishVehiclesEn && serviceType != fishVehiclesTe && serviceType != boreWellEn && serviceType != boreWellTe && serviceType != earthMoversEn && serviceType != earthMoversTe)
                    }

                    else -> category.uppercase() == selectedCategoryFilter
                }

                matchesSearch && matchesCategory
            }
        }

    val chunkedListings = remember(filteredListings) {
        filteredListings.chunked(2)
    }

    val selectedSellerPosts = remember(selectedSellerId, listings) {
        if (selectedSellerId.isEmpty()) emptyList()
        else listings.filter { item -> item["userId"]?.toString() == selectedSellerId }
    }

    // Chat State
    var chats by remember { mutableStateOf(listOf<ChatListItemData>()) }
    var isLoadingChats by remember { mutableStateOf(true) }
    var chatSearchText by remember { mutableStateOf("") }
    var chatSelectedTabIndex by remember { mutableIntStateOf(0) }

    val totalUnreadCount by remember {
        derivedStateOf { chats.sumOf { it.unreadCount } }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) {
            isLoadingChats = false
            return@LaunchedEffect
        }

        db.collection("chats").whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                isLoadingChats = false
                if (e != null || snapshot == null) return@addSnapshotListener

                chats = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val sellerId = data["sellerId"]?.toString() ?: ""
                    val buyerId = data["buyerId"]?.toString() ?: ""
                    val isBuying =
                        if (sellerId.isNotEmpty()) sellerId != currentUserId else buyerId == currentUserId

                    val unreadCounts = data["unreadCounts"] as? Map<*, *>
                    val unreadCount = (unreadCounts?.get(currentUserId) as? Number)?.toInt()
                        ?: (data["unreadCounts.$currentUserId"] as? Number)?.toInt() ?: 0

                    val lid = data["listingId"]?.toString() ?: ""

                    ChatListItemData(
                        chatId = doc.id,
                        name = if (isBuying) data["sellerName"]?.toString()
                            ?: context.getString(R.string.seller_label) else data["buyerName"]?.toString()
                            ?: context.getString(R.string.buyer_label),
                        otherUserId = if (isBuying) data["sellerId"]?.toString()
                            ?: "" else data["buyerId"]?.toString() ?: "",
                        type = if (isBuying) "Buying" else "Selling",
                        listingId = lid,
                        listingInfo = data["listingTitle"]?.toString() ?: "Listing",
                        lastMessage = data["lastMessage"]?.toString() ?: "",
                        time = when (val ts = data["lastMessageTimestamp"]) {
                            is com.google.firebase.Timestamp -> ts.toDate().time
                            is Number -> ts.toLong()
                            else -> 0L
                        },
                        unreadCount = unreadCount,
                        listingImage = data["listingImage"]?.toString(),
                        fullData = data + mapOf("id" to lid)
                    )
                }.sortedByDescending { it.time }
            }
    }

    val listingStatusMap = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(chats) {
        val uniqueListingIds = chats.map { it.listingId }
            .filter { it.isNotEmpty() && !listingStatusMap.containsKey(it) }.distinct()

        if (uniqueListingIds.isNotEmpty()) {
            uniqueListingIds.chunked(10).forEach { chunk ->
                db.collection("listings")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get()
                    .addOnSuccessListener { snapshot ->
                        val foundIds = snapshot.documents.map { it.id }.toSet()
                        chunk.forEach { id ->
                            val exists = foundIds.contains(id)
                            listingStatusMap[id] = exists

                            // Cleanup logic for inactive chats
                            if (!exists) {
                                val associatedChats = chats.filter { it.listingId == id }
                                associatedChats.forEach { chatItem ->
                                    val inactiveSince = chatItem.fullData["inactiveSince"]
                                    if (inactiveSince == null) {
                                        // Tag as inactive
                                        db.collection("chats").document(chatItem.chatId).update(
                                            "inactiveSince",
                                            com.google.firebase.firestore.FieldValue.serverTimestamp()
                                        )
                                    } else {
                                        // Check if 7 days passed
                                        val inactiveTime = when (inactiveSince) {
                                            is com.google.firebase.Timestamp -> inactiveSince.toDate().time
                                            is Number -> inactiveSince.toLong()
                                            else -> 0L
                                        }

                                        if (inactiveTime > 0) {
                                            val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
                                            if (System.currentTimeMillis() - inactiveTime > sevenDaysInMillis) {
                                                // Delete chat
                                                db.collection("chats").document(chatItem.chatId)
                                                    .delete()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
        }
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

    Scaffold(topBar = {
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
                icon = { Icon(Icons.Default.Home, stringResource(R.string.home)) },
                selected = selectedItem == 0,
                onClick = {
                    keyboardController?.hide()
                    selectedItem = 0
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaBlue, unselectedIconColor = GrayText
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Search, stringResource(R.string.search_tab)) },
                selected = selectedItem == 1,
                onClick = {
                    keyboardController?.hide()
                    selectedItem = 1
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaBlue, unselectedIconColor = GrayText
                )
            )
            FloatingActionButton(
                onClick = {
                    keyboardController?.hide()
                    onAddClick()
                },
                containerColor = AquaBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-10).dp)
            ) {
                Icon(
                    Icons.Default.Add, stringResource(R.string.add), modifier = Modifier.size(30.dp)
                )
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
                    Icon(Icons.AutoMirrored.Filled.Chat, stringResource(R.string.chats))
                }
            }, selected = selectedItem == 2, onClick = {
                keyboardController?.hide()
                selectedItem = 2
            }, colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AquaBlue, unselectedIconColor = GrayText
            )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, stringResource(R.string.profile)) },
                selected = selectedItem == 3,
                onClick = {
                    keyboardController?.hide()
                    selectedItem = 3
                    onProfileClick()
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AquaBlue, unselectedIconColor = GrayText
                )
            )
        }
    }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (selectedItem == 0 || selectedItem == 1) {
                    item {
                        SearchHeader(
                            productSearchText = productSearchText,
                            onProductSearchChange = { productSearchText = it })
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
                            onSelect = { selectedCategoryFilter = it })
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

                    if (isLoadingListings) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(200.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = AquaBlue, modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            stringResource(R.string.loading_marketplace),
                                            color = GrayText,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (filteredListings.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
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
                                val row = chunkedListings[index]
                                row.joinToString("-") { it["id"]?.toString() ?: "" }
                            }) { index ->
                            val rowItems = chunkedListings[index]

                            // Load more when reaching near the end
                            if (index >= chunkedListings.size - 2 && !isLastPage && !isPaginating) {
                                SideEffect {
                                    loadListings(isFirstPage = false)
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
                                                if (type.contains(
                                                        "Paise", ignoreCase = true
                                                    )
                                                ) "$formattedRate Paise/Seed" else "₹$formattedRate/Seed"
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AquaBlue)
                            }
                        }
                    } else if (sortedChats.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
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
fun CategoryFilterRow(selected: String, onSelect: (String) -> Unit) {
    val categories = listOf(
        "All",
        "FISH",
        "PRAWNS",
        "EQUIPMENTS",
        "VEHICLES",
        "FEED",
        "SERVICES",
        "TANKS",
        "BUSINESS",
        "JOBS"
    )

    LazyRow(
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
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            stringResource(R.string.footer_text),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD1D9E6),
            lineHeight = 39.sp
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
