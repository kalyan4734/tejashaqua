package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.tejashaqua.app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.ui.components.LoadingOverlay
import com.tejashaqua.app.ui.components.MarketItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedItemsScreen(
    onBackClick: () -> Unit,
    onItemClick: (Map<String, Any>) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var savedItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                .collection("favorites")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        savedItems = snapshot.documents.map { doc ->
                            val data = doc.data?.toMutableMap() ?: mutableMapOf()
                            data["id"] = doc.id
                            data
                        }
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.saved_items_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (savedItems.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.no_saved_items), color = GrayText)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedItems) { data ->
                        val listingId = data["id"]?.toString() ?: ""
                        val images = (data["images"] as? List<*>)?.filterIsInstance<String>()
                        
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
                            title = data["title"]?.toString()?.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_title),
                            price = priceLabel,
                            category = categoryStr,
                            location = data["location"]?.toString() ?: stringResource(R.string.unknown_location),
                            posterName = data["posterName"]?.toString() ?: stringResource(R.string.user_label),
                            imageUrl = images?.firstOrNull(),
                            isFavorited = true,
                            onFavoriteClick = {
                                if (currentUserId != null && listingId.isNotEmpty()) {
                                    db.collection("users").document(currentUserId)
                                        .collection("favorites").document(listingId)
                                        .delete()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { 
                                keyboardController?.hide()
                                onItemClick(data) 
                            }
                        )
                    }
                }
            }
            
            if (isLoading) {
                LoadingOverlay(stringResource(R.string.loading_saved_items))
            }
        }
    }
}
