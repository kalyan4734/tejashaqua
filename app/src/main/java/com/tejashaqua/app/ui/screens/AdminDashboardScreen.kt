package com.tejashaqua.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.LocaleList
import com.tejashaqua.app.R
import com.tejashaqua.app.utils.LocaleHelper
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
    val tabs = listOf(stringResource(R.string.fish_rates), stringResource(R.string.prawn_rates))
    
    var showDatePicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                title = { Text(stringResource(R.string.admin_portal), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .clickable { 
                                keyboardController?.hide()
                                showDatePicker = true 
                            }
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
        ) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = AquaBlue) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            keyboardController?.hide()
                            selectedTab = index 
                        },
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    val fishTypes = listOf(
        "Rohu", "Pangasius", "Roopchand"
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(rates) { rate ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(rate.getDisplayName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = rate.price,
                            onValueChange = { newPrice ->
                                val prevPrice = previousRates[rate.name] ?: 0.0
                                val currentPrice = newPrice.split("-").first().filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                                
                                var newTrend = rate.trend
                                var newChange = rate.change
                                
                                if (prevPrice > 0 && currentPrice > 0) {
                                    val diff = (currentPrice - prevPrice).toInt()
                                    newTrend = when {
                                        diff > 0 -> RateTrend.UP
                                        diff < 0 -> RateTrend.DOWN
                                        else -> RateTrend.FLAT
                                    }
                                    newChange = when {
                                        diff > 0 -> "+₹$diff"
                                        diff < 0 -> "-₹${Math.abs(diff)}"
                                        else -> "No Change"
                                    }
                                }
                                
                                rates = rates.map { 
                                    if (it.name == rate.name) it.copy(price = newPrice, trend = newTrend, change = newChange) 
                                    else it 
                                }
                            },
                            label = { Text(stringResource(R.string.price_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = keyboardOptions
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                val batch = db.batch()
                rates.forEach { rate ->
                    val ref = db.collection("aqua_rates").document(rate.name)
                    val data = mapOf(
                        "price" to rate.price,
                        "change" to rate.change,
                        "trend" to rate.trend.name,
                        "isPrawn" to rate.isPrawn,
                        "lastUpdated" to selectedDate
                    )
                    batch.set(ref, data)
                    
                    // Save to history for the graph
                    val historyId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDate))
                    val cleanPrice = rate.price.split("-").first().filter { it.isDigit() || it == '.' }
                    val priceVal = cleanPrice.toDoubleOrNull() ?: 0.0
                    if (priceVal > 0) {
                        val historyData = mapOf(
                            "price" to priceVal,
                            "timestamp" to selectedDate,
                            "displayPrice" to rate.price
                        )
                        batch.set(ref.collection("history").document(historyId), historyData)
                    }
                }
                batch.commit().addOnSuccessListener {
                    Toast.makeText(context, "All Rates Updated Successfully", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE ALL RATES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrawnRatesAdmin(selectedDate: Long) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )
    var selectedMarket by remember { mutableStateOf("Bhimavaram") }
    val markets = listOf("Bhimavaram", "Nellore", "Kakinada", "Machilipatnam")
    var expanded by remember { mutableStateOf(false) }

    val counts = listOf("100", "90", "80", "70", "60", "50", "47", "45", "40", "37", "35", "30", "25", "20", "200")
    var prices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var previousSummaryPrice by remember { mutableDoubleStateOf(0.0) }

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

    LaunchedEffect(selectedDate) {
        val prevCal = Calendar.getInstance().apply {
            time = Date(selectedDate)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val prevId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(prevCal.time)
        
        db.collection("aqua_rates").document("Prawns").collection("history").document(prevId).get()
            .addOnSuccessListener { doc ->
                previousSummaryPrice = doc.getDouble("price") ?: 0.0
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { 
                keyboardController?.hide()
                expanded = !expanded 
            }
        ) {
            OutlinedTextField(
                value = selectedMarket,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_market_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                markets.forEach { market ->
                    DropdownMenuItem(
                        text = { Text(market) }, 
                        onClick = { 
                            keyboardController?.hide()
                            selectedMarket = market
                            expanded = false 
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(counts) { count ->
                val price = prices[count] ?: ""
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.count_label, count), modifier = Modifier.width(80.dp), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = price,
                        onValueChange = { newValue ->
                            prices = prices.toMutableMap().apply { put(count, newValue) }
                        },
                        label = { Text(stringResource(R.string.rupees_per_kg_label)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = keyboardOptions
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                val batch = db.batch()
                
                // 1. Update market-specific prawn rates
                val marketRef = db.collection("prawn_rates").document(selectedMarket)
                batch.set(marketRef, mapOf(
                    "rates" to prices,
                    "lastUpdated" to selectedDate
                ))

                // 2. Automatically update summary "Prawns" rate in aqua_rates
                if (prices.containsKey("100")) {
                    val summaryRef = db.collection("aqua_rates").document("Prawns")
                    val price100 = prices["100"] ?: ""
                    
                    if (price100.isNotEmpty()) {
                        val displayPrice = "₹$price100"
                        val currentVal = price100.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                        
                        var trend = RateTrend.FLAT
                        var change = context.getString(R.string.no_change)
                        
                        if (previousSummaryPrice > 0 && currentVal > 0) {
                            val diff = (currentVal - previousSummaryPrice).toInt()
                            trend = when {
                                diff > 0 -> RateTrend.UP
                                diff < 0 -> RateTrend.DOWN
                                else -> RateTrend.FLAT
                            }
                            change = when {
                                diff > 0 -> "+₹$diff"
                                diff < 0 -> "-₹${Math.abs(diff)}"
                                else -> context.getString(R.string.no_change)
                            }
                        }

                        batch.set(summaryRef, mapOf(
                            "price" to displayPrice,
                            "change" to change,
                            "trend" to trend.name,
                            "isPrawn" to true,
                            "lastUpdated" to selectedDate
                        ), com.google.firebase.firestore.SetOptions.merge())

                        // 3. Save to history for the summary graph
                        val historyId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDate))
                        if (currentVal > 0) {
                            val historyData = mapOf(
                                "price" to currentVal,
                                "timestamp" to selectedDate,
                                "displayPrice" to displayPrice
                            )
                            batch.set(summaryRef.collection("history").document(historyId), historyData)
                        }
                    }
                }

                batch.commit().addOnSuccessListener { 
                    Toast.makeText(context, "All Prawn Rates Updated Successfully", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SAVE ALL PRAWN RATES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

