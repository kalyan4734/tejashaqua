package com.tejashaqua.app

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tejashaqua.app.ui.AuthState
import com.tejashaqua.app.ui.AuthViewModel
import com.tejashaqua.app.ui.screens.*
import com.tejashaqua.app.ui.theme.TejashAquaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TejashAquaTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.authState.collectAsState()
                val context = LocalContext.current

                var currentScreen by remember { mutableStateOf("splash") }
                var mobileNumber by remember { mutableStateOf("") }
                var userName by remember { mutableStateOf("User") } // Default dummy name
                var selectedCategory by remember { mutableStateOf(ListingCategory.FISH) }
                var isEditMode by remember { mutableStateOf(false) }
                
                var currentLocationName by remember { mutableStateOf("Fetching location...") }
                var currentSubLocation by remember { mutableStateOf("") }

                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(currentScreen) {
                    if (currentScreen == "dashboard") {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                LaunchedEffect(authState) {
                    when (authState) {
                        is AuthState.OtpSent -> {
                            currentScreen = "otp"
                        }
                        is AuthState.Success -> {
                            userName = (authState as AuthState.Success).userName
                            currentScreen = "dashboard"
                        }
                        is AuthState.RequireName -> {
                            currentScreen = "dashboard" // Show dashboard, which triggers name sheet
                        }
                        is AuthState.Error -> {
                            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        "splash" -> SplashScreen(onTimeout = { currentScreen = "login" })
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
                            }
                        )
                        "dashboard" -> DashboardScreen(
                            locationName = currentLocationName,
                            subLocation = currentSubLocation,
                            userName = userName,
                            onSeeAllRatesClick = { currentScreen = "aqua_rates" },
                            onAddClick = { 
                                isEditMode = false
                                currentScreen = "select_category" 
                            },
                            onProfileClick = { currentScreen = "profile" },
                            onLocationClick = { currentScreen = "select_location" },
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
                            }
                        )
                        "select_location" -> SelectLocationScreen(
                            onBackClick = { currentScreen = "dashboard" },
                            onLocationConfirm = { name, sub ->
                                currentLocationName = name
                                currentSubLocation = sub
                                currentScreen = "dashboard"
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
                                currentScreen = "edit_listing"
                            }
                        )
                        "edit_listing" -> EditListingScreen(
                            category = selectedCategory,
                            isEditMode = isEditMode,
                            onBackClick = { 
                                if (isEditMode) currentScreen = "dashboard" else currentScreen = "select_category" 
                            },
                            onPostClick = { currentScreen = "dashboard" },
                            onDeleteClick = { currentScreen = "dashboard" }
                        )
                        "profile" -> ProfileScreen(
                            userName = userName,
                            mobileNumber = mobileNumber,
                            onBackClick = { currentScreen = "dashboard" },
                            onLogoutClick = { 
                                authViewModel.resetState()
                                currentScreen = "login" 
                            }
                        )
                    }

                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}
