package com.tejashaqua.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tejashaqua.app.R
import com.tejashaqua.app.data.model.AquaRate
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.data.model.RateTrend
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.AquaLightBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import com.tejashaqua.app.utils.LocaleHelper
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(onBackClick: () -> Unit) {
    var selectedMainTab by remember { mutableIntStateOf(0) }
    var selectedRatesSubTab by remember { mutableIntStateOf(0) }
    var selectedAdminSubTab by remember { mutableIntStateOf(0) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

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

    val isKeyboardOpen = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (selectedMainTab == 1) {
                    TabRow(
                        selectedTabIndex = selectedRatesSubTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedRatesSubTab < tabPositions.size) {
                                Box(
                                    Modifier
                                        .tabIndicatorOffset(tabPositions[selectedRatesSubTab])
                                        .height(3.dp)
                                        .background(color = Color.White)
                                )
                            }
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedRatesSubTab == 0,
                            onClick = { selectedRatesSubTab = 0 },
                            text = { Text(stringResource(R.string.fish_rates), color = Color.White, fontWeight = if (selectedRatesSubTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedRatesSubTab == 1,
                            onClick = { selectedRatesSubTab = 1 },
                            text = { Text(stringResource(R.string.prawn_rates), color = Color.White, fontWeight = if (selectedRatesSubTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                } else if (selectedMainTab == 2) {
                    TabRow(
                        selectedTabIndex = selectedAdminSubTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedAdminSubTab < tabPositions.size) {
                                Box(
                                    Modifier
                                        .tabIndicatorOffset(tabPositions[selectedAdminSubTab])
                                        .height(3.dp)
                                        .background(color = Color.White)
                                )
                            }
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedAdminSubTab == 0,
                            onClick = { selectedAdminSubTab = 0 },
                            text = { Text(stringResource(R.string.users), color = Color.White, fontWeight = if (selectedAdminSubTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedAdminSubTab == 1,
                            onClick = { selectedAdminSubTab = 1 },
                            text = { Text(stringResource(R.string.reports), color = Color.White, fontWeight = if (selectedAdminSubTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedAdminSubTab == 2,
                            onClick = { selectedAdminSubTab = 2 },
                            text = { Text("Notify", color = Color.White, fontWeight = if (selectedAdminSubTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (!isKeyboardOpen) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        Triple(stringResource(R.string.stats), Icons.Default.Dashboard, 0),
                        Triple("Rates", Icons.Default.SetMeal, 1),
                        Triple("Admin", Icons.Default.ManageAccounts, 2)
                    )
                    
                    items.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedMainTab == index,
                            onClick = { 
                                keyboardController?.hide()
                                selectedMainTab = index 
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedMainTab == index) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(icon, contentDescription = label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AquaBlue,
                                selectedTextColor = AquaBlue,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = GrayText,
                                unselectedTextColor = GrayText
                            )
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
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedMainTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> StatsAdmin()
                        1 -> {
                            if (selectedRatesSubTab == 0) FishRatesAdmin(selectedDate, onBackClick)
                            else PrawnRatesAdmin(selectedDate, onBackClick)
                        }
                        2 -> {
                            when (selectedAdminSubTab) {
                                0 -> UsersAdmin()
                                1 -> ReportsAdmin()
                                else -> NotificationsAdmin()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: Map<String, Int>,
    colors: Map<String, Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum().toFloat()
    if (total == 0f) return

    val sweepAngles = remember(data) {
        val angles = mutableListOf<Float>()
        data.values.forEach { count ->
            angles.add(360f * count / total)
        }
        angles
    }

    // Animation state
    var animationPlayed by remember { mutableStateOf(false) }
    val animateSweepAngle by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "pie_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (total > 0) {
            Canvas(modifier = Modifier.size(200.dp)) {
                var startAngle = -90f
                data.entries.forEachIndexed { index, entry ->
                    val sweepAngle = sweepAngles[index] * animateSweepAngle
                    drawArc(
                        color = colors[entry.key] ?: Color.Gray,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 40.dp.toPx())
                    )
                    startAngle += sweepAngle
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total",
                fontSize = 14.sp,
                color = GrayText
            )
            Text(
                text = total.toInt().toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun GrowthChart(
    data: List<Pair<String, Int>>,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No growth data available", color = GrayText)
                }
            } else {
                val maxValue = (data.maxOfOrNull { it.second } ?: 0).toFloat().coerceAtLeast(1f)
                
                Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp, end = 10.dp)) {
                    val width = size.width
                    val height = size.height
                    val spacing = width / (data.size - 1).coerceAtLeast(1)
                    
                    val points = data.mapIndexed { index, pair ->
                        val x = index * spacing
                        val y = height - (pair.second / maxValue * height)
                        androidx.compose.ui.geometry.Offset(x, y)
                    }
                    
                    // Draw grid lines
                    for (i in 0..4) {
                        val y = height - (i * height / 4)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    
                    // Draw line
                    if (points.size > 1) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = AquaBlue,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        
                        // Fill under line
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            addPath(path)
                            lineTo(points.last().x, height)
                            lineTo(points.first().x, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(AquaBlue.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                    }
                    
                    // Draw points and labels
                    points.forEachIndexed { index, offset ->
                        drawCircle(
                            color = AquaBlue,
                            radius = 4.dp.toPx(),
                            center = offset
                        )
                    }
                }
                
                // Horizontal axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEachIndexed { index, pair ->
                        if (index % 2 == 0 || index == data.size - 1) {
                            Text(pair.first, fontSize = 10.sp, color = GrayText)
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsAdmin() {
    val db = FirebaseFirestore.getInstance()
    var categoryCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalPosts by remember { mutableIntStateOf(0) }
    var listingsToday by remember { mutableIntStateOf(0) }
    var totalUsers by remember { mutableIntStateOf(0) }
    var totalReports by remember { mutableIntStateOf(0) }
    var totalChats by remember { mutableIntStateOf(0) }
    var blockedUsersCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var period by remember { mutableStateOf("Weekly") }
    var appDownloadsCount by remember { mutableIntStateOf(0) }
    
    val revenue = "₹0.00"
    val pendingApprovals = 0
    var growthData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(period) {
        isLoading = true
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        val limitDate = Calendar.getInstance()
        when (period) {
            "Daily" -> limitDate.add(Calendar.DAY_OF_YEAR, -1)
            "Weekly" -> limitDate.add(Calendar.DAY_OF_YEAR, -7)
            "Monthly" -> limitDate.add(Calendar.MONTH, -1)
        }
        val startTime = limitDate.timeInMillis

        db.collection("listings").get().addOnSuccessListener { snapshot ->
            val counts = mutableMapOf<String, Int>()
            var todayCount = 0
            val growthMap = mutableMapOf<String, Int>()
            val sdf = when(period) {
                "Monthly" -> SimpleDateFormat("dd MMM", Locale.getDefault())
                else -> SimpleDateFormat("EEE", Locale.getDefault())
            }

            snapshot.documents.forEach { doc ->
                val category = doc.getString("category") ?: "UNKNOWN"
                counts[category] = (counts[category] ?: 0) + 1
                
                val timestamp = doc.getLong("timestamp") ?: 0L
                if (timestamp >= startOfToday) {
                    todayCount++
                }
                
                if (timestamp >= startTime) {
                    // Use a sortable key for growthMap: YYYYMMDD or YYYY-WW or similar
                    val sortKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
                    val displayLabel = sdf.format(Date(timestamp))
                    // Combine them to sort later
                    growthMap[sortKey + "|" + displayLabel] = (growthMap[sortKey + "|" + displayLabel] ?: 0) + 1
                }
            }
            categoryCounts = counts
            totalPosts = snapshot.size()
            listingsToday = todayCount
            
            growthData = growthMap.toList()
                .sortedBy { it.first.split("|")[0] }
                .map { it.first.split("|")[1] to it.second }
            
            db.collection("users").get().addOnSuccessListener { userSnapshot ->
                totalUsers = userSnapshot.size()
                
                val blockCounts = mutableMapOf<String, Int>()
                userSnapshot.documents.forEach { doc ->
                    val blocked = doc.get("blockedUsers") as? List<*>
                    blocked?.forEach { id ->
                        val bId = id.toString()
                        blockCounts[bId] = (blockCounts[bId] ?: 0) + 1
                    }
                }
                blockedUsersCount = blockCounts.size
                
                db.collection("reports").get().addOnSuccessListener { reportSnapshot ->
                    totalReports = reportSnapshot.size()
                    
                    db.collection("chats").get().addOnSuccessListener { chatSnapshot ->
                        totalChats = chatSnapshot.size()
                        
                        db.collection("app_config").document("version").get().addOnSuccessListener { configDoc ->
                            appDownloadsCount = configDoc.getLong("download_count")?.toInt() ?: totalUsers
                            isLoading = false
                        }.addOnFailureListener {
                            appDownloadsCount = totalUsers
                            isLoading = false
                        }
                    }.addOnFailureListener { isLoading = false }
                }.addOnFailureListener { isLoading = false }
            }.addOnFailureListener { isLoading = false }
        }.addOnFailureListener {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AquaBlue)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Overall Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    val cardModifier = Modifier.weight(1f).heightIn(min = 120.dp)
                    StatCard("Total Users", totalUsers.toString(), Color(0xFF673AB7), Icons.Default.People, cardModifier)
                    StatCard("Active Listings", totalPosts.toString(), AquaBlue, Icons.Default.Inventory, cardModifier)
                    StatCard("Posted Today", listingsToday.toString(), Color(0xFF009688), Icons.Default.Today, cardModifier)
                    StatCard("Revenue", revenue, Color(0xFF2E7D32), Icons.Default.MonetizationOn, cardModifier)
                    StatCard("Pending", pendingApprovals.toString(), Color(0xFFF57C00), Icons.Default.HourglassEmpty, cardModifier)
                    StatCard("Reports", totalReports.toString(), Color(0xFFD32F2F), Icons.Default.Report, cardModifier)
                    StatCard("Active Chats", totalChats.toString(), Color(0xFF0288D1), Icons.AutoMirrored.Filled.Chat, cardModifier)
                    StatCard("Blocked Users", blockedUsersCount.toString(), Color(0xFF455A64), Icons.Default.Block, cardModifier)
                    StatCard("Downloads", appDownloadsCount.toString(), Color(0xFF5D4037), Icons.Default.Download, cardModifier)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Growth Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Row {
                        listOf("Daily", "Weekly", "Monthly").forEach { p ->
                            FilterChip(
                                selected = period == p,
                                onClick = { period = p },
                                label = { Text(p, fontSize = 12.sp) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                GrowthChart(
                    data = growthData,
                    title = "$period Listing Growth",
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Posts by Category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val categoryColors = remember {
                            ListingCategory.entries.associate { category ->
                                category.name to getCategoryColor(category)
                            }
                        }
                        
                        PieChart(
                            data = categoryCounts,
                            colors = categoryColors,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ListingCategory.entries.forEach { category ->
                                val count = categoryCounts[category.name] ?: 0
                                if (count > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Box(modifier = Modifier.size(10.dp).background(categoryColors[category.name] ?: Color.Gray, CircleShape))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = getCategoryName(category),
                                            fontSize = 12.sp,
                                            color = GrayText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(ListingCategory.entries.toList()) { category ->
                val count = categoryCounts[category.name] ?: 0
                if (count > 0) {
                    CategoryStatItem(category, count)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label, 
                color = Color.White.copy(alpha = 0.8f), 
                fontSize = 12.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = value, 
                color = Color.White, 
                fontSize = 26.sp, 
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp
            )
        }
    }
}

@Composable
fun CategoryStatItem(category: ListingCategory, count: Int) {
    val categoryColor = getCategoryColor(category)
    val displayName = getCategoryName(category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(categoryColor, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            
            Surface(
                color = categoryColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun getCategoryName(category: ListingCategory): String = when (category) {
    ListingCategory.FISH -> stringResource(R.string.cat_fish_seed)
    ListingCategory.PRAWNS -> stringResource(R.string.cat_prawns)
    ListingCategory.EQUIPMENTS -> stringResource(R.string.cat_equipments)
    ListingCategory.VEHICLES -> stringResource(R.string.cat_vehicles)
    ListingCategory.FEED -> stringResource(R.string.cat_feed)
    ListingCategory.MEDICINE -> stringResource(R.string.cat_medicine)
    ListingCategory.SERVICES -> stringResource(R.string.cat_services)
    ListingCategory.TANKS -> stringResource(R.string.cat_tanks)
    ListingCategory.BUSINESS -> stringResource(R.string.cat_business)
    ListingCategory.JOBS -> stringResource(R.string.cat_jobs)
}

fun getCategoryColor(category: ListingCategory): Color = when (category) {
    ListingCategory.FISH -> Color(0xFF009688)
    ListingCategory.PRAWNS -> Color(0xFF3F51B5)
    ListingCategory.EQUIPMENTS -> Color(0xFF1976D2)
    ListingCategory.VEHICLES -> Color(0xFF0288D1)
    ListingCategory.FEED -> Color(0xFFE65100)
    ListingCategory.MEDICINE -> Color(0xFFD81B60)
    ListingCategory.SERVICES -> Color(0xFFF57C00)
    ListingCategory.TANKS -> Color(0xFF388E3C)
    ListingCategory.BUSINESS -> Color(0xFFB71C1C)
    ListingCategory.JOBS -> Color(0xFF673AB7)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersAdmin() {
    val db = FirebaseFirestore.getInstance()
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    
    var showDirectNotifySheet by remember { mutableStateOf(false) }
    var selectedUserForNotify by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        db.collection("users")
            .orderBy("joinedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                users = snapshot.documents.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    val filteredUsers = remember(users, searchText) {
        if (searchText.isBlank()) users
        else users.filter { 
            val name = it["name"]?.toString()?.lowercase() ?: ""
            val phone = it["phone"]?.toString() ?: ""
            name.contains(searchText.lowercase()) || phone.contains(searchText)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AquaBlue,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AquaBlue)
            }
        } else if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found", color = GrayText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Total Registered Users: ${users.size}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrayText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                items(filteredUsers) { user ->
                    UserAdminCard(user, onNotifyClick = {
                        selectedUserForNotify = user
                        showDirectNotifySheet = true
                    })
                }
            }
        }
    }

    if (showDirectNotifySheet && selectedUserForNotify != null) {
        ModalBottomSheet(
            onDismissRequest = { showDirectNotifySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Box(modifier = Modifier.padding(bottom = 32.dp)) {
                NotificationsAdmin(targetUser = selectedUserForNotify) {
                    showDirectNotifySheet = false
                }
            }
        }
    }
}

@Composable
fun UserAdminCard(user: Map<String, Any>, onNotifyClick: () -> Unit = {}) {
    val name = user["name"]?.toString() ?: "No Name"
    val phone = user["phone"]?.toString() ?: "No Phone"
    val joinedAt = user["joinedAt"] as? Long ?: 0L
    val dateStr = if (joinedAt > 0) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(joinedAt))
    } else "Unknown Date"

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
                modifier = Modifier
                    .size(48.dp)
                    .background(AquaBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = AquaBlue,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = phone, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = GrayText, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Joined: $dateStr", fontSize = 11.sp, color = GrayText)
                }
            }

            IconButton(onClick = onNotifyClick) {
                Icon(Icons.Default.NotificationsActive, contentDescription = "Notify", tint = AquaBlue)
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
    var userNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var reportTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        db.collection("users").get().addOnSuccessListener { snapshot ->
            val nameMap = mutableMapOf<String, String>()
            val blockCounts = mutableMapOf<String, Int>()
            
            snapshot.documents.forEach { doc ->
                val userId = doc.id
                val name = doc.getString("name") ?: "Unknown User"
                nameMap[userId] = name
                
                val blocked = doc.get("blockedUsers") as? List<*>
                blocked?.forEach { id ->
                    val bId = id.toString()
                    blockCounts[bId] = (blockCounts[bId] ?: 0) + 1
                }
            }
            userNames = nameMap
            mostBlockedUsers = blockCounts.toList().sortedByDescending { it.second }.take(20)
            
            db.collection("reports")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { rSnapshot ->
                    listingReports = rSnapshot.documents.map { doc -> 
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data 
                    }
                }

            db.collection("user_reports")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { urSnapshot ->
                    userReports = urSnapshot.documents.map { doc -> 
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data 
                    }
                    isLoading = false
                }.addOnFailureListener { isLoading = false }
                
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
                Text("Listings (${listingReports.size})", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
            }
            Tab(selected = reportTab == 1, onClick = { reportTab = 1 }) {
                Text("Users (${userReports.size})", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
            }
            Tab(selected = reportTab == 2, onClick = { reportTab = 2 }) {
                Text("Blocked (${mostBlockedUsers.size})", modifier = Modifier.padding(12.dp), fontSize = 14.sp)
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
                                ReportCard(report, isUserReport = reportTab == 1, userNames = userNames) { id ->
                                    val collection = if (reportTab == 0) "reports" else "user_reports"
                                    db.collection(collection).document(id).delete().addOnSuccessListener {
                                        Toast.makeText(context, "Report dismissed", Toast.LENGTH_SHORT).show()
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
                                BlockedUserCard(userId, userNames[userId] ?: userId, count)
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
fun BlockedUserCard(userId: String, userName: String, blockCount: Int) {
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
                Text(userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("ID: $userId", fontSize = 10.sp, color = GrayText, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                Text("Blocked by $blockCount other users", fontSize = 12.sp, color = GrayText)
            }
        }
    }
}

@Composable
fun ReportCard(report: Map<String, Any>, isUserReport: Boolean, userNames: Map<String, String>, onDelete: (String) -> Unit) {
    val id = report["id"]?.toString() ?: ""
    val reason = report["reason"]?.toString() ?: "No reason provided"
    val timestamp = report["timestamp"] as? Timestamp
    val dateStr = timestamp?.toDate()?.let { 
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(it)
    } ?: "Unknown date"
    
    val targetId = if (isUserReport) report["reportedUserId"]?.toString() else report["listingId"]?.toString()
    val reporterId = report["reporterId"]?.toString() ?: "Unknown"
    
    val reporterName = userNames[reporterId] ?: reporterId
    val targetName = if (isUserReport) (userNames[targetId] ?: targetId) else targetId

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
                        fontSize = 14.sp,
                        color = Color(0xFFD32F2F)
                    )
                    Text(text = "Reason: $reason", fontSize = 14.sp, color = Color.Black)
                }
                IconButton(onClick = { onDelete(id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Dismiss", tint = GrayText, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isUserReport) "Reported User: $targetName" else "Listing ID: $targetName",
                fontSize = 12.sp,
                color = GrayText,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "By Reporter: $reporterName",
                fontSize = 12.sp,
                color = GrayText
            )
            
            if (!isUserReport && targetId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                val listingViewModel: com.tejashaqua.app.ui.viewmodel.ListingViewModel = viewModel()
                var isDeleting by remember { mutableStateOf(false) }
                
                Button(
                    onClick = {
                        isDeleting = true
                        listingViewModel.deleteListing(targetId) {
                            onDelete(id) // Delete the report too after listing is gone
                            isDeleting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DELETE REPORTED LISTING", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateStr,
                fontSize = 12.sp,
                color = GrayText,
                modifier = Modifier.align(Alignment.End)
            )
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

    val fishTypes = listOf("Rohu", "Katla", "Pangasius", "Tilapia", "Roopchand")

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
            
            val newRates = fishTypes.map { name -> 
                fetched[name] ?: AquaRate(name, isPrawn = name == "Prawns") 
            }
            
            rates = newRates
            
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
                        Text("Rate Status", fontSize = 14.sp, color = GrayText)
                        Text(
                            if (noDataAvailable) "Marked as No Data" else "Updating Rates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (noDataAvailable) Color(0xFFD32F2F) else AquaBlue
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.no_data_available), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                        Text("Auto-fill from Yesterday", fontSize = 14.sp)
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
            Box(modifier = Modifier.padding(16.dp)) {
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
                        Text("Prev: ₹$prevPrice", fontSize = 14.sp, color = GrayText)
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
                                    fontSize = 14.sp,
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
                    disabledBorderColor = Color(0xFFF5F5F5),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = AquaBlue
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

    val counts = listOf("20", "25", "30", "35", "37", "40", "45", "47", "50", "60", "70", "80", "90", "100", "200")
    var prices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var previousSummaryPrice by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(selectedMarket, selectedDate) {
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
                        Text("Market & Status", fontSize = 14.sp, color = GrayText)
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
                                Text(selectedMarket, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AquaBlue)
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
                        Text(stringResource(R.string.no_data_available), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                                        Text(count, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                                    }
                                }
                                Text("Count", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            
                            OutlinedTextField(
                                value = price,
                                onValueChange = { newValue: String ->
                                    prices = prices.toMutableMap().apply { put(count, newValue) }
                                    if (newValue != context.getString(R.string.no_data_available)) {
                                        noDataAvailable = false
                                    }
                                },
                                placeholder = { Text("Rate", fontSize = 14.sp) },
                                prefix = { Text("₹ ", color = GrayText, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = keyboardOptions,
                                enabled = !noDataAvailable,
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontFamily = com.tejashaqua.app.ui.theme.Inter),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AquaBlue,
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    disabledContainerColor = Color(0xFFF9F9F9),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    cursorColor = AquaBlue
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
            Box(modifier = Modifier.padding(16.dp)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsAdmin(targetUser: Map<String, Any>? = null, onComplete: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isScheduled by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            imageUrl = "" // Clear URL if image picked from device
        }
    }
    
    val calendar = remember { Calendar.getInstance() }
    var scheduledDateTime by remember { mutableLongStateOf(calendar.timeInMillis) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = scheduledDateTime)
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )
    
    var isSending by remember { mutableStateOf(false) }
    var scheduledList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var notificationLogs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        
        db.collection("scheduled_notifications")
            .whereGreaterThan("scheduledTime", Timestamp.now())
            .addSnapshotListener { value, _ ->
                value?.let { snapshot ->
                    scheduledList = snapshot.documents.map { doc -> 
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
                    }.sortedBy { (it["scheduledTime"] as? Timestamp)?.seconds ?: 0L }
                }
            }

        db.collection("notification_logs")
            .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { value, _ ->
                value?.let { snapshot ->
                    notificationLogs = snapshot.documents.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
                    }
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                if (targetUser != null) "Send Direct Message" else "Send Push Notification", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold
            )
            if (targetUser != null) {
                Text(
                    text = "To: ${targetUser["name"]} (${targetUser["phone"]})",
                    color = AquaBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        item {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Message Body") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notification Image", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                if (selectedImageUri != null) {
                    Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).size(32.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { 
                            imageUrl = it
                            if (it.isNotBlank()) selectedImageUri = null
                        },
                        label = { Text("Image URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/image.jpg") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Text("OR", modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 12.sp, color = GrayText)
                    
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload from device")
                    }
                }
            }
        }
        
        if (targetUser == null) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isScheduled, 
                        onCheckedChange = { isScheduled = it },
                        colors = CheckboxDefaults.colors(checkedColor = AquaBlue)
                    )
                    Text("Schedule for later")
                }
            }
        }
        
        if (isScheduled && targetUser == null) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true }, 
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date")
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true }, 
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Time")
                    }
                }
                
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                Surface(
                    color = AquaBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                ) {
                    Text(
                        "Scheduled for: ${sdf.format(Date(scheduledDateTime))}", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = AquaBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        
        item {
            Button(
                onClick = {
                    if (title.isBlank() || body.isBlank()) {
                        Toast.makeText(context, "Title and body are required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isSending = true
                    coroutineScope.launch {
                        try {
                            var finalImageUrl = imageUrl
                            
                            if (selectedImageUri != null) {
                                val storageRef = FirebaseStorage.getInstance().reference
                                    .child("notifications/${System.currentTimeMillis()}.jpg")
                                storageRef.putFile(selectedImageUri!!).await()
                                finalImageUrl = storageRef.downloadUrl.await().toString()
                            }

                            if (targetUser != null) {
                                val functions = FirebaseFunctions.getInstance("asia-south1")
                                val data = hashMapOf(
                                    "userId" to (targetUser["id"] as String),
                                    "title" to title,
                                    "body" to body,
                                    "imageUrl" to finalImageUrl
                                )
                                functions.getHttpsCallable("sendDirectNotification").call(data).await()
                                Toast.makeText(context, "Direct message sent to ${targetUser["name"]}", Toast.LENGTH_SHORT).show()
                                onComplete()
                            } else if (isScheduled) {
                                val data = hashMapOf(
                                    "title" to title,
                                    "body" to body,
                                    "imageUrl" to finalImageUrl,
                                    "scheduledTime" to Timestamp(Date(scheduledDateTime)),
                                    "status" to "pending",
                                    "createdAt" to Timestamp.now()
                                )
                                FirebaseFirestore.getInstance().collection("scheduled_notifications").add(data).await()
                                Toast.makeText(context, "Notification scheduled", Toast.LENGTH_SHORT).show()
                            } else {
                                val functions = FirebaseFunctions.getInstance("asia-south1")
                                val data = hashMapOf(
                                    "title" to title,
                                    "body" to body,
                                    "imageUrl" to finalImageUrl
                                )
                                functions.getHttpsCallable("sendAdminNotification").call(data).await()
                                Toast.makeText(context, "Broadcast notification sent", Toast.LENGTH_SHORT).show()
                            }
                            title = ""
                            body = ""
                            imageUrl = ""
                            selectedImageUri = null
                            isScheduled = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSending = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSending,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(if (targetUser != null) "Send Message" else if (isScheduled) "Schedule Notification" else "Send Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        
        if (targetUser == null && scheduledList.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Upcoming Scheduled Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            items(scheduledList) { item ->
                ScheduledNotificationCard(item) { id ->
                    FirebaseFirestore.getInstance().collection("scheduled_notifications").document(id).delete()
                }
            }
        }

        if (targetUser == null && notificationLogs.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text("Recent Notification Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            items(notificationLogs) { log ->
                NotificationLogCard(log)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = datePickerState.selectedDateMillis ?: scheduledDateTime
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = date
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    scheduledDateTime = cal.timeInMillis
                    showDatePicker = false
                }) { Text("OK", color = AquaBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = scheduledDateTime
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    scheduledDateTime = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK", color = AquaBlue, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { 
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
fun NotificationLogCard(log: Map<String, Any>) {
    val type = log["type"] as? String ?: ""
    val title = log["title"] as? String ?: ""
    val body = log["body"] as? String ?: ""
    val targetName = log["targetUserName"] as? String ?: log["targetTopic"] as? String ?: "Everyone"
    val sentAt = log["sentAt"] as? Timestamp
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (type == "direct") Color(0xFFE3F2FD) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = type.uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (type == "direct") Color(0xFF1976D2) else Color(0xFF388E3C)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "To: $targetName",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(body, fontSize = 13.sp, color = GrayText, maxLines = 2)
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sentAt?.let { sdf.format(it.toDate()) } ?: "",
                fontSize = 11.sp,
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun ScheduledNotificationCard(item: Map<String, Any>, onDelete: (String) -> Unit) {
    val title = item["title"] as? String ?: ""
    val body = item["body"] as? String ?: ""
    val time = item["scheduledTime"] as? Timestamp
    val id = item["id"] as? String ?: ""
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(body, style = MaterialTheme.typography.bodySmall, color = GrayText, maxLines = 2)
                Text(
                    text = time?.let { "Scheduled for: ${sdf.format(it.toDate())}" } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = AquaBlue
                )
            }
            IconButton(onClick = { onDelete(id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}
