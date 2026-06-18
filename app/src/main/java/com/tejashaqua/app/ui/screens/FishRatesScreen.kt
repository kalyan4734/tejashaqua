package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.R
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.data.model.AquaRate
import com.tejashaqua.app.data.model.RateTrend
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.ui.components.RateGraphBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishRatesScreen(onBackClick: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val keyboardController = LocalSoftwareKeyboardController.current

    val fishTypes = listOf(
        "Rohu", "Katla", "Karamosu", "Gaddi chepa", "Pangasius", 
        "Roopchand", "Pandu gappa", "Tilapia", "Chitala", "Koramenu", 
        "Valuga", "Jalla", "Tuna", "Pulasa", "Crab", "Others"
    )
    
    var rates by remember { mutableStateOf<List<AquaRate>>(emptyList()) }
    var lastUpdatedDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    var showGraphSheet by remember { mutableStateOf(false) }
    var selectedRateForGraph by remember { mutableStateOf<AquaRate?>(null) }

    LaunchedEffect(Unit) {
        db.collection("aqua_rates").addSnapshotListener { value, _ ->
            if (value != null) {
                val fetchedMap = value.documents.associateBy({ it.id.lowercase(java.util.Locale.ROOT) }, { doc ->
                    val price = doc.getString("price") ?: "--"
                    val change = doc.getString("change") ?: ""
                    val trendStr = doc.getString("trend") ?: "FLAT"
                    val trend = try { RateTrend.valueOf(trendStr) } catch (_: Exception) { RateTrend.FLAT }
                    val isPrawn = doc.getBoolean("isPrawn") ?: (doc.id.lowercase(java.util.Locale.ROOT) == "prawns")
                    val lastUpdated = doc.getLong("lastUpdated") ?: 0L
                    
                    AquaRate(doc.id, price, change, trend, isPrawn, lastUpdated)
                })

                rates = fishTypes.map { fish ->
                    fetchedMap[fish.lowercase(java.util.Locale.ROOT)] ?: AquaRate(fish)
                }

                val maxTs = rates.maxOfOrNull { it.lastUpdated } ?: 0L
                if (maxTs > 0) {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    lastUpdatedDate = sdf.format(java.util.Date(maxTs))
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
                        Text(stringResource(R.string.fish_rates_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (lastUpdatedDate.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.last_updated, lastUpdatedDate),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AquaBlue)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column {
                                // Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AquaBlue)
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.fish_table_header), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f).padding(start = 12.dp), fontSize = 13.sp)
                                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.3f)))
                                    Text(stringResource(R.string.average_table_header), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp), fontSize = 13.sp)
                                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.3f)))
                                    Text(stringResource(R.string.price_table_header), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f).padding(start = 12.dp), fontSize = 13.sp)
                                }

                                // Rows
                                rates.forEachIndexed { index, rate ->
                                    val isEven = index % 2 == 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isEven) Color.White else Color(0xFFF1F8FF))
                                            .clickable { 
                                                selectedRateForGraph = rate
                                                showGraphSheet = true
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = rate.getDisplayName(), 
                                            color = Color.Black, 
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1.2f).padding(start = 12.dp), 
                                            fontSize = 14.sp
                                        )
                                        
                                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                                        
                                        Text(
                                            text = stringResource(R.string.one_kg),
                                            color = GrayText, 
                                            modifier = Modifier.weight(1f).padding(start = 12.dp), 
                                            fontSize = 14.sp
                                        )
                                        
                                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray.copy(alpha = 0.3f)))
                                        
                                        Row(
                                            modifier = Modifier.weight(1.2f).padding(start = 12.dp, end = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (rate.price == "--") "N/A" else rate.price, 
                                                    color = AquaBlue, 
                                                    fontWeight = FontWeight.Bold, 
                                                    fontSize = 14.sp
                                                )
                                                if (rate.change.isNotEmpty() && rate.change != stringResource(R.string.no_change)) {
                                                    Text(
                                                        text = rate.change,
                                                        fontSize = 11.sp,
                                                        color = if (rate.trend == RateTrend.UP) Color(0xFF4CAF50) else if (rate.trend == RateTrend.DOWN) Color(0xFFF44336) else GrayText
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                                                contentDescription = null, 
                                                tint = GrayText.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    if (index < rates.size - 1) {
                                        HorizontalDivider(color = Color(0xFFEEEEEE).copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
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
