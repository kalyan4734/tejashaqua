package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sellerName: String,
    sellerUserId: String,
    listingId: String,
    listingData: Map<String, Any>,
    currentUserId: String,
    currentUserName: String,
    currentUserPhone: String,
    currentUserLocation: String,
    onBackClick: () -> Unit,
    sendInitialMessage: Boolean = false
) {
    val db = FirebaseFirestore.getInstance()
    var listingDetails by remember { mutableStateOf(listingData) }
    
    val title = listingDetails["title"]?.toString() ?: listingDetails["listingTitle"]?.toString() ?: "No Title"
    val priceValue = listingDetails["price"] ?: listingDetails["rateValue"] ?: listingDetails["listingPrice"] ?: "N/A"
    val price = "₹$priceValue/kg"
    val fullLocation = listingDetails["location"]?.toString() ?: listingDetails["listingLocation"]?.toString() ?: "Unknown"
    val location = fullLocation.split(",").firstOrNull()?.trim() ?: fullLocation
    
    val imagesFromData = listingDetails["images"] as? List<*>
    val singleImage = listingDetails["listingImage"]?.toString()
    val images = if (!imagesFromData.isNullOrEmpty()) imagesFromData else if (!singleImage.isNullOrEmpty()) listOf(singleImage) else emptyList<String>()
    
    var messageText by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    var initialMessageSent by remember { mutableStateOf(false) }

    // Fetch full listing details if missing (e.g. when coming from ChatList)
    LaunchedEffect(listingId) {
        if (listingId.isNotEmpty() && (images.isEmpty() || listingDetails["price"] == null)) {
            db.collection("listings").document(listingId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val data = doc.data
                    if (data != null) {
                        listingDetails = listingDetails + data
                    }
                }
            }
        }
    }

    // Determine unique chat ID (senderId_receiverId_listingId)
    val chatRoomId = if (currentUserId < sellerUserId) {
        "${currentUserId}_${sellerUserId}_$listingId"
    } else {
        "${sellerUserId}_${currentUserId}_$listingId"
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val messageData = hashMapOf(
            "text" to text,
            "senderId" to currentUserId,
            "senderName" to currentUserName,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats").document(chatRoomId).collection("messages").add(messageData)
        
        val listingSellerId = listingData["userId"]?.toString() ?: ""
        val isMeSeller = currentUserId == listingSellerId
        
        val buyerId = if (isMeSeller) sellerUserId else currentUserId
        val buyerName = if (isMeSeller) sellerName else currentUserName
        val sellerId = listingSellerId
        val sellerNameLabel = if (isMeSeller) currentUserName else sellerName

        val chatMeta = hashMapOf(
            "lastMessage" to text,
            "lastMessageTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "lastMessageSenderId" to currentUserId,
            "listingId" to listingId,
            "listingTitle" to title,
            "listingPrice" to priceValue,
            "listingLocation" to fullLocation,
            "listingImage" to (images.firstOrNull()?.toString() ?: ""),
            "sellerId" to sellerId,
            "sellerName" to sellerNameLabel,
            "buyerId" to buyerId,
            "buyerName" to buyerName,
            "participants" to listOf(currentUserId, sellerUserId)
        )
        
        // First ensure doc exists with basic meta
        db.collection("chats").document(chatRoomId).set(chatMeta, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                // Then increment the specific unread count field
                db.collection("chats").document(chatRoomId).update(
                    "unreadCounts.$sellerUserId", com.google.firebase.firestore.FieldValue.increment(1),
                    "lastMessageTimestamp", com.google.firebase.firestore.FieldValue.serverTimestamp() // Update timestamp again to be sure
                )
            }
    }

    // Load messages from Firestore
    LaunchedEffect(chatRoomId) {
        // Clear unread count for current user
        db.collection("chats").document(chatRoomId).update("unreadCounts.$currentUserId", 0)
            .addOnFailureListener {
                // If update fails (e.g. field doesn't exist), use set with merge to initialize
                db.collection("chats").document(chatRoomId).set(
                    mapOf("unreadCounts" to mapOf(currentUserId to 0)),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }

        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: ""
                        val senderId = doc.getString("senderId") ?: ""
                        ChatMessage(text, senderId == currentUserId)
                    }

                    // Logic to send initial message only if requested and chat is empty
                    if (sendInitialMessage && !initialMessageSent && messages.isEmpty() && currentUserId.isNotEmpty() && currentUserId != sellerUserId) {
                        initialMessageSent = true
                        val city = currentUserLocation.split(",").firstOrNull()?.trim() ?: currentUserLocation
                        val defaultMsg = "Hi $sellerName, I'm interested in your listing: $title. My name is $currentUserName from $city, please call me at $currentUserPhone."
                        sendMessage(defaultMsg)
                    }

                    chatMessages.clear()
                    chatMessages.addAll(messages)
                }
            }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sellerName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AquaBlue)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { sendMessage(context.getString(R.string.is_available)) },
                        label = { Text(stringResource(R.string.is_available)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE3F2FD))
                    )
                    SuggestionChip(
                        onClick = { sendMessage(context.getString(R.string.best_price)) },
                        label = { Text(stringResource(R.string.best_price)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE3F2FD))
                    )
                }
                
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.type_message)) },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    sendMessage(messageText)
                                    messageText = ""
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFF5F5F5)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier.background(AquaBlue, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    val firstImage = images.firstOrNull()?.toString() ?: ""
                    
                    if (firstImage.isNotEmpty()) {
                        AsyncImage(
                            model = firstImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.app_logo)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .padding(16.dp),
                            alpha = 0.3f
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                            val isBuying = currentUserId != sellerUserId
                            Text(
                                if (isBuying) stringResource(R.string.buying) else stringResource(R.string.selling),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = price, fontWeight = FontWeight.Bold, color = AquaBlue, fontSize = 16.sp)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, null, tint = GrayText, modifier = Modifier.size(14.dp))
                            Text(stringResource(R.string.recently), color = GrayText, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.LocationOn, null, tint = GrayText, modifier = Modifier.size(14.dp))
                            Text(" $location", color = GrayText, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(msg)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (message.isFromMe) AquaBlue else Color(0xFFF5F7FB),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromMe) 16.dp else 0.dp,
                bottomEnd = if (message.isFromMe) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isFromMe) Color.White else Color.Black,
                fontSize = 14.sp
            )
        }
    }
}

data class ChatMessage(
    val text: String,
    val isFromMe: Boolean
)
