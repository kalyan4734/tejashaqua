package com.tejashaqua.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText
import java.io.ByteArrayOutputStream

enum class ListingCategory {
    FISH, PRAWNS, EQUIPMENTS, VEHICLES, FEED, BOREWELL, TANKS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    category: ListingCategory,
    isEditMode: Boolean = false,
    listingId: String? = null,
    userName: String = "User",
    initialLocation: String = "",
    onBackClick: () -> Unit,
    onPostClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    
    var isLoading by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    // State fields
    var selectedServiceType by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(initialLocation.ifBlank { "Rajahmundry, AP" }) }
    var contactNumber by remember { mutableStateOf(auth.currentUser?.phoneNumber?.removePrefix("+91") ?: "") }

    var selectedPhotoBitmaps by remember { mutableStateOf(emptyList<Bitmap>()) }

    // Specific category fields
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

    // Load existing data if in edit mode
    LaunchedEffect(listingId) {
        if (isEditMode && listingId != null) {
            isLoading = true
            db.collection("listings").document(listingId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        title = doc.getString("title") ?: ""
                        description = doc.getString("description") ?: ""
                        location = doc.getString("location") ?: ""
                        contactNumber = doc.getString("contactNumber") ?: ""
                        price = doc.get("price")?.toString() ?: ""
                        
                        // Category specific
                        when (category) {
                            ListingCategory.FISH -> {
                                fishType = doc.getString("fishType") ?: ""
                                sizeType = doc.getString("sizeType") ?: "Inches"
                                sizeValue = doc.get("sizeValue")?.toString() ?: ""
                                fishAge = doc.get("fishAge")?.toString() ?: ""
                                quantity = doc.get("quantity")?.toString() ?: ""
                                unitType = doc.getString("unitType") ?: "Lakhs"
                            }
                            ListingCategory.PRAWNS -> {
                                prawnType = doc.getString("prawnType") ?: ""
                                hatcheryName = doc.getString("hatcheryName") ?: ""
                                rateType = doc.getString("rateType") ?: "Paise"
                                rateValue = doc.get("rateValue")?.toString() ?: ""
                                quantity = doc.get("quantity")?.toString() ?: ""
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
                                ratePerTon = doc.get("ratePerTon")?.toString() ?: ""
                            }
                            ListingCategory.BOREWELL -> {
                                selectedServiceType = doc.getString("serviceType") ?: ""
                                boreWellType = doc.getString("boreWellType") ?: ""
                            }
                            ListingCategory.TANKS -> {
                                tankAcres = doc.get("tankAcres")?.toString() ?: ""
                                estPricePerAcre = doc.get("estPricePerAcre")?.toString() ?: ""
                                tankLocation = doc.getString("tankLocation") ?: ""
                            }
                        }

                        // Load images
                        val images = doc.get("images") as? List<String>
                        if (images != null) {
                            selectedPhotoBitmaps = images.mapNotNull { base64 ->
                                try {
                                    val decodedString = Base64.decode(base64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                    Toast.makeText(context, "Failed to load listing", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedPhotoBitmaps = selectedPhotoBitmaps + bitmap
        }
    }

    val screenTitle = if (isEditMode) "Edit Listing" else category.name.lowercase().replaceFirstChar { it.uppercase() }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    fun validateFields(): Boolean {
        val baseValidation = when (category) {
            ListingCategory.FISH -> fishType.isNotBlank() && title.isNotBlank() && sizeValue.isNotBlank() && fishAge.isNotBlank() && quantity.isNotBlank() && price.isNotBlank()
            ListingCategory.PRAWNS -> prawnType.isNotBlank() && hatcheryName.isNotBlank() && rateValue.isNotBlank() && quantity.isNotBlank()
            ListingCategory.EQUIPMENTS -> equipmentType.isNotBlank() && title.isNotBlank() && price.isNotBlank()
            ListingCategory.VEHICLES -> selectedServiceType.isNotBlank() && vehicleName.isNotBlank() && vehicleCapacity.isNotBlank() && title.isNotBlank()
            ListingCategory.FEED -> businessType.isNotBlank() && feedName.isNotBlank() && title.isNotBlank() && ratePerTon.isNotBlank()
            ListingCategory.BOREWELL -> selectedServiceType.isNotBlank() && boreWellType.isNotBlank() && title.isNotBlank()
            ListingCategory.TANKS -> title.isNotBlank() && tankAcres.isNotBlank() && estPricePerAcre.isNotBlank() && tankLocation.isNotBlank()
        }
        
        val commonValidation = contactNumber.isNotBlank() && contactNumber.length == 10
        
        return baseValidation && commonValidation
    }

    fun saveToFirestore() {
        showErrors = true
        
        if (!validateFields()) {
            Toast.makeText(context, "Please check all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        
        val data = mutableMapOf<String, Any>(
            "category" to category.name,
            "title" to title,
            "description" to description,
            "location" to location,
            "contactNumber" to contactNumber,
            "posterName" to userName,
            "images" to selectedPhotoBitmaps.map { bitmapToBase64(it) }
        )

        if (!isEditMode) {
            data["timestamp"] = System.currentTimeMillis()
            data["userId"] = (auth.currentUser?.uid ?: "anonymous")
        }

        when (category) {
            ListingCategory.FISH -> {
                data["fishType"] = fishType; data["sizeType"] = sizeType; data["sizeValue"] = sizeValue
                data["fishAge"] = fishAge; data["quantity"] = quantity; data["unitType"] = unitType; data["price"] = price
            }
            ListingCategory.PRAWNS -> {
                data["prawnType"] = prawnType; data["hatcheryName"] = hatcheryName
                data["rateType"] = rateType; data["rateValue"] = rateValue; data["quantity"] = quantity; data["unitType"] = unitType
            }
            ListingCategory.EQUIPMENTS -> { data["equipmentType"] = equipmentType; data["price"] = price }
            ListingCategory.VEHICLES -> { data["serviceType"] = selectedServiceType; data["vehicleName"] = vehicleName; data["vehicleCapacity"] = vehicleCapacity }
            ListingCategory.FEED -> { data["businessType"] = businessType; data["feedName"] = feedName; data["ratePerTon"] = ratePerTon }
            ListingCategory.BOREWELL -> { data["serviceType"] = selectedServiceType; data["boreWellType"] = boreWellType }
            ListingCategory.TANKS -> { data["tankAcres"] = tankAcres; data["estPricePerAcre"] = estPricePerAcre; data["tankLocation"] = tankLocation }
        }

        val task = if (isEditMode && listingId != null) {
            db.collection("listings").document(listingId).update(data)
        } else {
            db.collection("listings").add(data)
        }

        task.addOnSuccessListener {
            isLoading = false
            Toast.makeText(context, if (isEditMode) "Listing updated successfully!" else "Listing posted successfully!", Toast.LENGTH_SHORT).show()
            onPostClick()
        }.addOnFailureListener { e ->
            isLoading = false
            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AquaBlue)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    else Button(onClick = { saveToFirestore() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AquaBlue)) {
                        Text(if (isEditMode) "Save Changes" else "Post Listing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            item {
                when (category) {
                    ListingCategory.FISH -> FishFields(fishType, { fishType = it }, title, { title = it }, sizeType, { sizeType = it }, sizeValue, { sizeValue = it }, fishAge, { fishAge = it }, quantity, { quantity = it }, unitType, { unitType = it }, price, { price = it }, showErrors)
                    ListingCategory.PRAWNS -> PrawnFields(prawnType, { prawnType = it }, hatcheryName, { hatcheryName = it }, rateType, { rateType = it }, rateValue, { rateValue = it }, quantity, { quantity = it }, unitType, { unitType = it }, showErrors)
                    ListingCategory.EQUIPMENTS -> EquipmentFields(equipmentType, { equipmentType = it }, title, { title = it }, price, { price = it }, showErrors)
                    ListingCategory.VEHICLES -> VehicleFields(selectedServiceType, { selectedServiceType = it }, vehicleName, { vehicleName = it }, vehicleCapacity, { vehicleCapacity = it }, title, { title = it }, showErrors)
                    ListingCategory.FEED -> FeedFields(businessType, { businessType = it }, feedName, { feedName = it }, title, { title = it }, ratePerTon, { ratePerTon = it }, showErrors)
                    ListingCategory.BOREWELL -> BorewellFields(selectedServiceType, { selectedServiceType = it }, boreWellType, { boreWellType = it }, title, { title = it }, showErrors)
                    ListingCategory.TANKS -> TankFields(title, { title = it }, tankAcres, { tankAcres = it }, estPricePerAcre, { estPricePerAcre = it }, tankLocation, { tankLocation = it }, showErrors)
                }
            }
            item { ListingTextField(label = "Description", value = description, onValueChange = { description = it }, minLines = 3, required = false, showError = showErrors) }
            item { PhotoSection(photoBitmaps = selectedPhotoBitmaps, onAddPhoto = { cameraLauncher.launch() }, onRemovePhoto = { index -> selectedPhotoBitmaps = selectedPhotoBitmaps.toMutableList().apply { removeAt(index) } }) }
            item { LocationSection(location) }
            item { ListingTextField(label = "Contact Number", value = contactNumber, onValueChange = { if(it.length <= 10) contactNumber = it }, keyboardType = KeyboardType.Phone, required = true, showError = showErrors) }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun ListingTextField(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    minLines: Int = 1, 
    required: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    showError: Boolean = false
) {
    val hasError = showError && required && value.isBlank()
    val isPhoneError = showError && label == "Contact Number" && value.length < 10

    Column {
        Text(text = buildAnnotatedString {
            append(label)
            if (required) {
                withStyle(style = SpanStyle(color = Color.Red)) { append(" *") }
            }
        }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value, 
            onValueChange = onValueChange, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(12.dp), 
            minLines = minLines,
            isError = hasError || isPhoneError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline, 
                focusedBorderColor = AquaBlue, 
                unfocusedContainerColor = MaterialTheme.colorScheme.surface, 
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                errorBorderColor = Color.Red
            )
        )
        if (hasError) {
            Text(text = "$label is required", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        } else if (isPhoneError) {
            Text(text = "Enter valid 10-digit number", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDropdown(
    label: String, 
    value: String, 
    options: List<String>, 
    onSelectionChange: (String) -> Unit, 
    required: Boolean = true,
    showError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val hasError = showError && required && value.isBlank()

    Column {
        Text(text = buildAnnotatedString {
            append(label)
            if (required) {
                withStyle(style = SpanStyle(color = Color.Red)) { append(" *") }
            }
        }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = value, 
                onValueChange = {}, 
                readOnly = true, 
                modifier = Modifier.fillMaxWidth().menuAnchor(), 
                shape = RoundedCornerShape(12.dp), 
                isError = hasError,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, 
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline, 
                    focusedBorderColor = AquaBlue, 
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface, 
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    errorBorderColor = Color.Red
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelectionChange(option); expanded = false }) }
            }
        }
        if (hasError) {
            Text(text = "$label is required", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@Composable
fun PhotoSection(photoBitmaps: List<Bitmap>, onAddPhoto: () -> Unit, onRemovePhoto: (Int) -> Unit) {
    Column {
        Text(text = "Photos (up to 3)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            photoBitmaps.forEachIndexed { index, bitmap ->
                Box(modifier = Modifier.size(100.dp).background(Color.LightGray, RoundedCornerShape(12.dp))) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Surface(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).clickable { onRemovePhoto(index) }, color = Color.Black.copy(alpha = 0.7f), shape = CircleShape) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                    }
                }
            }
            if (photoBitmaps.size < 3) {
                Box(modifier = Modifier.size(100.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).clickable { onAddPhoto() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AquaBlue, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun LocationSection(location: String) {
    Column {
        Text(text = "Location", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), color = MaterialTheme.colorScheme.surface) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(location, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun FishFields(fishType: String, onFishTypeChange: (String) -> Unit, title: String, onTitleChange: (String) -> Unit, sizeType: String, onSizeTypeChange: (String) -> Unit, sizeValue: String, onSizeValueChange: (String) -> Unit, fishAge: String, onFishAgeChange: (String) -> Unit, quantity: String, onQuantityChange: (String) -> Unit, unitType: String, onUnitTypeChange: (String) -> Unit, price: String, onPriceChange: (String) -> Unit, showError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Fish Type", value = fishType, options = listOf("Katla", "Rohu", "Tilapia"), onSelectionChange = onFishTypeChange, required = true, showError = showError)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange, required = true, showError = showError)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) { ListingDropdown(label = "Size", value = sizeType, options = listOf("Inches", "Centimeters"), onSelectionChange = onSizeTypeChange, required = true, showError = showError) }
            Box(modifier = Modifier.weight(1f)) { ListingTextField(label = "Size Value", value = sizeValue, onValueChange = onSizeValueChange, keyboardType = KeyboardType.Number, required = true, showError = showError) }
        }
        ListingTextField(label = "Fish Age (in Months)", value = fishAge, onValueChange = onFishAgeChange, keyboardType = KeyboardType.Number, required = true, showError = showError)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) { ListingTextField(label = "Quantity", value = quantity, onValueChange = onQuantityChange, keyboardType = KeyboardType.Number, required = true, showError = showError) }
            Box(modifier = Modifier.weight(1f)) { ListingDropdown(label = "Unit", value = unitType, options = listOf("Lakhs", "Thousands", "Kgs"), onSelectionChange = onUnitTypeChange, required = true, showError = showError) }
        }
        ListingTextField(label = "Price (in ₹)", value = price, onValueChange = onPriceChange, keyboardType = KeyboardType.Number, required = true, showError = showError)
    }
}

@Composable
fun PrawnFields(prawnType: String, onPrawnTypeChange: (String) -> Unit, hatcheryName: String, onHatcheryNameChange: (String) -> Unit, rateType: String, onRateTypeChange: (String) -> Unit, rateValue: String, onRateValueChange: (String) -> Unit, quantity: String, onQuantityChange: (String) -> Unit, unitType: String, onUnitTypeChange: (String) -> Unit, showError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Type of Prawns", value = prawnType, options = listOf("Growth Line Plus", "Vannamei"), onSelectionChange = onPrawnTypeChange, required = true, showError = showError)
        ListingTextField(label = "Hatchery Name", value = hatcheryName, onValueChange = onHatcheryNameChange, required = true, showError = showError)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) { ListingDropdown(label = "Rate", value = rateType, options = listOf("Paise", "Rupees"), onSelectionChange = onRateTypeChange, required = true, showError = showError) }
            Box(modifier = Modifier.weight(1f)) { ListingTextField(label = "Rate Value", value = rateValue, onValueChange = onRateValueChange, keyboardType = KeyboardType.Number, required = true, showError = showError) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) { ListingTextField(label = "Quantity", value = quantity, onValueChange = onQuantityChange, keyboardType = KeyboardType.Number, required = true, showError = showError) }
            Box(modifier = Modifier.weight(1f)) { ListingDropdown(label = "Unit", value = unitType, options = listOf("Lakhs", "Thousands"), onSelectionChange = onUnitTypeChange, required = true, showError = showError) }
        }
    }
}

@Composable
fun EquipmentFields(equipmentType: String, onEquipmentTypeChange: (String) -> Unit, title: String, onTitleChange: (String) -> Unit, price: String, onPriceChange: (String) -> Unit, showError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Equipment Type", value = equipmentType, options = listOf("Aerators", "Pumps", "Feeders"), onSelectionChange = onEquipmentTypeChange, required = true, showError = showError)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange, required = true, showError = showError)
        ListingTextField(label = "Price (in ₹)", value = price, onValueChange = onPriceChange, keyboardType = KeyboardType.Number, required = true, showError = showError)
    }
}

@Composable
fun VehicleFields(serviceType: String, onServiceTypeChange: (String) -> Unit, vehicleName: String, onVehicleNameChange: (String) -> Unit, vehicleCapacity: String, onVehicleCapacityChange: (String) -> Unit, title: String, onTitleChange: (String) -> Unit, showError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Service Type", value = serviceType, options = listOf("Live Fish Vehicles", "Feed Transport"), onSelectionChange = onServiceTypeChange, required = true, showError = showError)
        ListingTextField(label = "Vehicle Name", value = vehicleName, onValueChange = onVehicleNameChange, required = true, showError = showError)
        ListingTextField(label = "Vehicle Capacity", value = vehicleCapacity, onValueChange = onVehicleCapacityChange, required = true, showError = showError)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange, required = true, showError = showError)
    }
}

@Composable
fun FeedFields(businessType: String, onBusinessTypeChange: (String) -> Unit, feedName: String, onFeedNameChange: (String) -> Unit, title: String, onTitleChange: (String) -> Unit, ratePerTon: String, onRatePerTonChange: (String) -> Unit, showError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Business Type", value = businessType, options = listOf("Fish Feed", "Shrimp Feed"), onSelectionChange = onBusinessTypeChange, required = true, showError = showError)
        ListingDropdown(label = "Feed Name", value = feedName, options = listOf("Godrej", "CP", "Avanti"), onSelectionChange = onFeedNameChange, required = true, showError = showError)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange, required = true, showError = showError)
        ListingTextField(label = "Rate per ton", value = ratePerTon, onValueChange = onRatePerTonChange, keyboardType = KeyboardType.Number, required = true, showError = showError)
    }
}

@Composable
fun BorewellFields(serviceType: String, onServiceTypeChange: (String) -> Unit, boreWellType: String, onBoreWellTypeChange: (String) -> Unit, title: String, onTitleChange: (String) -> Unit, showError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingDropdown(label = "Service Type", value = serviceType, options = listOf("Bore Well", "Pump Installation"), onSelectionChange = onServiceTypeChange, required = true, showError = showError)
        ListingDropdown(label = "Bore Well", value = boreWellType, options = listOf("Bore Well", "Tube Well"), onSelectionChange = onBoreWellTypeChange, required = true, showError = showError)
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange, required = true, showError = showError)
    }
}

@Composable
fun TankFields(title: String, onTitleChange: (String) -> Unit, tankAcres: String, onTankAcresChange: (String) -> Unit, estPricePerAcre: String, onEstPricePerAcreChange: (String) -> Unit, tankLocation: String, onTankLocationChange: (String) -> Unit, showError: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListingTextField(label = "Title", value = title, onValueChange = onTitleChange, required = true, showError = showError)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) { ListingTextField(label = "Tank Acres", value = tankAcres, onValueChange = onTankAcresChange, keyboardType = KeyboardType.Number, required = true, showError = showError) }
            Box(modifier = Modifier.weight(1f)) { ListingTextField(label = "Est. Price/Acre (in ₹)", value = estPricePerAcre, onValueChange = onEstPricePerAcreChange, keyboardType = KeyboardType.Number, required = true, showError = showError) }
        }
        ListingTextField(label = "Tank Location (Village/Town)", value = tankLocation, onValueChange = onTankLocationChange, required = true, showError = showError)
    }
}
