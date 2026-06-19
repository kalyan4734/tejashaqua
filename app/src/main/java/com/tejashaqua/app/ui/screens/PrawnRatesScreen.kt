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
import com.tejashaqua.app.R
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrawnRatesScreen(onBackClick: () -> Unit) {
    val markets = listOf(
        "Bhimavaram" to stringResource(R.string.market_bhimavaram),
        "Nellore" to stringResource(R.string.market_nellore),
        "Kakinada" to stringResource(R.string.market_kakinada),
        "Machilipatnam" to stringResource(R.string.market_machilipatnam)
    )
    var selectedMarketId by remember { mutableStateOf("Bhimavaram") }
    var expanded by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val keyboardController = LocalSoftwareKeyboardController.current

    val counts = listOf("200", "100", "90", "80", "70", "60", "50", "45", "40", "35", "30")
    var prices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var lastUpdatedDate by remember { mutableStateOf("") }

    val currentMarketName = markets.find { it.first == selectedMarketId }?.second ?: selectedMarketId

    LaunchedEffect(selectedMarketId) {
        db.collection("prawn_rates").document(selectedMarketId).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                val data = doc.get("rates") as? Map<*, *>
                prices = data?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
                
                val ts = doc.getLong("lastUpdated")
                if (ts != null) {
                    val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                    lastUpdatedDate = sdf.format(java.util.Date(ts))
                }
            } else {
                prices = emptyMap()
                lastUpdatedDate = ""
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.prawn_rates_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
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
                            text = stringResource(R.string.prawn_info_text),
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
                    Text(stringResource(R.string.select_market), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { 
                            keyboardController?.hide()
                            expanded = !expanded 
                        }
                    ) {
                        OutlinedTextField(
                            value = currentMarketName,
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
                            markets.forEach { marketPair ->
                                DropdownMenuItem(
                                    text = { Text(marketPair.second) },
                                    onClick = {
                                        keyboardController?.hide()
                                        selectedMarketId = marketPair.first
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
                    Text("${if (lastUpdatedDate.isNotEmpty()) lastUpdatedDate else "--"} • ${stringResource(R.string.rates_vary)}", fontSize = 12.sp, color = GrayText)
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
                            Text(stringResource(R.string.count_per_kg), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.3f)))
                            Text(stringResource(R.string.rupees_per_kg), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 16.dp))
                        }

                        // Rows
                        counts.forEachIndexed { index, count ->
                            val price = prices[count] ?: "--"
                            val isEven = index % 2 == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isEven) Color.White else Color(0xFFF1F8FF))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(count, color = Color.Black, modifier = Modifier.weight(1f))
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                                Text(if (price == "--") price else "₹${CurrencyUtils.formatPrice(price)}", color = AquaBlue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 16.dp))
                            }
                            if (index < counts.size - 1) {
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                            }
                        }
                    }
                }
            }
        }
    }
}
