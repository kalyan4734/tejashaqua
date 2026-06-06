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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.VibrantBlue
import com.tejashaqua.app.ui.theme.GrayText
import com.tejashaqua.app.R
import com.tejashaqua.app.data.model.ListingCategory

data class CategoryItem(
    val title: String,
    val subtitle: String,
    val icon: Any, // Can be Int (Resource ID) or ImageVector
    val category: ListingCategory
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCategoryScreen(
    onBackClick: () -> Unit,
    onCategorySelect: (ListingCategory) -> Unit
) {
    val categories = listOf(
        CategoryItem(
            stringResource(R.string.prawn_hatchery),
            stringResource(R.string.prawn_hatchery_desc),
            R.drawable.prawn,
            ListingCategory.PRAWNS
        ),
        CategoryItem(stringResource(R.string.cat_fish), stringResource(R.string.fish_desc), R.drawable.fish, ListingCategory.FISH),
        CategoryItem(
            stringResource(R.string.cat_equipments),
            stringResource(R.string.equipments_desc),
            Icons.Default.Construction,
            ListingCategory.EQUIPMENTS
        ),
        CategoryItem(
            stringResource(R.string.fish_tank_lands),
            stringResource(R.string.fish_tank_lands_desc),
            Icons.Default.Landscape,
            ListingCategory.TANKS
        ),
        CategoryItem(
            stringResource(R.string.cat_jobs),
            stringResource(R.string.jobs_desc),
            Icons.Default.Work,
            ListingCategory.JOBS
        ),
        CategoryItem(
            stringResource(R.string.cat_services),
            stringResource(R.string.services_desc),
            Icons.Default.Settings,
            ListingCategory.SERVICES
        ),
        CategoryItem(
            stringResource(R.string.cat_business),
            stringResource(R.string.business_desc),
            Icons.Default.Storefront,
            ListingCategory.BUSINESS
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.select_category_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
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
                text = stringResource(R.string.select_category_desc),
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
    val bgColor = when (item.category) {
        ListingCategory.PRAWNS -> Color(0xFFE0F2F1)
        ListingCategory.FISH -> Color(0xFFFFF3E0)
        ListingCategory.EQUIPMENTS -> Color(0xFFE1F5FE)
        ListingCategory.TANKS -> Color(0xFFE8F5E9)
        ListingCategory.JOBS -> Color(0xFFF3E5F5)
        ListingCategory.SERVICES -> Color(0xFFFFFDE7)
        ListingCategory.BUSINESS -> Color(0xFFFFEBEE)
        ListingCategory.VEHICLES -> Color(0xFFE1F5FE)
        ListingCategory.FEED -> Color(0xFFFFF3E0)
    }

    val tintColor = when (item.category) {
        ListingCategory.PRAWNS -> Color(0xFF3F51B5)
        ListingCategory.FISH -> Color(0xFF009688)
        ListingCategory.EQUIPMENTS -> Color(0xFF1976D2)
        ListingCategory.TANKS -> Color(0xFF388E3C)
        ListingCategory.JOBS -> Color(0xFF673AB7)
        ListingCategory.SERVICES -> Color(0xFFF57C00)
        ListingCategory.BUSINESS -> Color(0xFFB71C1C)
        ListingCategory.VEHICLES -> Color(0xFF1976D2)
        ListingCategory.FEED -> Color(0xFFE65100)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                when (item.icon) {
                    is Int -> {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.title,
                            modifier = Modifier.size(60.dp),
                            tint = tintColor
                        )
                    }
                    is ImageVector -> {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(50.dp),
                            tint = tintColor
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = tintColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 11.sp,
                    color = GrayText,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
