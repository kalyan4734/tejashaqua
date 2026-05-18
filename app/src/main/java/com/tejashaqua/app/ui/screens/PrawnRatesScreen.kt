package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

data class PrawnRate(
    val count: String,
    val price: String,
    val change: String,
    val isUp: Boolean? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrawnRatesScreen(onBackClick: () -> Unit) {
    var selectedMarket by remember { mutableStateOf("Bhimavaram") }
    val markets = listOf("Bhimavaram", "Nellore", "Kakinada", "Machilipatnam")
    var expanded by remember { mutableStateOf(false) }

    val rates = listOf(
        PrawnRate("200", "₹175", "0"),
        PrawnRate("100", "₹270", "0"),
        PrawnRate("90", "₹282", "0"),
        PrawnRate("80", "₹300", "0"),
        PrawnRate("70", "₹330", "0"),
        PrawnRate("60", "₹346", "0"),
        PrawnRate("50", "₹365", "0"),
        PrawnRate("45", "₹372", "0"),
        PrawnRate("40", "₹400", "0"),
        PrawnRate("35", "₹412", "0"),
        PrawnRate("30", "₹515", "0")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Prawn Count-wise Rates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Box
            item {
                Surface(
                    color = Color(0xFFE3F2FD).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = null, 
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Count = prawns per kg. Lower count = bigger prawn = higher price. Rates in ₹/kg.",
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Market Selection
            item {
                Column {
                    Text("Select Seashore / Market", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMarket,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.LightGray,
                                focusedBorderColor = AquaBlue
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            markets.forEach { market ->
                                DropdownMenuItem(
                                    text = { Text(market) },
                                    onClick = {
                                        selectedMarket = market
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Date and Variation Text
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = GrayText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("19-02-2026 • Rates vary by seashore location", fontSize = 12.sp, color = GrayText)
                }
            }

            // Table
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
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Count/kg", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.3f)))
                            Text("₹/kg", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 16.dp))
                        }

                        // Rows
                        rates.forEachIndexed { index, rate ->
                            val isEven = index % 2 == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isEven) Color.White else Color(0xFFF1F8FF))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rate.count, color = Color.Black, modifier = Modifier.weight(1f))
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                                Text(rate.price, color = AquaBlue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 16.dp))
                            }
                            if (index < rates.size - 1) {
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                            }
                        }
                    }
                }
            }
        }
    }
}
