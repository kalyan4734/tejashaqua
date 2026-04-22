package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tejashaqua.app.ui.LocationSearchViewModel
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectLocationScreen(
    onBackClick: () -> Unit,
    onLocationConfirm: (String, String) -> Unit,
    locationViewModel: LocationSearchViewModel = viewModel()
) {
    var searchText by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedLocation by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    val searchResults by locationViewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AquaBlue)) {
                CenterAlignedTopAppBar(
                    title = { Text("Select Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
                )
                
                TextField(
                    value = searchText,
                    onValueChange = { 
                        searchText = it
                        locationViewModel.searchLocation(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(54.dp),
                    placeholder = { Text("Search city, area, or district...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { 
                        selectedLocation?.let { (loc, sub) ->
                            onLocationConfirm(loc, sub)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
                    enabled = selectedLocation != null
                ) {
                    Text("Confirm Location", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = AquaBlue,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        Box(
                            Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .height(3.dp)
                                .background(color = AquaBlue)
                        )
                    }
                },
                divider = {} // Remove default divider for a cleaner look
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Search", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Map", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults) { prediction ->
                        val primaryText = prediction.getPrimaryText(null).toString()
                        val secondaryText = prediction.getSecondaryText(null).toString()
                        val isSelected = selectedLocation?.first == primaryText
                        
                        LocationSearchItem(
                            location = primaryText,
                            subLocation = secondaryText,
                            isSelected = isSelected,
                            onClick = {
                                selectedLocation = primaryText to secondaryText
                            }
                        )
                    }
                }
            } else {
                // Map placeholder
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Map view coming soon", color = GrayText)
                }
            }
        }
    }
}

@Composable
fun LocationSearchItem(
    location: String,
    subLocation: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isSelected) Color(0xFFF0F7FF) else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn, 
                contentDescription = null, 
                tint = if (isSelected) AquaBlue else Color(0xFF00639B), 
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = location, 
                    fontWeight = FontWeight.SemiBold, 
                    fontSize = 16.sp, 
                    color = Color.Black
                )
                Text(
                    text = subLocation, 
                    fontSize = 13.sp, 
                    color = GrayText
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}
