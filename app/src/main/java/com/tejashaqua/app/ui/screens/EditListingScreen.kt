package com.tejashaqua.app.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.ui.components.LoadingOverlay
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.viewmodel.ListingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    category: ListingCategory,
    isEditMode: Boolean = false,
    listingId: String? = null,
    userName: String,
    userMobileNumber: String,
    initialLocation: String,
    initialLatLng: LatLng? = null,
    onBackClick: () -> Unit,
    onPostClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onLocationChangeClick: () -> Unit,
    listingViewModel: ListingViewModel = viewModel(),
    joinedAt: Long,
    userId: String
) {
    // General fields
    var selectedServiceType by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(initialLocation) }
    var latLng by remember { mutableStateOf(initialLatLng) }
    var contactNumber by remember { mutableStateOf(userMobileNumber) }

    // Photos state (Can be Bitmap or String URL)
    var selectedPhotos by remember { mutableStateOf(listOf<Any>()) }

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
    var plDays by remember { mutableStateOf("") }
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
    var jobType by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var netType by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFetchingData by remember { mutableStateOf(false) }
    val postState by listingViewModel.postState.collectAsState()
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedPhotos = selectedPhotos + bitmap
        }
    }

    // Sync location if it changes from outside (via picker)
    LaunchedEffect(initialLocation, initialLatLng) {
        location = initialLocation
        latLng = initialLatLng
    }

    // Sync contact number if it changes from outside (e.g. initial load)
    LaunchedEffect(userMobileNumber) {
        if (contactNumber.isEmpty() || contactNumber == "+91 9876543210") {
            contactNumber = userMobileNumber
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        listingId?.let { listingViewModel.deleteListing(it) }
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fetch existing data if in edit mode
    LaunchedEffect(isEditMode, listingId) {
        if (isEditMode && listingId != null) {
            isFetchingData = true
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("listings")
                .document(listingId)
                .get()
                .addOnSuccessListener { doc ->
                    isFetchingData = false
                    if (doc != null && doc.exists()) {
                        title = doc.getString("title") ?: ""
                        description = doc.getString("description") ?: ""
                        price = doc.get("price")?.toString() ?: ""
                        location = doc.getString("location") ?: initialLocation
                        val lat = doc.getDouble("lat")
                        val lng = doc.getDouble("lng")
                        if (lat != null && lng != null) {
                            latLng = LatLng(lat, lng)
                        }
                        contactNumber = doc.getString("contactNumber") ?: userMobileNumber

                        // Category specific fields
                        when (category) {
                            ListingCategory.FISH -> {
                                fishType = doc.getString("fishType") ?: ""
                                sizeType = doc.getString("sizeType") ?: "Inches"
                                sizeValue = doc.getString("sizeValue") ?: ""
                                fishAge = doc.getString("fishAge") ?: ""
                                quantity = doc.getString("quantity") ?: ""
                                unitType = doc.getString("unitType") ?: "Lakhs"
                            }
                            ListingCategory.PRAWNS -> {
                                prawnType = doc.getString("prawnType") ?: ""
                                hatcheryName = doc.getString("hatcheryName") ?: ""
                                rateType = doc.getString("rateType") ?: "Paise"
                                rateValue = doc.getString("rateValue") ?: ""
                                plDays = doc.getString("plDays") ?: ""
                                quantity = doc.getString("quantity") ?: ""
                                unitType = doc.getString("unitType") ?: "Lakhs"
                            }
                            ListingCategory.EQUIPMENTS -> {
                                equipmentType = doc.getString("equipmentType") ?: ""
                            }
                            ListingCategory.VEHICLES -> {
                                selectedServiceType = doc.getString("serviceType") ?: ""
                                vehicleName = doc.getString("vehicleName") ?: ""
                                vehicleCapacity = doc.getString("vehicleCapacity") ?: ""
                            }
                            ListingCategory.FEED -> {
                                businessType = doc.getString("businessType") ?: ""
                                feedName = doc.getString("feedName") ?: ""
                                ratePerTon = doc.getString("ratePerTon") ?: ""
                            }
                            ListingCategory.SERVICES -> {
                                selectedServiceType = doc.getString("serviceType") ?: ""
                                boreWellType = doc.getString("boreWellType") ?: ""
                                vehicleName = doc.getString("vehicleName") ?: ""
                                vehicleCapacity = doc.getString("vehicleCapacity") ?: ""
                                netType = doc.getString("netType") ?: ""
                            }
                            ListingCategory.TANKS -> {
                                tankAcres = doc.getString("tankAcres") ?: ""
                                estPricePerAcre = doc.getString("estPricePerAcre") ?: ""
                                tankLocation = doc.getString("tankLocation") ?: ""
                            }
                            ListingCategory.JOBS -> {
                                jobType = doc.getString("jobType") ?: ""
                                tankAcres = doc.getString("tankAcres") ?: ""
                                tankLocation = doc.getString("tankLocation") ?: ""
                                salary = doc.getString("salary") ?: ""
                            }
                        }

                        // Handle images if any
                        val imageUrls = doc.get("images") as? List<String>
                        if (imageUrls != null) {
                            selectedPhotos = imageUrls
                        }
                    }
                }
                .addOnFailureListener {
                    isFetchingData = false
                }
        }
    }

    LaunchedEffect(postState) {
        if (postState is ListingViewModel.PostState.Success) {
            listingViewModel.resetState()
            onPostClick()
        } else if (postState is ListingViewModel.PostState.Error) {
            val message = (postState as ListingViewModel.PostState.Error).message
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val screenTitle = if (isEditMode) "Edit Listing" else category.name.lowercase().replaceFirstChar { it.uppercase() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(screenTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding().imePadding()
                ) {
                    val onActionClick = {
                        val data = buildListingMap(
                            listingId, category, title, description, price, location, latLng, contactNumber, 
                            userName, selectedServiceType, fishType, sizeType, sizeValue, fishAge, quantity,
                            unitType, prawnType, hatcheryName, rateType, rateValue, plDays, equipmentType, 
                            vehicleName, vehicleCapacity, businessType, feedName, ratePerTon, boreWellType, 
                            tankAcres, estPricePerAcre, tankLocation, jobType, salary, netType, userId
                        )
                        
                        val newBitmaps = selectedPhotos.filterIsInstance<Bitmap>()
                        val existingUrls = selectedPhotos.filterIsInstance<String>()
                        
                        listingViewModel.saveListing(data, newBitmaps, existingUrls)
                    }

                    if (isEditMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFF44336)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Button(
                                onClick = onActionClick,
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
                                onClick = onActionClick,
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
                            plDays, { plDays = it },
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
                        ListingCategory.SERVICES -> ServiceFields(
                            selectedServiceType, { selectedServiceType = it },
                            boreWellType, { boreWellType = it },
                            vehicleName, { vehicleName = it },
                            vehicleCapacity, { vehicleCapacity = it },
                            netType, { netType = it },
                            title, { title = it }
                        )
                        ListingCategory.TANKS -> TankFields(
                            title, { title = it },
                            tankAcres, { tankAcres = it },
                            estPricePerAcre, { estPricePerAcre = it },
                            tankLocation, { tankLocation = it }
                        )
                        ListingCategory.JOBS -> JobFields(
                            jobType, { jobType = it },
                            tankAcres, { tankAcres = it },
                            tankLocation, { tankLocation = it },
                            salary, { salary = it },
                            title, { title = it }
                        )
                    }
                }

                item {
                    ListingTextField(label = "Description", value = description, onValueChange = { description = it }, minLines = 3)
                }

                if (category != ListingCategory.JOBS) {
                    item {
                        PhotoSection(
                            photos = selectedPhotos,
                            onAddPhoto = { cameraLauncher.launch() },
                            onRemovePhoto = { index -> 
                                selectedPhotos = selectedPhotos.toMutableList().apply { removeAt(index) }
                            }
                        )
                    }
                }

                item {
                    LocationSection(location, onClick = onLocationChangeClick)
                }

                item {
                    ListingTextField(label = "Contact Number", value = contactNumber, onValueChange = { contactNumber = it })
                }
                
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }

        if (isFetchingData || postState is ListingViewModel.PostState.Loading) {
            LoadingOverlay(if (isFetchingData) "Fetching listing details..." else "Saving listing...")
        }
    }
}

@Composable
fun PhotoSection(
    photos: List<Any>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit
) {
    Column {
        Text(text = "Photos (up to 3)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            photos.forEachIndexed { index, photo ->
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.LightGray, RoundedCornerShape(12.dp))
                ) {
                    if (photo is Bitmap) {
                        Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (photo is String) {
                        AsyncImage(
                            model = photo,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clickable { onRemovePhoto(index) },
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                    }
                }
            }
            if (photos.size < 3) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { onAddPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

private fun buildListingMap(
    id: String?,
    category: ListingCategory,
    title: String,
    description: String,
    price: String,
    location: String,
    latLng: LatLng?,
    contactNumber: String,
    posterName: String,
    serviceType: String,
    fishType: String,
    sizeType: String,
    sizeValue: String,
    fishAge: String,
    quantity: String,
    unitType: String,
    prawnType: String,
    hatcheryName: String,
    rateType: String,
    rateValue: String,
    plDays: String,
    equipmentType: String,
    vehicleName: String,
    vehicleCapacity: String,
    businessType: String,
    feedName: String,
    ratePerTon: String,
    boreWellType: String,
    tankAcres: String,
    estPricePerAcre: String,
    tankLocation: String,
    jobType: String,
    salary: String,
    netType: String,
    userId: String
): Map<String, Any> {
    val data = mutableMapOf<String, Any>()
    if (id != null) data["id"] = id
    data["userId"] = userId
    data["category"] = category.name
    data["title"] = title
    data["description"] = description
    data["price"] = price
    data["location"] = location
    latLng?.let {
        data["lat"] = it.latitude
        data["lng"] = it.longitude
    }
    data["contactNumber"] = contactNumber
    data["posterName"] = posterName
    
    // Images will be handled in ViewModel

    // Specific fields based on category
    when(category) {
        ListingCategory.FISH -> {
            data["fishType"] = fishType
            data["sizeType"] = sizeType
            data["sizeValue"] = sizeValue
            data["fishAge"] = fishAge
            data["quantity"] = quantity
            data["unitType"] = unitType
        }
        ListingCategory.PRAWNS -> {
            data["prawnType"] = prawnType
            data["hatcheryName"] = hatcheryName
            data["rateType"] = rateType
            data["rateValue"] = rateValue
            data["plDays"] = plDays
            data["quantity"] = quantity
            data["unitType"] = unitType
        }
        ListingCategory.EQUIPMENTS -> {
            data["equipmentType"] = equipmentType
        }
        ListingCategory.VEHICLES -> {
            data["vehicleName"] = vehicleName
            data["vehicleCapacity"] = vehicleCapacity
        }
        ListingCategory.FEED -> {
            data["businessType"] = businessType
            data["feedName"] = feedName
            data["ratePerTon"] = ratePerTon
        }
        ListingCategory.SERVICES -> {
            data["serviceType"] = serviceType
            data["boreWellType"] = boreWellType
            data["vehicleName"] = vehicleName
            data["vehicleCapacity"] = vehicleCapacity
            data["netType"] = netType
        }
        ListingCategory.TANKS -> {
            data["tankAcres"] = tankAcres
            data["estPricePerAcre"] = estPricePerAcre
            data["tankLocation"] = tankLocation
        }
        ListingCategory.JOBS -> {
            data["jobType"] = jobType
            data["tankAcres"] = tankAcres
            data["tankLocation"] = tankLocation
            data["salary"] = salary
        }
    }
    return data
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
        SearchableListingDropdown(label = "Fish Type", value = fishType, options = listOf("Rohu", "Katla", "Karamosu", "Gaddi chepa", "Pangasius", "Roopchand", "Pandu gappa", "Tilapia", "Chitala", "Koramenu", "Valuga", "Engilayi", "Jalla", "Tuna", "Pulasa", "Crab", "Others"), onSelectionChange = onFishTypeChange)
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
    plDays: String, onPlDaysChange: (String) -> Unit,
    quantity: String, onQuantityChange: (String) -> Unit,
    unitType: String, onUnitTypeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(label = "Type of Prawns", value = prawnType, options = listOf("Growth Line Plus", "Growth Line","Hard Line", "Hard Line Plus", "Sy Aqua", "Benchmark", "Cong", "Blue genetic"), onSelectionChange = onPrawnTypeChange)
        ListingTextField(label = "Hatchery Name", value = hatcheryName, onValueChange = onHatcheryNameChange)
        ListingTextField(label = "PL Days", value = plDays, onValueChange = onPlDaysChange)
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
        SearchableListingDropdown(label = "Equipment Type", value = equipmentType, options = listOf("Aerators", "Motors", "Pump Motors", "Bore Motors", "Generators", "Pump Engines", "Boats", "Electrical Wires", "Others"), onSelectionChange = onEquipmentTypeChange)
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
fun ServiceFields(
    serviceType: String, onServiceTypeChange: (String) -> Unit,
    boreWellType: String, onBoreWellTypeChange: (String) -> Unit,
    vehicleName: String, onVehicleNameChange: (String) -> Unit,
    vehicleCapacity: String, onVehicleCapacityChange: (String) -> Unit,
    netType: String, onNetTypeChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(
            label = "Service Type",
            value = serviceType,
            options = listOf("Bore Well", "Live Fish Vehicles", "Nets"),
            onSelectionChange = onServiceTypeChange
        )

        when (serviceType) {
            "Bore Well" -> {
                SearchableListingDropdown(
                    label = "Bore Well Type",
                    value = boreWellType,
                    options = listOf("Drill bore", "Hand bore", "Others"),
                    onSelectionChange = onBoreWellTypeChange
                )
            }
            "Live Fish Vehicles" -> {
                ListingTextField(label = "Vehicle Name", value = vehicleName, onValueChange = onVehicleNameChange)
                ListingTextField(label = "Vehicle Capacity (Tons)", value = vehicleCapacity, onValueChange = onVehicleCapacityChange)
            }
            "Nets" -> {
                SearchableListingDropdown(
                    label = "Net Type",
                    value = netType,
                    options = listOf("Harvest netting", "Hand netting", "Others"),
                    onSelectionChange = onNetTypeChange
                )
            }
        }
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
fun JobFields(
    jobType: String, onJobTypeChange: (String) -> Unit,
    tankAcres: String, onTankAcresChange: (String) -> Unit,
    tankLocation: String, onTankLocationChange: (String) -> Unit,
    salary: String, onSalaryChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(
            label = "Job Type",
            value = jobType,
            options = listOf(
                "Watch man on fish tank (For Feeding fish)",
                "Supervisor on fish tank (For Maintenance)",
                "Electrician",
                "Technician"
            ),
            onSelectionChange = onJobTypeChange
        )
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Tank Acres", value = tankAcres, onValueChange = onTankAcresChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = "Salary (in ₹)", value = salary, onValueChange = onSalaryChange)
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
fun SearchableListingDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "$label *",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            onClick = { showSheet = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value.ifEmpty { "Select $label" },
                    color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSheet) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredOptions = options.filter { it.contains(searchQuery, ignoreCase = true) }

        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(0.8f)
            ) {
                Text(
                    text = "Select $label",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search $label...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AquaBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredOptions) { option ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = option,
                                    fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == value) AquaBlue else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingContent = {
                                if (option == value) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AquaBlue)
                                }
                            },
                            modifier = Modifier.clickable {
                                onSelectionChange(option)
                                showSheet = false
                            }
                        )
                    }
                    if (filteredOptions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
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
fun LocationSection(location: String, onClick: () -> Unit) {
    Column {
        Text(text = "Location", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = location,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change", color = AquaBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
