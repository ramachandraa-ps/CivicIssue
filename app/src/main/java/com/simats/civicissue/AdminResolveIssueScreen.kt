package com.simats.civicissue

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.simats.civicissue.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminResolveIssueScreen(
    complaintId: String,
    onBack: () -> Unit = {},
    onResolveSuccess: () -> Unit = {}
) {
    var completionNote by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var showMapPicker by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            capturedBitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Task", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FE)
    ) { paddingValues ->
        if (isSubmitting) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(Modifier.height(16.dp))
                    Text("Submitting resolution...", fontWeight = FontWeight.Medium)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Complaint Header Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.Engineering, contentDescription = null, tint = PrimaryBlue)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Resolving $complaintId", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Assigned to you", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                // Photo Capture Section
                Column {
                    Text("Upload Completion Proof", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    val dashedStroke = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )
                    val dashedColor = PrimaryBlue.copy(alpha = 0.4f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (capturedBitmap != null) Color.White else Color(0xFFF8FAFF))
                            .then(
                                if (capturedBitmap == null) {
                                    Modifier.drawBehind {
                                        drawRoundRect(
                                            color = dashedColor,
                                            style = dashedStroke,
                                            cornerRadius = CornerRadius(12.dp.toPx())
                                        )
                                    }
                                } else {
                                    Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                                }
                            )
                            .clickable { cameraLauncher.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Remove button
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.TopEnd
                            ) {
                                IconButton(
                                    onClick = { capturedBitmap = null },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { cameraLauncher.launch() }
                                ) {
                                    Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp), tint = PrimaryBlue)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Camera", color = PrimaryBlue, fontSize = 12.sp)
                                }
                                
                                VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = Color.LightGray)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { galleryLauncher.launch("image/*") }
                                ) {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(40.dp), tint = PrimaryBlue)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Gallery", color = PrimaryBlue, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                // Location Section
                Column {
                    Text("Completed Location", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(Modifier.height(8.dp))

                    // Location text display
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color.Red) },
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    // GPS + Map buttons row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                // Get GPS location
                                isGettingLocation = true
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener { loc ->
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
                                                    isGettingLocation = false
                                                }
                                            } else {
                                                isGettingLocation = false
                                            }
                                        }.addOnFailureListener { isGettingLocation = false }
                                } else {
                                    isGettingLocation = false
                                    // Would need permission launcher here too
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGettingLocation) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, null)
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("My Location")
                        }

                        OutlinedButton(
                            onClick = { showMapPicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Pick on Map")
                        }
                    }

                    // Map preview
                    if (latitude != 0.0 && longitude != 0.0) {
                        Spacer(Modifier.height(8.dp))
                        MapViewCard(
                            latitude = latitude,
                            longitude = longitude,
                            height = 150,
                            onClick = { showMapPicker = true }
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                // Completion Note
                Column {
                    Text("Completion Remarks", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = completionNote,
                        onValueChange = { completionNote = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Describe what work was done...") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )
                }

                Spacer(Modifier.weight(1f))

                // Section divider
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)

                // Action Button
                val submitEnabled = completionNote.isNotBlank() && !isSubmitting
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            try {
                                RetrofitClient.instance.resolveComplaint(
                                    complaintId,
                                    ResolveRequest(resolution_notes = completionNote)
                                )
                                Toast.makeText(context, "Resolution submitted successfully!", Toast.LENGTH_LONG).show()
                                onResolveSuccess()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    enabled = submitEnabled
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (submitEnabled) Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                                ) else Brush.horizontalGradient(
                                    colors = listOf(Color.Gray, Color.Gray)
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Submit Resolution", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
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
