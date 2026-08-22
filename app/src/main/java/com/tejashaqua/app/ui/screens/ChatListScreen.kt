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
                    val sellerId = data["sellerId"]?.toString() ?: ""
                    val buyerId = data["buyerId"]?.toString() ?: ""
                    val isBuying = if (sellerId.isNotEmpty()) sellerId != currentUserId else buyerId == currentUserId
                    
                    val unreadCounts = data["unreadCounts"] as? Map<*, *>
                    val unreadCount = (unreadCounts?.get(currentUserId) as? Number)?.toInt() ?: 
                                     (data["unreadCounts.$currentUserId"] as? Number)?.toInt() ?: 0

                    val listingId = data["listingId"]?.toString() ?: ""

                    ChatListItemData(
                        chatId = doc.id,
                        name = if (isBuying) data["sellerName"]?.toString() ?: "Seller" else data["buyerName"]?.toString() ?: "Buyer",
                        otherUserId = if (isBuying) data["sellerId"]?.toString() ?: "" else data["buyerId"]?.toString() ?: "",
                        type = if (isBuying) "Buying" else "Selling",
                        listingId = listingId,
                        listingInfo = data["listingTitle"]?.toString() ?: "Listing",
                        lastMessage = data["lastMessage"]?.toString() ?: "",
                        time = when (val ts = data["lastMessageTimestamp"]) {
                            is com.google.firebase.Timestamp -> ts.toDate().time
                            is Number -> ts.toLong()
                            else -> 0L
                        },
                        unreadCount = unreadCount,
                        listingImage = data["listingImage"]?.toString(),
                        fullData = data + mapOf("id" to listingId)
                    )
                }.sortedByDescending { it.time }
                
                chats = chatList
            }
            
        onDispose {
            registration.remove()
        }
    }

    val listingStatusMap = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(chats) {
        val uniqueListingIds = chats.map { it.listingId }
            .filter { it.isNotEmpty() && !listingStatusMap.containsKey(it) }
            .distinct()
            
        if (uniqueListingIds.isNotEmpty()) {
            uniqueListingIds.chunked(10).forEach { chunk ->
                db.collection("listings")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val foundIds = snapshot.documents.map { it.id }.toSet()
                        chunk.forEach { id ->
                            val exists = foundIds.contains(id)
                            listingStatusMap[id] = exists
                            
                            // Cleanup logic for inactive chats
                            if (!exists) {
                                val associatedChats = chats.filter { it.listingId == id }
                                associatedChats.forEach { chatItem ->
                                    val inactiveSince = chatItem.fullData["inactiveSince"]
                                    if (inactiveSince == null) {
                                        // Tag as inactive
                                        db.collection("chats").document(chatItem.chatId)
                                            .update("inactiveSince", com.google.firebase.firestore.FieldValue.serverTimestamp())
                                    } else {
                                        // Check if 7 days passed
                                        val inactiveTime = when (inactiveSince) {
                                            is com.google.firebase.Timestamp -> inactiveSince.toDate().time
                                            is Number -> inactiveSince.toLong()
                                            else -> 0L
                                        }
                                        
                                        if (inactiveTime > 0) {
                                            val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
                                            if (System.currentTimeMillis() - inactiveTime > sevenDaysInMillis) {
                                                // Delete chat
                                                db.collection("chats").document(chatItem.chatId).delete()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
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

    val sortedChats = remember(filteredChats, listingStatusMap.toMap()) {
        filteredChats.sortedWith(
            compareByDescending<ChatListItemData> { listingStatusMap[it.listingId] ?: true }
                .thenByDescending { it.time }
        )
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
                    placeholder = { Text(stringResource(R.string.search_conversations), fontSize = 12.sp) },
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

                if (sortedChats.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_chats), color = GrayText)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(sortedChats) { chat ->
                            ChatListItem(
                                chat = chat, 
                                onClick = { 
                                    keyboardController?.hide()
                                    onChatClick(chat.fullData) 
                                },
                                initialListingExists = listingStatusMap[chat.listingId]
                            )
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
fun ChatListItem(chat: ChatListItemData, onClick: () -> Unit, initialListingExists: Boolean? = null) {
    val db = FirebaseFirestore.getInstance()
    var listingExists by remember(chat.listingId) { mutableStateOf(initialListingExists ?: true) }

    LaunchedEffect(chat.listingId) {
        if (chat.listingId.isNotEmpty()) {
            db.collection("listings").document(chat.listingId)
                .addSnapshotListener { snapshot, _ ->
                    listingExists = snapshot != null && snapshot.exists()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (listingExists) Color.Transparent else Color(0xFFF9F9F9))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (listingExists) Color(0xFFF5F5F5) else Color(0xFFEEEEEE),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 8.dp).weight(1f, fill = false)
            ) {
                Text(
                    text = chat.listingInfo,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    color = if (listingExists) Color.Black else Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!listingExists) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.inactive),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (listingExists) AquaBlue.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!chat.listingImage.isNullOrBlank()) {
                    AsyncImage(
                        model = chat.listingImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                        alpha = if (listingExists) 1f else 0.5f
                    )
                } else {
                    Text(
                        text = chat.name.split(" ").filter { it.isNotEmpty() }.map { it.take(1) }.joinToString("").uppercase(),
                        color = if (listingExists) AquaBlue else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp, 
                        color = if (listingExists) Color.Black else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (!listingExists) Color(0xFFEEEEEE) else if (chat.type == "Buying") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (chat.type == "Buying") stringResource(R.string.buying) else stringResource(R.string.selling),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = if (!listingExists) Color.Gray else if (chat.type == "Buying") Color(0xFF2E7D32) else AquaBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = chat.lastMessage,
                    fontSize = 13.sp,
                    color = if (!listingExists) Color.LightGray else if (chat.unreadCount > 0) Color.Black else Color.Gray,
                    fontWeight = if (listingExists && chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(text = formatTime(chat.time), fontSize = 11.sp, color = GrayText)
                if (chat.unreadCount > 0 && listingExists) {
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
    val listingId: String,
    val listingInfo: String,
    val lastMessage: String,
    val time: Long,
    val unreadCount: Int,
    val listingImage: String? = null,
    val fullData: Map<String, Any>,
    val isListingActive: Boolean = true
)
