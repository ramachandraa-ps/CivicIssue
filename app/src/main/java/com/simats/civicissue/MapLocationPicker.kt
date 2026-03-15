package com.simats.civicissue

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.simats.civicissue.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPicker(
    initialLat: Double = 0.0,
    initialLng: Double = 0.0,
    onLocationSelected: (lat: Double, lng: Double, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Whether we have a valid position (user picked or GPS returned)
    var hasPosition by remember { mutableStateOf(initialLat != 0.0 && initialLng != 0.0) }
    var selectedPosition by remember {
        mutableStateOf(
            if (initialLat != 0.0 && initialLng != 0.0) LatLng(initialLat, initialLng)
            else LatLng(20.5937, 78.9629) // Center of India as default view (zoomed out)
        )
    }
    var addressText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var isFetchingGPS by remember { mutableStateOf(false) }
    var showLocationOffDialog by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }

    val initialZoom = if (hasPosition) 16f else 5f
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedPosition, initialZoom)
    }

    // Reverse geocode only when user has actually selected a position
    LaunchedEffect(selectedPosition, hasPosition) {
        if (!hasPosition) return@LaunchedEffect
        isLoadingAddress = true
        try {
            val geo = RetrofitClient.instance.reverseGeocode(
                selectedPosition.latitude, selectedPosition.longitude
            )
            addressText = geo.displayName
        } catch (e: Exception) {
            addressText = "Lat: %.4f, Lng: %.4f".format(
                selectedPosition.latitude, selectedPosition.longitude
            )
        }
        isLoadingAddress = false
    }

    // Auto-fetch GPS location on first open if no initial position
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            fetchGPSLocation(context, fusedLocationClient, scope,
                onStart = { isFetchingGPS = true; gpsError = null },
                onSuccess = { lat, lng ->
                    selectedPosition = LatLng(lat, lng)
                    hasPosition = true
                    isFetchingGPS = false
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f)
                        )
                    }
                },
                onFailure = { msg ->
                    isFetchingGPS = false
                    gpsError = msg
                }
            )
        }
    }

    // Auto-fetch on open if no initial position provided
    LaunchedEffect(Unit) {
        if (!hasPosition) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val netOn = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (gpsOn || netOn) {
                    fetchGPSLocation(context, fusedLocationClient, scope,
                        onStart = { isFetchingGPS = true },
                        onSuccess = { lat, lng ->
                            selectedPosition = LatLng(lat, lng)
                            hasPosition = true
                            isFetchingGPS = false
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f)
                                )
                            }
                        },
                        onFailure = { isFetchingGPS = false }
                    )
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // Location off dialog
    if (showLocationOffDialog) {
        AlertDialog(
            onDismissRequest = { showLocationOffDialog = false },
            title = { Text("Location Required", fontWeight = FontWeight.Bold) },
            text = { Text("Please enable location services to detect your current position.") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationOffDialog = false
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }) { Text("Open Settings", color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationOffDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (hasPosition) {
                                onLocationSelected(
                                    selectedPosition.latitude,
                                    selectedPosition.longitude,
                                    addressText
                                )
                            }
                        },
                        enabled = hasPosition && !isLoadingAddress
                    ) {
                        Text(
                            "CONFIRM",
                            color = if (hasPosition) PrimaryBlue else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedPosition = latLng
                    hasPosition = true
                    gpsError = null
                },
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                if (hasPosition) {
                    Marker(
                        state = MarkerState(position = selectedPosition),
                        title = "Selected Location",
                        snippet = addressText
                    )
                }
            }

            // GPS Loading Overlay
            AnimatedVisibility(
                visible = isFetchingGPS,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Detecting your location...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Search bar at top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search for area, street name...", color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            isSearching = true
                            scope.launch {
                                try {
                                    val results = searchLocation(searchQuery)
                                    if (results != null) {
                                        selectedPosition = LatLng(results.first, results.second)
                                        hasPosition = true
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(selectedPosition, 16f)
                                        )
                                    }
                                } catch (_: Exception) { }
                                isSearching = false
                            }
                        }) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryBlue
                                )
                            } else {
                                Icon(Icons.Default.ArrowForward, "Search", tint = PrimaryBlue)
                            }
                        }
                    }
                }
            }

            // My Location FAB with loading state
            FloatingActionButton(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        val netOn = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                        if (!gpsOn && !netOn) {
                            showLocationOffDialog = true
                            return@FloatingActionButton
                        }
                        fetchGPSLocation(context, fusedLocationClient, scope,
                            onStart = { isFetchingGPS = true; gpsError = null },
                            onSuccess = { lat, lng ->
                                selectedPosition = LatLng(lat, lng)
                                hasPosition = true
                                isFetchingGPS = false
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f)
                                    )
                                }
                            },
                            onFailure = { msg ->
                                isFetchingGPS = false
                                gpsError = msg
                            }
                        )
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 110.dp),
                containerColor = Color.White,
                contentColor = PrimaryBlue,
                shape = CircleShape
            ) {
                if (isFetchingGPS) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue
                    )
                } else {
                    Icon(Icons.Default.MyLocation, "My Location")
                }
            }

            // Bottom address card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!hasPosition && !isFetchingGPS) {
                        // Empty state — no location selected yet
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, "Location", tint = Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "No location selected",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                                Text(
                                    "Tap on map or use GPS to set location",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else if (isLoadingAddress) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryBlue
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Fetching address...", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else if (gpsError != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, "Location", tint = Color.Red)
                            Spacer(Modifier.width(12.dp))
                            Text(gpsError!!, fontSize = 14.sp, color = Color.Red)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, "Location", tint = Color.Red)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    addressText.split(",").firstOrNull()?.trim() ?: "",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (addressText.contains(",")) {
                                    Text(
                                        addressText.substringAfter(",").trim(),
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to fetch GPS location with callbacks.
 */
private fun fetchGPSLocation(
    context: Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onStart: () -> Unit,
    onSuccess: (Double, Double) -> Unit,
    onFailure: (String) -> Unit
) {
    onStart()
    try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onSuccess(loc.latitude, loc.longitude)
                } else {
                    // Fallback to last known location
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                onSuccess(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                onFailure("Could not detect location. Ensure GPS is enabled.")
                            }
                        }.addOnFailureListener {
                            onFailure("Location detection failed.")
                        }
                    } catch (_: SecurityException) {
                        onFailure("Location permission denied.")
                    }
                }
            }
            .addOnFailureListener { e ->
                onFailure("GPS error: ${e.message}")
            }
    } catch (_: SecurityException) {
        onFailure("Location permission denied.")
    }
}

/**
 * Search for a location using Nominatim (OpenStreetMap) API.
 */
suspend fun searchLocation(query: String): Pair<Double, Double>? {
    return try {
        val client = okhttp3.OkHttpClient()
        val url = "https://nominatim.openstreetmap.org/search?q=${
            java.net.URLEncoder.encode(query, "UTF-8")
        }&format=json&limit=1"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "civicissue-app")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null
        val jsonArray = org.json.JSONArray(body)
        if (jsonArray.length() > 0) {
            val obj = jsonArray.getJSONObject(0)
            Pair(obj.getDouble("lat"), obj.getDouble("lon"))
        } else null
    } catch (e: Exception) {
        null
    }
}
