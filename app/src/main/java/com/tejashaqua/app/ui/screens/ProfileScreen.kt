package com.tejashaqua.app.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userName: String,
    mobileNumber: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
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
                .background(Color(0xFFF8F9FB))
        ) {
            // Header Profile Info
            item {
                ProfileHeader(userName, mobileNumber)
            }

            // Stats Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("4", "My Listings", Modifier.weight(1f))
                    StatCard("12", "Saved Items", Modifier.weight(1f))
                    StatCard("3", "Chats", Modifier.weight(1f))
                }
            }

            // Menu Items
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    color = Color.White
                ) {
                    Column {
                        ProfileMenuItem(Icons.Default.CheckCircle, "My Listings", Color(0xFF4CAF50))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Favorite, "Saved Items", Color(0xFFF44336))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    color = Color.White
                ) {
                    Column {
                        ProfileMenuItem(Icons.Default.Description, "Privacy Policy", Color(0xFF009688))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Assignment, "Terms & Conditions", Color(0xFF9C27B0))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Info, "About App", Color(0xFF03A9F4))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
                        ProfileMenuItem(Icons.Default.Star, "Rate the App", Color(0xFFFBC02D))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Logout Button
            item {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clickable { onLogoutClick() },
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEBEE)),
                    color = Color.White
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
fun ProfileHeader(userName: String, mobileNumber: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AquaBlue)
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp, top = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFE0F7FA).copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userName.isNotEmpty()) userName.take(2).uppercase() else "SM",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0097A7)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (userName.isNotEmpty()) userName else "Username_5875",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (mobileNumber.isNotEmpty()) "+91 $mobileNumber" else "+91 9876543210",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.EditNote, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AquaBlue)
            Text(text = label, fontSize = 12.sp, color = GrayText)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, label: String, iconTint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Black)
    }
}
