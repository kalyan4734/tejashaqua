package com.tejashaqua.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.ui.components.LoadingOverlay
import com.tejashaqua.app.ui.viewmodel.AuthState
import com.tejashaqua.app.ui.viewmodel.AuthViewModel
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.ui.screens.*
import com.tejashaqua.app.ui.theme.TejashAquaTheme
import com.tejashaqua.app.utils.LocaleHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        val lang = LocaleHelper.getSelectedLanguage(this) ?: "en"
        LocaleHelper.updateContextLocale(this, lang)
        enableEdgeToEdge()
        setContent {
            TejashAquaTheme {
                val authViewModel: AuthViewModel = viewModel()
                val locationViewModel: LocationSearchViewModel = viewModel()
                val authState by authViewModel.authState.collectAsState()
                val context = LocalContext.current

                var appVersion by remember { mutableStateOf("1.0.0") }
                var needsUpdate by remember { mutableStateOf(false) }
                var updateUrl by remember { mutableStateOf("https://play.google.com/store/apps/details?id=com.tejashaqua.app") }

                var currentScreen by remember { mutableStateOf("splash") }
                var languageSelectionSource by remember { mutableStateOf("splash") }
                var mobileNumber by remember { mutableStateOf("") }
                var userName by remember { mutableStateOf("User") }
                var userId by remember { mutableStateOf("") }
                var joinedAt by remember { mutableLongStateOf(0L) }
                var isAdmin by remember { mutableStateOf(false) }

                val isLanguageSelected = remember { mutableStateOf(LocaleHelper.getSelectedLanguage(context) != null) }

                LaunchedEffect(Unit) {
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
                                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                    val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        packageInfo.longVersionCode
                                    } else {
                                        @Suppress("DEPRECATION")
                                        packageInfo.versionCode.toLong()
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
                
                var selectedCategory by remember { mutableStateOf(ListingCategory.FISH) }
                var isEditMode by remember { mutableStateOf(false) }
                var selectedListingId by remember { mutableStateOf<String?>(null) }
                var selectedListingData by remember { mutableStateOf<Map<String, Any>?>(null) }
                var chatSourceScreen by remember { mutableStateOf("detailed_page") }
                var shouldSendInitialChatMessage by remember { mutableStateOf(false) }
                
                val fetchingLocText = stringResource(R.string.fetching_location)
                var currentLocationName by remember { mutableStateOf(fetchingLocText) }
                var currentSubLocation by remember { mutableStateOf("") }
                
                // Track where the location picker was opened from
                var locationPickerSource by remember { mutableStateOf("dashboard") } 
                var pickedListingLocation by remember { mutableStateOf<Pair<String, LatLng?>?>(null) }

                BackHandler(enabled = currentScreen != "dashboard" && currentScreen != "login" && currentScreen != "splash") {
                    when (currentScreen) {
                        "otp" -> {
                            authViewModel.resetState()
                            currentScreen = "login"
                        }
                        "aqua_rates" -> currentScreen = "dashboard"
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
                        "detailed_page" -> currentScreen = "dashboard"
                        "chat" -> currentScreen = chatSourceScreen
                        "chat_list" -> currentScreen = "profile"
                        "admin_dashboard" -> currentScreen = "dashboard"
                        "privacy_policy" -> {
                            currentScreen = if (authViewModel.authState.value is AuthState.Success) "profile" else "login"
                        }
                        "terms_conditions" -> {
                            currentScreen = if (authViewModel.authState.value is AuthState.Success) "profile" else "login"
                        }
                        "select_location" -> {
                            currentScreen = if (locationPickerSource == "listing") "edit_listing" else "dashboard"
                        }
                        "language_selection" -> {
                            if (languageSelectionSource == "profile") {
                                currentScreen = "profile"
                            } else {
                                // Close the app if it's the first time language selection
                                finish()
                            }
                        }
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val isGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
                    if (isGranted) {
                        locationViewModel.fetchCurrentLocation()
                    }
                }

                LaunchedEffect(currentScreen) {
                    when (currentScreen) {
                        "dashboard" -> {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.CAMERA
                                )
                            )
                        }
                        "otp" -> {
                            // SMS permissions removed as we moved to SmsRetriever API
                        }
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
                            
                            if (currentScreen == "otp" || currentScreen == "splash" || currentScreen == "login") {
                                currentScreen = if (isAdmin) "admin_dashboard" else "dashboard"
                            }
                        }
                        is AuthState.RequireName -> {
                            mobileNumber = state.phoneNumber
                            isAdmin = state.isAdmin
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
                    if (needsUpdate) {
                        ForceUpdateScreen(updateUrl = updateUrl)
                    } else {
                        when (currentScreen) {
                            "splash" -> SplashScreen(onTimeout = { 
                                if (!isLanguageSelected.value) {
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
                                    isLanguageSelected.value = true
                                    if (languageSelectionSource == "profile") {
                                        currentScreen = "profile"
                                    } else if (authState is AuthState.Success) {
                                        currentScreen = "dashboard"
                                    } else {
                                        currentScreen = "login"
                                    }
                                },
                                onBackClick = if (languageSelectionSource == "profile") {
                                    { currentScreen = "profile" }
                                } else null
                            )
                        "login" -> LoginScreen(
                            onSendOtp = { number ->
                                mobileNumber = number
                                authViewModel.sendOtp(number, this@MainActivity)
                            },
                            onPrivacyPolicyClick = {
                                currentScreen = "privacy_policy"
                            },
                            onTermsClick = {
                                currentScreen = "terms_conditions"
                            }
                        )
                        "otp" -> OtpScreen(
                            mobileNumber = mobileNumber,
                            onVerifyClick = { otp ->
                                authViewModel.verifyOtp(otp)
                            },
                            onResendClick = {
                                authViewModel.sendOtp(mobileNumber, this@MainActivity)
                            },
                            onBackClick = {
                                authViewModel.resetState()
                                currentScreen = "login"
                            }
                        )
                        "dashboard" -> DashboardScreen(
                            currentUserId = userId,
                            locationName = currentLocationName,
                            subLocation = currentSubLocation,
                            onSeeAllRatesClick = { currentScreen = "aqua_rates" },
                            onAddClick = { 
                                isEditMode = false
                                selectedListingId = null
                                pickedListingLocation = null
                                currentScreen = "select_category" 
                            },
                            onProfileClick = { currentScreen = "profile" },
                            onLocationClick = { 
                                locationPickerSource = "dashboard"
                                currentScreen = "select_location" 
                            },
                            onPrawnsClick = { currentScreen = "prawn_rates" },
                            onItemClick = { data ->
                                selectedListingData = data
                                chatSourceScreen = "dashboard"
                                currentScreen = "detailed_page"
                            },
                            onChatListClick = { data ->
                                val isBuying = data["buyerId"] == userId
                                val updatedData = data.toMutableMap()
                                updatedData["id"] = data["listingId"] ?: ""
                                updatedData["posterName"] = if (isBuying) data["sellerName"] ?: "Seller" else data["buyerName"] ?: "User"
                                updatedData["userId"] = if (isBuying) data["sellerId"] ?: "" else data["buyerId"] ?: ""
                                updatedData["title"] = data["listingTitle"] ?: ""
                                updatedData["price"] = data["listingPrice"] ?: ""
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
                            locationViewModel = locationViewModel
                        )
                        "detailed_page" -> selectedListingData?.let { data ->
                            DetailedPageScreen(
                                listingData = data,
                                currentUserId = userId,
                                onBackClick = { currentScreen = chatSourceScreen },
                                onChatClick = { updatedData ->
                                    selectedListingData = updatedData
                                    chatSourceScreen = "detailed_page"
                                    shouldSendInitialChatMessage = true
                                    currentScreen = "chat"
                                }
                            )
                        }
                        "chat" -> selectedListingData?.let { data ->
                            ChatScreen(
                                sellerName = data["posterName"]?.toString() ?: "Seller",
                                sellerUserId = data["userId"]?.toString() ?: "",
                                listingId = data["id"]?.toString() ?: "",
                                listingData = data,
                                currentUserId = userId,
                                currentUserName = userName,
                                currentUserPhone = mobileNumber,
                                currentUserLocation = currentLocationName,
                                onBackClick = { currentScreen = chatSourceScreen },
                                sendInitialMessage = shouldSendInitialChatMessage
                            )
                        }
                        "chat_list" -> ChatListScreen(
                            currentUserId = userId,
                            onBackClick = { currentScreen = "profile" },
                            onChatClick = { data ->
                                val isBuying = data["buyerId"] == userId
                                val updatedData = data.toMutableMap()
                                updatedData["id"] = data["listingId"] ?: ""
                                updatedData["posterName"] = if (isBuying) data["sellerName"] ?: "Seller" else data["buyerName"] ?: "User"
                                updatedData["userId"] = if (isBuying) data["sellerId"] ?: "" else data["buyerId"] ?: ""
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
                            }
                        )
                        "select_location" -> SelectLocationScreen(
                            onBackClick = { 
                                currentScreen = if (locationPickerSource == "listing") "edit_listing" else "dashboard" 
                            },
                            onLocationConfirm = { name, sub, latLng ->
                                if (locationPickerSource == "listing") {
                                    pickedListingLocation = "$name, $sub" to latLng
                                    currentScreen = "edit_listing"
                                } else {
                                    currentLocationName = name
                                    currentSubLocation = sub
                                    currentScreen = "dashboard"
                                }
                            }
                        )
                        "aqua_rates" -> AquaRatesScreen(
                            onBackClick = { currentScreen = "dashboard" }
                        )
                        "prawn_rates" -> PrawnRatesScreen(
                            onBackClick = { currentScreen = "dashboard" }
                        )
                        "select_category" -> SelectCategoryScreen(
                            onBackClick = { currentScreen = "dashboard" },
                            onCategorySelect = { category ->
                                selectedCategory = category
                                isEditMode = false
                                selectedListingId = null
                                currentScreen = "edit_listing"
                            }
                        )
                        "edit_listing" -> EditListingScreen(
                            category = selectedCategory,
                            isEditMode = isEditMode,
                            listingId = selectedListingId,
                            userName = userName,
                            userMobileNumber = mobileNumber,
                            initialLocation = pickedListingLocation?.first ?: if (currentSubLocation.isNotEmpty()) "$currentLocationName, $currentSubLocation" else currentLocationName,
                            initialLatLng = pickedListingLocation?.second,
                            onBackClick = {
                                currentScreen = if (isEditMode) "my_listings" else "select_category"
                            },
                            onPostClick = { currentScreen = "dashboard" },
                            onDeleteClick = { currentScreen = "dashboard" },
                            onLocationChangeClick = {
                                locationPickerSource = "listing"
                                currentScreen = "select_location"
                            },
                            joinedAt = joinedAt,
                            userId = userId
                        )
                        "privacy_policy" -> LegalScreen(
                            title = stringResource(R.string.privacy_policy),
                            content = LegalConstants.PRIVACY_POLICY,
                            onBackClick = {
                                currentScreen = if (authState is AuthState.Success) "profile" else "login"
                            }
                        )
                        "terms_conditions" -> LegalScreen(
                            title = stringResource(R.string.terms_conditions),
                            content = LegalConstants.TERMS_CONDITIONS,
                            onBackClick = {
                                currentScreen = if (authState is AuthState.Success) "profile" else "login"
                            }
                        )
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
                                authViewModel.logout()
                            },
                            onChangeLanguageClick = {
                                languageSelectionSource = "profile"
                                currentScreen = "language_selection"
                            },
                            isAdmin = isAdmin,
                            onAdminClick = { currentScreen = "admin_dashboard" }
                        )
                        "about_app" -> AboutAppScreen(
                            versionName = appVersion,
                            onBackClick = { currentScreen = "profile" }
                        )
                        "saved_items" -> SavedItemsScreen(
                            onBackClick = { currentScreen = "profile" },
                            onItemClick = { data ->
                                selectedListingData = data
                                chatSourceScreen = "saved_items"
                                currentScreen = "detailed_page"
                            }
                        )
                        "edit_profile" -> EditProfileScreen(
                            currentName = userName,
                            currentPhone = mobileNumber,
                            onBackClick = { currentScreen = "profile" },
                            onProfileUpdated = { newName ->
                                userName = newName
                            }
                        )
                        "my_listings" -> MyListingsScreen(
                            onBackClick = { currentScreen = "profile" },
                            onEditClick = { listingId, categoryStr ->
                                selectedListingId = listingId
                                isEditMode = true
                                pickedListingLocation = null
                                selectedCategory = try {
                                    ListingCategory.valueOf(categoryStr)
                                } catch (_: Exception) {
                                    ListingCategory.FISH
                                }
                                currentScreen = "edit_listing"
                            }
                        )
                        "admin_dashboard" -> AdminDashboardScreen(
                            onBackClick = { currentScreen = "dashboard" }
                        )
                    }

                    if (authState is AuthState.Loading) {
                        LoadingOverlay("Signing you in...")
                    }
                }
            }
        }
    }
}
}
