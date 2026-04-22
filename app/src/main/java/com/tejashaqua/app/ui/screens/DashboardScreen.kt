package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tejashaqua.app.ui.LocationSearchViewModel
import com.tejashaqua.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    locationName: String,
    subLocation: String,
    userName: String,
    onSeeAllRatesClick: () -> Unit,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLocationClick: () -> Unit,
    onLocationFetched: (String, String) -> Unit,
    showNameSheetInitial: Boolean,
    onNameSave: (String) -> Unit,
    onNameSkip: () -> Unit,
    locationViewModel: LocationSearchViewModel = viewModel()
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var productSearchText by remember { mutableStateOf("") }

    val fetchedName by locationViewModel.currentLocationName.collectAsState()
    val fetchedSub by locationViewModel.currentSubLocation.collectAsState()

    LaunchedEffect(Unit) {
        if (locationName == "Fetching location...") {
            locationViewModel.fetchCurrentLocation()
        }
    }

    LaunchedEffect(fetchedName, fetchedSub) {
        if (locationName == "Fetching location..." && 
            fetchedName != "Fetching location..." && 
            fetchedName != "Location not found" &&
            fetchedName != "Failed to get location") {
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
                    icon = { Icon(Icons.Default.Home, "Home") },
                    selected = selectedItem == 0,
                    onClick = { selectedItem = 0 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AquaBlue, unselectedIconColor = GrayText)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Work, "Jobs") },
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
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(30.dp))
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Article, "News") },
                    selected = selectedItem == 2,
                    onClick = { selectedItem = 2 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AquaBlue, unselectedIconColor = GrayText)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profile") },
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
                item { AquaRatesSection(onSeeAllRatesClick) }
                item { MarketplaceSection() }
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
                            Text("Welcome to Tejash Aqua! 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            IconButton(onClick = { 
                                showWelcomeSheet = false
                                onNameSkip()
                            }) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Set your name so buyers & sellers can identify you. This helps build trust in the marketplace.", fontSize = 14.sp, color = GrayText, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedTextField(value = tempName, onValueChange = { tempName = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Enter your name", color = Color.Gray) }, leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color.Black) }, shape = RoundedCornerShape(12.dp), singleLine = true)
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
                                Text("Skip for Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
                                Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
fun SearchHeader(
    productSearchText: String,
    onProductSearchChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.verticalGradient(colors = listOf(AquaLightBlue, MaterialTheme.colorScheme.background)))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextField(
            value = productSearchText,
            onValueChange = onProductSearchChange,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            placeholder = { Text("Search for fish, prawns and jobs...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(25.dp),
            singleLine = true
        )
    }
}

@Composable
fun MarketplaceSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fresh in Marketplace", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkBlueText)
            Spacer(modifier = Modifier.weight(1f))
            Text("See All →", color = AquaBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            MarketItem("Vannamei PL10 - Growth Line...", "₹180/kg", "Prawn Hatchery", "Hot", Modifier.weight(1f))
            MarketItem("Fresh Katla - 2.5kg avg", "₹450/kg", "Fish", "New", Modifier.weight(1f))
        }
    }
}

@Composable
fun MarketItem(title: String, price: String, category: String, tag: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        color = Color.White
    ) {
        Column {
            Box(modifier = Modifier.height(100.dp).fillMaxWidth().background(Color.LightGray)) {
                Surface(modifier = Modifier.padding(8.dp).align(Alignment.TopStart), color = Color.White, shape = RoundedCornerShape(4.dp)) {
                    Text(if(tag=="Hot") "🔥 Hot" else "⭐ New", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Default.FavoriteBorder, null, tint = Color.Black) }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Surface(color = Color(0xFFE8EAF6), shape = RoundedCornerShape(4.dp)) { Text(category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = AquaBlue, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GrayText, modifier = Modifier.size(10.dp))
                    Text("Nellore • 4.5 km", color = GrayText, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun FooterSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        val footerColor = if(androidx.compose.foundation.isSystemInDarkTheme()) Color.DarkGray else Color(0xFFD1D9E6)
        Text("India's most trusted #1 aqua marketplace app.", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = footerColor, lineHeight = 38.sp)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AquaRatesSection(onSeeAllClick: () -> Unit) {
    val currentDate = remember {
        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Today's Aqua Rates", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkBlueText)
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
                    Text("LIVE", color = LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 16.dp)) {
            item { RateCard("Prawns", "₹280-1000", "View All Prices", Color(0xFFE3F2FD), Icons.Default.Opacity) }
            item { RateCard("Rohu", "₹160/kg", "+₹5 (3%)", Color(0xFFFFF3E0), Icons.Default.Info) }
            item { RateCard("Tilapia", "₹120/kg", "No Change", Color(0xFFF3E5F5), Icons.Default.TrendingFlat) }
            item { RateCard("Katla", "₹180/kg", "+₹10 (5.9%)", Color(0xFFE0F2F1), Icons.Default.Info) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onSeeAllClick, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = AquaBlue), shape = RoundedCornerShape(12.dp)) {
            Text("See All Rates →", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RateCard(name: String, price: String, status: String, bgColor: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = Modifier.width(110.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(bgColor, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(name, fontSize = 12.sp, color = GrayText)
            Text(price, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            Text(status, fontSize = 10.sp, color = if (status.contains("+")) Color.Red else AquaBlue)
        }
    }
}
