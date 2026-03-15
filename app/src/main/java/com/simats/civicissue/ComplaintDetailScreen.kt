package com.simats.civicissue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import com.simats.civicissue.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import android.util.Log
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    complaintId: String,
    onBack: () -> Unit,
    onAssignOfficer: () -> Unit,
    onUpdateStatus: (ComplaintStatus) -> Unit,
    onResolveClick: (String) -> Unit = {}
) {
    var complaint by remember { mutableStateOf<Complaint?>(null) }
    var statusHistory by remember { mutableStateOf<List<StatusHistoryItem>>(emptyList()) }
    var similarIssues by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUpdatingStatus by remember { mutableStateOf(false) }
    var statusUpdateMessage by remember { mutableStateOf<String?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showFullMap by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(complaintId) {
        try {
            val api = RetrofitClient.instance
            val complaintDeferred = async { api.getComplaint(complaintId) }
            val historyDeferred = async { api.getComplaintHistory(complaintId) }
            val similarDeferred = async {
                try { api.getSimilarComplaints(complaintId) } catch (_: Exception) { emptyList() }
            }
            complaint = complaintDeferred.await()
            statusHistory = historyDeferred.await()
            val similarList = similarDeferred.await()
            similarIssues = similarList.take(3).map { s ->
                val dist = calculateDistance(
                    complaint?.latitude ?: 0.0, complaint?.longitude ?: 0.0,
                    s.latitude, s.longitude
                )
                val distStr = if (dist < 1.0) "${(dist * 1000).toInt()}m" else "%.1fkm".format(dist)
                Pair(s.complaintNumber.ifEmpty { s.id }, "${s.category ?: "Issue"} nearby - $distStr")
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load complaint"
        } finally { isLoading = false }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }
    if (complaint == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(errorMessage ?: "Complaint not found", color = Color.Gray)
        }
        return
    }
    val comp = complaint!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Title & Status Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = comp.complaintNumber.ifEmpty { comp.id },
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        StatusBadge(status = comp.statusLabel)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = comp.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            // Quick Info Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip(icon = Icons.Default.Category, text = comp.category ?: "N/A")
                    InfoChip(icon = Icons.Default.Person, text = comp.citizenName)
                    PriorityTag(priority = comp.priority)
                }
            }

            // Description Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = comp.description ?: "No description provided",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // AI Insights (Severity & Priority)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Analysis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF7B1FA2))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text("SEVERITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(comp.severityLevel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                                Text("PRIORITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(comp.priorityLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                            }
                        }
                    }
                }
            }

            // Image Gallery - real images
            item {
                Column {
                    Text("Evidence & Nearby Context", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (comp.images.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(comp.images) { imageUrl ->
                                val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')
                                val fullUrl = if (imageUrl.startsWith("http")) imageUrl else "$baseUrl$imageUrl"
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.LightGray)
                                        .border(2.dp, PrimaryBlue, RoundedCornerShape(12.dp))
                                        .clickable { fullScreenImageUrl = fullUrl }
                                ) {
                                    AsyncImage(
                                        model = fullUrl,
                                        contentDescription = "Complaint image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(PrimaryBlue)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Citizen Upload", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No images attached", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Clustered Issues List
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Similar Nearby Issues (Clustered)", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 14.sp, 
                            color = PrimaryBlue
                        )
                        Text(
                            "Issues grouped by visual similarity and proximity", 
                            fontSize = 11.sp, 
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        similarIssues.forEach { pair ->
                            val id = pair.first
                            val desc = pair.second
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(id, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(desc, fontSize = 12.sp, color = Color.DarkGray)
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            // Location Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp) )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Location Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = comp.locationText ?: "Location not available", fontSize = 14.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        // Map Preview
                        MapViewCard(
                            latitude = comp.latitude,
                            longitude = comp.longitude,
                            height = 200,
                            onClick = { showFullMap = true }
                        )
                    }
                }
            }

            // Timeline Section from real history
            item {
                Column {
                    Text("Complaint Timeline", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (statusHistory.isNotEmpty()) {
                        statusHistory.forEachIndexed { index, historyItem ->
                            TimelineItem(
                                title = "${historyItem.oldStatus ?: "Created"} -> ${historyItem.newStatus}",
                                time = historyItem.createdAt ?: "",
                                isCompleted = true,
                                isLast = index == statusHistory.lastIndex
                            )
                        }
                    } else {
                        TimelineItem("Complaint Raised", comp.createdAt ?: "", isCompleted = true, isLast = false)
                        TimelineItem(comp.statusLabel, comp.updatedAt ?: "Current", isCompleted = comp.status != "UNASSIGNED", isLast = true)
                    }
                }
            }

            // Status update message
            if (statusUpdateMessage != null) {
                item {
                    Surface(
                        color = if (statusUpdateMessage!!.startsWith("Error")) Color(0xFFFFCDD2) else Color(0xFFC8E6C9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusUpdateMessage!!,
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            fontSize = 14.sp,
                            color = if (statusUpdateMessage!!.startsWith("Error")) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (comp.status == "ASSIGNED") {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isUpdatingStatus = true
                                    statusUpdateMessage = null
                                    try {
                                        RetrofitClient.instance.updateComplaintStatus(
                                            comp.id,
                                            StatusUpdateRequest(status = "IN_PROGRESS", notes = "Work started by admin")
                                        )
                                        complaint = RetrofitClient.instance.getComplaint(complaintId)
                                        statusHistory = RetrofitClient.instance.getComplaintHistory(complaintId)
                                        statusUpdateMessage = "Status updated to In Progress"
                                    } catch (e: Exception) {
                                        Log.e("ComplaintDetail", "Failed to update status: ${e.message}")
                                        statusUpdateMessage = "Error: ${e.message ?: "Failed to update status"}"
                                    } finally {
                                        isUpdatingStatus = false
                                    }
                                }
                            },
                            enabled = !isUpdatingStatus,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isUpdatingStatus) Color.Gray else PrimaryBlue),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isUpdatingStatus) Color.Gray else PrimaryBlue)
                        ) {
                            if (isUpdatingStatus) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PrimaryBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Updating...", fontWeight = FontWeight.Bold)
                            } else {
                                Text("Start Work", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Button(
                        onClick = { onResolveClick(comp.id) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Complete & Photo", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Fullscreen image viewer
        if (fullScreenImageUrl != null) {
            FullScreenImageViewer(
                imageUrl = fullScreenImageUrl!!,
                onDismiss = { fullScreenImageUrl = null }
            )
        }

        // Fullscreen map viewer
        if (showFullMap && complaint != null) {
            MapLocationPicker(
                initialLat = complaint!!.latitude,
                initialLng = complaint!!.longitude,
                onLocationSelected = { _, _, _ -> showFullMap = false },
                onDismiss = { showFullMap = false }
            )
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, text: String) {
    Surface(
        color = Color(0xFFE8EAF6),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue)
        }
    }
}

@Composable
fun TimelineItem(title: String, time: String, isCompleted: Boolean, isLast: Boolean) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) PrimaryBlue else Color.LightGray)
                    .border(2.dp, Color.White, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(if (isCompleted) PrimaryBlue else Color.LightGray)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isCompleted) Color.Black else Color.Gray)
            Text(time, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// Helper functions for distance calculation
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2.0).pow(2.0) + 
            cos(Math.toRadians(lat1)) * 
            cos(Math.toRadians(lat2)) * 
            sin(dLon / 2.0).pow(2.0)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return r * c
}
