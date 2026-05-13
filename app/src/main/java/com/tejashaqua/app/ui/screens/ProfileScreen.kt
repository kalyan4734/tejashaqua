package com.tejashaqua.app.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    mobileNumber: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onMyListingsClick: () -> Unit,
    onChatsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var listingCount by remember { mutableIntStateOf(0) }
    var profilePicBase64 by remember { mutableStateOf<String?>(null) }
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid

    BackHandler { onBackClick() }

    DisposableEffect(currentUserId) {
        var listingListener: ListenerRegistration? = null
        var userListener: ListenerRegistration? = null

        if (currentUserId != null) {
            listingListener = db.collection("listings")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) listingCount = snapshot.size()
                }
            
            userListener = db.collection("users").document(currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        profilePicBase64 = snapshot.getString("profilePic")
                    }
                }
        }

        onDispose {
            listingListener?.remove()
            userListener?.remove()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Logout", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogoutClick()
                }) {
                    Text(text = "Logout", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancel", color = AquaBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            item {
                ProfileHeader(userName, mobileNumber, profilePicBase64, onEditClick)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(listingCount.toString(), "My Listings", Modifier.weight(1f).clickable { onMyListingsClick() })
                    StatCard("0", "Saved Items", Modifier.weight(1f))
                    StatCard("3", "Chats", Modifier.weight(1f).clickable { onChatsClick() })
                }
            }

            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        ProfileMenuItem(Icons.Default.CheckCircle, "My Listings", Color(0xFF4CAF50), onClick = onMyListingsClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Chat, "Chats", AquaBlue, onClick = onChatsClick)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Favorite, "Saved Items", Color(0xFFF44336), onClick = {})
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        ProfileMenuItem(Icons.Default.Description, "Privacy Policy", Color(0xFF009688), onClick = {})
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Assignment, "Terms & Conditions", Color(0xFF9C27B0), onClick = {})
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Info, "About App", Color(0xFF03A9F4), onClick = {})
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            item {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clickable { showLogoutDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFF44336))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Logout", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun ProfileHeader(userName: String, mobileNumber: String, profilePicBase64: String?, onEditClick: () -> Unit) {
    val bitmap = remember(profilePicBase64) {
        if (!profilePicBase64.isNullOrBlank()) {
            try {
                val decodedString = Base64.decode(profilePicBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            } catch (e: Exception) { null }
        } else null
    }

    Box(
        modifier = Modifier.fillMaxWidth().background(AquaBlue).padding(bottom = 32.dp, start = 24.dp, end = 24.dp, top = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(80.dp).background(Color.White.copy(alpha = 0.2f), CircleShape).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "U",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = if (userName.isNotEmpty()) userName else "User", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = if (mobileNumber.isNotEmpty()) "+91 $mobileNumber" else "Phone not available", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AquaBlue)
            Text(text = label, fontSize = 12.sp, color = GrayText)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, label: String, iconTint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(iconTint.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}
