package com.tejashaqua.app

import android.Manifest
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.LatLng
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.ui.viewmodel.AuthState
import com.tejashaqua.app.ui.viewmodel.AuthViewModel
import com.tejashaqua.app.ui.viewmodel.LocationSearchViewModel
import com.tejashaqua.app.ui.screens.*
import com.tejashaqua.app.ui.theme.TejashAquaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TejashAquaTheme {
                val authViewModel: AuthViewModel = viewModel()
                val locationViewModel: LocationSearchViewModel = viewModel()
                val authState by authViewModel.authState.collectAsState()
                val context = LocalContext.current

                var currentScreen by remember { mutableStateOf("splash") }
                var mobileNumber by remember { mutableStateOf("") }
                var userName by remember { mutableStateOf("User") }
                var userId by remember { mutableStateOf("") }
                var joinedAt by remember { mutableLongStateOf(0L) }
                
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
                        "my_listings" -> currentScreen = "profile"
                        "detailed_page" -> currentScreen = "dashboard"
                        "chat" -> currentScreen = chatSourceScreen
                        "chat_list" -> currentScreen = "profile"
                        "select_location" -> {
                            currentScreen = if (locationPickerSource == "listing") "edit_listing" else "dashboard"
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
                            if (currentScreen == "otp" || currentScreen == "splash" || currentScreen == "login") {
                                currentScreen = "dashboard"
                            }
                        }
                        is AuthState.RequireName -> {
                            mobileNumber = state.phoneNumber
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
                    when (currentScreen) {
                        "splash" -> SplashScreen(onTimeout = { 
                            if (authState is AuthState.Success) {
                                currentScreen = "dashboard"
                            } else if (authState is AuthState.Idle) {
                                currentScreen = "login"
                            }
                        })
                        "login" -> LoginScreen(onSendOtp = { number ->
                            mobileNumber = number
                            authViewModel.sendOtp(number, this@MainActivity)
                        })
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
                            locationName = currentLocationName,
                            subLocation = currentSubLocation,
                            userName = userName,
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
                            onItemClick = { data ->
                                selectedListingData = data
                                currentScreen = "detailed_page"
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
                                onBackClick = { currentScreen = "dashboard" },
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
                        "profile" -> ProfileScreen(
                            userName = userName,
                            mobileNumber = mobileNumber,
                            onBackClick = { currentScreen = "dashboard" },
                            onEditClick = { currentScreen = "edit_profile" },
                            onMyListingsClick = { currentScreen = "my_listings" },
                            onChatsClick = { currentScreen = "chat_list" },
                            onLogoutClick = { 
                                authViewModel.logout()
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
                    }

                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
