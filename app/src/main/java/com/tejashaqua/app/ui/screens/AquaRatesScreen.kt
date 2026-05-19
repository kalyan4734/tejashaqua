package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.data.model.AquaRate
import com.tejashaqua.app.data.model.RateTrend
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.ui.components.RateGraphBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaRatesScreen(onBackClick: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var rates by remember { mutableStateOf<List<AquaRate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showGraphSheet by remember { mutableStateOf(false) }
    var selectedRateForGraph by remember { mutableStateOf<AquaRate?>(null) }

    val fishTypes = listOf(
        "Prawns", "Rohu", "Katla", "Karamosu", "Gaddi chepa", "Pangasius", 
        "Roopchand", "Pandu gappa", "Tilapia", "Chitala", "Koramenu", 
        "Valuga", "Engilayi", "Jalla", "Tuna", "Pulasa", "Crab", "Others"
    )

    LaunchedEffect(Unit) {
        db.collection("aqua_rates")
            .addSnapshotListener { value, error ->
                if (value != null) {
                    val fetchedMap = value.documents.associateBy({ it.id }, { doc ->
                        val price = doc.getString("price") ?: "--"
                        val change = doc.getString("change") ?: "No Change"
                        val trendStr = doc.getString("trend") ?: "FLAT"
                        val trend = try { RateTrend.valueOf(trendStr) } catch (e: Exception) { RateTrend.FLAT }
                        val isPrawn = doc.getBoolean("isPrawn") ?: (doc.id == "Prawns")
                        val lastUpdated = doc.getLong("lastUpdated") ?: 0L
                        
                        AquaRate(doc.id, price, change, trend, isPrawn, lastUpdated)
                    })

                    // Merge with the fixed list of fish types
                    rates = fishTypes.map { fish ->
                        fetchedMap[fish] ?: AquaRate(fish, "--", "No Change", RateTrend.FLAT, isPrawn = fish == "Prawns")
                    }
                }
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.today_aqua_rates), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        val latestUpdate = rates.maxOfOrNull { it.lastUpdated } ?: 0L
                        if (latestUpdate > 0) {
                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            Text(
                                text = "Last Updated: ${sdf.format(java.util.Date(latestUpdate))}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AquaBlue)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F9FA))
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(rates) { rate ->
                        RateItemCard(
                            rate = rate,
                            onClick = {
                                selectedRateForGraph = rate
                                showGraphSheet = true
                            }
                        )
                    }
                }
            }

            if (showGraphSheet && selectedRateForGraph != null) {
                RateGraphBottomSheet(
                    rate = selectedRateForGraph!!,
                    onDismiss = { showGraphSheet = false }
                )
            }
        }
    }
}

@Composable
fun RateItemCard(rate: AquaRate, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = rate.getDisplayName(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = stringResource(R.string.fresh_water_fish),
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
