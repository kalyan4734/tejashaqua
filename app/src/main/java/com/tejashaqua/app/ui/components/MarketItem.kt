package com.tejashaqua.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
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
    onFavoriteClick: (() -> Unit)? = null
) {
    val displayLocation = remember(location) {
        location.split(",").firstOrNull()?.trim() ?: location
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        color = Color.White
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
                Surface(color = Color(0xFFE8EAF6), shape = RoundedCornerShape(4.dp)) { 
                    Text(category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = AquaBlue, fontWeight = FontWeight.Bold) 
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GrayText, modifier = Modifier.size(10.dp))
                    Text(displayLocation, color = GrayText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = AquaBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.by_label, posterName), fontSize = 10.sp, color = Color.DarkGray, maxLines = 1)
                }
            }
        }
    }
}
