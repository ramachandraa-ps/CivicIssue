package com.simats.civicissue

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
    initialLat: Double = 17.385,
    initialLng: Double = 78.4867,
    onLocationSelected: (lat: Double, lng: Double, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var selectedPosition by remember { mutableStateOf(LatLng(initialLat, initialLng)) }
    var addressText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingAddress by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedPosition, 15f)
    }

    // Reverse geocode when position changes
    LaunchedEffect(selectedPosition) {
        isLoadingAddress = true
        try {
            val geo = RetrofitClient.instance.reverseGeocode(selectedPosition.latitude, selectedPosition.longitude)
            addressText = geo.displayName
        } catch (e: Exception) {
            addressText = "Lat: ${selectedPosition.latitude}, Lng: ${selectedPosition.longitude}"
        }
        isLoadingAddress = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            selectedPosition = LatLng(loc.latitude, loc.longitude)
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(selectedPosition, 17f)
                                )
                            }
                        }
                    }
            } catch (_: SecurityException) { }
        }
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
                    TextButton(onClick = {
                        onLocationSelected(selectedPosition.latitude, selectedPosition.longitude, addressText)
                    }) {
                        Text("CONFIRM", color = PrimaryBlue, fontWeight = FontWeight.Bold)
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
                },
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = selectedPosition),
                    title = "Selected Location",
                    snippet = addressText
                )
            }

            // Search bar at top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search location...", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) }
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            isSearching = true
                            scope.launch {
                                try {
                                    val results = searchLocation(searchQuery)
                                    if (results != null) {
                                        selectedPosition = LatLng(results.first, results.second)
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(selectedPosition, 16f)
                                        )
                                    }
                                } catch (_: Exception) { }
                                isSearching = false
                            }
                        }) {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.ArrowForward, "Search", tint = PrimaryBlue)
                            }
                        }
                    }
                }
            }

            // My Location FAB
            FloatingActionButton(
                onClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        try {
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { loc ->
                                    if (loc != null) {
                                        selectedPosition = LatLng(loc.latitude, loc.longitude)
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(selectedPosition, 17f)
                                            )
                                        }
                                    }
                                }
                        } catch (_: SecurityException) { }
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
                    .padding(end = 16.dp, bottom = 100.dp),
                containerColor = Color.White,
                contentColor = PrimaryBlue
            ) {
                Icon(Icons.Default.MyLocation, "My Location")
            }

            // Address bar at bottom
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, "Location", tint = Color.Red)
                    Spacer(Modifier.width(12.dp))
                    if (isLoadingAddress) {
                        Text("Getting address...", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        Text(
                            text = addressText.ifBlank { "Tap on map to select location" },
                            fontSize = 14.sp,
                            color = Color.Black,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

/**
 * Search for a location using Nominatim (OpenStreetMap) API.
 * Returns (lat, lng) pair or null if not found.
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
