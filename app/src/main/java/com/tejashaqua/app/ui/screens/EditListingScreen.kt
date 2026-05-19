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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.tejashaqua.app.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import android.content.Context
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

    var fieldErrors by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var photoError by remember { mutableStateOf(false) }

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
//    LaunchedEffect(userMobileNumber) {
//        if (contactNumber.isEmpty() || contactNumber == "+91 9876543210") {
//            contactNumber = userMobileNumber
//        }
//    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_listing_title)) },
            text = { Text(stringResource(R.string.delete_listing_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        listingId?.let { listingViewModel.deleteListing(it) }
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
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

    val screenTitle = if (isEditMode) stringResource(R.string.edit_listing) else category.name.lowercase().replaceFirstChar { it.uppercase() }

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
                    val fetchingText = stringResource(R.string.fetching_location)
                    val onActionClick = {
                        // Validation logic with detailed error tracking
                        val errors = mutableMapOf<String, Boolean>()
                        
                        if (title.isBlank() && category != ListingCategory.PRAWNS) errors["title"] = true
                        if (location.isBlank() || location == fetchingText) errors["location"] = true
                        if (selectedPhotos.isEmpty() && category != ListingCategory.JOBS) photoError = true else photoError = false

                        when (category) {
                            ListingCategory.FISH -> {
                                if (fishType.isBlank()) errors["fishType"] = true
                                if (sizeValue.isBlank()) errors["sizeValue"] = true
                                if (fishAge.isBlank()) errors["fishAge"] = true
                                if (quantity.isBlank()) errors["quantity"] = true
                                if (price.isBlank()) errors["price"] = true
                            }
                            ListingCategory.PRAWNS -> {
                                if (prawnType.isBlank()) errors["prawnType"] = true
                                if (title.isBlank()) errors["title"] = true
                                if (hatcheryName.isBlank()) errors["hatcheryName"] = true
                                if (plDays.isBlank()) errors["plDays"] = true
                                if (rateValue.isBlank()) errors["rateValue"] = true
                                if (quantity.isBlank()) errors["quantity"] = true
                            }
                            ListingCategory.EQUIPMENTS -> {
                                if (equipmentType.isBlank()) errors["equipmentType"] = true
                                if (title.isBlank()) errors["title"] = true
                                if (price.isBlank()) errors["price"] = true
                            }
                            ListingCategory.VEHICLES -> {
                                if (selectedServiceType.isBlank()) errors["serviceType"] = true
                                if (vehicleName.isBlank()) errors["vehicleName"] = true
                                if (vehicleCapacity.isBlank()) errors["vehicleCapacity"] = true
                                if (title.isBlank()) errors["title"] = true
                            }
                            ListingCategory.FEED -> {
                                if (businessType.isBlank()) errors["businessType"] = true
                                if (feedName.isBlank()) errors["feedName"] = true
                                if (title.isBlank()) errors["title"] = true
                                if (ratePerTon.isBlank()) errors["ratePerTon"] = true
                            }
                            ListingCategory.SERVICES -> {
                                if (selectedServiceType.isBlank()) errors["serviceType"] = true
                                if (selectedServiceType == "Bore Well" && boreWellType.isBlank()) errors["boreWellType"] = true
                                if (selectedServiceType == "Live Fish Vehicles") {
                                    if (vehicleName.isBlank()) errors["vehicleName"] = true
                                    if (vehicleCapacity.isBlank()) errors["vehicleCapacity"] = true
                                }
                                if (selectedServiceType == "Nets" && netType.isBlank()) errors["netType"] = true
                                if (title.isBlank()) errors["title"] = true
                            }
                            ListingCategory.TANKS -> {
                                if (title.isBlank()) errors["title"] = true
                                if (tankAcres.isBlank()) errors["tankAcres"] = true
                                if (estPricePerAcre.isBlank()) errors["estPricePerAcre"] = true
                                if (tankLocation.isBlank()) errors["tankLocation"] = true
                            }
                            ListingCategory.JOBS -> {
                                if (jobType.isBlank()) errors["jobType"] = true
                                if (title.isBlank()) errors["title"] = true
                                if (tankAcres.isBlank()) errors["tankAcres"] = true
                                if (salary.isBlank()) errors["salary"] = true
                                if (tankLocation.isBlank()) errors["tankLocation"] = true
                            }
                        }

                        fieldErrors = errors

                        if (errors.isNotEmpty() || photoError) {
                            Toast.makeText(context, "Please fill all mandatory fields highlighted in red", Toast.LENGTH_SHORT).show()
                        } else {
                            val finalDescription = if (description.isBlank()) {
                                generateDefaultDescription(
                                    context, category, title, price, fishType, sizeValue, sizeType,
                                    quantity, unitType, prawnType, hatcheryName, rateValue, rateType,
                                    equipmentType, vehicleName, vehicleCapacity, feedName, ratePerTon,
                                    tankAcres, tankLocation, jobType, salary, selectedServiceType
                                )
                            } else {
                                description
                            }

                            val data = buildListingMap(
                                listingId, category, title, finalDescription, price, location, latLng, userMobileNumber,
                                userName, selectedServiceType, fishType, sizeType, sizeValue, fishAge, quantity,
                                unitType, prawnType, hatcheryName, rateType, rateValue, plDays, equipmentType, 
                                vehicleName, vehicleCapacity, businessType, feedName, ratePerTon, boreWellType, 
                                tankAcres, estPricePerAcre, tankLocation, jobType, salary, netType, userId
                            )
                            
                            val newBitmaps = selectedPhotos.filterIsInstance<Bitmap>()
                            val existingUrls = selectedPhotos.filterIsInstance<String>()
                            
                            listingViewModel.saveListing(data, newBitmaps, existingUrls)
                        }
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
                                Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Button(
                                onClick = onActionClick,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)
                            ) {
                                Text(stringResource(R.string.save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                                Text(stringResource(R.string.post_listing), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                            price, { price = it },
                            errors = fieldErrors
                        )
                        ListingCategory.PRAWNS -> PrawnFields(
                            prawnType, { prawnType = it },
                            hatcheryName, { hatcheryName = it },
                            rateType, { rateType = it },
                            rateValue, { rateValue = it },
                            plDays, { plDays = it },
                            quantity, { quantity = it },
                            unitType, { unitType = it },
                            title, { title = it },
                            errors = fieldErrors
                        )
                        ListingCategory.EQUIPMENTS -> EquipmentFields(
                            equipmentType, { equipmentType = it },
                            title, { title = it },
                            price, { price = it },
                            errors = fieldErrors
                        )
                        ListingCategory.VEHICLES -> VehicleFields(
                            selectedServiceType, { selectedServiceType = it },
                            vehicleName, { vehicleName = it },
                            vehicleCapacity, { vehicleCapacity = it },
                            title, { title = it },
                            errors = fieldErrors
                        )
                        ListingCategory.FEED -> FeedFields(
                            businessType, { businessType = it },
                            feedName, { feedName = it },
                            title, { title = it },
                            ratePerTon, { ratePerTon = it },
                            errors = fieldErrors
                        )
                        ListingCategory.SERVICES -> ServiceFields(
                            selectedServiceType, { selectedServiceType = it },
                            boreWellType, { boreWellType = it },
                            vehicleName, { vehicleName = it },
                            vehicleCapacity, { vehicleCapacity = it },
                            netType, { netType = it },
                            title, { title = it },
                            errors = fieldErrors
                        )
                        ListingCategory.TANKS -> TankFields(
                            title, { title = it },
                            tankAcres, { tankAcres = it },
                            estPricePerAcre, { estPricePerAcre = it },
                            tankLocation, { tankLocation = it },
                            errors = fieldErrors
                        )
                        ListingCategory.JOBS -> JobFields(
                            jobType, { jobType = it },
                            tankAcres, { tankAcres = it },
                            tankLocation, { tankLocation = it },
                            salary, { salary = it },
                            title, { title = it },
                            errors = fieldErrors
                        )
                    }
                }

                item {
                    ListingTextField(label = stringResource(R.string.description_label), value = description, onValueChange = { description = it }, minLines = 3, isRequired = false)
                }

                if (category != ListingCategory.JOBS) {
                    item {
                        PhotoSection(
                            photos = selectedPhotos,
                            onAddPhoto = { cameraLauncher.launch() },
                            onRemovePhoto = { index -> 
                                selectedPhotos = selectedPhotos.toMutableList().apply { removeAt(index) }
                                if (selectedPhotos.isNotEmpty()) photoError = false
                            },
                            isError = photoError
                        )
                    }
                }

                item {
                    LocationSection(location, onClick = onLocationChangeClick, isError = fieldErrors["location"] == true)
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }

        if (isFetchingData || postState is ListingViewModel.PostState.Loading) {
            LoadingOverlay(if (isFetchingData) stringResource(R.string.fetching_details) else stringResource(R.string.saving_listing))
        }
    }
}

@Composable
fun PhotoSection(
    photos: List<Any>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    isError: Boolean = false
) {
    Column {
        Text(text = androidx.compose.ui.text.buildAnnotatedString {
            append(stringResource(R.string.photos_label))
            append(" *")
        }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground)
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
                        .border(1.dp, if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { onAddPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else AquaBlue, modifier = Modifier.size(32.dp))
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
    price: String, onPriceChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(label = "Fish Type", value = fishType, options = listOf("Rohu", "Katla", "Karamosu", "Gaddi chepa", "Pangasius", "Roopchand", "Pandu gappa", "Tilapia", "Chitala", "Koramenu", "Valuga", "Engilayi", "Jalla", "Tuna", "Pulasa", "Crab", "Others"), onSelectionChange = onFishTypeChange, isError = errors["fishType"] == true)
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = stringResource(R.string.size_label), value = sizeType, options = listOf("Inches", "Centimeters"), onSelectionChange = onSizeTypeChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.size_value_label), value = sizeValue, onValueChange = onSizeValueChange, isError = errors["sizeValue"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        }
        ListingTextField(label = stringResource(R.string.fish_age_label), value = fishAge, onValueChange = onFishAgeChange, isError = errors["fishAge"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.quantity_label), value = quantity, onValueChange = onQuantityChange, isError = errors["quantity"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = stringResource(R.string.unit_label), value = unitType, options = listOf("Lakhs", "Thousands", "Kgs"), onSelectionChange = onUnitTypeChange)
            }
        }
        ListingTextField(label = stringResource(R.string.price_label), value = price, onValueChange = onPriceChange, isError = errors["price"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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
    unitType: String, onUnitTypeChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(label = "Type of Prawns", value = prawnType, options = listOf("Growth Line Plus", "Growth Line","Hard Line", "Hard Line Plus", "Sy Aqua", "Benchmark", "Cong", "Blue genetic"), onSelectionChange = onPrawnTypeChange, isError = errors["prawnType"] == true)
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
        ListingTextField(label = stringResource(R.string.hatchery_name_label), value = hatcheryName, onValueChange = onHatcheryNameChange, isError = errors["hatcheryName"] == true)
        ListingTextField(label = stringResource(R.string.pl_days_label), value = plDays, onValueChange = onPlDaysChange, isError = errors["plDays"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = stringResource(R.string.rate_label), value = rateType, options = listOf("Paise", "Rupees"), onSelectionChange = onRateTypeChange)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.rate_label), value = rateValue, onValueChange = onRateValueChange, isError = errors["rateValue"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.quantity_label), value = quantity, onValueChange = onQuantityChange, isError = errors["quantity"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = stringResource(R.string.unit_label), value = unitType, options = listOf("Lakhs", "Thousands"), onSelectionChange = onUnitTypeChange)
            }
        }
    }
}

@Composable
fun EquipmentFields(
    equipmentType: String, onEquipmentTypeChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(label = "Equipment Type", value = equipmentType, options = listOf("Aerators", "Motors", "Pump Motors", "Bore Motors", "Generators", "Pump Engines", "Boats", "Electrical Wires", "Others"), onSelectionChange = onEquipmentTypeChange, isError = errors["equipmentType"] == true)
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
        ListingTextField(label = stringResource(R.string.price_label), value = price, onValueChange = onPriceChange, isError = errors["price"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun VehicleFields(
    serviceType: String, onServiceTypeChange: (String) -> Unit,
    vehicleName: String, onVehicleNameChange: (String) -> Unit,
    vehicleCapacity: String, onVehicleCapacityChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Service Type", value = serviceType, options = listOf("Live Fish Vehicles", "Feed Transport"), onSelectionChange = onServiceTypeChange, isError = errors["serviceType"] == true)
        ListingTextField(label = stringResource(R.string.vehicle_name_label), value = vehicleName, onValueChange = onVehicleNameChange, isError = errors["vehicleName"] == true)
        ListingTextField(label = stringResource(R.string.vehicle_capacity_label), value = vehicleCapacity, onValueChange = onVehicleCapacityChange, isError = errors["vehicleCapacity"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
    }
}

@Composable
fun FeedFields(
    businessType: String, onBusinessTypeChange: (String) -> Unit,
    feedName: String, onFeedNameChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    ratePerTon: String, onRatePerTonChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Business Type", value = businessType, options = listOf("Fish Feed", "Shrimp Feed"), onSelectionChange = onBusinessTypeChange, isError = errors["businessType"] == true)
        ListingDropdown(label = "Feed Name", value = feedName, options = listOf("Godrej", "CP", "Avanti"), onSelectionChange = onFeedNameChange, isError = errors["feedName"] == true)
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
        ListingTextField(label = stringResource(R.string.rate_per_ton_label), value = ratePerTon, onValueChange = onRatePerTonChange, isError = errors["ratePerTon"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
}

@Composable
fun ServiceFields(
    serviceType: String, onServiceTypeChange: (String) -> Unit,
    boreWellType: String, onBoreWellTypeChange: (String) -> Unit,
    vehicleName: String, onVehicleNameChange: (String) -> Unit,
    vehicleCapacity: String, onVehicleCapacityChange: (String) -> Unit,
    netType: String, onNetTypeChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(
            label = "Service Type",
            value = serviceType,
            options = listOf("Bore Well", "Live Fish Vehicles", "Nets"),
            onSelectionChange = onServiceTypeChange,
            isError = errors["serviceType"] == true
        )

        when (serviceType) {
            "Bore Well" -> {
                SearchableListingDropdown(
                    label = "Bore Well Type",
                    value = boreWellType,
                    options = listOf("Drill bore", "Hand bore", "Others"),
                    onSelectionChange = onBoreWellTypeChange,
                    isError = errors["boreWellType"] == true
                )
            }
            "Live Fish Vehicles" -> {
                ListingTextField(label = stringResource(R.string.vehicle_name_label), value = vehicleName, onValueChange = onVehicleNameChange, isError = errors["vehicleName"] == true)
                ListingTextField(label = stringResource(R.string.vehicle_capacity_label), value = vehicleCapacity, onValueChange = onVehicleCapacityChange, isError = errors["vehicleCapacity"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            "Nets" -> {
                SearchableListingDropdown(
                    label = "Net Type",
                    value = netType,
                    options = listOf("Harvest netting", "Hand netting", "Others"),
                    onSelectionChange = onNetTypeChange,
                    isError = errors["netType"] == true
                )
            }
        }
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
    }
}

@Composable
fun TankFields(
    title: String, onTitleChange: (String) -> Unit,
    tankAcres: String, onTankAcresChange: (String) -> Unit,
    estPricePerAcre: String, onEstPricePerAcreChange: (String) -> Unit,
    tankLocation: String, onTankLocationChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.tank_acres_label), value = tankAcres, onValueChange = onTankAcresChange, isError = errors["tankAcres"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.est_price_per_acre_label), value = estPricePerAcre, onValueChange = onEstPricePerAcreChange, isError = errors["estPricePerAcre"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        ListingTextField(label = stringResource(R.string.tank_location_label), value = tankLocation, onValueChange = onTankLocationChange, isError = errors["tankLocation"] == true)
    }
}

@Composable
fun JobFields(
    jobType: String, onJobTypeChange: (String) -> Unit,
    tankAcres: String, onTankAcresChange: (String) -> Unit,
    tankLocation: String, onTankLocationChange: (String) -> Unit,
    salary: String, onSalaryChange: (String) -> Unit,
    title: String, onTitleChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap()
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
            onSelectionChange = onJobTypeChange,
            isError = errors["jobType"] == true
        )
        ListingTextField(label = stringResource(R.string.title_label), value = title, onValueChange = onTitleChange, isError = errors["title"] == true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.tank_acres_label), value = tankAcres, onValueChange = onTankAcresChange, isError = errors["tankAcres"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.salary_label), value = salary, onValueChange = onSalaryChange, isError = errors["salary"] == true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        ListingTextField(label = stringResource(R.string.tank_location_label), value = tankLocation, onValueChange = onTankLocationChange, isError = errors["tankLocation"] == true)
    }
}

@Composable
fun ListingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    isRequired: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column {
        Text(text = androidx.compose.ui.text.buildAnnotatedString {
            append(label)
            if (isRequired) {
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
            isError = isError,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else AquaBlue,
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
    onSelectionChange: (String) -> Unit,
    isError: Boolean = false
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
            border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value.ifEmpty { stringResource(R.string.select_label, label) },
                    color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = stringResource(R.string.select_label, label),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_label, label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
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
                                Text(stringResource(R.string.no_match_search), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun ListingDropdown(label: String, value: String, options: List<String>, onSelectionChange: (String) -> Unit, isError: Boolean = false) {
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
                isError = isError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else AquaBlue,
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
fun LocationSection(location: String, onClick: () -> Unit, isError: Boolean = false) {
    Column {
        Text(text = stringResource(R.string.location_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else Color.Red, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = location,
                    fontSize = 14.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.change_btn), color = if (isError) MaterialTheme.colorScheme.error else AquaBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun generateDefaultDescription(
    context: Context,
    category: ListingCategory,
    title: String,
    price: String,
    fishType: String,
    sizeValue: String,
    sizeType: String,
    quantity: String,
    unitType: String,
    prawnType: String,
    hatcheryName: String,
    rateValue: String,
    rateType: String,
    equipmentType: String,
    vehicleName: String,
    vehicleCapacity: String,
    feedName: String,
    ratePerTon: String,
    tankAcres: String,
    tankLocation: String,
    jobType: String,
    salary: String,
    serviceType: String
): String {
    val parts = mutableListOf<String>()

    when (category) {
        ListingCategory.FISH -> {
            if (fishType.isNotEmpty()) parts.add(fishType)
            if (title.isNotEmpty()) parts.add(title)
            if (sizeValue.isNotEmpty()) parts.add(context.getString(R.string.desc_size_prefix, "$sizeValue $sizeType"))
            if (quantity.isNotEmpty()) parts.add(context.getString(R.string.desc_quantity_prefix, "$quantity $unitType"))
            if (price.isNotEmpty()) parts.add(context.getString(R.string.desc_price_prefix, price))
        }
        ListingCategory.PRAWNS -> {
            if (prawnType.isNotEmpty()) parts.add(prawnType)
            if (title.isNotEmpty()) parts.add(title)
            if (hatcheryName.isNotEmpty()) parts.add(context.getString(R.string.desc_hatchery_prefix, hatcheryName))
            if (quantity.isNotEmpty()) parts.add(context.getString(R.string.desc_quantity_prefix, "$quantity $unitType"))
            if (rateValue.isNotEmpty()) parts.add(context.getString(R.string.desc_rate_prefix, "$rateValue per $rateType"))
        }
        ListingCategory.EQUIPMENTS -> {
            if (equipmentType.isNotEmpty()) parts.add(equipmentType)
            if (title.isNotEmpty()) parts.add(title)
            if (price.isNotEmpty()) parts.add(context.getString(R.string.desc_price_prefix, price))
        }
        ListingCategory.VEHICLES -> {
            if (serviceType.isNotEmpty()) parts.add(serviceType)
            if (vehicleName.isNotEmpty()) parts.add(vehicleName)
            if (vehicleCapacity.isNotEmpty()) parts.add(context.getString(R.string.desc_capacity_prefix, vehicleCapacity))
            if (title.isNotEmpty()) parts.add(title)
        }
        ListingCategory.FEED -> {
            if (feedName.isNotEmpty()) parts.add(feedName)
            if (title.isNotEmpty()) parts.add(title)
            if (ratePerTon.isNotEmpty()) parts.add(context.getString(R.string.desc_rate_prefix, "$ratePerTon/ton"))
        }
        ListingCategory.SERVICES -> {
            if (serviceType.isNotEmpty()) parts.add(serviceType)
            if (title.isNotEmpty()) parts.add(title)
        }
        ListingCategory.TANKS -> {
            if (title.isNotEmpty()) parts.add(title)
            if (tankAcres.isNotEmpty()) parts.add(context.getString(R.string.desc_tank_acres_prefix, tankAcres))
            if (tankLocation.isNotEmpty()) parts.add(tankLocation)
        }
        ListingCategory.JOBS -> {
            if (jobType.isNotEmpty()) parts.add(jobType)
            if (salary.isNotEmpty()) parts.add(context.getString(R.string.desc_salary_prefix, salary))
            if (tankAcres.isNotEmpty()) parts.add(context.getString(R.string.desc_tank_acres_prefix, tankAcres))
            if (title.isNotEmpty()) parts.add(title)
        }
    }

    return parts.filter { it.isNotEmpty() }.joinToString(". ")
}
