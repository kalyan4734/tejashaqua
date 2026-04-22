package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.R

data class CategoryItem(
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val category: ListingCategory
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCategoryScreen(
    onBackClick: () -> Unit,
    onCategorySelect: (ListingCategory) -> Unit
) {
    val categories = listOf(
        CategoryItem("Prawn Hatchery", "List PL stock, type & rates", R.drawable.app_logo, ListingCategory.PRAWNS),
        CategoryItem("Fish", "Sell live or harvested fish", R.drawable.app_logo, ListingCategory.FISH),
        CategoryItem("Equipments", "Wires, aerators, motors", R.drawable.app_logo, ListingCategory.EQUIPMENTS),
        CategoryItem("Fish Tank Lands", "Lease or sell fish ponds", R.drawable.app_logo, ListingCategory.TANKS),
        CategoryItem("Jobs", "Hire or find aqua jobs", R.drawable.app_logo, ListingCategory.VEHICLES), // Using Vehicles as placeholder for Jobs logic
        CategoryItem("Services", "------", R.drawable.app_logo, ListingCategory.BOREWELL)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("What are you selling?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "Select a category to start listing your product or service.",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(categories) { item ->
                    CategoryCard(item) { onCategorySelect(item.category) }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(item: CategoryItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        color = Color.White
    ) {
        Column {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = item.subtitle,
                    fontSize = 12.sp,
                    color = GrayText,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
