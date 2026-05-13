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
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var listingToDelete by remember { mutableStateOf<UserListing?>(null) }

    if (showDeleteDialog && listingToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.collection("listings").document(listingToDelete!!.id).delete()
                        showDeleteDialog = false
                        listingToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
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
                            UserListing(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                category = doc.getString("category") ?: "FISH",
                                price = doc.get("price")?.toString() ?: "",
                                unit = doc.getString("unitType") ?: "kg",
                                location = fullLocation.split(",").firstOrNull()?.trim() ?: fullLocation,
                                imageUrl = (doc.get("images") as? List<*>)?.firstOrNull() as? String
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
                title = { Text("My Listings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (listings.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No listings found", color = GrayText)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFFF8F9FA)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listings) { listing ->
                        ListingCard(
                            listing = listing,
                            onEditClick = { onEditClick(listing.id, listing.category) },
                            onDeleteClick = {
                                listingToDelete = listing
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
            
            if (isLoading) {
                LoadingOverlay("Loading your listings...")
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
                    Text(text = "₹${listing.price}/${listing.unit}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
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
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }

                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Post", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
