package com.simats.civicissue

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.civicissue.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReviewScreen(
    complaintId: String,
    onBack: () -> Unit,
    onReviewComplete: () -> Unit
) {
    var complaint by remember { mutableStateOf<Complaint?>(null) }
    var statusHistory by remember { mutableStateOf<List<StatusHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isApproving by remember { mutableStateOf(false) }
    var isRequestingRework by remember { mutableStateOf(false) }
    var showReworkDialog by remember { mutableStateOf(false) }
    var reworkReason by remember { mutableStateOf("") }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }
    var isHistoryExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
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
            Log.e("AdminReview", "Failed to load: ${e.message}")
            errorMessage = e.message ?: "Failed to load complaint details"
        } finally {
            isLoading = false
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
                Text(errorMessage ?: "Complaint not found", color = Color.Gray, fontSize = 16.sp)
            }
        }
        return
    }
    val comp = complaint!!

    // Rework dialog
    if (showReworkDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRequestingRework) {
                    showReworkDialog = false
                    reworkReason = ""
                }
            },
            icon = {
                Icon(
                    Icons.Default.Replay,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Request Rework",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        "Please provide a reason for requesting rework on this complaint.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reworkReason,
                        onValueChange = { reworkReason = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Describe what needs to be redone...") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reworkReason.isBlank()) return@Button
                        isRequestingRework = true
                        scope.launch {
                            try {
                                RetrofitClient.instance.reworkComplaint(
                                    complaintId,
                                    ReworkRequest(reason = reworkReason)
                                )
                                Toast.makeText(context, "Rework requested successfully!", Toast.LENGTH_LONG).show()
                                showReworkDialog = false
                                reworkReason = ""
                                onReviewComplete()
                            } catch (e: Exception) {
                                Log.e("AdminReview", "Rework failed: ${e.message}")
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isRequestingRework = false
                            }
                        }
                    },
                    enabled = reworkReason.isNotBlank() && !isRequestingRework,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isRequestingRework) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submitting...", color = Color.White)
                    } else {
                        Text("Submit", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReworkDialog = false
                        reworkReason = ""
                    },
                    enabled = !isRequestingRework
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Completion", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
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

            // ===== 1. Complaint Summary Card =====
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

                        // Category, Priority, Severity badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InfoChip(icon = Icons.Default.Category, text = comp.category ?: "N/A")
                            PriorityTag(priority = comp.priority)
                            ReviewSeverityBadge(severity = comp.severityLevel)
                        }
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
                        Spacer(modifier = Modifier.height(12.dp))

                        // Status badge - "Completed - Awaiting Review" in amber
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFF9A825).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFF9A825),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Completed - Awaiting Review",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57F17)
                                )
                            }
                        }
                    }
                }
            }

            // ===== 2. Complaint Images Section =====
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

            // ===== 3. Officer's Resolution Section =====
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF4CAF50).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header with green accent
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Officer's Resolution",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Resolution notes
                        Text(
                            text = "Resolution Notes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comp.resolutionNotes ?: "No resolution notes provided.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )

                        // Resolution proof image
                        if (!comp.resolutionImage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Completion Proof",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')
                            val fullUrl = if (comp.resolutionImage.startsWith("http")) comp.resolutionImage else "$baseUrl${comp.resolutionImage}"
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.LightGray)
                                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { fullScreenImageUrl = fullUrl }
                            ) {
                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Resolution proof photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Tap to view overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.ZoomIn,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Tap to view",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ===== 4. Progress Updates Timeline =====
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
                            ReviewUpdateTimelineItem(
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
                                        "No progress updates were posted",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== 5. Status History (Collapsible) =====
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        // Header - clickable to expand/collapse
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHistoryExpanded = !isHistoryExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                if (statusHistory.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = PrimaryBlue.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${statusHistory.size}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                            }
                            Icon(
                                if (isHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isHistoryExpanded) "Collapse" else "Expand",
                                tint = Color.Gray
                            )
                        }

                        // Expanded content
                        if (isHistoryExpanded) {
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (statusHistory.isNotEmpty()) {
                                    statusHistory.forEachIndexed { index, historyItem ->
                                        ReviewStatusHistoryItem(
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
                                        isCompleted = true,
                                        isLast = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== 6. Review Actions Section =====
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Section header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Review Decision",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PrimaryBlue
                        )
                    }

                    // Approve Button (green, filled, full width)
                    Button(
                        onClick = {
                            isApproving = true
                            scope.launch {
                                try {
                                    RetrofitClient.instance.approveComplaint(complaintId)
                                    Toast.makeText(context, "Complaint approved and resolved!", Toast.LENGTH_LONG).show()
                                    onReviewComplete()
                                } catch (e: Exception) {
                                    Log.e("AdminReview", "Approve failed: ${e.message}")
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isApproving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isApproving && !isRequestingRework
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (!isApproving && !isRequestingRework) Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))
                                    ) else Brush.horizontalGradient(
                                        colors = listOf(Color.Gray, Color.Gray)
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isApproving) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Approving...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Approve Resolution",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Request Rework Button (red, outlined, full width)
                    OutlinedButton(
                        onClick = { showReworkDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (!isApproving && !isRequestingRework) Color(0xFFD32F2F) else Color.Gray
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (!isApproving && !isRequestingRework) Color(0xFFD32F2F) else Color.Gray
                        ),
                        enabled = !isApproving && !isRequestingRework
                    ) {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Request Rework",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
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

// ===== Severity Badge for Review Screen =====
@Composable
private fun ReviewSeverityBadge(severity: String) {
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

// ===== Officer Update Timeline Item for Review Screen =====
@Composable
private fun ReviewUpdateTimelineItem(
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

// ===== Status History Timeline Item for Review Screen =====
@Composable
private fun ReviewStatusHistoryItem(
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
