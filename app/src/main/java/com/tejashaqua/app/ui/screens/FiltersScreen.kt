package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tejashaqua.app.R
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.ui.theme.DarkBlueText
import com.tejashaqua.app.ui.theme.Inter
import com.tejashaqua.app.ui.viewmodel.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    onBackClick: () -> Unit,
    onApplyFilters: () -> Unit,
    viewModel: MarketplaceViewModel = viewModel(),
) {
    val currentCategory by viewModel.selectedCategory.collectAsState()
    val currentRadius by viewModel.radiusKm.collectAsState()
    val currentPriceRange by viewModel.priceRange.collectAsState()
    val currentSortBy by viewModel.sortBy.collectAsState()

    var tempCategory by remember { mutableStateOf(currentCategory) }
    var tempRadius by remember { mutableFloatStateOf(if (currentRadius == 0) 200f else currentRadius.toFloat()) }
    var tempPriceRange by remember { mutableStateOf(currentPriceRange) }
    var tempSortBy by remember { mutableStateOf(currentSortBy ?: "Newest First") }

    val categories = listOf(
        "All" to stringResource(R.string.all),
        "FISH" to stringResource(R.string.cat_fish_seed),
        "PRAWNS" to stringResource(R.string.cat_prawns),
        "EQUIPMENTS" to stringResource(R.string.cat_equipments),
        "VEHICLES" to stringResource(R.string.cat_vehicles),
        "FEED" to stringResource(R.string.cat_feed),
        "MEDICINE" to stringResource(R.string.cat_medicine),
        "SERVICES" to stringResource(R.string.cat_services),
        "TANKS" to stringResource(R.string.cat_tanks),
        "BUSINESS" to stringResource(R.string.cat_business),
        "JOBS" to stringResource(R.string.cat_jobs)
    )

    val priceRanges = listOf(
        (null to 500.0) to stringResource(R.string.price_under_500),
        (500.0 to 5000.0) to stringResource(R.string.price_500_5k),
        (5000.0 to 50000.0) to stringResource(R.string.price_5k_50k),
        (50000.0 to null) to stringResource(R.string.price_50k_plus)
    )

    val sortOptions = listOf(
        "Newest First" to stringResource(R.string.sort_newest),
        "Price: Low to High" to stringResource(R.string.sort_price_low_high),
        "Price: High to Low" to stringResource(R.string.sort_price_high_low),
        "Nearest First" to stringResource(R.string.sort_nearest),
        "Most Viewed" to stringResource(R.string.sort_most_viewed)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.filters_title), 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            tempCategory = "All"
                            tempRadius = 200f
                            tempPriceRange = null to null
                            tempSortBy = "Newest First"
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE91E63)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE91E63))
                    ) {
                        Text(stringResource(R.string.clear_all), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, fontFamily = Inter)
                    }
                    Button(
                        onClick = {
                            viewModel.setSelectedCategory(tempCategory)
                            val finalRadius = if (tempRadius >= 200f) 0 else tempRadius.toInt()
                            viewModel.setFilters(finalRadius, tempPriceRange, tempSortBy)
                            onApplyFilters()
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                    ) {
                        Text(stringResource(R.string.apply_filters), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, fontFamily = Inter)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
        ) {
            // Categories
            item {
                Column {
                    Text(stringResource(R.string.categories_label), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlueText, fontFamily = Inter)
                    Spacer(modifier = Modifier.height(16.dp))
                    FilterChipGroup(
                        items = categories,
                        selectedItem = tempCategory
                    ) { tempCategory = it }
                }
            }

            // Price Range
            item {
                Column {
                    Text(stringResource(R.string.price_range_label), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlueText, fontFamily = Inter)
                    Spacer(modifier = Modifier.height(16.dp))
                    PriceRangeGroup(
                        ranges = priceRanges,
                        selectedRange = tempPriceRange
                    ) { tempPriceRange = it }
                }
            }

            // Location Radius
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.location_radius), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlueText, fontFamily = Inter)
                        val radiusDisplay = if (tempRadius >= 200f) stringResource(R.string.anywhere) else stringResource(R.string.within_km, tempRadius.toInt())
                        Text(radiusDisplay, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = AquaBlue, fontFamily = Inter)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = tempRadius,
                        onValueChange = { tempRadius = it },
                        valueRange = 5f..200f,
                        steps = 38,
                        colors = SliderDefaults.colors(
                            thumbColor = AquaBlue,
                            activeTrackColor = AquaBlue,
                            inactiveTrackColor = Color(0xFFF0F0F0),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5 km", fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium, fontFamily = Inter)
                        Text(stringResource(R.string.anywhere), fontSize = 12.sp, color = GrayText, fontWeight = FontWeight.Medium, fontFamily = Inter)
                    }
                }
            }

            // Sort By
            item {
                Column {
                    Text("Sort By", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlueText, fontFamily = Inter)
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        sortOptions.forEachIndexed { index, option ->
                            SortOptionItem(
                                label = option.second,
                                isSelected = tempSortBy == option.first,
                                onClick = { tempSortBy = option.first }
                            )
                            if (index < (sortOptions.size - 1)) {
                                HorizontalDivider(color = Color(0xFFF8F8F8), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipGroup(
    items: List<Pair<String, String>>,
    selectedItem: String,
    onSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { (key, label) ->
            val isSelected = selectedItem == key
            Surface(
                onClick = { onSelected(key) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AquaBlue else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AquaBlue else Color(0xFFE0E0E0)),
                tonalElevation = if (isSelected) 2.dp else 0.dp
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    color = if (isSelected) Color.White else Color(0xFF424242),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    fontFamily = Inter
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceRangeGroup(
    ranges: List<Pair<Pair<Double?, Double?>, String>>,
    selectedRange: Pair<Double?, Double?>,
    onSelected: (Pair<Double?, Double?>) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ranges.forEach { (range, label) ->
            val isSelected = selectedRange == range
            Surface(
                onClick = { onSelected(range) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AquaBlue else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AquaBlue else Color(0xFFE0E0E0))
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    color = if (isSelected) Color.White else Color(0xFF424242),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    fontFamily = Inter
                )
            }
        }
    }
}

@Composable
fun SortOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (isSelected) AquaBlue else Color(0xFF333333),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = Inter
        )
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AquaBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
