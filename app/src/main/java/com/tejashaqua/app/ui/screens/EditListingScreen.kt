package com.tejashaqua.app.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
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
import android.net.Uri
import com.tejashaqua.app.data.model.ListingCategory
import com.tejashaqua.app.ui.components.LoadingOverlay
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.viewmodel.ListingViewModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.intl.LocaleList
import com.tejashaqua.app.utils.LocaleHelper
import com.tejashaqua.app.utils.ImageUtils
import com.tejashaqua.app.utils.CurrencyUtils
import com.tejashaqua.app.utils.PermissionType
import androidx.compose.ui.text.input.VisualTransformation

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
    onPostClick: (Map<String, Any>) -> Unit,
    onDeleteClick: () -> Unit = {},
    onLocationChangeClick: () -> Unit,
    listingViewModel: ListingViewModel = viewModel(),
    pViewModel: com.tejashaqua.app.ui.viewmodel.PermissionViewModel = viewModel(),
    joinedAt: Long,
    userId: String,
    showMobileNumberPreference: Boolean
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
    val defaultSizeType = stringResource(R.string.unit_inches)
    val defaultUnitType = stringResource(R.string.unit_lakhs)
    val defaultRateType = stringResource(R.string.unit_paise)

    var fishType by remember { mutableStateOf("") }
    var sizeType by remember { mutableStateOf(defaultSizeType) }
    var sizeValue by remember { mutableStateOf("") }
    var fishAge by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf(defaultUnitType) }
    var prawnType by remember { mutableStateOf("") }
    var hatcheryName by remember { mutableStateOf("") }
    var rateType by remember { mutableStateOf(defaultRateType) }
    var rateValue by remember { mutableStateOf("") }
    var equipmentType by remember { mutableStateOf("") }
    var vehicleName by remember { mutableStateOf("") }
    var vehicleCapacity by remember { mutableStateOf("") }
    var vehicleCapacityUnit by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }
    var businessSubCategory by remember { mutableStateOf("") } // Feed or Medicine
    var feedName by remember { mutableStateOf("") }
    var medicineName by remember { mutableStateOf("") }
    var ratePerTon by remember { mutableStateOf("") }
    var boreWellType by remember { mutableStateOf("") }
    var tankAcres by remember { mutableStateOf("") }
    var tankType by remember { mutableStateOf("") } // Lease/Sell
    var estPricePerAcre by remember { mutableStateOf("") }
    var tankLocation by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var netType by remember { mutableStateOf("") }

    var fieldErrors by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var photoError by remember { mutableStateOf(false) }

    var isWaitingForLocation by remember { mutableStateOf(false) }
    var isRefreshingLocation by remember { mutableStateOf(false) }
    val fetchingLocText = stringResource(R.string.fetching_location)
    val deniedLocText = stringResource(R.string.location_permission_denied)
    val failedLocText = stringResource(R.string.failed_get_location)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isFetchingData by remember { mutableStateOf(false) }
    val postState by listingViewModel.postState.collectAsState()
    val context = LocalContext.current

    val isLocationFetched = location.isNotBlank() && 
                           !location.contains(fetchingLocText) && 
                           !location.contains(deniedLocText) && 
                           !location.contains(failedLocText)

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                selectedPhotos += uri.toString()
                photoError = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        val remainingSlots = 5 - selectedPhotos.size
        if (remainingSlots > 0 && uris.isNotEmpty()) {
            val toAdd = uris.take(remainingSlots).map { it.toString() }
            selectedPhotos += toAdd
            if (uris.size > remainingSlots) {
                Toast.makeText(context, "Only 5 photos allowed. Added $remainingSlots photos.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showPhotoOptions by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentLang = LocaleHelper.getSelectedLanguage(context) ?: "en"
    val keyboardOptionsBase = KeyboardOptions(
        hintLocales = if (currentLang == "te") LocaleList("te") else null
    )

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { 
                keyboardController?.hide()
                showPhotoOptions = false 
            },
            title = { Text(stringResource(R.string.choose_photo_source)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.camera)) },
                        leadingContent = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                        modifier = Modifier.clickable {
                            keyboardController?.hide()
                            showPhotoOptions = false
                            
                            pViewModel.requestFeaturePermissions(listOf(PermissionType.CAMERA)) {
                                val uri = ImageUtils.createImageUri(context)
                                tempCameraUri = uri
                                if (uri != null) {
                                    try {
                                        cameraLauncher.launch(uri)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Could not open camera app", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.gallery)) },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            keyboardController?.hide()
                            showPhotoOptions = false
                            pViewModel.requestFeaturePermissions(listOf(PermissionType.PHOTOS)) {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    keyboardController?.hide()
                    showPhotoOptions = false 
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Sync location if it changes from outside (via picker)
    LaunchedEffect(initialLocation, initialLatLng) {
        location = initialLocation
        latLng = initialLatLng
    }

    val boreWellService = stringResource(R.string.service_bore_well)
    val fishVehiclesService = stringResource(R.string.service_live_fish_vehicles)
    val netsService = stringResource(R.string.service_nets)

    val onActionClickInternal = {
        // Validation logic with detailed error tracking
        val errors = mutableMapOf<String, Boolean>()
        
        // Title is hidden for all categories, so we don't validate it here
        if (!isLocationFetched) errors["location"] = true
        photoError = selectedPhotos.isEmpty() && category != ListingCategory.JOBS

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
                if (hatcheryName.isBlank()) errors["hatcheryName"] = true
                if (rateValue.isBlank()) errors["rateValue"] = true
                if (quantity.isBlank()) errors["quantity"] = true
            }
            ListingCategory.EQUIPMENTS -> {
                if (equipmentType.isBlank()) errors["equipmentType"] = true
                if (price.isBlank()) errors["price"] = true
            }
            ListingCategory.VEHICLES -> {
                if (selectedServiceType.isBlank()) errors["serviceType"] = true
                if (vehicleName.isBlank()) errors["vehicleName"] = true
                if (vehicleCapacity.isBlank()) errors["vehicleCapacity"] = true
                if (price.isBlank()) errors["price"] = true
            }
            ListingCategory.FEED -> {
                if (businessType.isBlank()) errors["businessType"] = true
                if (feedName.isBlank()) errors["feedName"] = true
                if (ratePerTon.isBlank()) errors["ratePerTon"] = true
            }
            ListingCategory.BUSINESS -> {
                if (businessSubCategory.isBlank()) errors["businessSubCategory"] = true
                if (businessSubCategory == "Feed") {
                    if (businessType.isBlank()) errors["businessType"] = true
                    if (feedName.isBlank()) errors["feedName"] = true
                    if (ratePerTon.isBlank()) errors["ratePerTon"] = true
                } else if (businessSubCategory == "Medicine") {
                    if (businessType.isBlank()) errors["businessType"] = true
                    if (medicineName.isBlank()) errors["medicineName"] = true
                    if (ratePerTon.isBlank()) errors["ratePerTon"] = true
                }
            }
            ListingCategory.SERVICES -> {
                if (selectedServiceType.isBlank()) errors["serviceType"] = true
                if ((selectedServiceType == "Bore Well" || selectedServiceType == boreWellService) && boreWellType.isBlank()) errors["boreWellType"] = true
                if (selectedServiceType == "Live Fish Vehicles" || selectedServiceType == fishVehiclesService) {
                    if (vehicleName.isBlank()) errors["vehicleName"] = true
                    if (vehicleCapacity.isBlank()) errors["vehicleCapacity"] = true
                }
                if ((selectedServiceType == "Nets" || selectedServiceType == netsService) && netType.isBlank()) errors["netType"] = true
                if (price.isBlank()) errors["price"] = true
            }
            ListingCategory.TANKS -> {
                if (tankType.isBlank()) errors["tankType"] = true
                if (tankAcres.isBlank()) errors["tankAcres"] = true
                if (estPricePerAcre.isBlank()) errors["estPricePerAcre"] = true
                if (tankLocation.isBlank()) errors["tankLocation"] = true
            }
            ListingCategory.JOBS -> {
                if (jobType.isBlank()) errors["jobType"] = true
                if (tankAcres.isBlank()) errors["tankAcres"] = true
                if (salary.isBlank()) errors["salary"] = true
                if (tankLocation.isBlank()) errors["tankLocation"] = true
            }
        }

        fieldErrors = errors

        if (errors.isNotEmpty() || photoError) {
            Toast.makeText(context, context.getString(R.string.fill_mandatory_fields), Toast.LENGTH_SHORT).show()
        } else {
            val finalTitle = title.ifBlank {
                when (category) {
                    ListingCategory.FISH -> fishType
                    ListingCategory.PRAWNS -> prawnType
                    ListingCategory.EQUIPMENTS -> equipmentType
                    ListingCategory.VEHICLES -> vehicleName
                    ListingCategory.FEED -> feedName
                    ListingCategory.BUSINESS -> if (businessSubCategory == "Feed") feedName else if (businessSubCategory == "Medicine") medicineName else businessType
                    ListingCategory.SERVICES -> selectedServiceType
                    ListingCategory.TANKS -> tankType
                    ListingCategory.JOBS -> jobType
                }
            }

            val finalDescription = description.ifBlank {
                generateDefaultDescription(
                    context, category, finalTitle, price, fishType, sizeValue, sizeType,
                    quantity, unitType, prawnType, hatcheryName, rateValue, rateType,
                    equipmentType, vehicleName, vehicleCapacity, feedName, ratePerTon,
                    businessType, medicineName, businessSubCategory,
                    tankAcres, tankLocation, jobType, salary, selectedServiceType,
                    tankType
                )
            }

            val data = buildListingMap(
                listingId, category, finalTitle, finalDescription, price, location, latLng, userMobileNumber,
                userName, selectedServiceType, fishType, sizeType, sizeValue, fishAge, quantity,
                unitType, prawnType, hatcheryName, rateType, rateValue, equipmentType,
                vehicleName, vehicleCapacity, vehicleCapacityUnit, businessType, feedName, ratePerTon,
                medicineName, businessSubCategory,
                boreWellType, tankAcres, estPricePerAcre, tankLocation, jobType, salary, netType, 
                tankType, userId, showMobileNumberPreference, joinedAt
            )
            
            listingViewModel.saveListing(data, selectedPhotos)
        }
    }

    LaunchedEffect(location, isWaitingForLocation, isRefreshingLocation) {
        if (isWaitingForLocation && location != fetchingLocText && location != deniedLocText && location != failedLocText && location.isNotBlank()) {
            isWaitingForLocation = false
            onActionClickInternal()
        } else if (isWaitingForLocation && location == failedLocText) {
            isWaitingForLocation = false
            onActionClickInternal()
        }
        
        if (isRefreshingLocation && location != fetchingLocText && location != deniedLocText && location != failedLocText && location.isNotBlank()) {
            isRefreshingLocation = false
        } else if (isRefreshingLocation && (location == failedLocText || location == deniedLocText)) {
            isRefreshingLocation = false
        }
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
                                vehicleCapacityUnit = doc.getString("vehicleCapacityUnit") ?: ""
                            }
                            ListingCategory.FEED -> {
                                businessType = doc.getString("businessType") ?: ""
                                feedName = doc.getString("feedName") ?: ""
                                ratePerTon = doc.getString("ratePerTon") ?: ""
                            }
                            ListingCategory.BUSINESS -> {
                                businessSubCategory = doc.getString("businessSubCategory") ?: ""
                                businessType = doc.getString("businessType") ?: ""
                                feedName = doc.getString("feedName") ?: ""
                                medicineName = doc.getString("medicineName") ?: ""
                                ratePerTon = doc.getString("ratePerTon") ?: ""
                            }
                            ListingCategory.SERVICES -> {
                                selectedServiceType = doc.getString("serviceType") ?: ""
                                boreWellType = doc.getString("boreWellType") ?: ""
                                vehicleName = doc.getString("vehicleName") ?: ""
                                vehicleCapacity = doc.getString("vehicleCapacity") ?: ""
                                vehicleCapacityUnit = doc.getString("vehicleCapacityUnit") ?: ""
                                netType = doc.getString("netType") ?: ""
                            }
                            ListingCategory.TANKS -> {
                                tankAcres = doc.getString("tankAcres") ?: ""
                                tankType = doc.getString("tankType") ?: ""
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
                        (doc.get("images") as? List<*>)?.let { imageUrls ->
                            selectedPhotos = imageUrls.filterIsInstance<String>()
                        }
                    }
                }
                .addOnFailureListener {
                    isFetchingData = false
                }
        }
    }

    LaunchedEffect(postState) {
        val currentState = postState
        if (currentState is ListingViewModel.PostState.Success) {
            val data = currentState.data
            listingViewModel.resetState()
            onPostClick(data)
        } else if (currentState is ListingViewModel.PostState.Error) {
            val message = currentState.message
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val screenTitle = if (isEditMode) stringResource(R.string.edit_listing) else {
        when(category) {
            ListingCategory.FISH -> stringResource(R.string.cat_fish_seed)
            ListingCategory.PRAWNS -> stringResource(R.string.cat_prawns)
            ListingCategory.EQUIPMENTS -> stringResource(R.string.cat_equipments)
            ListingCategory.VEHICLES -> stringResource(R.string.cat_vehicles)
            ListingCategory.FEED -> stringResource(R.string.cat_feed)
            ListingCategory.BUSINESS -> stringResource(R.string.cat_business)
            ListingCategory.SERVICES -> stringResource(R.string.cat_services)
            ListingCategory.TANKS -> stringResource(R.string.cat_tanks)
            ListingCategory.JOBS -> stringResource(R.string.cat_jobs)
        }
    }

    val categoryColor = remember(category) {
        when (category) {
            ListingCategory.FISH -> Color(0xFF009688)
            ListingCategory.PRAWNS -> Color(0xFF3F51B5)
            ListingCategory.EQUIPMENTS -> Color(0xFF1976D2)
            ListingCategory.VEHICLES -> Color(0xFF1976D2)
            ListingCategory.FEED -> Color(0xFFE65100)
            ListingCategory.SERVICES -> Color(0xFFF57C00)
            ListingCategory.TANKS -> Color(0xFF388E3C)
            ListingCategory.BUSINESS -> Color(0xFFB71C1C)
            ListingCategory.JOBS -> Color(0xFF673AB7)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(screenTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            onBackClick()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = categoryColor)
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp, 
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding().imePadding()
                ) {
                    val onActionClick = {
                        keyboardController?.hide()
                        
                        pViewModel.requestFeaturePermissions(listOf(PermissionType.LOCATION)) {
                            val status = com.tejashaqua.app.utils.PermissionHelper.getStatus(context, PermissionType.LOCATION)
                            if (status == com.tejashaqua.app.utils.PermissionStatus.GRANTED) {
                                if (!isLocationFetched) {
                                    isWaitingForLocation = true
                                } else {
                                    onActionClickInternal()
                                }
                            } else {
                                onActionClickInternal()
                            }
                        }
                    }

                    if (isEditMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val isActionEnabled = isLocationFetched && !isRefreshingLocation && !isWaitingForLocation && !isFetchingData && postState !is ListingViewModel.PostState.Loading
                            
                            OutlinedButton(
                                onClick = { 
                                    keyboardController?.hide()
                                    showDeleteDialog = true 
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFF44336)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                                enabled = isActionEnabled
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Button(
                                onClick = onActionClick,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = categoryColor),
                                enabled = isActionEnabled
                            ) {
                                Text(stringResource(R.string.save_changes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            val isActionEnabled = isLocationFetched && !isRefreshingLocation && !isWaitingForLocation && !isFetchingData && postState !is ListingViewModel.PostState.Loading
                            
                            Button(
                                onClick = onActionClick,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = categoryColor),
                                enabled = isActionEnabled
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
                            sizeType, { sizeType = it },
                            sizeValue, { sizeValue = it },
                            fishAge, { fishAge = it },
                            quantity, { quantity = it },
                            unitType, { unitType = it },
                            price, { price = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.PRAWNS -> PrawnFields(
                            prawnType, { prawnType = it },
                            hatcheryName, { hatcheryName = it },
                            rateType, { rateType = it },
                            rateValue, { rateValue = it },
                            quantity, { quantity = it },
                            unitType, { unitType = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.EQUIPMENTS -> EquipmentFields(
                            equipmentType, { equipmentType = it },
                            price, { price = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.VEHICLES -> VehicleFields(
                            selectedServiceType, { selectedServiceType = it },
                            vehicleName, { vehicleName = it },
                            vehicleCapacity, { vehicleCapacity = it },
                            vehicleCapacityUnit, { vehicleCapacityUnit = it },
                            price, { price = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.FEED -> FeedFields(
                            businessType, { businessType = it },
                            feedName, { feedName = it },
                            ratePerTon, { ratePerTon = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.BUSINESS -> BusinessFields(
                            businessSubCategory, { businessSubCategory = it },
                            businessType, { businessType = it },
                            feedName, { feedName = it },
                            medicineName, { medicineName = it },
                            ratePerTon, { ratePerTon = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.SERVICES -> ServiceFields(
                            selectedServiceType, { selectedServiceType = it },
                            boreWellType, { boreWellType = it },
                            vehicleName, { vehicleName = it },
                            vehicleCapacity, { vehicleCapacity = it },
                            vehicleCapacityUnit, { vehicleCapacityUnit = it },
                            netType, { netType = it },
                            price, { price = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.TANKS -> TankFields(
                            tankType, { tankType = it },
                            tankAcres, { tankAcres = it },
                            estPricePerAcre, { estPricePerAcre = it },
                            tankLocation, { tankLocation = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                        ListingCategory.JOBS -> JobFields(
                            jobType, { jobType = it },
                            tankAcres, { tankAcres = it },
                            tankLocation, { tankLocation = it },
                            salary, { salary = it },
                            errors = fieldErrors,
                            keyboardOptions = keyboardOptionsBase,
                            accentColor = categoryColor
                        )
                    }
                }

                item {
                    ListingTextField(
                        label = stringResource(R.string.description_label), 
                        value = description, 
                        onValueChange = { description = it }, 
                        minLines = 3, 
                        isRequired = false,
                        keyboardOptions = keyboardOptionsBase,
                        accentColor = categoryColor
                    )
                }

                if (category != ListingCategory.JOBS) {
                    item {
                        PhotoSection(
                            photos = selectedPhotos,
                            onAddPhoto = { 
                                keyboardController?.hide()
                                showPhotoOptions = true 
                            },
                            onRemovePhoto = { index -> 
                                keyboardController?.hide()
                                selectedPhotos = selectedPhotos.toMutableList().apply { removeAt(index) }
                                if (selectedPhotos.isNotEmpty()) photoError = false
                            },
                            isError = photoError,
                            accentColor = categoryColor
                        )
                    }
                }

                item {
                    LocationSection(location, onClick = {
                        keyboardController?.hide()
                        isRefreshingLocation = true
                        onLocationChangeClick()
                    }, isError = fieldErrors["location"] == true, accentColor = categoryColor)
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }

        if (isFetchingData || postState is ListingViewModel.PostState.Loading || isWaitingForLocation || isRefreshingLocation) {
            LoadingOverlay(
                if (isFetchingData) stringResource(R.string.fetching_details) 
                else if (isWaitingForLocation || isRefreshingLocation) stringResource(R.string.fetching_location)
                else stringResource(R.string.saving_listing)
            )
        }
    }
}

@Composable
fun PhotoSection(
    photos: List<Any>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    isError: Boolean = false,
    accentColor: Color = AquaBlue
) {
    Column {
        Text(text = androidx.compose.ui.text.buildAnnotatedString {
            append(stringResource(R.string.photos_label))
            append(" *")
        }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
            if (photos.size < 5) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(1.dp, if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { onAddPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else accentColor, modifier = Modifier.size(32.dp))
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
    equipmentType: String,
    vehicleName: String,
    vehicleCapacity: String,
    vehicleCapacityUnit: String,
    businessType: String,
    feedName: String,
    ratePerTon: String,
    medicineName: String,
    businessSubCategory: String,
    boreWellType: String,
    tankAcres: String,
    estPricePerAcre: String,
    tankLocation: String,
    jobType: String,
    salary: String,
    netType: String,
    tankType: String,
    userId: String,
    showMobileNumberPreference: Boolean,
    joinedAt: Long
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
    data["sellerShowMobile"] = showMobileNumberPreference
    data["sellerJoinedAt"] = joinedAt
    
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
            data["quantity"] = quantity
            data["unitType"] = unitType
        }
        ListingCategory.EQUIPMENTS -> {
            data["equipmentType"] = equipmentType
        }
        ListingCategory.VEHICLES -> {
            data["vehicleName"] = vehicleName
            data["vehicleCapacity"] = vehicleCapacity
            data["vehicleCapacityUnit"] = vehicleCapacityUnit
            data["price"] = price
        }
        ListingCategory.FEED -> {
            data["businessType"] = businessType
            data["feedName"] = feedName
            data["ratePerTon"] = ratePerTon
        }
        ListingCategory.BUSINESS -> {
            data["businessSubCategory"] = businessSubCategory
            data["businessType"] = businessType
            data["feedName"] = feedName
            data["medicineName"] = medicineName
            data["ratePerTon"] = ratePerTon
        }
        ListingCategory.SERVICES -> {
            data["serviceType"] = serviceType
            data["boreWellType"] = boreWellType
            data["vehicleName"] = vehicleName
            data["vehicleCapacity"] = vehicleCapacity
            data["vehicleCapacityUnit"] = vehicleCapacityUnit
            data["netType"] = netType
            data["price"] = price
        }
        ListingCategory.TANKS -> {
            data["tankAcres"] = tankAcres
            data["tankType"] = tankType
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
    sizeType: String, onSizeTypeChange: (String) -> Unit,
    sizeValue: String, onSizeValueChange: (String) -> Unit,
    fishAge: String, onFishAgeChange: (String) -> Unit,
    quantity: String, onQuantityChange: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") unitType: String, @Suppress("UNUSED_PARAMETER") onUnitTypeChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val othersStr = stringResource(R.string.fish_others)
    val rohu = stringResource(R.string.fish_rohu)
    val katla = stringResource(R.string.fish_katla)
    val karamosu = stringResource(R.string.fish_karamosu)
    val gaddiChepa = stringResource(R.string.fish_gaddi_chepa)
    val pangasius = stringResource(R.string.fish_pangasius)
    val roopchand = stringResource(R.string.fish_roopchand)
    val panduGappa = stringResource(R.string.fish_pandu_gappa)
    val tilapia = stringResource(R.string.fish_tilapia)
    val chitala = stringResource(R.string.fish_chitala)
    val koramenu = stringResource(R.string.fish_koramenu)
    val valuga = stringResource(R.string.fish_valuga)
    val engilayi = stringResource(R.string.fish_engilayi)
    val jalla = stringResource(R.string.fish_jalla)
    val tuna = stringResource(R.string.fish_tuna)
    val pulasa = stringResource(R.string.fish_pulasa)
    val crab = stringResource(R.string.fish_crab)
    val bangaruTeega = stringResource(R.string.fish_bangaru_teega)

    val seedSuffix = " " + stringResource(R.string.seed_suffix)
    val options = remember { 
        listOf(rohu, katla, karamosu, gaddiChepa, pangasius, roopchand, panduGappa, tilapia, chitala, koramenu, valuga, engilayi, jalla, tuna, pulasa, crab, bangaruTeega)
            .map { "$it$seedSuffix" } + othersStr 
    }
    
    var isOthers by remember { mutableStateOf(fishType.isNotEmpty() && !options.filter { it != othersStr }.contains(fishType)) }
    var otherName by remember { mutableStateOf(if (isOthers) fishType else "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(
            label = stringResource(R.string.fish_type_label), 
            value = if (isOthers) othersStr else fishType, 
            options = options, 
            onSelectionChange = {
                if (it == othersStr) {
                    isOthers = true
                    onFishTypeChange(otherName)
                } else {
                    isOthers = false
                    onFishTypeChange(it)
                }
            },
            isError = errors["fishType"] == true,
            accentColor = accentColor
        )

        if (isOthers) {
            ListingTextField(
                label = stringResource(R.string.others_mention),
                value = otherName,
                onValueChange = {
                    otherName = it
                    onFishTypeChange(it)
                },
                isRequired = true,
                isError = errors["fishType"] == true,
                keyboardOptions = keyboardOptions,
                accentColor = accentColor
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.size_label), value = sizeValue, onValueChange = onSizeValueChange, isError = errors["sizeValue"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Decimal), accentColor = accentColor)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = stringResource(R.string.unit_label), value = sizeType, options = listOf(stringResource(R.string.unit_inches), stringResource(R.string.size_kilo_label)), onSelectionChange = onSizeTypeChange, accentColor = accentColor)
            }
        }
        ListingTextField(label = stringResource(R.string.fish_age_label), value = fishAge, onValueChange = onFishAgeChange, isError = errors["fishAge"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
        
        ListingTextField(
            label = stringResource(R.string.no_of_stock_label), 
            value = quantity, 
            onValueChange = onQuantityChange,
            isError = errors["quantity"] == true, 
            keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), 
            accentColor = accentColor
        )
        
        ListingTextField(label = stringResource(R.string.price_label), value = price, onValueChange = onPriceChange, isError = errors["price"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
    }
}

@Composable
fun PrawnFields(
    prawnType: String, onPrawnTypeChange: (String) -> Unit,
    hatcheryName: String, onHatcheryNameChange: (String) -> Unit,
    rateType: String, onRateTypeChange: (String) -> Unit,
    rateValue: String, onRateValueChange: (String) -> Unit,
    quantity: String, onQuantityChange: (String) -> Unit,
    unitType: String, onUnitTypeChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val othersStr = stringResource(R.string.fish_others)
    val g1 = stringResource(R.string.prawn_growth_line_plus)
    val g2 = stringResource(R.string.prawn_growth_line)
    val h1 = stringResource(R.string.prawn_hard_line)
    val h2 = stringResource(R.string.prawn_hard_line_plus)
    val sy = stringResource(R.string.prawn_sy_aqua)
    val bm = stringResource(R.string.prawn_benchmark)
    val cg = stringResource(R.string.prawn_cong)
    val bg = stringResource(R.string.prawn_blue_genetic)

    val options = remember { listOf(g1, g2, h1, h2, sy, bm, cg, bg, othersStr) }
    
    var isOthers by remember { mutableStateOf(prawnType.isNotEmpty() && !options.filter { it != othersStr }.contains(prawnType)) }
    var otherName by remember { mutableStateOf(if (isOthers) prawnType else "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(
            label = stringResource(R.string.prawn_type_label), 
            value = if (isOthers) othersStr else prawnType, 
            options = options,
            onSelectionChange = {
                if (it == othersStr) {
                    isOthers = true
                    onPrawnTypeChange(otherName)
                } else {
                    isOthers = false
                    onPrawnTypeChange(it)
                }
            }, 
            isError = errors["prawnType"] == true,
            accentColor = accentColor
        )

        if (isOthers) {
            ListingTextField(
                label = stringResource(R.string.others_mention),
                value = otherName,
                onValueChange = {
                    otherName = it
                    onPrawnTypeChange(it)
                },
                isRequired = true,
                isError = errors["prawnType"] == true,
                keyboardOptions = keyboardOptions,
                accentColor = accentColor
            )
        }

        ListingTextField(label = stringResource(R.string.hatchery_name_label), value = hatcheryName, onValueChange = onHatcheryNameChange, isError = errors["hatcheryName"] == true, keyboardOptions = keyboardOptions, accentColor = accentColor)
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.rate_label), value = rateValue, onValueChange = onRateValueChange, isError = errors["rateValue"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = stringResource(R.string.unit_label), value = rateType, options = listOf(stringResource(R.string.unit_paise), stringResource(R.string.unit_rupees)), onSelectionChange = onRateTypeChange, accentColor = accentColor)
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.stock_label), value = quantity, onValueChange = onQuantityChange, isError = errors["quantity"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(label = stringResource(R.string.unit_label), value = unitType, options = listOf(stringResource(R.string.unit_lakhs), stringResource(R.string.unit_million)), onSelectionChange = onUnitTypeChange, accentColor = accentColor)
            }
        }
    }
}

@Composable
fun EquipmentFields(
    equipmentType: String, onEquipmentTypeChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val othersStr = stringResource(R.string.fish_others)
    val options = listOf(
        stringResource(R.string.equip_aerators), stringResource(R.string.equip_motors), 
        stringResource(R.string.equip_pump_motors), stringResource(R.string.equip_bore_motors), 
        stringResource(R.string.equip_generators), stringResource(R.string.equip_pump_engines), 
        stringResource(R.string.equip_boats), stringResource(R.string.equip_mavulu), stringResource(R.string.equip_wires), 
        othersStr
    )
    
    var isOthers by remember { mutableStateOf(equipmentType.isNotEmpty() && !options.filter { it != othersStr }.contains(equipmentType)) }
    var otherName by remember { mutableStateOf(if (isOthers) equipmentType else "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(
            label = stringResource(R.string.equipment_type_label), 
            value = if (isOthers) othersStr else equipmentType, 
            options = options,
            onSelectionChange = {
                if (it == othersStr) {
                    isOthers = true
                    onEquipmentTypeChange(otherName)
                } else {
                    isOthers = false
                    onEquipmentTypeChange(it)
                }
            }, 
            isError = errors["equipmentType"] == true,
            accentColor = accentColor
        )

        if (isOthers) {
            ListingTextField(
                label = stringResource(R.string.others_mention),
                value = otherName,
                onValueChange = {
                    otherName = it
                    onEquipmentTypeChange(it)
                },
                isRequired = true,
                isError = errors["equipmentType"] == true,
                keyboardOptions = keyboardOptions,
                accentColor = accentColor
            )
        }

        ListingTextField(label = stringResource(R.string.price_label), value = price, onValueChange = onPriceChange, isError = errors["price"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
    }
}

@Composable
fun VehicleFields(
    serviceType: String, onServiceTypeChange: (String) -> Unit,
    vehicleName: String, onVehicleNameChange: (String) -> Unit,
    vehicleCapacity: String, onVehicleCapacityChange: (String) -> Unit,
    vehicleCapacityUnit: String, onVehicleCapacityUnitChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val unitTon = stringResource(R.string.unit_ton)
    val unitKg = stringResource(R.string.unit_kg)
    val unitOptions = listOf(unitTon, unitKg)

    LaunchedEffect(Unit) {
        if (vehicleCapacityUnit.isEmpty()) onVehicleCapacityUnitChange(unitTon)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(
            label = stringResource(R.string.service_type_label), 
            value = serviceType, 
            options = listOf(stringResource(R.string.service_live_fish_vehicles), stringResource(R.string.service_feed_transport)), 
            onSelectionChange = onServiceTypeChange, 
            isError = errors["serviceType"] == true,
            accentColor = accentColor
        )
        ListingTextField(label = stringResource(R.string.vehicle_name_label), value = vehicleName, onValueChange = onVehicleNameChange, isError = errors["vehicleName"] == true, keyboardOptions = keyboardOptions, accentColor = accentColor)
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1.5f)) {
                ListingTextField(
                    label = stringResource(R.string.vehicle_capacity_label), 
                    value = vehicleCapacity, 
                    onValueChange = onVehicleCapacityChange, 
                    isError = errors["vehicleCapacity"] == true, 
                    keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), 
                    accentColor = accentColor
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingDropdown(
                    label = stringResource(R.string.capacity_unit_label),
                    value = vehicleCapacityUnit.ifEmpty { unitTon },
                    options = unitOptions,
                    onSelectionChange = onVehicleCapacityUnitChange,
                    accentColor = accentColor
                )
            }
        }

        ListingTextField(label = stringResource(R.string.price_label), value = price, onValueChange = onPriceChange, isError = errors["price"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
    }
}

@Composable
fun BusinessFields(
    businessSubCategory: String, onBusinessSubCategoryChange: (String) -> Unit,
    businessType: String, onBusinessTypeChange: (String) -> Unit,
    feedName: String, onFeedNameChange: (String) -> Unit,
    medicineName: String, onMedicineNameChange: (String) -> Unit,
    ratePerTon: String, onRatePerTonChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val othersStr = stringResource(R.string.fish_others)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val fishFeed = stringResource(R.string.biz_fish_feed)
        val prawnFeed = stringResource(R.string.biz_prawn_feed)
        val fishMed = stringResource(R.string.biz_fish_med)
        val prawnMed = stringResource(R.string.biz_prawn_med)

        SearchableListingDropdown(
            label = stringResource(R.string.business_type_label),
            value = businessType.ifEmpty { businessSubCategory },
            options = listOf(fishFeed, prawnFeed, fishMed, prawnMed, othersStr),
            onSelectionChange = {
                if (it == othersStr) {
                    onBusinessSubCategoryChange("Others")
                    onBusinessTypeChange(othersStr)
                    onFeedNameChange("")
                    onMedicineNameChange("")
                    onRatePerTonChange("")
                } else {
                    onBusinessTypeChange(it)
                    if (it == fishFeed || it == prawnFeed) {
                        onBusinessSubCategoryChange("Feed")
                        onMedicineNameChange("")
                    } else {
                        onBusinessSubCategoryChange("Medicine")
                        onFeedNameChange("")
                        onRatePerTonChange("")
                    }
                }
            },
            isError = errors["businessType"] == true,
            accentColor = accentColor
        )

        if (businessSubCategory == "Feed") {
            val feedOptions = if (businessType == prawnFeed) {
                listOf(
                    stringResource(R.string.feed_avanti), 
                    stringResource(R.string.feed_cp), 
                    stringResource(R.string.feed_growel), 
                    stringResource(R.string.feed_ifeed), 
                    stringResource(R.string.feed_godrej_agrovet), 
                    stringResource(R.string.feed_sandhya), 
                    othersStr
                )
            } else {
                listOf(stringResource(R.string.feed_rice_bran), stringResource(R.string.feed_fine_rice_bran), stringResource(R.string.feed_pellets), othersStr)
            }

            var isOtherFeed by remember { mutableStateOf(feedName.isNotEmpty() && !feedOptions.filter { it != othersStr }.contains(feedName)) }
            var otherFeedName by remember { mutableStateOf(if (isOtherFeed) feedName else "") }

            SearchableListingDropdown(
                label = stringResource(R.string.feed_name_label),
                value = if (isOtherFeed) othersStr else feedName,
                options = feedOptions,
                onSelectionChange = {
                    if (it == othersStr) {
                        isOtherFeed = true
                        onFeedNameChange(otherFeedName)
                    } else {
                        isOtherFeed = false
                        onFeedNameChange(it)
                    }
                },
                isError = errors["feedName"] == true,
                accentColor = accentColor
            )

            if (isOtherFeed) {
                ListingTextField(
                    label = stringResource(R.string.others_mention),
                    value = otherFeedName,
                    onValueChange = {
                        otherFeedName = it
                        onFeedNameChange(it)
                    },
                    isRequired = true,
                    isError = errors["feedName"] == true,
                    keyboardOptions = keyboardOptions,
                    accentColor = accentColor
                )
            }

            ListingTextField(
                label = stringResource(R.string.rate_per_ton_label),
                value = ratePerTon,
                onValueChange = onRatePerTonChange,
                isError = errors["ratePerTon"] == true,
                keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number),
                accentColor = accentColor
            )
        } else if (businessSubCategory == "Medicine") {
            ListingTextField(
                label = stringResource(R.string.medicine_name_label),
                value = medicineName,
                onValueChange = onMedicineNameChange,
                isError = errors["medicineName"] == true,
                keyboardOptions = keyboardOptions,
                accentColor = accentColor
            )

            ListingTextField(
                label = stringResource(R.string.medicine_rate_label),
                value = ratePerTon,
                onValueChange = onRatePerTonChange,
                isError = errors["ratePerTon"] == true,
                keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number),
                accentColor = accentColor
            )
        } else if (businessSubCategory == "Others") {
            var otherBusinessName by remember { mutableStateOf(if (businessType != othersStr) businessType else "") }
            ListingTextField(
                label = stringResource(R.string.others_mention),
                value = otherBusinessName,
                onValueChange = {
                    otherBusinessName = it
                    onBusinessTypeChange(it)
                },
                isRequired = true,
                isError = errors["businessType"] == true,
                keyboardOptions = keyboardOptions,
                accentColor = accentColor
            )

            ListingTextField(
                label = stringResource(R.string.price_label),
                value = ratePerTon,
                onValueChange = onRatePerTonChange,
                isError = errors["ratePerTon"] == true,
                keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number),
                accentColor = accentColor
            )
        }
    }
}

@Composable
fun FeedFields(
    businessType: String, onBusinessTypeChange: (String) -> Unit,
    feedName: String, onFeedNameChange: (String) -> Unit,
    ratePerTon: String, onRatePerTonChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = stringResource(R.string.business_type_label), value = businessType, options = listOf(stringResource(R.string.biz_fish_feed), stringResource(R.string.biz_prawn_feed)), onSelectionChange = onBusinessTypeChange, isError = errors["businessType"] == true, accentColor = accentColor)
        ListingDropdown(label = stringResource(R.string.feed_name_label), value = feedName, options = listOf("Godrej", "CP", "Avanti"), onSelectionChange = onFeedNameChange, isError = errors["feedName"] == true, accentColor = accentColor)
        ListingTextField(label = stringResource(R.string.rate_per_ton_label), value = ratePerTon, onValueChange = onRatePerTonChange, isError = errors["ratePerTon"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
    }
}

@Composable
fun ServiceFields(
    serviceType: String, onServiceTypeChange: (String) -> Unit,
    boreWellType: String, onBoreWellTypeChange: (String) -> Unit,
    vehicleName: String, onVehicleNameChange: (String) -> Unit,
    vehicleCapacity: String, onVehicleCapacityChange: (String) -> Unit,
    vehicleCapacityUnit: String, onVehicleCapacityUnitChange: (String) -> Unit,
    netType: String, onNetTypeChange: (String) -> Unit,
    price: String, onPriceChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val unitTon = stringResource(R.string.unit_ton)
    val unitKg = stringResource(R.string.unit_kg)
    val unitOptions = listOf(unitTon, unitKg)

    LaunchedEffect(Unit) {
        if (vehicleCapacityUnit.isEmpty()) onVehicleCapacityUnitChange(unitTon)
    }

    val othersStr = stringResource(R.string.fish_others)
    val serviceOptions = listOf(stringResource(R.string.service_bore_well), stringResource(R.string.service_live_fish_vehicles), stringResource(R.string.service_nets), stringResource(R.string.service_chart_writing), stringResource(R.string.service_earth_movers), othersStr)
    
    var isOthers by remember { mutableStateOf(serviceType.isNotEmpty() && !serviceOptions.filter { it != othersStr }.contains(serviceType)) }
    var otherName by remember { mutableStateOf(if (isOthers) serviceType else "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val boreWell = stringResource(R.string.service_bore_well)
        val fishVehicles = stringResource(R.string.service_live_fish_vehicles)
        val nets = stringResource(R.string.service_nets)

        SearchableListingDropdown(
            label = stringResource(R.string.service_type_label),
            value = if (isOthers) othersStr else serviceType,
            options = serviceOptions,
            onSelectionChange = {
                if (it == othersStr) {
                    isOthers = true
                    onServiceTypeChange(otherName)
                } else {
                    isOthers = false
                    onServiceTypeChange(it)
                }
            },
            isError = errors["serviceType"] == true,
            accentColor = accentColor
        )

        if (isOthers) {
            ListingTextField(
                label = stringResource(R.string.others_mention),
                value = otherName,
                onValueChange = {
                    otherName = it
                    onServiceTypeChange(it)
                },
                isRequired = true,
                isError = errors["serviceType"] == true,
                keyboardOptions = keyboardOptions,
                accentColor = accentColor
            )
        }

        when (serviceType) {
            boreWell -> {
                SearchableListingDropdown(
                    label = stringResource(R.string.bore_type_label),
                    value = boreWellType,
                    options = listOf(stringResource(R.string.bore_drill), stringResource(R.string.bore_hand), stringResource(R.string.fish_others)),
                    onSelectionChange = onBoreWellTypeChange,
                    isError = errors["boreWellType"] == true,
                    accentColor = accentColor
                )
            }
            fishVehicles -> {
                ListingTextField(label = stringResource(R.string.vehicle_name_label), value = vehicleName, onValueChange = onVehicleNameChange, isError = errors["vehicleName"] == true, keyboardOptions = keyboardOptions, accentColor = accentColor)
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1.5f)) {
                        ListingTextField(
                            label = stringResource(R.string.vehicle_capacity_label), 
                            value = vehicleCapacity, 
                            onValueChange = onVehicleCapacityChange, 
                            isError = errors["vehicleCapacity"] == true, 
                            keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), 
                            accentColor = accentColor
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ListingDropdown(
                            label = stringResource(R.string.capacity_unit_label),
                            value = vehicleCapacityUnit.ifEmpty { unitTon },
                            options = unitOptions,
                            onSelectionChange = onVehicleCapacityUnitChange,
                            accentColor = accentColor
                        )
                    }
                }
            }
            nets -> {
                SearchableListingDropdown(
                    label = stringResource(R.string.net_type_label),
                    value = netType,
                    options = listOf(stringResource(R.string.net_harvest), stringResource(R.string.net_hand), stringResource(R.string.fish_others)),
                    onSelectionChange = onNetTypeChange,
                    isError = errors["netType"] == true,
                    accentColor = accentColor
                )
            }
        }

        ListingTextField(label = stringResource(R.string.price_label), value = price, onValueChange = onPriceChange, isError = errors["price"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
    }
}

@Composable
fun TankFields(
    tankType: String, onTankTypeChange: (String) -> Unit,
    tankAcres: String, onTankAcresChange: (String) -> Unit,
    estPricePerAcre: String, onEstPricePerAcreChange: (String) -> Unit,
    tankLocation: String, onTankLocationChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = stringResource(R.string.type_label), value = tankType, options = listOf(stringResource(R.string.tank_lease), stringResource(R.string.tank_sell)), onSelectionChange = onTankTypeChange, isError = errors["tankType"] == true, accentColor = accentColor)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.tank_acres_label), value = tankAcres, onValueChange = onTankAcresChange, isError = errors["tankAcres"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Decimal), accentColor = accentColor)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.est_price_per_acre_label), value = estPricePerAcre, onValueChange = onEstPricePerAcreChange, isError = errors["estPricePerAcre"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
            }
        }
        ListingTextField(label = stringResource(R.string.tank_location_label), value = tankLocation, onValueChange = onTankLocationChange, isError = errors["tankLocation"] == true, keyboardOptions = keyboardOptions, accentColor = accentColor)
    }
}

@Composable
fun JobFields(
    jobType: String, onJobTypeChange: (String) -> Unit,
    tankAcres: String, onTankAcresChange: (String) -> Unit,
    tankLocation: String, onTankLocationChange: (String) -> Unit,
    salary: String, onSalaryChange: (String) -> Unit,
    errors: Map<String, Boolean> = emptyMap(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val othersStr = stringResource(R.string.fish_others)
    val options = listOf(
        stringResource(R.string.job_watchman),
        stringResource(R.string.job_supervisor),
        stringResource(R.string.job_electrician),
        stringResource(R.string.job_technician),
        othersStr
    )
    
    var isOthers by remember { mutableStateOf(jobType.isNotEmpty() && !options.filter { it != othersStr }.contains(jobType)) }
    var otherName by remember { mutableStateOf(if (isOthers) jobType else "") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SearchableListingDropdown(
            label = stringResource(R.string.job_type_label),
            value = if (isOthers) othersStr else jobType,
            options = options,
            onSelectionChange = {
                if (it == othersStr) {
                    isOthers = true
                    onJobTypeChange(otherName)
                } else {
                    isOthers = false
                    onJobTypeChange(it)
                }
            },
            isError = errors["jobType"] == true,
            accentColor = accentColor
        )

        if (isOthers) {
            ListingTextField(
                label = stringResource(R.string.others_mention),
                value = otherName,
                onValueChange = {
                    otherName = it
                    onJobTypeChange(it)
                },
                isRequired = true,
                isError = errors["jobType"] == true,
                keyboardOptions = keyboardOptions,
                accentColor = accentColor
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.tank_acres_label), value = tankAcres, onValueChange = onTankAcresChange, isError = errors["tankAcres"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Decimal), accentColor = accentColor)
            }
            Box(modifier = Modifier.weight(1f)) {
                ListingTextField(label = stringResource(R.string.salary_label), value = salary, onValueChange = onSalaryChange, isError = errors["salary"] == true, keyboardOptions = keyboardOptions.copy(keyboardType = KeyboardType.Number), accentColor = accentColor)
            }
        }
        ListingTextField(label = stringResource(R.string.tank_location_label), value = tankLocation, onValueChange = onTankLocationChange, isError = errors["tankLocation"] == true, keyboardOptions = keyboardOptions, accentColor = accentColor)
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = AquaBlue
) {
    val visualTransformation = if (keyboardOptions.keyboardType == KeyboardType.Number || 
        keyboardOptions.keyboardType == KeyboardType.Decimal) {
        CurrencyUtils.IndianNumberVisualTransformation()
    } else {
        VisualTransformation.None
    }

    Column {
        Text(text = androidx.compose.ui.text.buildAnnotatedString {
            append(label)
            if (isRequired) {
                append(" *")
            }
        }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            minLines = minLines,
            isError = isError,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else accentColor,
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
    isError: Boolean = false,
    accentColor: Color = AquaBlue
) {
    var showSheet by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column {
        Text(
            text = "$label *",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            onClick = { 
                keyboardController?.hide()
                showSheet = true 
            },
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
                        focusedBorderColor = accentColor,
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
                                    color = if (option == value) accentColor else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            trailingContent = {
                                if (option == value) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = accentColor)
                                }
                            },
                            modifier = Modifier.clickable {
                                keyboardController?.hide()
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
fun ListingDropdown(label: String, value: String, options: List<String>, onSelectionChange: (String) -> Unit, isError: Boolean = false, accentColor: Color = AquaBlue) {
    var expanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column {
        Text(text = "$label *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { 
                keyboardController?.hide()
                expanded = !expanded 
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp),
                isError = isError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else accentColor,
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
                            keyboardController?.hide()
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
fun LocationSection(location: String, onClick: () -> Unit, isError: Boolean = false, accentColor: Color = AquaBlue) {
    Column {
        Text(text = stringResource(R.string.location_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
                    fontSize = 12.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.change_btn), color = if (isError) MaterialTheme.colorScheme.error else accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    businessType: String,
    medicineName: String,
    businessSubCategory: String,
    tankAcres: String,
    tankLocation: String,
    jobType: String,
    salary: String,
    serviceType: String,
    tankType: String
): String {
    val parts = mutableListOf<String>()

    when (category) {
        ListingCategory.FISH -> {
            if (fishType.isNotEmpty()) parts.add(fishType)
            if (title.isNotEmpty() && title != fishType) parts.add(title)
            if (sizeValue.isNotEmpty()) parts.add(context.getString(R.string.desc_size_prefix, "$sizeValue $sizeType"))
            if (quantity.isNotEmpty()) parts.add(context.getString(R.string.desc_quantity_prefix, CurrencyUtils.formatPrice(quantity)))
            if (price.isNotEmpty()) parts.add(context.getString(R.string.desc_price_prefix, price))
        }
        ListingCategory.PRAWNS -> {
            if (prawnType.isNotEmpty()) parts.add(prawnType)
            if (title.isNotEmpty() && title != prawnType) parts.add(title)
            if (hatcheryName.isNotEmpty()) parts.add(context.getString(R.string.desc_hatchery_prefix, hatcheryName))
            if (quantity.isNotEmpty()) parts.add(context.getString(R.string.desc_quantity_prefix, "$quantity $unitType"))
            if (rateValue.isNotEmpty()) parts.add(context.getString(R.string.desc_rate_prefix, "$rateValue per $rateType"))
        }
        ListingCategory.EQUIPMENTS -> {
            if (equipmentType.isNotEmpty()) parts.add(equipmentType)
            if (title.isNotEmpty() && title != equipmentType) parts.add(title)
            if (price.isNotEmpty()) parts.add(context.getString(R.string.desc_price_prefix, price))
        }
        ListingCategory.VEHICLES -> {
            if (serviceType.isNotEmpty()) parts.add(serviceType)
            if (vehicleName.isNotEmpty()) parts.add(vehicleName)
            if (vehicleCapacity.isNotEmpty()) parts.add(context.getString(R.string.desc_capacity_prefix, vehicleCapacity))
            if (title.isNotEmpty() && title != vehicleName && title != serviceType) parts.add(title)
        }
        ListingCategory.FEED -> {
            if (feedName.isNotEmpty()) parts.add(feedName)
            if (title.isNotEmpty() && title != feedName) parts.add(title)
            if (ratePerTon.isNotEmpty()) parts.add(context.getString(R.string.desc_rate_prefix, "$ratePerTon/ton"))
        }
        ListingCategory.BUSINESS -> {
            val mainType = if (businessSubCategory == "Feed") feedName else if (businessSubCategory == "Medicine") medicineName else businessType
            if (businessSubCategory.isNotEmpty()) parts.add(businessSubCategory)
            if (businessType.isNotEmpty()) parts.add(businessType)
            if (businessSubCategory == "Feed" && feedName.isNotEmpty()) parts.add(feedName)
            if (businessSubCategory == "Medicine" && medicineName.isNotEmpty()) parts.add(medicineName)
            if (title.isNotEmpty() && title != mainType && title != businessSubCategory && title != businessType) parts.add(title)
            if (ratePerTon.isNotEmpty()) parts.add(context.getString(R.string.desc_rate_prefix, "$ratePerTon/ton"))
        }
        ListingCategory.SERVICES -> {
            if (serviceType.isNotEmpty()) parts.add(serviceType)
            if (title.isNotEmpty() && title != serviceType) parts.add(title)
        }
        ListingCategory.TANKS -> {
            if (tankType.isNotEmpty()) parts.add(tankType)
            if (title.isNotEmpty() && title != tankType) parts.add(title)
            if (tankAcres.isNotEmpty()) parts.add(context.getString(R.string.desc_tank_acres_prefix, tankAcres))
            if (tankLocation.isNotEmpty()) parts.add(tankLocation)
        }
        ListingCategory.JOBS -> {
            if (jobType.isNotEmpty()) parts.add(jobType)
            if (salary.isNotEmpty()) parts.add(context.getString(R.string.desc_salary_prefix, salary))
            if (tankAcres.isNotEmpty()) parts.add(context.getString(R.string.desc_tank_acres_prefix, tankAcres))
            if (title.isNotEmpty() && title != jobType) parts.add(title)
        }
    }

    return parts.filter { it.isNotEmpty() }.joinToString(" || ") + "||"
}
