package com.tejashaqua.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

@Composable
fun MarketItem(
    title: String,
    price: String,
    category: String,
    location: String,
    posterName: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    isFavorited: Boolean = false,
    timestamp: Long = 0L,
    onFavoriteClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onPosterClick: (() -> Unit)? = null,
    rawCategory: String = ""
) {
    val context = LocalContext.current
    val displayLocation = remember(location) {
        location.split(",").firstOrNull()?.trim() ?: location
    }

    val timeAgo = remember(timestamp) {
        if (timestamp == 0L) "" else {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            when {
                diff < 60000 -> context.getString(R.string.just_now)
                diff < 3600000 -> context.getString(R.string.mins_ago, diff / 60000)
                diff < 86400000 -> context.getString(R.string.hours_ago, diff / 3600000)
                else -> context.getString(R.string.days_ago, diff / 86400000)
            }
        }
    }

    val categoryColor = remember(category, rawCategory) {
        val cat = (if (rawCategory.isNotEmpty()) rawCategory else category).uppercase()
        when {
            cat.contains("FISH") -> Color(0xFF009688)
            cat.contains("PRAWN") -> Color(0xFF3F51B5)
            cat.contains("EQUIPMENT") -> Color(0xFF1976D2)
            cat.contains("VEHICLE") -> Color(0xFF1976D2)
            cat.contains("FEED") -> Color(0xFFE65100)
            cat.contains("SERVICE") -> Color(0xFFF57C00)
            cat.contains("TANK") -> Color(0xFF388E3C)
            cat.contains("BUSINESS") -> Color(0xFFB71C1C)
            cat.contains("JOB") -> Color(0xFF673AB7)
            else -> AquaBlue
        }
    }

    val categoryBgColor = remember(category, rawCategory) {
        val cat = (if (rawCategory.isNotEmpty()) rawCategory else category).uppercase()
        when {
            cat.contains("FISH") -> Color(0xFFFFF3E0)
            cat.contains("PRAWN") -> Color(0xFFE0F2F1)
            cat.contains("EQUIPMENT") -> Color(0xFFE1F5FE)
            cat.contains("VEHICLE") -> Color(0xFFE1F5FE)
            cat.contains("FEED") -> Color(0xFFFFF3E0)
            cat.contains("SERVICE") -> Color(0xFFFFFDE7)
            cat.contains("TANK") -> Color(0xFFE8F5E9)
            cat.contains("BUSINESS") -> Color(0xFFFFEBEE)
            cat.contains("JOB") -> Color(0xFFF3E5F5)
            else -> Color(0xFFE8EAF6)
        }
    }

    Surface(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column {
            Box(modifier = Modifier.height(110.dp).fillMaxWidth().background(Color(0xFFF5F5F5))) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.app_logo)
                    )
                } else if (rawCategory.uppercase().contains("JOB") || category.uppercase().contains("JOB")) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF3E5F5)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color(0xFF673AB7).copy(alpha = 0.5f))
                    }
                } else {
                    Image(painter = painterResource(id = R.drawable.app_logo), contentDescription = null, modifier = Modifier.size(60.dp).align(Alignment.Center), alpha = 0.3f)
                }

                if (onFavoriteClick != null) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.Red else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Surface(color = categoryBgColor, shape = RoundedCornerShape(4.dp)) { 
                    Text(category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = categoryColor, fontWeight = FontWeight.Bold) 
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GrayText, modifier = Modifier.size(10.dp))
                    Text(displayLocation, color = GrayText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (timeAgo.isNotEmpty()) {
                        Text(timeAgo, color = GrayText, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = onPosterClick != null) { 
                        onPosterClick?.invoke() 
                    }
                ) {
                    Icon(Icons.Default.Person, null, tint = categoryColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.by_label, posterName), fontSize = 10.sp, color = Color.DarkGray, maxLines = 1)
                }
            }
        }
    }
}
