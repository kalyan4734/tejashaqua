package com.tejashaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

enum class ListingCategory {
    FISH, PRAWNS, EQUIPMENTS, VEHICLES, FEED, BOREWELL, TANKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    category: ListingCategory,
    isEditMode: Boolean = false,
    onBackClick: () -> Unit,
    onPostClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    var selectedServiceType by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Rajahmundry, AP") }
    var contactNumber by remember { mutableStateOf("+91 9876543210") }

    // Photos state
    var selectedPhotos by remember { mutableStateOf(emptyList<String>()) }

    // Specific fields
    var fishType by remember { mutableStateOf("") }
    var sizeType by remember { mutableStateOf("Inches") }
    var sizeValue by remember { mutableStateOf("") }
    var fishAge by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("Lakhs") }
    
    var prawnType by remember { mutableStateOf("") }
    var hatcheryName by remember { mutableStateOf("") }
    var rateType by remember { mutableStateOf("Paise") }
    var rateValue by remember { mutableStateOf("") }

    var equipmentType by remember { mutableStateOf("") }
    
    var vehicleName by remember { mutableStateOf("") }
    var vehicleCapacity by remember { mutableStateOf("") }

    var businessType by remember { mutableStateOf("") }
    var feedName by remember { mutableStateOf("") }
    var ratePerTon by remember { mutableStateOf("") }

    var boreWellType by remember { mutableStateOf("") }

    var tankAcres by remember { mutableStateOf("") }
    var estPricePerAcre by remember { mutableStateOf("") }
    var tankLocation by remember { mutableStateOf("") }

    val screenTitle = if (isEditMode) "Edit Listing" else category.name.lowercase().replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                if (isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Button(
                            onClick = onPostClick,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = onPostClick,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                        ) {
                            Text("Post Listing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            item {
                when (category) {
                    ListingCategory.FISH -> FishFields(
                        fishType, { fishType = it },
                        title, { title = it },
                        sizeType, { sizeType = it },
                        sizeValue, { sizeValue = it },
                        fishAge, { fishAge = it },
                        quantity, { quantity = it },
                        unitType, { unitType = it },
                        price, { price = it }
                    )
                    ListingCategory.PRAWNS -> PrawnFields(
                        prawnType, { prawnType = it },
                        hatcheryName, { hatcheryName = it },
                        rateType, { rateType = it },
                        rateValue, { rateValue = it },
                        quantity, { quantity = it },
                        unitType, { unitType = it }
                    )
                    ListingCategory.EQUIPMENTS -> EquipmentFields(
                        equipmentType, { equipmentType = it },
                        title, { title = it },
                        price, { price = it }
                    )
                    ListingCategory.VEHICLES -> VehicleFields(
                        selectedServiceType, { selectedServiceType = it },
                        vehicleName, { vehicleName = it },
                        vehicleCapacity, { vehicleCapacity = it },
                        title, { title = it }
                    )
                    ListingCategory.FEED -> FeedFields(
                        businessType, { businessType = it },
                        feedName, { feedName = it },
                        title, { title = it },
                        ratePerTon, { ratePerTon = it }
                    )
                    ListingCategory.BOREWELL -> BorewellFields(
                        selectedServiceType, { selectedServiceType = it },
                        boreWellType, { boreWellType = it },
                        title, { title = it }
                    )
                    ListingCategory.TANKS -> TankFields(
                        title, { title = it },
                        tankAcres, { tankAcres = it },
                        estPricePerAcre, { estPricePerAcre = it },
                        tankLocation, { tankLocation = it }
                    )
                }
            }

            item {
                ListingTextField(label = "Description", value = description, onValueChange = { description = it }, minLines = 3)
            }

            item {
                PhotoSection(
                    photos = selectedPhotos,
                    onAddPhoto = { /* Logic to pick photo */ },
                    onRemovePhoto = { index -> 
                        selectedPhotos = selectedPhotos.toMutableList().apply { removeAt(index) }
                    }
                )
            }

            item {
                LocationSection(location)
            }

            item {
                ListingTextField(label = "Contact Number", value = contactNumber, onValueChange = { contactNumber = it })
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun FishFields(
    fishType: String, onFishTypeChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    sizeType: String, onSizeTypeChange: (String) -> Unit,
    sizeValue: String, onSizeValueChange: (String) -> Unit,
    fishAge: String, onFishAgeChange: (String) -> Unit,
    quantity: String, onQuantityChange: (String) -> Unit,
    unitType: String, onUnitTypeChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Fish Type", value = fishType, options = listOf("Katla", "Rohu", "Tilapia"), onSelectionChange = onFishTypeChange)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = "Size", value = sizeType, options = listOf("Inches", "Centimeters"), onSelectionChange = onSizeTypeChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Size Value", value = sizeValue, onValueChange = onSizeValueChange)
            }
        }
        ListingTextField(label = "Fish Age (in Months)", value = fishAge, onValueChange = onFishAgeChange)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Quantity", value = quantity, onValueChange = onQuantityChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = "Unit", value = unitType, options = listOf("Lakhs", "Thousands", "Kgs"), onSelectionChange = onUnitTypeChange)
            }
        }
        ListingTextField(label = "Price (in ₹)", value = price, onValueChange = onPriceChange)
    }
}

@Composable
fun PrawnFields(
    prawnType: String, onPrawnTypeChange: (String) -> Unit,
    hatcheryName: String, onHatcheryNameChange: (String) -> Unit,
    rateType: String, onRateTypeChange: (String) -> Unit,
    rateValue: String, onRateValueChange: (String) -> Unit,
    quantity: String, onQuantityChange: (String) -> Unit,
    unitType: String, onUnitTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Type of Prawns", value = prawnType, options = listOf("Growth Line Plus", "Vannamei"), onSelectionChange = onPrawnTypeChange)
        ListingTextField(label = "Hatchery Name", value = hatcheryName, onValueChange = onHatcheryNameChange)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = "Rate", value = rateType, options = listOf("Paise", "Rupees"), onSelectionChange = onRateTypeChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Rate", value = rateValue, onValueChange = onRateValueChange)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Quantity", value = quantity, onValueChange = onQuantityChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = "Unit", value = unitType, options = listOf("Lakhs", "Thousands"), onSelectionChange = onUnitTypeChange)
            }
        }
    }
}

@Composable
fun EquipmentFields(
    equipmentType: String, onEquipmentTypeChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Equipment Type", value = equipmentType, options = listOf("Aerators", "Pumps", "Feeders"), onSelectionChange = onEquipmentTypeChange)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange)
        ListingTextField(label = "Price (in ₹)", value = price, onValueChange = onPriceChange)
    }
}

@Composable
fun VehicleFields(
    serviceType: String, onServiceTypeChange: (String) -> Unit,
    vehicleName: String, onVehicleNameChange: (String) -> Unit,
    vehicleCapacity: String, onVehicleCapacityChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Service Type", value = serviceType, options = listOf("Live Fish Vehicles", "Feed Transport"), onSelectionChange = onServiceTypeChange)
        ListingTextField(label = "Vehicle Name", value = vehicleName, onValueChange = onVehicleNameChange)
        ListingTextField(label = "Vehicle Capacity", value = vehicleCapacity, onValueChange = onVehicleCapacityChange)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange)
    }
}

@Composable
fun FeedFields(
    businessType: String, onBusinessTypeChange: (String) -> Unit,
    feedName: String, onFeedNameChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    ratePerTon: String, onRatePerTonChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Business Type", value = businessType, options = listOf("Fish Feed", "Shrimp Feed"), onSelectionChange = onBusinessTypeChange)
        ListingDropdown(label = "Feed Name", value = feedName, options = listOf("Godrej", "CP", "Avanti"), onSelectionChange = onFeedNameChange)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange)
        ListingTextField(label = "Rate per ton", value = ratePerTon, onValueChange = onRatePerTonChange)
    }
}

@Composable
fun BorewellFields(
    serviceType: String, onServiceTypeChange: (String) -> Unit,
    boreWellType: String, onBoreWellTypeChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Service Type", value = serviceType, options = listOf("Bore Well", "Pump Installation"), onSelectionChange = onServiceTypeChange)
        ListingDropdown(label = "Bore Well", value = boreWellType, options = listOf("Bore Well", "Tube Well"), onSelectionChange = onBoreWellTypeChange)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange)
    }
}

@Composable
fun TankFields(
    title: String, onTitleChange: (String) -> Unit,
    tankAcres: String, onTankAcresChange: (String) -> Unit,
    estPricePerAcre: String, onEstPricePerAcreChange: (String) -> Unit,
    tankLocation: String, onTankLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Tank Acres", value = tankAcres, onValueChange = onTankAcresChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Est. Price/Acre (in ₹)", value = estPricePerAcre, onValueChange = onEstPricePerAcreChange)
            }
        }
        ListingTextField(label = "Tank Location (Village/Town)", value = tankLocation, onValueChange = onTankLocationChange)
    }
}

@Composable
fun ListingTextField(label: String, value: String, onValueChange: (String) -> Unit, minLines: Int = 1) {
    Column {
        Text(text = androidx.compose.ui.text.buildAnnotatedString {
            append(label)
            if (label != "Description" && label != "Location" && label != "Photos (up to 3)") {
                append(" *")
            }
        }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = minLines,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = AquaBlue,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDropdown(label: String, value: String, options: List<String>, onSelectionChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(text = "$label *", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = AquaBlue,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelectionChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoSection(
    photos: List<String>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit
) {
    Column {
        Text(text = "Photos (up to 3)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            photos.forEachIndexed { index, photo ->
                PhotoItem(onRemove = { onRemovePhoto(index) })
            }
            if (photos.size < 3) {
                AddPhotoItem(onClick = onAddPhoto)
            }
        }
    }
}

@Composable
fun PhotoItem(onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.LightGray, RoundedCornerShape(12.dp))
    ) {
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clickable { onRemove() },
            color = Color.Black.copy(alpha = 0.7f),
            shape = CircleShape
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
        }
    }
}

@Composable
fun AddPhotoItem(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(32.dp))
    }
}

@Composable
fun LocationSection(location: String) {
    Column {
        Text(text = "Location", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(location, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
