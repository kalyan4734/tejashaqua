package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
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
import com.tejashaqua.app.utils.CurrencyUtils

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
                        
                        val naText = stringResource(R.string.not_available_short)
                        val tonText = stringResource(R.string.unit_ton)
                        val acreText = stringResource(R.string.unit_acre)
                        
                        val categoryStr = data["category"]?.toString() ?: "Other"
                        val displayCategory = when(categoryStr.uppercase()) {
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

                        val priceLabel = when (categoryStr.uppercase()) {
                            "PRAWNS" -> {
                                val rate = data["rateValue"]?.toString() ?: naText
                                val formattedRate = CurrencyUtils.formatPrice(rate)
                                val type = data["rateType"]?.toString() ?: "Paise"
                                if (type.contains("Paise", ignoreCase = true)) "$formattedRate Paise/Seed" else "₹$formattedRate/Seed"
                            }
                            "FEED" -> "₹${CurrencyUtils.formatPrice(data["ratePerTon"] ?: naText)}/$tonText"
                            "BUSINESS" -> {
                                if (data["businessSubCategory"] == "Feed") {
                                    "₹${CurrencyUtils.formatPrice(data["ratePerTon"] ?: naText)}/$tonText"
                                } else {
                                    "₹${CurrencyUtils.formatPrice(data["price"] ?: data["rateValue"] ?: data["ratePerTon"] ?: naText)}"
                                }
                            }
                            "JOBS" -> "₹${CurrencyUtils.formatPrice(data["salary"] ?: naText)}"
                            "TANKS" -> "₹${CurrencyUtils.formatPrice(data["estPricePerAcre"] ?: naText)}/$acreText"
                            else -> "₹${CurrencyUtils.formatPrice(data["price"] ?: data["rateValue"] ?: naText)}"
                        }

                        MarketItem(
                            title = data["title"]?.toString()?.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_title),
                            price = priceLabel,
                            category = displayCategory,
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
                            onClick = {
                                keyboardController?.hide()
                                onItemClick(data)
                            },
                            rawCategory = categoryStr,
                            modifier = Modifier.fillMaxWidth()
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
