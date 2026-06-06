package com.tejashaqua.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import com.tejashaqua.app.utils.AppStateTracker
import com.tejashaqua.app.utils.LocaleHelper
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
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
    
    // Update listingDetails if parent passes new listingData
    LaunchedEffect(listingData) {
        listingDetails = listingDetails + listingData
    }
    
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Send,
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )
    
    val title = listingDetails["title"]?.toString() ?: listingDetails["listingTitle"]?.toString() ?: stringResource(R.string.no_title)
    
    // Improved logic to find the best available price value
    val rawPrice = listingDetails["rateValue"]?.toString()?.takeIf { it.isNotBlank() }
        ?: listingDetails["price"]?.toString()?.takeIf { it.isNotBlank() }
        ?: listingDetails["listingPrice"]?.toString()?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.not_available_short)
    
    val priceValue = rawPrice
    
    val categoryStr = listingDetails["category"]?.toString() ?: ""
    val price = when {
        categoryStr.uppercase() == "FEED" || (listingDetails["businessSubCategory"] == "Feed") -> {
            if (priceValue == stringResource(R.string.not_available_short)) priceValue else "₹$priceValue/ton"
        }
        categoryStr.uppercase() == "JOBS" -> {
            if (priceValue == stringResource(R.string.not_available_short)) priceValue else "₹$priceValue"
        }
        categoryStr.uppercase() == "PRAWNS" -> {
            val unit = listingDetails["rateType"]?.toString()?.lowercase() ?: "paise"
            if (priceValue == stringResource(R.string.not_available_short)) priceValue else "₹$priceValue/$unit"
        }
        else -> {
            if (priceValue == stringResource(R.string.not_available_short)) priceValue else "₹$priceValue"
        }
    }
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
        if (listingId.isNotEmpty()) {
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

    DisposableEffect(chatRoomId) {
        AppStateTracker.activeChatId = chatRoomId
        onDispose {
            AppStateTracker.activeChatId = null
        }
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
            "rateType" to (listingDetails["rateType"]?.toString() ?: ""),
            "category" to categoryStr,
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
                        val time = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        ChatMessage(text, senderId == currentUserId, time)
                    }

                    // Logic to send initial message only if requested and chat is empty
                    if (sendInitialMessage && !initialMessageSent && messages.isEmpty() && currentUserId.isNotEmpty() && currentUserId != sellerUserId) {
                        initialMessageSent = true
                        val city = currentUserLocation.split(",").firstOrNull()?.trim() ?: currentUserLocation
                        val defaultMsg = context.getString(R.string.initial_chat_message, sellerName, title, currentUserName, city, currentUserPhone)
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
                val listingSellerId = listingDetails["userId"]?.toString() ?: ""
                val isMeSeller = currentUserId == listingSellerId

                val buyerSuggestions = listOf(
                    stringResource(R.string.is_available),
                    stringResource(R.string.best_price),
                    "Where is the location?",
                    "Can I call you?"
                )

                val sellerSuggestions = listOf(
                    "Yes, it is available.",
                    "Price is slightly negotiable.",
                    "Price is fixed.",
                    "Please call me for details."
                )

                val currentSuggestions = if (isMeSeller) sellerSuggestions else buyerSuggestions
                val sentMessagesTexts = chatMessages.filter { it.isFromMe }.map { it.text }
                val visibleSuggestions = currentSuggestions.filter { it !in sentMessagesTexts }

                if (visibleSuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        visibleSuggestions.take(3).forEach { suggestion ->
                            SuggestionChip(
                                onClick = {
                                    keyboardController?.hide()
                                    sendMessage(suggestion)
                                },
                                label = { Text(suggestion, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE3F2FD))
                            )
                        }
                    }
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
                        keyboardOptions = keyboardOptions,
                        keyboardActions = KeyboardActions(
                            onSend = {
                                keyboardController?.hide()
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
                            keyboardController?.hide()
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
                    .background(Color(0xFFF8F9FA)),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { msg ->
                    ChatBubble(msg, if (!msg.isFromMe) sellerName else currentUserName)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, senderName: String) {
    val timeFormat = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()) }
    val timeString = timeFormat.format(java.util.Date(message.timestamp))
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = if (message.isFromMe) AquaBlue else Color.White,
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (message.isFromMe) 12.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 12.dp
                ),
                shadowElevation = 0.5.dp,
                modifier = Modifier.widthIn(max = 240.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    val phoneRegex = remember { Regex("""(\+91|0)?[6-9][0-9]{9}""") }
                    val annotatedString = buildAnnotatedString {
                        append(message.text)
                        val matches = phoneRegex.findAll(message.text)
                        for (match in matches) {
                            addStringAnnotation(
                                tag = "phone",
                                annotation = match.value,
                                start = match.range.first,
                                end = match.range.last + 1
                            )
                            addStyle(
                                style = SpanStyle(
                                    color = if (message.isFromMe) Color(0xFF81D4FA) else Color(0xFF1976D2),
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                start = match.range.first,
                                end = match.range.last + 1
                            )
                        }
                    }

                    ClickableText(
                        text = annotatedString,
                        style = TextStyle(
                            color = if (message.isFromMe) Color.White else Color.Black,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "phone", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${annotation.item}")
                                    }
                                    context.startActivity(intent)
                                }
                        }
                    )
                    Text(
                        text = timeString,
                        fontSize = 8.sp,
                        color = if (message.isFromMe) Color.White.copy(alpha = 0.7f) else GrayText,
                        modifier = Modifier.align(Alignment.End).padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long
)
