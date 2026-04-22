package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
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

data class AquaRate(
    val name: String,
    val price: String,
    val change: String,
    val trend: RateTrend,
    val icon: ImageVector,
    val iconBgColor: Color
)

enum class RateTrend {
    UP, DOWN, FLAT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaRatesScreen(onBackClick: () -> Unit) {
    val rates = listOf(
        AquaRate("Katla", "₹180/kg", "+₹10 (5.9%)", RateTrend.UP, Icons.Default.Info, Color(0xFFE0F2F1)),
        AquaRate("Rohu", "₹160/kg", "+₹5 (3%)", RateTrend.DOWN, Icons.Default.Info, Color(0xFFFFF3E0)),
        AquaRate("Tilapia", "₹120/kg", "No Change", RateTrend.FLAT, Icons.Default.Info, Color(0xFFE8EAF6)),
        AquaRate("Katla", "₹120/kg", "+₹8 (3%)", RateTrend.DOWN, Icons.Default.Info, Color(0xFFFCE4EC)),
        AquaRate("Katla", "₹360/kg", "+₹10 (5.9%)", RateTrend.UP, Icons.Default.Info, Color(0xFFE8F5E9)),
        AquaRate("Katla", "₹280/kg", "+₹20 (3%)", RateTrend.DOWN, Icons.Default.Info, Color(0xFFE3F2FD)),
        AquaRate("Katla", "₹32/kg", "No Change", RateTrend.FLAT, Icons.Default.Info, Color(0xFFFFF9C4))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Today's Aqua Rates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                .background(Color.White)
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
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(rate.iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(rate.icon, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = rate.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rate.price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(rate.trend) {
                            RateTrend.UP -> Icons.Default.TrendingUp
                            RateTrend.DOWN -> Icons.Default.TrendingDown
                            RateTrend.FLAT -> Icons.Default.TrendingFlat
                        },
                        contentDescription = null,
                        tint = when(rate.trend) {
                            RateTrend.UP -> Color(0xFF4CAF50)
                            RateTrend.DOWN -> Color(0xFFF44336)
                            RateTrend.FLAT -> GrayText
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rate.change,
                        fontSize = 12.sp,
                        color = when(rate.trend) {
                            RateTrend.UP -> Color(0xFF4CAF50)
                            RateTrend.DOWN -> Color(0xFFF44336)
                            RateTrend.FLAT -> GrayText
                        }
                    )
                }
            }
        }
    }
}
