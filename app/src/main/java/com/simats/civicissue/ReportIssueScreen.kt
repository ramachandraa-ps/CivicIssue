package com.simats.civicissue

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIssueScreen(
    onBack: () -> Unit = {},
    onViewComplaints: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var location by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var submittedComplaintNumber by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // AI States
    var isDetectingAI by remember { mutableStateOf(false) }
    var showSeverityPopup by remember { mutableStateOf(false) }
    var detectedSeverity by remember { mutableStateOf("") }
    var detectedCategory by remember { mutableStateOf("") }
    var aiConfidence by remember { mutableStateOf(0f) }
    var aiDescriptionSuggestion by remember { mutableStateOf("") }

    // Submission state
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    // Location state
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }

    // Map State
    var showMapPicker by remember { mutableStateOf(false) }

    // GPS Location
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var locationLoading by remember { mutableStateOf(false) }
    var showLocationOffDialog by remember { mutableStateOf(false) }

    val categories = listOf("Pothole", "Street Light", "Waste Collection", "Water Leakage", "Drainage", "Other")

    val scope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            // Check if GPS/network location is actually enabled
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                showLocationOffDialog = true
                return@rememberLauncherForActivityResult
            }

            locationLoading = true
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { loc ->
                    if (loc != null) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                        scope.launch {
                            try {
                                val geo = RetrofitClient.instance.reverseGeocode(loc.latitude, loc.longitude)
                                location = geo.displayName
                            } catch (e: Exception) {
                                location = "Lat: ${loc.latitude}, Lng: ${loc.longitude}"
                            }
                            locationLoading = false
                        }
                    } else {
                        locationLoading = false
                        // Try last known location as fallback
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                latitude = lastLoc.latitude
                                longitude = lastLoc.longitude
                                scope.launch {
                                    try {
                                        val geo = RetrofitClient.instance.reverseGeocode(lastLoc.latitude, lastLoc.longitude)
                                        location = geo.displayName
                                    } catch (e: Exception) {
                                        location = "Lat: ${lastLoc.latitude}, Lng: ${lastLoc.longitude}"
                                    }
                                }
                            } else {
                                submitError = "Could not get location. Please ensure GPS is enabled and try again."
                            }
                        }
                    }
                }.addOnFailureListener {
                    locationLoading = false
                }
            } catch (e: SecurityException) {
                locationLoading = false
            }
        }
    }

    fun runAIAnalysis(bitmap: Bitmap?) {
        if (bitmap == null) return
        isDetectingAI = true
        scope.launch {
            try {
                val api = RetrofitClient.instance
                val imagePart = bitmap.toMultipartPart("image")
                val result = api.analyzeImage(imagePart)
                detectedCategory = result.detectedCategory
                detectedSeverity = result.severityLevel
                aiConfidence = result.confidenceScore
                aiDescriptionSuggestion = result.descriptionSuggestion
                if (selectedCategory.isEmpty() && detectedCategory.isNotEmpty()) {
                    selectedCategory = detectedCategory
                }
                showSeverityPopup = true
            } catch (e: Exception) {
                Log.e("ReportIssue", "AI analysis failed: ${e.message}")
                detectedSeverity = "MEDIUM"
                showSeverityPopup = true
            } finally {
                isDetectingAI = false
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            selectedImageUri = null
            runAIAnalysis(bitmap)
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            capturedBitmap = null
            // Convert URI to bitmap for AI analysis
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    capturedBitmap = bitmap
                    runAIAnalysis(bitmap)
                }
            } catch (e: Exception) {
                Log.e("ReportIssue", "Failed to read gallery image: ${e.message}")
            }
        }
    }

    if (isSubmitted) {
        SuccessView(
            complaintNumber = submittedComplaintNumber,
            onBackToHome = onBack,
            onTrackStatus = onViewComplaints
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Report Issue",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onBack,
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* Already on Report */ },
                        icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Report") },
                        label = { Text("Report", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onViewComplaints,
                        icon = { Icon(Icons.Filled.Assignment, contentDescription = "Issues") },
                        label = { Text("Issues", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onProfileClick,
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            },
            containerColor = BackgroundLight
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Photo Upload Area
                ReportLabel("Photo Evidence", Icons.Default.CameraAlt)
                Spacer(modifier = Modifier.height(8.dp))
                PhotoUploadSection(
                    capturedBitmap = capturedBitmap,
                    selectedImageUri = selectedImageUri,
                    onCameraClick = { cameraLauncher.launch() },
                    onGalleryClick = { galleryLauncher.launch("image/*") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Location Input
                ReportLabel("Location", Icons.Default.LocationOn)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Fetching location...", color = Color.DarkGray) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "Open Map",
                            tint = PrimaryBlue,
                            modifier = Modifier.clickable { showMapPicker = true }
                        )
                    },
                    trailingIcon = {
                        if (locationLoading) {
                            CircularProgressIndicator(
                                color = PrimaryBlue,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Locate Me",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable {
                                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        if (hasFine || hasCoarse) {
                                            // Check if GPS/network location is actually enabled
                                            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                                            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                                            if (!isGpsEnabled && !isNetworkEnabled) {
                                                showLocationOffDialog = true
                                                return@clickable
                                            }

                                            locationLoading = true
                                            try {
                                                fusedLocationClient.getCurrentLocation(
                                                    Priority.PRIORITY_HIGH_ACCURACY,
                                                    CancellationTokenSource().token
                                                ).addOnSuccessListener { loc ->
                                                    if (loc != null) {
                                                        latitude = loc.latitude
                                                        longitude = loc.longitude
                                                        scope.launch {
                                                            try {
                                                                val geo = RetrofitClient.instance.reverseGeocode(loc.latitude, loc.longitude)
                                                                location = geo.displayName
                                                            } catch (e: Exception) {
                                                                location = "Lat: ${loc.latitude}, Lng: ${loc.longitude}"
                                                            }
                                                            locationLoading = false
                                                        }
                                                    } else {
                                                        locationLoading = false
                                                        // Try last known location as fallback
                                                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                                            if (lastLoc != null) {
                                                                latitude = lastLoc.latitude
                                                                longitude = lastLoc.longitude
                                                                scope.launch {
                                                                    try {
                                                                        val geo = RetrofitClient.instance.reverseGeocode(lastLoc.latitude, lastLoc.longitude)
                                                                        location = geo.displayName
                                                                    } catch (e: Exception) {
                                                                        location = "Lat: ${lastLoc.latitude}, Lng: ${lastLoc.longitude}"
                                                                    }
                                                                }
                                                            } else {
                                                                submitError = "Could not get location. Please ensure GPS is enabled and try again."
                                                            }
                                                        }
                                                    }
                                                }.addOnFailureListener {
                                                    locationLoading = false
                                                }
                                            } catch (e: SecurityException) {
                                                locationPermissionLauncher.launch(arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                ))
                                            }
                                        } else {
                                            locationPermissionLauncher.launch(arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            ))
                                        }
                                    },
                                fontSize = 12.sp
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedBorderColor = PrimaryBlue,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pick on Map button
                OutlinedButton(
                    onClick = { showMapPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick on Map")
                }

                // Map preview when location is set
                if (latitude != 0.0 && longitude != 0.0) {
                    Spacer(Modifier.height(8.dp))
                    MapViewCard(
                        latitude = latitude,
                        longitude = longitude,
                        height = 150,
                        onClick = { showMapPicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Category Dropdown
                ReportLabel("Category", Icons.Default.Category)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedCategory.ifEmpty { "Select issue category" },
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable, true),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Category, contentDescription = null, tint = Color.DarkGray)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedBorderColor = PrimaryBlue,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedPlaceholderColor = Color.DarkGray,
                            focusedPlaceholderColor = Color.DarkGray
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        categories.forEach { selectionOption ->
                            val dotColor = when (selectionOption) {
                                "Pothole" -> StatusError
                                "Street Light" -> StatusWarning
                                "Waste Collection" -> StatusSuccess
                                "Water Leakage" -> PrimaryBlue
                                "Drainage" -> Color(0xFF8B5CF6)
                                else -> Color.Gray
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(selectionOption, color = Color.Black)
                                    }
                                },
                                onClick = {
                                    selectedCategory = selectionOption
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Description Input
                ReportLabel("Description", Icons.Default.Description)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = {
                        Text(
                            "Describe the issue in detail (e.g. depth of pothole, exact pole number)...",
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedBorderColor = PrimaryBlue,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Error message
                if (submitError != null) {
                    Text(
                        text = submitError!!,
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Submit Button
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        submitError = null
                        scope.launch {
                            try {
                                val api = RetrofitClient.instance
                                // Build image parts
                                val imageParts = mutableListOf<okhttp3.MultipartBody.Part>()
                                capturedBitmap?.let { bmp ->
                                    imageParts.add(bmp.toMultipartPart("images"))
                                }
                                if (imageParts.isEmpty() && selectedImageUri != null) {
                                    imageParts.add(selectedImageUri!!.toMultipartPart(context, "images"))
                                }

                                val jsonData = Gson().toJson(mapOf(
                                    "title" to description.take(100).ifEmpty { selectedCategory },
                                    "description" to description,
                                    "category" to selectedCategory,
                                    "location_text" to location,
                                    "latitude" to latitude,
                                    "longitude" to longitude,
                                    "severity_level" to detectedSeverity.ifEmpty { "MEDIUM" },
                                    "priority" to if (detectedSeverity.equals("HIGH", true) || detectedSeverity.equals("CRITICAL", true)) "HIGH" else "MEDIUM",
                                    "ai_detected_category" to detectedCategory,
                                    "ai_confidence" to aiConfidence
                                ))
                                val dataBody = jsonData.toRequestBody("application/json".toMediaType())
                                val result = api.createComplaint(imageParts, dataBody)
                                submittedComplaintNumber = result.complaintNumber
                                isSubmitted = true
                            } catch (e: Exception) {
                                submitError = e.message ?: "Submission failed"
                                Log.e("ReportIssue", "Submit failed", e)
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Submit Complaint", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Location Services Off Dialog
    if (showLocationOffDialog) {
        AlertDialog(
            onDismissRequest = { showLocationOffDialog = false },
            title = { Text("Location Required", color = Color.Black) },
            text = { Text("Please enable location services to use the Locate Me feature.", color = Color.Black) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationOffDialog = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }) {
                    Text("Open Settings", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationOffDialog = false }) {
                    Text("Cancel", color = Color.DarkGray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // AI Processing Overlay
    if (isDetectingAI) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.size(200.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Analyzing...", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Detecting Severity", fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }
    }

    // AI Severity Popup
    if (showSeverityPopup) {
        val isHighSeverity = detectedSeverity.equals("HIGH", true) || detectedSeverity.equals("CRITICAL", true)
        AlertDialog(
            onDismissRequest = { showSeverityPopup = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            },
            title = { Text("AI Insight", color = Color.Black, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Based on the photo, AI has detected a potential issue.", color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (detectedCategory.isNotEmpty()) {
                        Text("Category: $detectedCategory", color = Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (aiConfidence > 0f) {
                        Text("Confidence: ${"%.0f".format(aiConfidence * 100)}%", color = Color.DarkGray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Surface(
                        color = if (isHighSeverity) Color(0xFFFFEBEE) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isHighSeverity) Icons.Default.PriorityHigh else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isHighSeverity) Color.Red else Color(0xFFFFA000),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Detected Severity: $detectedSeverity",
                                fontWeight = FontWeight.Bold,
                                color = if (isHighSeverity) Color.Red else Color(0xFFE65100),
                                fontSize = 14.sp
                            )
                        }
                    }
                    if (aiDescriptionSuggestion.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Suggested description:", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                        Text(aiDescriptionSuggestion, fontSize = 13.sp, color = Color.Black)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSeverityPopup = false
                    if (description.isEmpty() && aiDescriptionSuggestion.isNotEmpty()) {
                        description = aiDescriptionSuggestion
                    }
                }) {
                    Text("Confirm")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Map Picker Dialog
    if (showMapPicker) {
        MapLocationPicker(
            initialLat = latitude,
            initialLng = longitude,
            onLocationSelected = { lat, lng, address ->
                latitude = lat
                longitude = lng
                location = address
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }
}

@Composable
fun SuccessView(
    complaintNumber: String = "",
    onBackToHome: () -> Unit,
    onTrackStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Complaint Submitted Successfully!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        
        Text(
            "Your report ${if (complaintNumber.isNotEmpty()) "#$complaintNumber" else ""} has been registered. You can track its progress below.",
            fontSize = 14.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Status Timeline
        Text(
            "Status Tracking",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        StatusTimelineItem("Submitted", "Today, 10:40 AM", true, true)
        StatusTimelineItem("Technical Review", "Pending", false, true)
        StatusTimelineItem("Officer Assigned", "Pending", false, true)
        StatusTimelineItem("Resolved", "Waiting", false, false)

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Back to Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatusTimelineItem(title: String, subtitle: String, isDone: Boolean, hasNext: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDone) Color(0xFF4CAF50) else Color.LightGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            if (hasNext) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(if (isDone) Color(0xFF4CAF50) else Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = if (isDone) Color.Black else Color.DarkGray)
            Text(subtitle, fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun PhotoUploadSection(
    capturedBitmap: Bitmap?,
    selectedImageUri: Uri?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    val hasImage = capturedBitmap != null || selectedImageUri != null

    val dashedBorderModifier = if (!hasImage) {
        Modifier.drawBehind {
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(12.dp.toPx(), 8.dp.toPx()), 0f
                )
            )
            drawRoundRect(
                color = PrimaryBlue.copy(alpha = 0.4f),
                style = stroke,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
            )
        }
    } else {
        Modifier.border(
            width = 1.dp,
            color = PrimaryBlue.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .then(dashedBorderModifier)
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryBlue.copy(alpha = 0.02f))
            .clickable { showOptions = true },
        contentAlignment = Alignment.Center
    ) {
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (selectedImageUri != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Photo, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(48.dp))
                Text("Photo Selected from Gallery", color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = PrimaryBlue.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Upload Photo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    "PNG, JPG up to 10MB",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text("Add Photo", color = Color.Black) },
            text = { Text("Choose an option to add a photo of the issue.", color = Color.Black) },
            confirmButton = {
                TextButton(onClick = {
                    onCameraClick()
                    showOptions = false
                }) {
                    Text("Camera", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onGalleryClick()
                    showOptions = false
                }) {
                    Text("Choose File", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ReportLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportIssueScreenPreview() {
    CivicIssueTheme {
        ReportIssueScreen()
    }
}
