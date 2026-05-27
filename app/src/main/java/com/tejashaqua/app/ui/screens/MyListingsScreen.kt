package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.ui.components.LoadingOverlay
import com.tejashaqua.app.R

data class UserListing(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val price: String = "",
    val unit: String = "",
    val location: String = "",
    val distance: String = "4.5 km",
    val timeAgo: String = "2 hrs ago",
    val views: Int = 200,
    val imageUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onBackClick: () -> Unit,
    onEditClick: (String, String) -> Unit // Pass ID and Category
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid
    
    var listings by remember { mutableStateOf<List<UserListing>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var listingToDelete by remember { mutableStateOf<UserListing?>(null) }

    if (showDeleteDialog && listingToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_listing)) },
            text = { Text(stringResource(R.string.delete_listing_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        keyboardController?.hide()
                        db.collection("listings").document(listingToDelete!!.id).delete()
                        showDeleteDialog = false
                        listingToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    keyboardController?.hide()
                    showDeleteDialog = false 
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            db.collection("listings")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        listings = snapshot.documents.map { doc ->
                            val fullLocation = doc.getString("location") ?: ""
                            val categoryStr = doc.getString("category") ?: "Other"
                            val priceLabel = when (categoryStr.uppercase()) {
                                "PRAWNS" -> "${doc.get("rateValue") ?: "N/A"}/${doc.getString("rateType")?.lowercase() ?: "paise"}"
                                "FEED" -> "${doc.get("ratePerTon") ?: "N/A"}/ton"
                                "BUSINESS" -> if (doc.getString("businessSubCategory") == "Feed") "${doc.get("ratePerTon") ?: "N/A"}/ton" else "${doc.get("price") ?: doc.get("rateValue") ?: "N/A"}"
                                "JOBS" -> "${doc.get("salary") ?: "N/A"}"
                                "TANKS" -> "${doc.get("estPricePerAcre") ?: "N/A"}/acre"
                                else -> "${doc.get("price") ?: doc.get("rateValue") ?: "N/A"}"
                            }

                            UserListing(
                                id = doc.id,
                                title = doc.getString("title")?.takeIf { it.isNotBlank() } ?: "No Title",
                                category = categoryStr,
                                price = priceLabel,
                                unit = "", // Unit is now included in priceLabel
                                location = fullLocation.split(",").firstOrNull()?.trim() ?: fullLocation,
                                imageUrl = (doc.get("images") as? List<*>)?.filterIsInstance<String>()?.firstOrNull()
                            )
                        }
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.my_listings), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onBackClick()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (listings.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_listings_found), color = GrayText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listings) { listing ->
                        ListingCard(
                            listing = listing,
                            onEditClick = { 
                                keyboardController?.hide()
                                onEditClick(listing.id, listing.category) 
                            },
                            onDeleteClick = {
                                keyboardController?.hide()
                                listingToDelete = listing
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
            
            if (isLoading) {
                LoadingOverlay(stringResource(R.string.loading_listings))
            }
        }
    }
}

@Composable
fun ListingCard(
    listing: UserListing,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F0F0))
                ) {
                    if (!listing.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = listing.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo)
                        )
                    } else {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).align(Alignment.Center),
                            alpha = 0.3f
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(color = Color(0xFFE8EAF6), shape = RoundedCornerShape(4.dp)) {
                        Text(text = listing.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color(0xFF3F51B5), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(text = listing.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, color = Color.Black)
                    Text(text = "₹${listing.price}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = GrayText, modifier = Modifier.size(14.dp))
                        Text(
                            text = " ${listing.location} • ${listing.distance}",
                            color = GrayText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }

                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.edit_post), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
