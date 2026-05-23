package com.tejashaqua.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DataExploration
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.data.model.AquaRate
import com.tejashaqua.app.data.model.RateTrend
import com.tejashaqua.app.ui.theme.AquaBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBackClick: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fish Rates", "Prawn Rates")
    
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin Portal", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .clickable { showDatePicker = true }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(sdf.format(Date(selectedDate)), color = Color.White, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = AquaBlue) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> FishRatesAdmin(selectedDate)
                1 -> PrawnRatesAdmin(selectedDate)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishRatesAdmin(selectedDate: Long) {
    val db = FirebaseFirestore.getInstance()
    var rates by remember { mutableStateOf<List<AquaRate>>(emptyList()) }
    var previousRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val context = LocalContext.current

    val fishTypes = listOf(
        "Prawns", "Rohu", "Katla", "Karamosu", "Gaddi chepa", "Pangasius", 
        "Roopchand", "Pandu gappa", "Tilapia", "Chitala", "Koramenu", 
        "Valuga", "Engilayi", "Jalla", "Tuna", "Pulasa", "Crab", "Others"
    )

    // Fetch current rates and previous day rates
    LaunchedEffect(selectedDate) {
        // Fetch previous day history for auto-calculation
        val prevCal = Calendar.getInstance().apply {
            time = Date(selectedDate)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val prevId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(prevCal.time)
        
        val prevMap = mutableMapOf<String, Double>()
        fishTypes.forEach { type ->
            db.collection("aqua_rates").document(type).collection("history").document(prevId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        prevMap[type] = doc.getDouble("price") ?: 0.0
                    } else {
                        prevMap[type] = 0.0 // No prev data
                    }
                    if (prevMap.size == fishTypes.size) {
                        previousRates = prevMap.toMap()
                    }
                }
        }

        db.collection("aqua_rates").get().addOnSuccessListener { snapshot ->
            val fetched = snapshot.documents.associateBy({ it.id }, { doc ->
                val price = doc.getString("price") ?: ""
                val change = doc.getString("change") ?: ""
                val trendStr = doc.getString("trend") ?: "FLAT"
                val trend = try { RateTrend.valueOf(trendStr) } catch (_: Exception) { RateTrend.FLAT }
                val isPrawn = doc.getBoolean("isPrawn") ?: (doc.id == "Prawns")
                AquaRate(doc.id, price, change, trend, isPrawn)
            })
            rates = fishTypes.map { name -> fetched[name] ?: AquaRate(name, isPrawn = name == "Prawns") }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(rates) { rate ->
            var priceText by remember(rate.price) { mutableStateOf(rate.price) }
            var changeText by remember(rate.change) { mutableStateOf(rate.change) }
            var trendState by remember(rate.trend) { mutableStateOf(rate.trend) }

            // Auto-calculate change and trend when price changes
            LaunchedEffect(priceText) {
                val prevPrice = previousRates[rate.name] ?: 0.0
                val currentPrice = priceText.split("-").first().filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                
                if (prevPrice > 0 && currentPrice > 0) {
                    val diff = (currentPrice - prevPrice).toInt()
                    trendState = when {
                        diff > 0 -> RateTrend.UP
                        diff < 0 -> RateTrend.DOWN
                        else -> RateTrend.FLAT
                    }
                    changeText = when {
                        diff > 0 -> "+₹$diff"
                        diff < 0 -> "-₹${Math.abs(diff)}"
                        else -> "No Change"
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(rate.getDisplayName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Price (e.g. ₹160/kg)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Button(
                            onClick = {
                                val data = mapOf(
                                    "price" to priceText,
                                    "change" to changeText,
                                    "trend" to trendState.name,
                                    "isPrawn" to rate.isPrawn,
                                    "lastUpdated" to selectedDate
                                )
                                // Update current rate
                                db.collection("aqua_rates").document(rate.name).set(data)
                                    .addOnSuccessListener { 
                                        // Save to history for the graph
                                        val historyId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDate))
                                        val cleanPrice = priceText.split("-").first().filter { it.isDigit() || it == '.' }
                                        val priceVal = cleanPrice.toDoubleOrNull() ?: 0.0
                                        if (priceVal > 0) {
                                            val historyData = mapOf(
                                                "price" to priceVal,
                                                "timestamp" to selectedDate,
                                                "displayPrice" to priceText
                                            )
                                            db.collection("aqua_rates").document(rate.name)
                                                .collection("history").document(historyId).set(historyData)
                                        }
                                        Toast.makeText(context, "${rate.name} updated", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrawnRatesAdmin(selectedDate: Long) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var selectedMarket by remember { mutableStateOf("Bhimavaram") }
    val markets = listOf("Bhimavaram", "Nellore", "Kakinada", "Machilipatnam")
    var expanded by remember { mutableStateOf(false) }

    val counts = listOf("200", "100", "90", "80", "70", "60", "50", "45", "40", "35", "30")
    var prices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(selectedMarket) {
        db.collection("prawn_rates").document(selectedMarket).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val data = doc.get("rates") as? Map<*, *>
                prices = data?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
            } else {
                prices = emptyMap()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedMarket,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Market") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                markets.forEach { market ->
                    DropdownMenuItem(text = { Text(market) }, onClick = { selectedMarket = market; expanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(counts) { count ->
                var price by remember(count, prices) { mutableStateOf(prices[count] ?: "") }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Count $count", modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("₹/kg") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val newPrices = prices.toMutableMap()
                        newPrices[count] = price
                        val data = mapOf(
                            "rates" to newPrices,
                            "lastUpdated" to selectedDate
                        )
                        db.collection("prawn_rates").document(selectedMarket).set(data)
                            .addOnSuccessListener { 
                                prices = newPrices
                                Toast.makeText(context, "Count $count updated for ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))}", Toast.LENGTH_SHORT).show() 
                            }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = AquaBlue)
                    }
                }
            }
        }
    }
}

