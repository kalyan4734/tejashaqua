package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String,
    onBackClick: () -> Unit,
    onChatClick: (Map<String, Any>) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val db = FirebaseFirestore.getInstance()
    var chats by remember { mutableStateOf(listOf<ChatListItemData>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@LaunchedEffect
        
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val chatList = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val buyerId = data["buyerId"] as? String ?: ""
                    val isBuying = buyerId == currentUserId
                    
                    ChatListItemData(
                        chatId = doc.id,
                        name = if (isBuying) data["sellerName"] as? String ?: "Seller" else data["buyerName"] as? String ?: "Buyer",
                        otherUserId = if (isBuying) data["sellerId"] as? String ?: "" else data["buyerId"] as? String ?: "",
                        type = if (isBuying) "Buying" else "Selling",
                        listingInfo = data["listingTitle"] as? String ?: "Listing",
                        lastMessage = data["lastMessage"] as? String ?: "",
                        time = data["lastMessageTimestamp"] as? Long ?: 0L,
                        unreadCount = 0, // Implement unread logic if needed
                        fullData = data + mapOf("id" to (data["listingId"] ?: ""))
                    )
                }
                chats = chatList
            }
    }

    val filteredChats = chats.filter {
        (it.name.contains(searchText, ignoreCase = true) || it.listingInfo.contains(searchText, ignoreCase = true)) &&
        when (selectedTabIndex) {
            1 -> it.type == "Buying"
            2 -> it.type == "Selling"
            else -> true
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AquaBlue)) {
                CenterAlignedTopAppBar(
                    title = { Text("Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
                )
                
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    placeholder = { Text("Search Conversations...", fontSize = 14.sp) },
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
                divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
            ) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                    Text("All", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                    Text("Buying", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }) {
                    Text("Selling", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AquaBlue)
                }
            } else if (filteredChats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No chats found", color = GrayText)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChats) { chat ->
                        ChatListItem(chat, onClick = { onChatClick(chat.fullData) })
                        HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chat: ChatListItemData, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Surface(
            color = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = chat.listingInfo,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                color = Color.DarkGray
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(AquaBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.name.split(" ").filter { it.isNotEmpty() }.map { it.take(1) }.joinToString("").uppercase(),
                    color = AquaBlue,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = chat.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (chat.type == "Buying") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = chat.type,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = if (chat.type == "Buying") Color(0xFF2E7D32) else AquaBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = chat.lastMessage,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(text = formatTime(chat.time), fontSize = 11.sp, color = GrayText)
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(containerColor = AquaBlue, contentColor = Color.White) {
                        Text(chat.unreadCount.toString())
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return if (now.get(Calendar.DATE) == time.get(Calendar.DATE)) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

data class ChatListItemData(
    val chatId: String,
    val name: String,
    val otherUserId: String,
    val type: String,
    val listingInfo: String,
    val lastMessage: String,
    val time: Long,
    val unreadCount: Int,
    val fullData: Map<String, Any>
)
