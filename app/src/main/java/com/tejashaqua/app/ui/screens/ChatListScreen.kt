package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.intl.LocaleList
import com.tejashaqua.app.utils.LocaleHelper
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.components.LoadingOverlay
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var blockedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("users").document(currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val blocked = snapshot.get("blockedUsers") as? List<*>
                        blockedUsers = blocked?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                    }
                }
        }
    }

    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Search,
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    DisposableEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@DisposableEffect onDispose {}
        
        val registration = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                isLoading = false
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val chatList = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val buyerId = data["buyerId"] as? String ?: ""
                    val isBuying = buyerId == currentUserId
                    
                    val unreadCounts = data["unreadCounts"] as? Map<*, *>
                    val unreadCount = (unreadCounts?.get(currentUserId) as? Long)?.toInt() ?: 
                                     (data["unreadCounts.$currentUserId"] as? Long)?.toInt() ?: 0

                    ChatListItemData(
                        chatId = doc.id,
                        name = if (isBuying) data["sellerName"] as? String ?: "Seller" else data["buyerName"] as? String ?: "Buyer",
                        otherUserId = if (isBuying) data["sellerId"] as? String ?: "" else data["buyerId"] as? String ?: "",
                        type = if (isBuying) "Buying" else "Selling",
                        listingInfo = data["listingTitle"] as? String ?: "Listing",
                        lastMessage = data["lastMessage"] as? String ?: "",
                        time = (data["lastMessageTimestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 
                               (data["lastMessageTimestamp"] as? Long) ?: 0L,
                        unreadCount = unreadCount,
                        listingImage = data["listingImage"] as? String,
                        fullData = data + mapOf("id" to (data["listingId"] ?: ""))
                    )
                }.sortedByDescending { it.time }
                
                chats = chatList
            }
            
        onDispose {
            registration.remove()
        }
    }

    val filteredChats = chats.filter {
        val otherUserId = it.fullData["userId"]?.toString() ?: ""
        if (blockedUsers.contains(otherUserId)) return@filter false

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
                    title = { Text(stringResource(R.string.chat_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(min = 50.dp),
                    placeholder = { Text(stringResource(R.string.search_conversations), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = keyboardOptions
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = AquaBlue,
                    divider = { HorizontalDivider(color = Color(0xFFEEEEEE)) }
                ) {
                    Tab(selected = selectedTabIndex == 0, onClick = { 
                        keyboardController?.hide()
                        selectedTabIndex = 0 
                    }) {
                        Text(stringResource(R.string.all), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = selectedTabIndex == 1, onClick = { 
                        keyboardController?.hide()
                        selectedTabIndex = 1 
                    }) {
                        Text(stringResource(R.string.buying), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = selectedTabIndex == 2, onClick = { 
                        keyboardController?.hide()
                        selectedTabIndex = 2 
                    }) {
                        Text(stringResource(R.string.selling), modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    }
                }

                if (filteredChats.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_chats), color = GrayText)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredChats) { chat ->
                            ChatListItem(chat, onClick = { 
                                keyboardController?.hide()
                                onChatClick(chat.fullData) 
                            })
                            HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            if (isLoading) {
                LoadingOverlay(stringResource(R.string.chats) + "...")
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
                color = Color.Black
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AquaBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!chat.listingImage.isNullOrBlank()) {
                    AsyncImage(
                        model = chat.listingImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo)
                    )
                } else {
                    Text(
                        text = chat.name.split(" ").filter { it.isNotEmpty() }.map { it.take(1) }.joinToString("").uppercase(),
                        color = AquaBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = chat.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (chat.type == "Buying") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (chat.type == "Buying") stringResource(R.string.buying) else stringResource(R.string.selling),
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
                    color = if (chat.unreadCount > 0) Color.Black else Color.Gray,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
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
    val listingImage: String? = null,
    val fullData: Map<String, Any>
)
