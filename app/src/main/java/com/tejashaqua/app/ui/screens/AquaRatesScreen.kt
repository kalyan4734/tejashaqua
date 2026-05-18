package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

data class AquaRate(
    val name: String,
    val price: String,
    val change: String,
    val trend: RateTrend,
    val icon: Int,
    val iconBgColor: Color
)

enum class RateTrend {
    UP, DOWN, FLAT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaRatesScreen(onBackClick: () -> Unit) {
    val rates = listOf(
        AquaRate("Rohu", "₹160/kg", "+₹5 (3%)", RateTrend.UP, R.drawable.fish, Color(0xFFFFF3E0)),
        AquaRate("Katla", "₹180/kg", "-₹10 (5.9%)", RateTrend.DOWN, R.drawable.fish, Color(0xFFE0F2F1)),
        AquaRate("Tilapia", "₹120/kg", "No Change", RateTrend.FLAT, R.drawable.fish, Color(0xFFE8EAF6)),
        AquaRate("Pangasius", "₹95/kg", "+₹2 (2%)", RateTrend.UP, R.drawable.fish, Color(0xFFFCE4EC)),
        AquaRate("Mrigal", "₹140/kg", "-₹5 (3.5%)", RateTrend.DOWN, R.drawable.fish, Color(0xFFE8F5E9)),
        AquaRate("Grass Carp", "₹170/kg", "No Change", RateTrend.FLAT, R.drawable.fish, Color(0xFFE3F2FD)),
        AquaRate("Common Carp", "₹130/kg", "+₹4 (3%)", RateTrend.UP, R.drawable.fish, Color(0xFFFFF9C4))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Today's Aqua Rates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rates) { rate ->
                RateItemCard(rate)
            }
        }
    }
}

@Composable
fun RateItemCard(rate: AquaRate) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        color = Color.White,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(rate.iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = rate.icon), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = rate.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Fresh Water Fish",
                    fontSize = 12.sp,
                    color = GrayText
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rate.price,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rate.trend != RateTrend.FLAT) {
                        Icon(
                            imageVector = when(rate.trend) {
                                RateTrend.UP -> Icons.AutoMirrored.Filled.TrendingUp
                                RateTrend.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
                                else -> Icons.AutoMirrored.Filled.TrendingFlat
                            },
                            contentDescription = null,
                            tint = when(rate.trend) {
                                RateTrend.UP -> Color(0xFF4CAF50)
                                RateTrend.DOWN -> Color(0xFFF44336)
                                else -> GrayText
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = rate.change,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = when(rate.trend) {
                            RateTrend.UP -> Color(0xFF4CAF50)
                            RateTrend.DOWN -> Color(0xFFF44336)
                            else -> GrayText
                        }
                    )
                }
            }
        }
    }
}
