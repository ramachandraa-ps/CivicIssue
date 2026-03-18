package com.simats.civicissue

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.civicissue.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerComplaintDetailScreen(
    complaintId: String,
    onBack: () -> Unit,
    onPostUpdate: () -> Unit,
    onComplete: () -> Unit
) {
    var complaint by remember { mutableStateOf<Complaint?>(null) }
    var statusHistory by remember { mutableStateOf<List<StatusHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUpdatingStatus by remember { mutableStateOf(false) }
    var statusUpdateMessage by remember { mutableStateOf<String?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Data fetching
    LaunchedEffect(complaintId) {
        try {
            val api = RetrofitClient.instance
            val complaintDeferred = async { api.getComplaint(complaintId) }
            val historyDeferred = async { api.getComplaintHistory(complaintId) }
            complaint = complaintDeferred.await()
            statusHistory = historyDeferred.await()
        } catch (e: Exception) {
            Log.e("OfficerDetail", "Failed to load: ${e.message}")
            errorMessage = e.message ?: "Failed to load task details"
        } finally {
            isLoading = false
        }
    }

    // Refresh helper
    fun refreshData() {
        scope.launch {
            try {
                val api = RetrofitClient.instance
                complaint = api.getComplaint(complaintId)
                statusHistory = api.getComplaintHistory(complaintId)
            } catch (e: Exception) {
                Log.e("OfficerDetail", "Refresh failed: ${e.message}")
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }
    if (complaint == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage ?: "Task not found", color = Color.Gray, fontSize = 16.sp)
            }
        }
        return
    }
    val comp = complaint!!

    // Find rework reason from status history
    val reworkReason = if (comp.status == "REWORK") {
        statusHistory.lastOrNull { it.newStatus == "REWORK" }?.notes
    } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
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

            // ===== 1. Complaint Info Card =====
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title
                        Text(
                            text = comp.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Complaint number
                        Text(
                            text = comp.complaintNumber.ifEmpty { comp.id },
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Category chip, Priority & Severity badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfoChip(icon = Icons.Default.Category, text = comp.category ?: "N/A")
                            PriorityTag(priority = comp.priority)
                            SeverityBadge(severity = comp.severityLevel)
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // Description
                        Text(
                            "Description",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comp.description ?: "No description provided",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Location
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = comp.locationText ?: "Location not available",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Created date
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Created: ${comp.createdAt ?: "Unknown"}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // ===== Complaint Images =====
            item {
                Column {
                    Text(
                        "Complaint Images",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryBlue
                    )
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
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.6f)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "Citizen Upload",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.CenterStart)
                                        )
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

            // ===== 2. Status Section =====
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Current Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusBadge(status = comp.statusLabel)
                    }
                }
            }

            // ===== Rework Card (if status is REWORK) =====
            if (comp.status == "REWORK") {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.dp, Color(0xFFEF5350))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Rework Requested",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Reason:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reworkReason ?: "No reason provided by admin.",
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }
            }

            // ===== 3. Action Buttons (based on status) =====
            item {
                // Status update feedback message
                if (statusUpdateMessage != null) {
                    Surface(
                        color = if (statusUpdateMessage!!.startsWith("Error")) Color(0xFFFFCDD2) else Color(0xFFC8E6C9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusUpdateMessage!!,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            fontSize = 14.sp,
                            color = if (statusUpdateMessage!!.startsWith("Error")) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (comp.status) {
                    // ASSIGNED -> "Start Work" button
                    "ASSIGNED" -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    isUpdatingStatus = true
                                    statusUpdateMessage = null
                                    try {
                                        RetrofitClient.instance.updateComplaintStatus(
                                            comp.id,
                                            StatusUpdateRequest(
                                                status = "IN_PROGRESS",
                                                notes = "Officer started working on the task"
                                            )
                                        )
                                        statusUpdateMessage = "Status updated to In Progress"
                                        refreshData()
                                    } catch (e: Exception) {
                                        Log.e("OfficerDetail", "Start work failed: ${e.message}")
                                        statusUpdateMessage = "Error: ${e.message ?: "Failed to start work"}"
                                    } finally {
                                        isUpdatingStatus = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            enabled = !isUpdatingStatus
                        ) {
                            if (isUpdatingStatus) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Starting...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Work", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }

                    // IN_PROGRESS -> "Post Update" + "Mark Complete"
                    "IN_PROGRESS" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPostUpdate,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, PrimaryBlue),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Post Update", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onComplete,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark Complete", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // REWORK -> "Resume Work" button
                    "REWORK" -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    isUpdatingStatus = true
                                    statusUpdateMessage = null
                                    try {
                                        RetrofitClient.instance.updateComplaintStatus(
                                            comp.id,
                                            StatusUpdateRequest(
                                                status = "IN_PROGRESS",
                                                notes = "Officer resumed work after rework request"
                                            )
                                        )
                                        statusUpdateMessage = "Status updated to In Progress"
                                        refreshData()
                                    } catch (e: Exception) {
                                        Log.e("OfficerDetail", "Resume work failed: ${e.message}")
                                        statusUpdateMessage = "Error: ${e.message ?: "Failed to resume work"}"
                                    } finally {
                                        isUpdatingStatus = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            enabled = !isUpdatingStatus
                        ) {
                            if (isUpdatingStatus) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resuming...", fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resume Work", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }

                    // COMPLETED -> "Awaiting Review" info card
                    "COMPLETED" -> {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Awaiting Admin Review",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // RESOLVED -> "Resolved" with green checkmark
                    "RESOLVED" -> {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Resolved",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }

            // ===== 4. Progress Updates Section =====
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Update,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Progress Updates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (comp.updates.isNotEmpty()) {
                        comp.updates.forEachIndexed { index, update ->
                            OfficerUpdateTimelineItem(
                                update = update,
                                isLast = index == comp.updates.lastIndex,
                                onImageClick = { url -> fullScreenImageUrl = url }
                            )
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SpeakerNotes,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No updates yet",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== 5. Status History Section =====
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Status History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (statusHistory.isNotEmpty()) {
                        statusHistory.forEachIndexed { index, historyItem ->
                            StatusHistoryTimelineItem(
                                historyItem = historyItem,
                                isLast = index == statusHistory.lastIndex
                            )
                        }
                    } else {
                        TimelineItem(
                            title = "Complaint Raised",
                            time = comp.createdAt ?: "",
                            isCompleted = true,
                            isLast = false
                        )
                        TimelineItem(
                            title = comp.statusLabel,
                            time = comp.updatedAt ?: "Current",
                            isCompleted = comp.status != "UNASSIGNED",
                            isLast = true
                        )
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
    }
}

// ===== Severity Badge Composable =====
@Composable
private fun SeverityBadge(severity: String) {
    val bgColor = when (severity.uppercase()) {
        "HIGH", "CRITICAL" -> Color(0xFFFFEBEE)
        "MEDIUM" -> Color(0xFFFFF3E0)
        "LOW" -> Color(0xFFE8F5E9)
        else -> Color(0xFFE3F2FD)
    }
    val textColor = when (severity.uppercase()) {
        "HIGH", "CRITICAL" -> Color(0xFFC62828)
        "MEDIUM" -> Color(0xFFE65100)
        "LOW" -> Color(0xFF2E7D32)
        else -> Color(0xFF1565C0)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = severity,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ===== Officer Update Timeline Item =====
@Composable
private fun OfficerUpdateTimelineItem(
    update: ComplaintUpdate,
    isLast: Boolean,
    onImageClick: (String) -> Unit
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // Timeline dot and line
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue)
                    .border(2.dp, Color.White, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(PrimaryBlue.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Update content card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Officer name and timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = update.officerName ?: "Officer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                    Text(
                        text = update.createdAt ?: "",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Message
                Text(
                    text = update.message,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.DarkGray
                )

                // Optional image thumbnail
                if (!update.imageUrl.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')
                    val fullUrl = if (update.imageUrl.startsWith("http")) update.imageUrl else "$baseUrl${update.imageUrl}"
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onImageClick(fullUrl) }
                    ) {
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = "Update image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

// ===== Status History Timeline Item =====
@Composable
private fun StatusHistoryTimelineItem(
    historyItem: StatusHistoryItem,
    isLast: Boolean
) {
    val statusColor = when (historyItem.newStatus) {
        "RESOLVED" -> Color(0xFF2E7D32)
        "COMPLETED" -> Color(0xFF4CAF50)
        "IN_PROGRESS" -> Color(0xFFF9A825)
        "ASSIGNED" -> Color(0xFF1976D2)
        "REWORK" -> Color(0xFFD32F2F)
        "UNASSIGNED" -> Color(0xFF9E9E9E)
        else -> Color.Gray
    }

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // Timeline dot and line
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                    .border(2.dp, Color.White, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(statusColor.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))

        // History content
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            // Status transition
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = historyItem.oldStatus ?: "Created",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(14.dp)
                )
                Text(
                    text = historyItem.newStatus,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))

            // Changed by
            Text(
                text = "by ${historyItem.changedByName ?: "System"}",
                fontSize = 11.sp,
                color = Color.Gray
            )

            // Notes if present
            if (!historyItem.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = historyItem.notes,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Normal
                )
            }

            // Timestamp
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = historyItem.createdAt ?: "",
                fontSize = 10.sp,
                color = TextTertiary
            )
        }
    }
}
