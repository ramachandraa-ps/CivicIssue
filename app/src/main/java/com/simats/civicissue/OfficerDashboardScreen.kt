package com.simats.civicissue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerDashboardScreen(
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onComplaintClick: (String) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var unreadNotifications by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf(TokenManager.getUser()?.full_name ?: "Officer") }

    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    // Compute stats from complaints
    val activeTasksCount = complaints.count { it.status in listOf("ASSIGNED", "IN_PROGRESS", "REWORK") }
    val completedCount = complaints.count { it.status in listOf("COMPLETED", "RESOLVED") }
    val reworkCount = complaints.count { it.status == "REWORK" }
    val totalAssignedCount = complaints.size

    // Sort: REWORK first, then by priority (HIGH -> MEDIUM -> LOW)
    val sortedComplaints = complaints.sortedWith(
        compareBy<Complaint> { if (it.status == "REWORK") 0 else 1 }
            .thenBy {
                when (it.priority) {
                    "HIGH" -> 0
                    "MEDIUM" -> 1
                    "LOW" -> 2
                    else -> 3
                }
            }
    )

    LaunchedEffect(Unit) {
        val api = RetrofitClient.instance
        try {
            val response = api.getComplaints()
            complaints = response.items
        } catch (_: Exception) {
            complaints = emptyList()
        }
        try {
            val unread = api.getUnreadCount()
            unreadNotifications = unread.count
        } catch (_: Exception) {}
        try {
            val profile = api.getProfile()
            userName = profile.full_name
            TokenManager.saveUser(profile)
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = greeting,
                            fontSize = 16.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge { Text("$unreadNotifications") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Officer Dashboard Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryBlue, PrimaryDark)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Officer Dashboard",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                Icons.Default.Engineering,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Manage and resolve assigned civic issues",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Stats Cards Row (horizontal scroll)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OfficerStatCard(
                        label = "Active Tasks",
                        value = activeTasksCount.toString().padStart(2, '0'),
                        icon = Icons.Default.Assignment,
                        color = StatusWarning
                    )
                    OfficerStatCard(
                        label = "Completed",
                        value = completedCount.toString().padStart(2, '0'),
                        icon = Icons.Default.Verified,
                        color = StatusSuccess
                    )
                    OfficerStatCard(
                        label = "Rework",
                        value = reworkCount.toString().padStart(2, '0'),
                        icon = Icons.Default.Replay,
                        color = StatusError
                    )
                    OfficerStatCard(
                        label = "Total Assigned",
                        value = totalAssignedCount.toString().padStart(2, '0'),
                        icon = Icons.Default.Inbox,
                        color = StatusInfo
                    )
                }
            }

            // My Tasks Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "My Tasks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        "${sortedComplaints.size} tasks",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Loading / Empty / Task List
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else if (sortedComplaints.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No tasks assigned",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "You have no pending tasks at the moment.",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(sortedComplaints) { complaint ->
                    OfficerTaskCard(
                        complaint = complaint,
                        onClick = { onComplaintClick(complaint.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

@Composable
fun OfficerStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.width(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            // Thin top colored border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(color, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            )
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun OfficerTaskCard(
    complaint: Complaint,
    onClick: () -> Unit
) {
    val borderColor = complaint.statusColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Colored left border strip
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(120.dp)
                    .background(borderColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Title row
                Text(
                    text = complaint.title.ifEmpty { complaint.category ?: "Issue" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category and Priority chips row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category chip
                    complaint.category?.let { category ->
                        Surface(
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    // Priority badge
                    Surface(
                        color = complaint.priorityColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, complaint.priorityColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = complaint.priorityLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = complaint.priorityColor
                        )
                    }

                    // Status badge
                    Surface(
                        color = complaint.statusColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, complaint.statusColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = complaint.statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = complaint.statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location
                complaint.locationText?.let { location ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = location,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Created date
                complaint.createdAt?.let { date ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(date),
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OfficerDashboardScreenPreview() {
    CivicIssueTheme {
        OfficerDashboardScreen()
    }
}
