package com.tejashaqua.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.utils.CurrencyUtils

@Composable
fun SellerPostsDialog(
    sellerName: String,
    sellerPosts: List<Map<String, Any>>,
    onDismiss: () -> Unit,
    onItemClick: (Map<String, Any>) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = sellerName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.items_count, sellerPosts.size),
                            fontSize = 12.sp,
                            color = GrayText
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                if (sellerPosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_listings), color = GrayText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sellerPosts) { data ->
                            val categoryStr = data["category"]?.toString() ?: "Other"
                            val naText = stringResource(R.string.not_available_short)
                            val tonText = stringResource(R.string.unit_ton)
                            val acreText = stringResource(R.string.unit_acre)
                            val priceLabel = when (categoryStr.uppercase()) {
                                "PRAWNS" -> {
                                    val rate = data["rateValue"]?.toString() ?: naText
                                    val formattedRate = CurrencyUtils.formatPrice(rate)
                                    val type = data["rateType"]?.toString() ?: "Paise"
                                    if (type.contains("Paise", ignoreCase = true)) "$formattedRate Paise/Seed" else "₹$formattedRate/Seed"
                                }
                                "FEED" -> "₹${CurrencyUtils.formatPrice(data["ratePerTon"] ?: naText)}/$tonText"
                                "BUSINESS" -> {
                                    if (data["businessSubCategory"] == "Feed") {
                                        "₹${CurrencyUtils.formatPrice(data["ratePerTon"] ?: naText)}/$tonText"
                                    } else {
                                        "₹${CurrencyUtils.formatPrice(data["price"] ?: data["rateValue"] ?: naText)}"
                                    }
                                }
                                "JOBS" -> "₹${CurrencyUtils.formatPrice(data["salary"] ?: naText)}"
                                "TANKS" -> "₹${CurrencyUtils.formatPrice(data["estPricePerAcre"] ?: naText)}/$acreText"
                                else -> "₹${CurrencyUtils.formatPrice(data["price"] ?: data["rateValue"] ?: naText)}"
                            }

                            MarketItem(
                                title = data["title"]?.toString() ?: "No Title",
                                price = priceLabel,
                                category = categoryStr,
                                location = data["location"]?.toString() ?: "Unknown",
                                posterName = data["posterName"]?.toString() ?: "User",
                                imageUrl = (data["images"] as? List<*>)?.firstOrNull()?.toString(),
                                timestamp = (data["timestamp"] as? Timestamp)?.toDate()?.time ?:
                                            (data["timestamp"] as? Long) ?: 0L,
                                onClick = { 
                                    onDismiss()
                                    onItemClick(data) 
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
