package com.tejashaqua.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.ui.components.LoadingOverlay
import com.tejashaqua.app.ui.screens.AboutAppScreen
import com.tejashaqua.app.ui.screens.AdminDashboardScreen
import com.tejashaqua.app.ui.screens.AquaRatesScreen
import com.tejashaqua.app.ui.screens.ChatListScreen
import com.tejashaqua.app.ui.screens.ChatScreen
import com.tejashaqua.app.ui.screens.DashboardScreen
import com.tejashaqua.app.ui.screens.DetailedPageScreen
import com.tejashaqua.app.ui.screens.EditListingScreen
import com.tejashaqua.app.ui.screens.EditProfileScreen
import com.tejashaqua.app.ui.screens.FishRatesScreen
import com.tejashaqua.app.ui.screens.ForceUpdateScreen
import com.tejashaqua.app.ui.screens.LanguageSelectionScreen
import com.tejashaqua.app.ui.screens.LegalScreen
import com.tejashaqua.app.ui.screens.LoginScreen
import com.tejashaqua.app.ui.screens.MyListingsScreen
import com.tejashaqua.app.ui.screens.OtpScreen
import com.tejashaqua.app.ui.screens.PrawnRatesScreen
import com.tejashaqua.app.ui.screens.ProfileScreen
import com.tejashaqua.app.ui.screens.SavedItemsScreen
import com.tejashaqua.app.ui.screens.SelectCategoryScreen
import com.tejashaqua.app.ui.screens.SelectLocationScreen
import com.tejashaqua.app.ui.screens.SplashScreen
import com.tejashaqua.app.ui.theme.TejashAquaTheme
import com.tejashaqua.app.ui.viewmodel.AuthState
import com.tejashaqua.app.ui.viewmodel.AuthViewModel
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.utils.LocaleHelper
import com.tejashaqua.app.utils.NetworkObserver
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.app.ActivityCompat
import com.tejashaqua.app.utils.AppStateTracker

import com.tejashaqua.app.ui.viewmodel.PermissionViewModel
import com.tejashaqua.app.utils.PermissionType
import com.tejashaqua.app.utils.PermissionStatus
import com.tejashaqua.app.utils.PermissionHelper
import com.tejashaqua.app.ui.components.PermissionRationaleDialog
import com.tejashaqua.app.ui.components.AppLaunchPermissionsDialog
import com.tejashaqua.app.ui.components.SettingsRedirectDialog

class MainActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val intentFlow = MutableStateFlow<Intent?>(null)
    private var permissionViewModel: PermissionViewModel? = null

    override fun onResume() {
        super.onResume()
        AppStateTracker.isAppInForeground = true
        permissionViewModel?.updatePermissionStates()
    }

    override fun onPause() {
        super.onPause()
        AppStateTracker.isAppInForeground = false
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update current intent to the new one
        intentFlow.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- CHECK GOOGLE PLAY SERVICES ---
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (availability != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, availability, 9000)?.show()
        }

        // --- PRINT APP HASH FOR OTP AUTO-FILL ---
        // Copy this string from Logcat and add it to your MSG91 SMS Template
        val helper = com.tejashaqua.app.utils.AppSignatureHelper(this)
        android.util.Log.d("AppSignature", "App Hash: ${helper.appSignatures}")

        intentFlow.value = intent
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        LocaleHelper.applySavedLocale(this)
        enableEdgeToEdge()
        setContent {
            TejashAquaTheme {
                val authViewModel: AuthViewModel = viewModel()
                val locationViewModel: LocationSearchViewModel = viewModel()
                val authState by authViewModel.authState.collectAsState()
                val deviceLatLng by locationViewModel.currentLatLng.collectAsState()
                val context = LocalContext.current
                
                val networkObserver = remember { NetworkObserver(context) }
                val networkStatus by networkObserver.observe.collectAsState(initial = NetworkObserver.Status.Available)
                val currentIntent by intentFlow.collectAsState()

                var selectedLanguageCode by rememberSaveable { 
                    mutableStateOf(LocaleHelper.getSelectedLanguage(context)) 
                }

                var appVersion by remember { mutableStateOf("1.2") }
                var needsUpdate by remember { mutableStateOf(false) }
                var updateUrl by remember { mutableStateOf("https://play.google.com/store/apps/details?id=com.tejashaqua.app") }

                var currentScreen by rememberSaveable { mutableStateOf("splash") }
                var languageSelectionSource by rememberSaveable { mutableStateOf("splash") }
                var mobileNumber by rememberSaveable { mutableStateOf("") }
                var userName by rememberSaveable { mutableStateOf("User") }
                var userId by rememberSaveable { mutableStateOf("") }
                var joinedAt by rememberSaveable { mutableLongStateOf(0L) }
                var isAdmin by rememberSaveable { mutableStateOf(false) }
                val showMobileNumber = (authState as? AuthState.Success)?.showMobileNumber ?: false

                val isLanguageSelected = selectedLanguageCode != null

                var selectedCategory by remember { mutableStateOf(ListingCategory.FISH) }
                var isEditMode by remember { mutableStateOf(false) }
                var selectedListingId by remember { mutableStateOf<String?>(null) }
                var selectedListingData by remember { mutableStateOf<Map<String, Any>?>(null) }
                var listingBackStack by remember { mutableStateOf(listOf<Map<String, Any>>()) }
                var detailedPageSource by remember { mutableStateOf("dashboard") }
                var chatSourceScreen by remember { mutableStateOf("detailed_page") }
                var shouldSendInitialChatMessage by remember { mutableStateOf(false) }

                var dashboardTab by rememberSaveable { mutableIntStateOf(0) }
                var showNoInternetDialog by rememberSaveable { mutableStateOf(false) }
                var isNavigatingToDetailedPage by remember { mutableStateOf(false) }

                var lastBackPressTime by remember { mutableLongStateOf(0L) }

                // Reset dialog when internet returns
                LaunchedEffect(networkStatus) {
                    if (networkStatus == NetworkObserver.Status.Available) {
                        showNoInternetDialog = false
                    }
                }

                LaunchedEffect(currentScreen) {
                    val bundle = Bundle()
                    bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, currentScreen)
                    bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
                }

                val fetchingLocText = stringResource(R.string.fetching_location)
                var currentLocationName by remember { mutableStateOf(fetchingLocText) }
                var currentSubLocation by remember { mutableStateOf("") }

                val fetchedName by locationViewModel.currentLocationName.collectAsState()
                val fetchedSub by locationViewModel.currentSubLocation.collectAsState()

                LaunchedEffect(fetchedName, fetchedSub) {
                    if (fetchedName.isNotBlank()) {
                        currentLocationName = fetchedName
                        currentSubLocation = fetchedSub
                    }
                }

                // Track where the location picker was opened from
                var locationPickerSource by remember { mutableStateOf("dashboard") }
                var pickedListingLocation by remember { mutableStateOf<Pair<String, LatLng?>?>(null) }

                // --- NAVIGATION HELPERS ---
                val navigateToDetailedPage: (Map<String, Any>, String) -> Unit = { data, source ->
                    val lid = data["id"]?.toString() ?: data["listingId"]?.toString() ?: ""
                    val sellerId = data["userId"]?.toString() ?: data["sellerId"]?.toString() ?: ""
                    
                    if (sellerId.isEmpty()) {
                        selectedListingData = data
                        detailedPageSource = source
                        listingBackStack = emptyList()
                        currentScreen = "detailed_page"
                    } else {
                        isNavigatingToDetailedPage = true
                        // ALWAYS fetch user preference before navigating to ensure NO flicker
                        // Firestore 'get()' will use cache if available, so it's very fast.
                        FirebaseFirestore.getInstance().collection("users").document(sellerId).get()
                            .addOnSuccessListener { doc ->
                                isNavigatingToDetailedPage = false
                                val updatedData = data.toMutableMap()
                                val showMobile = doc.getBoolean("showMobileNumber") ?: false
                                val joined = doc.getLong("joinedAt") ?: 0L
                                updatedData["sellerShowMobile"] = showMobile
                                updatedData["sellerJoinedAt"] = joined
                                if (lid.isNotEmpty()) updatedData["id"] = lid
                                
                                selectedListingData = updatedData
                                detailedPageSource = source
                                // Only reset backstack if not coming from detailed page itself
                                if (source != "detailed_page") {
                                    listingBackStack = emptyList()
                                }
                                currentScreen = "detailed_page"
                            }
                            .addOnFailureListener {
                                isNavigatingToDetailedPage = false
                                selectedListingData = data
                                detailedPageSource = source
                                if (source != "detailed_page") {
                                    listingBackStack = emptyList()
                                }
                                currentScreen = "detailed_page"
                            }
                    }
                }

                val onRateUsClick: () -> Unit = {
                    val appId = "com.tejashaqua.app"
                    val marketUri = android.net.Uri.parse("market://details?id=$appId")
                    val marketIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, marketUri).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NO_HISTORY or
                                android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                                android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        setPackage("com.android.vending")
                    }
                    
                    try {
                        context.startActivity(marketIntent)
                    } catch (e: Exception) {
                        // If specifically targeting Play Store fails, try generic market intent
                        val genericMarketIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, marketUri).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(genericMarketIntent)
                        } catch (e2: Exception) {
                            // Fallback to browser
                            val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, 
                                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$appId")).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(webIntent)
                            } catch (e3: Exception) {
                                Toast.makeText(context, "Unable to open Play Store", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                // Handle Notification Click Navigation
                LaunchedEffect(currentIntent, userId) {
                    val intentToProcess = currentIntent
                    if (intentToProcess == null || userId.isEmpty()) return@LaunchedEffect
                    
                    val type = intentToProcess.getStringExtra("type") 
                        ?: if (intentToProcess.action == "OPEN_CHAT") "chat" 
                        else if (intentToProcess.action == "OPEN_RATES") "rates" 
                        else if (intentToProcess.action == "OPEN_LISTING") "listing"
                        else null
                    val chatId = intentToProcess.getStringExtra("chatId")

                    android.util.Log.d("NAV", "Processing intent: type=$type, chatId=$chatId, action=${intentToProcess.action}")

                    if (type == "chat" && chatId != null) {
                        // Consume intent immediately to prevent double processing
                        intentFlow.value = null
                        
                        FirebaseFirestore.getInstance().collection("chats").document(chatId)
                            .get().addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val data = doc.data ?: return@addOnSuccessListener
                                    val isBuying = data["buyerId"] == userId

                                    val updatedData = data.toMutableMap()
                                    val lid = data["listingId"]?.toString() ?: ""
                                    updatedData["id"] = lid
                                    updatedData["posterName"] = if (isBuying) data["sellerName"]
                                        ?: "Seller" else data["buyerName"] ?: "User"
                                    updatedData["userId"] = if (isBuying) data["sellerId"]
                                        ?: "" else data["buyerId"] ?: ""
                                    updatedData["title"] = data["listingTitle"] ?: ""
                                    updatedData["price"] = data["listingPrice"] ?: ""
                                    updatedData["location"] = data["listingLocation"] ?: ""
                                    val img = data["listingImage"]?.toString() ?: ""
                                    if (img.isNotEmpty()) {
                                        updatedData["images"] = listOf(img)
                                    }

                                    // Pre-fetch detailed info if possible but don't overwrite screen
                                    val sellerId = updatedData["userId"].toString()
                                    if (sellerId.isNotEmpty() && lid.isNotEmpty()) {
                                        // Check if listing exists first
                                        FirebaseFirestore.getInstance().collection("listings").document(lid).get()
                                            .addOnSuccessListener { listingDoc ->
                                                if (listingDoc.exists()) {
                                                    FirebaseFirestore.getInstance().collection("users").document(sellerId).get()
                                                        .addOnSuccessListener { sellerDoc ->
                                                            val showMobile = sellerDoc.getBoolean("showMobileNumber") ?: false
                                                            val joined = sellerDoc.getLong("joinedAt") ?: 0L
                                                            updatedData["sellerShowMobile"] = showMobile
                                                            updatedData["sellerJoinedAt"] = joined
                                                            
                                                            selectedListingData = updatedData
                                                            chatSourceScreen = "dashboard"
                                                            dashboardTab = 2
                                                            shouldSendInitialChatMessage = false
                                                            currentScreen = "chat"
                                                        }
                                                        .addOnFailureListener {
                                                            selectedListingData = updatedData
                                                            chatSourceScreen = "dashboard"
                                                            dashboardTab = 2
                                                            shouldSendInitialChatMessage = false
                                                            currentScreen = "chat"
                                                        }
                                                } else {
                                                    // Listing doesn't exist anymore
                                                    Toast.makeText(context, context.getString(R.string.listing_deleted_title), Toast.LENGTH_SHORT).show()
                                                    currentScreen = "dashboard"
                                                    dashboardTab = 2 // Go to chat list instead
                                                }
                                            }
                                            .addOnFailureListener {
                                                currentScreen = "dashboard"
                                                dashboardTab = 2
                                            }
                                    } else {
                                        selectedListingData = updatedData
                                        chatSourceScreen = "dashboard"
                                        dashboardTab = 2
                                        shouldSendInitialChatMessage = false
                                        currentScreen = "chat"
                                    }
                                } else {
                                    // Chat doesn't exist
                                    currentScreen = "dashboard"
                                    dashboardTab = 2
                                }
                            }
                            .addOnFailureListener {
                                currentScreen = "dashboard"
                                dashboardTab = 2
                            }
                    } else if (type == "rates") {
                        intentFlow.value = null
                        currentScreen = "aqua_rates"
                    } else if (type == "listing") {
                        intentFlow.value = null
                        currentScreen = "dashboard"
                        dashboardTab = 0
                    }
                }

                LaunchedEffect(Unit) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("FCM_TOKEN", task.result)
                        }
                    }
                    val db = FirebaseFirestore.getInstance()
                    db.collection("app_config").document("version").get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val minVersion = document.getLong("min_version_code") ?: 0L
                                val url = document.getString("update_url") ?: ""
                                if (url.isNotEmpty()) updateUrl = url

                                val remoteVersionName = document.getString("app_version")
                                if (remoteVersionName != null) appVersion = remoteVersionName

                                try {
                                    val packageInfo = context.packageManager.getPackageInfo(
                                        context.packageName,
                                        0
                                    )
                                    val currentVersion =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            packageInfo.longVersionCode
                                        } else {
                                            @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
                                        }

                                    if (currentVersion < minVersion) {
                                        needsUpdate = true
                                    }
                                } catch (e: PackageManager.NameNotFoundException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                }

                BackHandler(enabled = true) {
                    if (currentScreen == "dashboard" || currentScreen == "login" || currentScreen == "splash") {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000) {
                            finish()
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(context, context.getString(R.string.press_back_again), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        when (currentScreen) {
                            "otp" -> {
                                authViewModel.resetState()
                                currentScreen = "login"
                            }
                            "aqua_rates" -> currentScreen = "dashboard"
                            "fish_rates" -> currentScreen = "dashboard"
                            "select_category" -> currentScreen = "dashboard"
                            "edit_listing" -> {
                                currentScreen = if (isEditMode) "my_listings" else "select_category"
                            }
                            "profile" -> currentScreen = "dashboard"
                            "edit_profile" -> currentScreen = "profile"
                            "about_app" -> currentScreen = "profile"
                            "my_listings" -> currentScreen = "profile"
                            "saved_items" -> currentScreen = "profile"
                            "prawn_rates" -> currentScreen = "dashboard"
                            "detailed_page" -> {
                                if (listingBackStack.isNotEmpty()) {
                                    val previous = listingBackStack.last()
                                    listingBackStack = listingBackStack.dropLast(1)
                                    selectedListingData = previous
                                } else {
                                    currentScreen = detailedPageSource
                                }
                            }
                            "chat" -> currentScreen = chatSourceScreen
                            "chat_list" -> currentScreen = "profile"
                            "admin_dashboard" -> currentScreen = "dashboard"
                            "privacy_policy" -> {
                                currentScreen =
                                    if (authViewModel.authState.value is AuthState.Success) "profile" else "login"
                            }
                            "terms_conditions" -> {
                                currentScreen =
                                    if (authViewModel.authState.value is AuthState.Success) "profile" else "login"
                            }
                            "select_location" -> {
                                currentScreen =
                                    if (locationPickerSource == "listing") "edit_listing" else "dashboard"
                            }
                            "language_selection" -> {
                                currentScreen = if (languageSelectionSource == "profile") {
                                    "profile"
                                } else {
                                    finish()
                                    "splash" // Unreachable but needed for type
                                }
                            }
                        }
                    }
                }

                val pViewModel: PermissionViewModel = viewModel()
                permissionViewModel = pViewModel
                val pStates by pViewModel.permissionStates.collectAsState()
                val visibleRationale by pViewModel.visiblePermissionRationale.collectAsState()
                val settingsDialogType by pViewModel.showSettingsDialog.collectAsState()
                val requestTrigger by pViewModel.requestPermissionTrigger.collectAsState()

                val locationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { pViewModel.handlePermissionResult(PermissionType.LOCATION, this@MainActivity) }

                val cameraLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { pViewModel.handlePermissionResult(PermissionType.CAMERA, this@MainActivity) }

                val photosLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { pViewModel.handlePermissionResult(PermissionType.PHOTOS, this@MainActivity) }

                val notificationsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { pViewModel.handlePermissionResult(PermissionType.NOTIFICATIONS, this@MainActivity) }

                // Observe trigger to launch actual system dialogs
                LaunchedEffect(requestTrigger) {
                    requestTrigger?.let { type ->
                        when (type) {
                            PermissionType.LOCATION -> locationLauncher.launch(PermissionHelper.getPermissionsForType(type).toTypedArray())
                            PermissionType.CAMERA -> cameraLauncher.launch(PermissionHelper.getPermissionsForType(type).first())
                            PermissionType.PHOTOS -> photosLauncher.launch(PermissionHelper.getPermissionsForType(type).first())
                            PermissionType.NOTIFICATIONS -> notificationsLauncher.launch(PermissionHelper.getPermissionsForType(type).first())
                        }
                    }
                }

                // Handle Home Screen specific permissions (Location and Notifications) only after logging in
                LaunchedEffect(currentScreen, authState) {
                    val isHomeScreen = currentScreen == "dashboard" || currentScreen == "admin_dashboard"
                    val isLoggedIn = authState is AuthState.Success || authState is AuthState.RequireName
                    
                    if (isHomeScreen && isLoggedIn) {
                        pViewModel.requestFeaturePermissions(
                            permissions = listOf(PermissionType.LOCATION, PermissionType.NOTIFICATIONS),
                            skipNagging = true,
                            mustGrantAll = false
                        ) {
                            // Both handled (granted or denied). If location is granted, fetch it.
                            if (PermissionHelper.getStatus(context, PermissionType.LOCATION) == PermissionStatus.GRANTED) {
                                locationViewModel.fetchCurrentLocation()
                            }
                        }
                    } else if (currentScreen == "select_location") {
                        if (PermissionHelper.getStatus(context, PermissionType.LOCATION) == PermissionStatus.GRANTED) {
                            locationViewModel.fetchCurrentLocation()
                        }
                    }
                }

                LaunchedEffect(pStates[PermissionType.LOCATION]) {
                    if (pStates[PermissionType.LOCATION] == PermissionStatus.DENIED) {
                        locationViewModel.onPermissionDenied()
                    }
                }

                LaunchedEffect(pStates[PermissionType.LOCATION], currentScreen) {
                    if (pStates[PermissionType.LOCATION] == PermissionStatus.GRANTED) {
                        if (currentScreen == "dashboard" || currentScreen == "select_location" || currentScreen == "edit_listing") {
                            locationViewModel.fetchCurrentLocation()
                        }
                    }
                }

                LaunchedEffect(isAdmin) {
                    if (isAdmin) {
                        FirebaseMessaging.getInstance().subscribeToTopic("admins")
                            .addOnSuccessListener {
                                android.util.Log.d(
                                    "FCM",
                                    "Subscribed to admins topic"
                                )
                            }
                    } else {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("admins")
                    }
                }

                LaunchedEffect(authState) {
                    when (val state = authState) {
                        is AuthState.OtpSent -> {
                            currentScreen = "otp"
                        }

                        is AuthState.Success -> {
                            userName = state.userName
                            mobileNumber = state.mobileNumber
                            userId = state.userId
                            joinedAt = state.joinedAt
                            isAdmin = state.isAdmin

                            // Subscribe to all required topics
                            val messaging = FirebaseMessaging.getInstance()
                            val topics = listOf("all_users", "all_listings", "user_$userId")
                            topics.forEach { topic ->
                                messaging.subscribeToTopic(topic)
                                    .addOnSuccessListener { android.util.Log.d("FCM", "Subscribed to $topic") }
                                    .addOnFailureListener { e -> android.util.Log.e("FCM", "Failed to subscribe to $topic", e) }
                            }

                            // Also ensure token is up to date in Firestore
                            messaging.token.addOnSuccessListener { token ->
                                FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .update("fcmToken", token)
                            }

                            if (currentScreen == "otp" || currentScreen == "splash" || currentScreen == "login") {
                                currentScreen = if (isAdmin) "admin_dashboard" else "dashboard"
                            }
                        }

                        is AuthState.RequireName -> {
                            mobileNumber = state.phoneNumber
                            isAdmin = state.isAdmin
                            userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            
                            // Also subscribe here just in case they are stuck on onboarding
                            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            if (uid != null) {
                                val messaging = FirebaseMessaging.getInstance()
                                val topics = listOf("all_users", "all_listings", "user_$uid")
                                topics.forEach { topic ->
                                    messaging.subscribeToTopic(topic)
                                }
                            }

                            currentScreen = "dashboard"
                        }

                        is AuthState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }

                        AuthState.Idle -> {
                            if (currentScreen != "splash" && currentScreen != "login") {
                                currentScreen = "login"
                            }
                        }

                        else -> {}
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val localizedContext = remember(selectedLanguageCode) {
                        LocaleHelper.wrapContext(context, selectedLanguageCode)
                    }
                    
                    CompositionLocalProvider(
                        LocalContext provides localizedContext,
                        LocalConfiguration provides localizedContext.resources.configuration,
                        LocalActivityResultRegistryOwner provides this@MainActivity
                    ) {
                        if (needsUpdate) {
                            ForceUpdateScreen(updateUrl = updateUrl)
                        } else {
                            when (currentScreen) {
                                "splash" -> SplashScreen(onTimeout = {
                                    if (!isLanguageSelected) {
                                        languageSelectionSource = "splash"
                                        currentScreen = "language_selection"
                                    } else if (authState is AuthState.Success) {
                                        currentScreen = "dashboard"
                                    } else if (authState is AuthState.Idle) {
                                        currentScreen = "login"
                                    }
                                })

                                "language_selection" -> LanguageSelectionScreen(
                                    onLanguageSelected = {
                                        selectedLanguageCode = LocaleHelper.getSelectedLanguage(context)
                                        if (languageSelectionSource == "profile") {
                                            currentScreen = "profile"
                                        } else if (authState is AuthState.Success) {
                                            currentScreen = "dashboard"
                                        } else {
                                            currentScreen = "login"
                                        }
                                    }, onBackClick = if (languageSelectionSource == "profile") {
                                        { currentScreen = "profile" }
                                    } else null)

                            "login" -> {
                                // Clear any stale verification data when entering login screen
                                DisposableEffect(Unit) {
                                    authViewModel.clearVerificationData()
                                    onDispose {}
                                }

                                LoginScreen(onSendOtp = { number ->
                                    if (networkStatus != NetworkObserver.Status.Available) {
                                        showNoInternetDialog = true
                                        return@LoginScreen
                                    }
                                    mobileNumber = number
                                    authViewModel.sendOtp(number, this@MainActivity)
                                }, onPrivacyPolicyClick = {
                                    currentScreen = "privacy_policy"
                                }, onTermsClick = {
                                    currentScreen = "terms_conditions"
                                }, isLoading = authState is AuthState.Loading)
                            }

                            "otp" -> OtpScreen(mobileNumber = mobileNumber, onVerifyClick = { otp ->
                                if (networkStatus != NetworkObserver.Status.Available) {
                                    showNoInternetDialog = true
                                    return@OtpScreen
                                }
                                authViewModel.verifyOtp(otp)
                            }, onResendClick = {
                                if (networkStatus != NetworkObserver.Status.Available) {
                                    showNoInternetDialog = true
                                    return@OtpScreen
                                }
                                authViewModel.resendOtp(mobileNumber)
                            }, onBackClick = {
                                authViewModel.resetState()
                                currentScreen = "login"
                            }, isLoading = authState is AuthState.Loading)

                            "dashboard" -> DashboardScreen(
                                currentUserId = userId,
                                onAddClick = {
                                    isEditMode = false
                                    selectedListingId = null
                                    pickedListingLocation = null
                                    currentScreen = "select_category"
                                },
                                onProfileClick = { currentScreen = "profile" },
                                onLocationClick = {
                                    pViewModel.requestFeaturePermissions(listOf(PermissionType.LOCATION)) {
                                        locationPickerSource = "dashboard"
                                        currentScreen = "select_location"
                                    }
                                },
                                onPrawnsClick = { currentScreen = "prawn_rates" },
                                onFishRatesClick = { currentScreen = "fish_rates" },
                                onItemClick = { data ->
                                    navigateToDetailedPage(data, "dashboard")
                                },
                                onChatListClick = { data ->
                                    val sellerId = data["sellerId"]?.toString() ?: ""
                                    val buyerId = data["buyerId"]?.toString() ?: ""
                                    val isBuying = if (sellerId.isNotEmpty()) sellerId != userId else buyerId == userId
                                    
                                    val updatedData = data.toMutableMap()
                                    updatedData["id"] = data["listingId"] ?: ""
                                    updatedData["posterName"] = if (isBuying) data["sellerName"]
                                        ?: "Seller" else data["buyerName"] ?: "User"
                                    updatedData["userId"] =
                                        if (isBuying) data["sellerId"] ?: "" else data["buyerId"]
                                            ?: ""
                                    updatedData["title"] = data["listingTitle"] ?: ""

                                    // Only set price if listingPrice is not null/empty
                                    data["listingPrice"]?.toString()?.takeIf { it.isNotBlank() }
                                        ?.let {
                                            updatedData["price"] = it
                                        }

                                    updatedData["location"] = data["listingLocation"] ?: ""
                                    val img = data["listingImage"]?.toString() ?: ""
                                    if (img.isNotEmpty()) {
                                        updatedData["images"] = listOf(img)
                                    }

                                    selectedListingData = updatedData
                                    chatSourceScreen = "dashboard"
                                    shouldSendInitialChatMessage = false
                                    currentScreen = "chat"
                                },
                                onLocationFetched = { name, sub ->
                                    currentLocationName = name
                                    currentSubLocation = sub
                                },
                                showNameSheetInitial = authState is AuthState.RequireName,
                                onNameSave = { name ->
                                    authViewModel.saveUserName(name)
                                },
                                onNameSkip = {
                                    authViewModel.skipOnboarding()
                                },
                                initialTab = dashboardTab,
                                onTabChange = { dashboardTab = it },
                                locationViewModel = locationViewModel
                            )

                            "detailed_page" -> selectedListingData?.let { data ->
                                DetailedPageScreen(
                                    listingData = data,
                                    currentUserId = userId,
                                    onBackClick = { 
                                        if (listingBackStack.isNotEmpty()) {
                                            val previous = listingBackStack.last()
                                            listingBackStack = listingBackStack.dropLast(1)
                                            selectedListingData = previous
                                        } else {
                                            currentScreen = detailedPageSource 
                                        }
                                    },
                                    onChatClick = { updatedData ->
                                        selectedListingData = updatedData
                                        chatSourceScreen = "detailed_page"
                                        shouldSendInitialChatMessage = true
                                        currentScreen = "chat"
                                    },
                                    onItemClick = { newData ->
                                        selectedListingData?.let { current ->
                                            listingBackStack = listingBackStack + current
                                        }
                                        navigateToDetailedPage(newData, "detailed_page")
                                    }
                                )
                            }

                            "chat" -> selectedListingData?.let { data ->
                                val sId = data["userId"]?.toString() ?: data["posterId"]?.toString() ?: ""
                                val lId = data["id"]?.toString() ?: data["listingId"]?.toString() ?: ""
                                
                                ChatScreen(
                                    sellerName = data["posterName"]?.toString() ?: "Seller",
                                    sellerUserId = sId,
                                    listingId = lId,
                                    listingData = data,
                                    currentUserId = userId,
                                    currentUserName = userName,
                                    currentUserPhone = mobileNumber,
                                    currentUserLocation = currentLocationName,
                                    onBackClick = { 
                                        if (chatSourceScreen == "dashboard") {
                                            dashboardTab = 2 // Ensure we go back to Chat tab
                                        }
                                        currentScreen = chatSourceScreen 
                                    },
                                    onListingClick = { listing ->
                                        navigateToDetailedPage(listing, "chat")
                                    },
                                    sendInitialMessage = shouldSendInitialChatMessage
                                )
                            }

                            "chat_list" -> ChatListScreen(
                                currentUserId = userId,
                                onBackClick = { currentScreen = "profile" },
                                onChatClick = { data ->
                                    val sellerId = data["sellerId"]?.toString() ?: ""
                                    val buyerId = data["buyerId"]?.toString() ?: ""
                                    val isBuying = if (sellerId.isNotEmpty()) sellerId != userId else buyerId == userId

                                    val updatedData = data.toMutableMap()
                                    updatedData["id"] = data["listingId"] ?: ""
                                    updatedData["posterName"] = if (isBuying) data["sellerName"]
                                        ?: "Seller" else data["buyerName"] ?: "User"
                                    updatedData["userId"] =
                                        if (isBuying) data["sellerId"] ?: "" else data["buyerId"]
                                            ?: ""
                                    updatedData["title"] = data["listingTitle"] ?: ""
                                    updatedData["price"] = data["listingPrice"] ?: ""
                                    updatedData["location"] = data["listingLocation"] ?: ""
                                    val img = data["listingImage"]?.toString() ?: ""
                                    if (img.isNotEmpty()) {
                                        updatedData["images"] = listOf(img)
                                    }

                                    selectedListingData = updatedData
                                    chatSourceScreen = "chat_list"
                                    shouldSendInitialChatMessage = false
                                    currentScreen = "chat"
                                })

                            "select_location" -> SelectLocationScreen(onBackClick = {
                                currentScreen =
                                    if (locationPickerSource == "listing") "edit_listing" else "dashboard"
                            }, onLocationConfirm = { name, sub, latLng ->
                                if (locationPickerSource == "listing") {
                                    pickedListingLocation = "$name, $sub" to latLng
                                    currentScreen = "edit_listing"
                                } else {
                                    locationViewModel.setManualLocation(name, sub, latLng)
                                    currentLocationName = name
                                    currentSubLocation = sub
                                    currentScreen = "dashboard"
                                }
                            })

                            "aqua_rates" -> AquaRatesScreen(
                                onBackClick = { currentScreen = "dashboard" },
                                onFishRatesClick = { currentScreen = "fish_rates" },
                                onPrawnsClick = { currentScreen = "prawn_rates" }
                            )

                            "prawn_rates" -> PrawnRatesScreen(
                                onBackClick = { currentScreen = "dashboard" })

                            "fish_rates" -> FishRatesScreen(
                                onBackClick = { currentScreen = "dashboard" })

                            "select_category" -> SelectCategoryScreen(
                                onBackClick = {
                                    currentScreen = "dashboard"
                                },
                                onCategorySelect = { category ->
                                    if (networkStatus != NetworkObserver.Status.Available) {
                                        showNoInternetDialog = true
                                        return@SelectCategoryScreen
                                    }
                                    selectedCategory = category
                                    isEditMode = false
                                    selectedListingId = null
                                    currentScreen = "edit_listing"
                                })

                            "edit_listing" -> {
                                val fetchingText = stringResource(R.string.fetching_location)
                                val deniedText = stringResource(R.string.location_permission_denied)
                                val failedText = stringResource(R.string.failed_get_location)

                                val isActualLocation = currentLocationName != fetchingText &&
                                        currentLocationName != deniedText &&
                                        currentLocationName != failedText

                                val initialLoc = pickedListingLocation?.first
                                    ?: if (isActualLocation && currentSubLocation.isNotEmpty()) "$currentLocationName, $currentSubLocation" else currentLocationName

                                EditListingScreen(
                                    category = selectedCategory,
                                    isEditMode = isEditMode,
                                    listingId = selectedListingId,
                                    userName = userName,
                                    userMobileNumber = mobileNumber,
                                    initialLocation = initialLoc,
                                    initialLatLng = pickedListingLocation?.second ?: if (!isEditMode) deviceLatLng else null,
                                    onBackClick = {
                                        currentScreen =
                                            if (isEditMode) "my_listings" else "select_category"
                                    },
                                    onPostClick = { data ->
                                        navigateToDetailedPage(data, "dashboard")
                                    },
                                    onDeleteClick = { currentScreen = "dashboard" },
                                    onLocationChangeClick = {
                                        pViewModel.requestFeaturePermissions(listOf(PermissionType.LOCATION)) {
                                            locationViewModel.fetchCurrentLocation(force = true)
                                        }
                                    },
                                    joinedAt = joinedAt,
                                    userId = userId,
                                    showMobileNumberPreference = showMobileNumber
                                )
                            }

                            "privacy_policy" -> LegalScreen(
                                title = stringResource(R.string.privacy_policy),
                                content = stringResource(R.string.privacy_policy_content),
                                onBackClick = {
                                    currentScreen =
                                        if (authState is AuthState.Success) "profile" else "login"
                                })

                            "terms_conditions" -> LegalScreen(
                                title = stringResource(R.string.terms_conditions),
                                content = stringResource(R.string.terms_conditions_content),
                                onBackClick = {
                                    currentScreen =
                                        if (authState is AuthState.Success) "profile" else "login"
                                })

                            "profile" -> ProfileScreen(
                                userName = userName,
                                mobileNumber = mobileNumber,
                                onBackClick = { currentScreen = "dashboard" },
                                onEditClick = { currentScreen = "edit_profile" },
                                onMyListingsClick = { currentScreen = "my_listings" },
                                onSavedItemsClick = { currentScreen = "saved_items" },
                                onChatsClick = { currentScreen = "chat_list" },
                                onPrivacyPolicyClick = {
                                    currentScreen = "privacy_policy"
                                },
                                onTermsClick = {
                                    currentScreen = "terms_conditions"
                                },
                                onAboutClick = { currentScreen = "about_app" },
                                onLogoutClick = {
                                    val currentId = userId
                                    if (currentId.isNotEmpty()) {
                                        FirebaseMessaging.getInstance().unsubscribeFromTopic("user_$currentId")
                                        FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users")
                                        FirebaseMessaging.getInstance().unsubscribeFromTopic("admins")
                                    }
                                    authViewModel.logout()
                                },
                                onChangeLanguageClick = {
                                    languageSelectionSource = "profile"
                                    currentScreen = "language_selection"
                                },
                                onRateUsClick = onRateUsClick,
                                isAdmin = isAdmin,
                                onAdminClick = { currentScreen = "admin_dashboard" },
                                initialShowMobileNumber = showMobileNumber,
                                onPrivacyToggle = { authViewModel.updatePrivacyPreference(it) }
                            )

                            "about_app" -> AboutAppScreen(
                                versionName = appVersion,
                                onBackClick = { currentScreen = "profile" })

                            "saved_items" -> SavedItemsScreen(onBackClick = {
                                currentScreen = "profile"
                            }, onItemClick = { data ->
                                navigateToDetailedPage(data, "saved_items")
                            })

                            "edit_profile" -> EditProfileScreen(
                                currentName = userName,
                                currentPhone = mobileNumber,
                                onBackClick = { currentScreen = "profile" },
                                onProfileUpdated = { newName ->
                                    if (networkStatus != NetworkObserver.Status.Available) {
                                        showNoInternetDialog = true
                                        return@EditProfileScreen
                                    }
                                    userName = newName
                                })

                            "my_listings" -> MyListingsScreen(
                                onBackClick = {
                                    currentScreen = "profile"
                                },
                                onEditClick = { listingId, categoryStr ->
                                    selectedListingId = listingId
                                    isEditMode = true
                                    pickedListingLocation = null
                                    selectedCategory = try {
                                        ListingCategory.valueOf(categoryStr.uppercase().trim())
                                    } catch (_: Exception) {
                                        ListingCategory.FISH
                                    }
                                    currentScreen = "edit_listing"
                                })

                            "admin_dashboard" -> AdminDashboardScreen(
                                onBackClick = { currentScreen = "dashboard" })
                        }
                    }

                    if (isNavigatingToDetailedPage) {
                        LoadingOverlay(stringResource(R.string.fetching_details))
                    }

                    if (authState is AuthState.Loading) {
                        LoadingOverlay(stringResource(R.string.signing_in))
                    }

                    if (showNoInternetDialog) {
                        AlertDialog(
                            onDismissRequest = { showNoInternetDialog = false },
                            title = { Text(stringResource(R.string.no_internet_title)) },
                            text = { Text(stringResource(R.string.no_internet_desc)) },
                            confirmButton = {
                                TextButton(onClick = { showNoInternetDialog = false }) {
                                    Text(stringResource(R.string.ok))
                                }
                            }
                        )
                    }

                    visibleRationale?.let { type ->
                        PermissionRationaleDialog(
                            type = type,
                            onConfirm = { pViewModel.onRationaleConfirm() },
                            onDismiss = { pViewModel.onRationaleDismiss() }
                        )
                    }

                    settingsDialogType?.let { type ->
                        SettingsRedirectDialog(
                            type = type,
                            onConfirm = {
                                pViewModel.dismissSettingsDialog()
                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = android.net.Uri.fromParts("package", packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                            },
                            onDismiss = { pViewModel.dismissSettingsDialog() }
                        )
                    }
                    }
                }
            }
        }
    }
}
