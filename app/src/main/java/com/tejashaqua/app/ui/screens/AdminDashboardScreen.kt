package com.tejashaqua.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.tejashaqua.app.ui.theme.AquaLightBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBackClick: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.fish_rates), 
        stringResource(R.string.prawn_rates),
        "Reports"
    )
    
    var showDatePicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(brush = Brush.verticalGradient(colors = listOf(AquaBlue, AquaLightBlue)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        stringResource(R.string.admin_portal),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Surface(
                        onClick = { 
                            keyboardController?.hide()
                            showDatePicker = true 
                        },
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sdf.format(Date(selectedDate)),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = Color.White,
                                height = 3.dp
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { 
                                keyboardController?.hide()
                                selectedTab = index 
                            },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .imePadding()
        ) {
            when (selectedTab) {
                0 -> FishRatesAdmin(selectedDate, onBackClick)
                1 -> PrawnRatesAdmin(selectedDate, onBackClick)
                2 -> ReportsAdmin()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FishRatesAdmin(selectedDate: Long, onBackClick: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var rates by remember { mutableStateOf<List<AquaRate>>(emptyList()) }
    var previousRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var noDataAvailable by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptions = KeyboardOptions(
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    val fishTypes = listOf("Rohu", "Pangasius", "Roopchand")

    LaunchedEffect(selectedDate) {
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
                        prevMap[type] = 0.0
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
            
            noDataAvailable = rates.all { 
                it.price == context.getString(R.string.no_data_available) || it.price.isBlank()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Rate Status", fontSize = 12.sp, color = GrayText)
                        Text(
                            if (noDataAvailable) "Marked as No Data" else "Updating Rates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (noDataAvailable) Color(0xFFD32F2F) else AquaBlue
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.no_data_available), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = noDataAvailable,
                            onCheckedChange = { 
                                noDataAvailable = it
                                if (it) {
                                    rates = rates.map { r -> r.copy(price = context.getString(R.string.no_data_available), change = "", trend = RateTrend.FLAT) }
                                }
                            }
                        )
                    }
                }
                
                if (!noDataAvailable) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            rates = rates.map { rate ->
                                val prevPrice = previousRates[rate.name] ?: 0.0
                                if (prevPrice > 0) {
                                    rate.copy(price = prevPrice.toString(), change = "", trend = RateTrend.FLAT)
                                } else rate
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-fill from Yesterday", fontSize = 12.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            items(rates) { rate ->
                AdminRateItem(
                    rate = rate,
                    prevPrice = previousRates[rate.name] ?: 0.0,
                    isEnabled = !noDataAvailable,
                    keyboardOptions = keyboardOptions,
                    onPriceChange = { newPrice ->
                        val prevPrice = previousRates[rate.name] ?: 0.0
                        val currentPrice = newPrice.split("-").first().filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                        
                        var newTrend = RateTrend.FLAT
                        var newChange = ""
                        
                        if (prevPrice > 0 && currentPrice > 0) {
                            val diff = (currentPrice - prevPrice).toInt()
                            newTrend = when {
                                diff > 0 -> RateTrend.UP
                                diff < 0 -> RateTrend.DOWN
                                else -> RateTrend.FLAT
                            }
                            newChange = when {
                                diff > 0 -> "+₹$diff"
                                diff < 0 -> "-₹${kotlin.math.abs(diff)}"
                                else -> ""
                            }
                        }
                        
                        rates = rates.map { 
                            if (it.name == rate.name) it.copy(price = newPrice, trend = newTrend, change = newChange) 
                            else it 
                        }
                        if (newPrice != context.getString(R.string.no_data_available)) {
                            noDataAvailable = false
                        }
                    }
                )
            }
        }

        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        isSaving = true
                        val batch = db.batch()
                        rates.forEachIndexed { index, rate ->
                            val ref = db.collection("aqua_rates").document(rate.name)
                            
                            val displayPrice = if (noDataAvailable) {
                                context.getString(R.string.no_data_available)
                            } else {
                                val clean = rate.price.filter { it.isDigit() || it == '.' || it == '-' }
                                if (!rate.isPrawn && clean.isNotEmpty() && !rate.price.contains("/")) {
                                    "₹$clean/kg"
                                } else if (clean.isNotEmpty() && !rate.price.startsWith("₹")) {
                                    "₹${rate.price}"
                                } else {
                                    rate.price
                                }
                            }

                            val data = mutableMapOf(
                                "price" to displayPrice,
                                "change" to if (noDataAvailable) "" else rate.change,
                                "trend" to if (noDataAvailable) RateTrend.FLAT.name else rate.trend.name,
                                "isPrawn" to rate.isPrawn,
                                "lastUpdated" to selectedDate,
                                "notify" to (index == 0)
                            )
                            batch.set(ref, data)
                            
                            val historyId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDate))
                            val cleanPrice = rate.price.split("-").first().filter { it.isDigit() || it == '.' }
                            val priceVal = cleanPrice.toDoubleOrNull() ?: 0.0
                            if (priceVal > 0 && !noDataAvailable) {
                                val historyData = mapOf(
                                    "price" to priceVal,
                                    "timestamp" to selectedDate,
                                    "displayPrice" to displayPrice
                                )
                                batch.set(ref.collection("history").document(historyId), historyData)
                            }
                        }
                        batch.commit().addOnSuccessListener {
                            isSaving = false
                            Toast.makeText(context, "All Rates Updated Successfully", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }.addOnFailureListener {
                            isSaving = false
                            Toast.makeText(context, "Failed to update rates", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE ALL FISH RATES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRateItem(
    rate: AquaRate,
    prevPrice: Double,
    isEnabled: Boolean,
    keyboardOptions: KeyboardOptions,
    onPriceChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = rate.iconBgColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = rate.icon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (rate.isPrawn) Color(0xFF3F51B5) else Color(0xFF009688)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(rate.getDisplayName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                if (prevPrice > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Prev: ₹$prevPrice", fontSize = 11.sp, color = GrayText)
                        if (rate.change.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when(rate.trend) {
                                        RateTrend.UP -> Icons.AutoMirrored.Filled.TrendingUp
                                        RateTrend.DOWN -> Icons.AutoMirrored.Filled.TrendingDown
                                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = when(rate.trend) {
                                        RateTrend.UP -> Color(0xFF4CAF50)
                                        RateTrend.DOWN -> Color(0xFFF44336)
                                        else -> GrayText
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    rate.change,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = rate.price,
                onValueChange = onPriceChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 150 or 150-160") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = GrayText) },
                suffix = { Text("/kg", color = GrayText) },
                shape = RoundedCornerShape(10.dp),
                enabled = isEnabled,
                keyboardOptions = keyboardOptions,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AquaBlue,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    disabledBorderColor = Color(0xFFF5F5F5)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrawnRatesAdmin(selectedDate: Long, onBackClick: () -> Unit) {
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
    var noDataAvailable by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val counts = listOf("100", "90", "80", "70", "60", "50", "47", "45", "40", "37", "35", "30", "25", "20", "200")
    var prices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var previousSummaryPrice by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(selectedMarket) {
        isLoading = true
        db.collection("prawn_rates").document(selectedMarket).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val data = doc.get("rates") as? Map<*, *>
                prices = data?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
                
                noDataAvailable = prices.values.all { 
                    it == context.getString(R.string.no_data_available) || it.isBlank()
                }
            } else {
                prices = emptyMap()
            }
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(selectedDate) {
        val prevId = Calendar.getInstance().apply {
            time = Date(selectedDate)
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(it.time) }
        
        db.collection("aqua_rates").document("Prawns").collection("history").document(prevId).get()
            .addOnSuccessListener { doc ->
                previousSummaryPrice = doc.getDouble("price") ?: 0.0
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Market & Status", fontSize = 12.sp, color = GrayText)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { 
                                keyboardController?.hide()
                                expanded = !expanded 
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .clickable { expanded = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedMarket, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AquaBlue)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AquaBlue)
                            }
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
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.no_data_available), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = noDataAvailable,
                            onCheckedChange = { 
                                noDataAvailable = it
                                if (it) {
                                    prices = counts.associateWith { context.getString(R.string.no_data_available) }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AquaBlue)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(counts) { count ->
                    val price = prices[count] ?: ""
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFE8EAF6),
                                    shape = CircleShape,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(count, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                                    }
                                }
                                Text("Count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            
                            OutlinedTextField(
                                value = price,
                                onValueChange = { newValue: String ->
                                    prices = prices.toMutableMap().apply { put(count, newValue) }
                                    if (newValue != context.getString(R.string.no_data_available)) {
                                        noDataAvailable = false
                                    }
                                },
                                placeholder = { Text("Rate", fontSize = 12.sp) },
                                prefix = { Text("₹", color = GrayText, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = keyboardOptions,
                                enabled = !noDataAvailable,
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AquaBlue,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    disabledContainerColor = Color(0xFFF9F9F9)
                                )
                            )
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        keyboardController?.hide()
                        isSaving = true
                        val batch = db.batch()
                        
                        val marketRef = db.collection("prawn_rates").document(selectedMarket)
                        batch.set(marketRef, mapOf(
                            "rates" to prices,
                            "lastUpdated" to selectedDate
                        ))

                        if (prices.containsKey("100")) {
                            val summaryRef = db.collection("aqua_rates").document("Prawns")
                            val price100 = prices["100"] ?: ""
                            
                            if (price100.isNotEmpty()) {
                                val isNoData = price100 == context.getString(R.string.no_data_available)
                                val displayPrice = if (isNoData) context.getString(R.string.no_data_available) else "₹$price100/kg"
                                val currentVal = price100.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                                
                                var trend = RateTrend.FLAT
                                var change = ""
                                
                                if (!isNoData && previousSummaryPrice > 0 && currentVal > 0) {
                                    val diff = (currentVal - previousSummaryPrice).toInt()
                                    trend = when {
                                        diff > 0 -> RateTrend.UP
                                        diff < 0 -> RateTrend.DOWN
                                        else -> RateTrend.FLAT
                                    }
                                    change = when {
                                        diff > 0 -> "+₹$diff"
                                        diff < 0 -> "-₹${kotlin.math.abs(diff)}"
                                        else -> ""
                                    }
                                }

                                batch.set(summaryRef, mapOf(
                                    "price" to displayPrice,
                                    "change" to change,
                                    "trend" to trend.name,
                                    "isPrawn" to true,
                                    "lastUpdated" to selectedDate,
                                    "notify" to true
                                ), com.google.firebase.firestore.SetOptions.merge())

                                val historyId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(selectedDate))
                                if (currentVal > 0 && !isNoData) {
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
                            isSaving = false
                            Toast.makeText(context, "All Prawn Rates Updated Successfully", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        }.addOnFailureListener {
                            isSaving = false
                            Toast.makeText(context, "Failed to update prawn rates", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaBlue),
                    enabled = !isSaving && !isLoading
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE ALL PRAWN RATES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsAdmin() {
    val db = FirebaseFirestore.getInstance()
    var listingReports by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var userReports by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var mostBlockedUsers by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var reportTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isLoading = true
        // Fetch listing reports
        db.collection("reports")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                listingReports = snapshot.documents.map { doc -> 
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data 
                }
            }

        // Fetch user reports
        db.collection("user_reports")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                userReports = snapshot.documents.map { doc -> 
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data 
                }
            }

        // Fetch all users to calculate most blocked
        db.collection("users").get().addOnSuccessListener { snapshot ->
            val blockCounts = mutableMapOf<String, Int>()
            snapshot.documents.forEach { doc ->
                val blocked = doc.get("blockedUsers") as? List<*>
                blocked?.forEach { id ->
                    val userId = id.toString()
                    blockCounts[userId] = (blockCounts[userId] ?: 0) + 1
                }
            }
            mostBlockedUsers = blockCounts.toList().sortedByDescending { it.second }.take(20)
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = reportTab,
            containerColor = Color.White,
            contentColor = AquaBlue,
            divider = {}
        ) {
            Tab(selected = reportTab == 0, onClick = { reportTab = 0 }) {
                Text("Listings (${listingReports.size})", modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
            Tab(selected = reportTab == 1, onClick = { reportTab = 1 }) {
                Text("Users (${userReports.size})", modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
            Tab(selected = reportTab == 2, onClick = { reportTab = 2 }) {
                Text("Blocked (${mostBlockedUsers.size})", modifier = Modifier.padding(12.dp), fontSize = 12.sp)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AquaBlue)
            }
        } else {
            when (reportTab) {
                0, 1 -> {
                    val items = if (reportTab == 0) listingReports else userReports
                    if (items.isEmpty()) {
                        EmptyReportsView()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items) { report ->
                                ReportCard(report, isUserReport = reportTab == 1) { id ->
                                    val collection = if (reportTab == 0) "reports" else "user_reports"
                                    db.collection(collection).document(id).delete().addOnSuccessListener {
                                        if (reportTab == 0) {
                                            listingReports = listingReports.filter { it["id"] != id }
                                        } else {
                                            userReports = userReports.filter { it["id"] != id }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (mostBlockedUsers.isEmpty()) {
                        EmptyReportsView()
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(mostBlockedUsers) { (userId, count) ->
                                BlockedUserCard(userId, count)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyReportsView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No reports found", color = GrayText)
        }
    }
}

@Composable
fun BlockedUserCard(userId: String, blockCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFFFF3E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF9800))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("User ID: $userId", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Blocked by $blockCount other users", fontSize = 12.sp, color = GrayText)
            }
        }
    }
}

@Composable
fun ReportCard(report: Map<String, Any>, isUserReport: Boolean, onDelete: (String) -> Unit) {
    val id = report["id"]?.toString() ?: ""
    val reason = report["reason"]?.toString() ?: "No reason provided"
    val timestamp = report["timestamp"] as? Timestamp
    val dateStr = timestamp?.toDate()?.let { 
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Unknown date"
    
    val targetId = if (isUserReport) report["reportedUserId"]?.toString() else report["listingId"]?.toString()
    val reporterId = report["reporterId"]?.toString() ?: "Unknown"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUserReport) "User Report" else "Listing Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)
                    )
                    Text(text = "Reason: $reason", fontSize = 12.sp, color = Color.Black)
                }
                IconButton(onClick = { onDelete(id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Dismiss", tint = GrayText, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isUserReport) "Reported User ID: $targetId" else "Listing ID: $targetId",
                fontSize = 12.sp,
                color = GrayText,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "By Reporter: $reporterId",
                fontSize = 12.sp,
                color = GrayText
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = GrayText,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
