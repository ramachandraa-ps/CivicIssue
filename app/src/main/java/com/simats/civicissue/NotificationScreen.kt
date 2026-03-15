package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.CivicIssueTheme
import com.simats.civicissue.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit = {}
) {
    var notifications by remember { mutableStateOf<List<CivicNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            notifications = RetrofitClient.instance.getNotifications()
        } catch (_: Exception) { }
        finally { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
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
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                RetrofitClient.instance.markAllNotificationsRead()
                                notifications = notifications.map { it.copy(isRead = true) }
                            } catch (_: Exception) { }
                        }
                    }) {
                        Text("Mark all read", color = PrimaryBlue, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No notifications", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(notifications) { notification ->
                    val iconColor = when (notification.priority) {
                        "HIGH" -> Color(0xFFD32F2F)
                        "MEDIUM" -> Color(0xFFFFA000)
                        else -> PrimaryBlue
                    }
                    val icon = when (notification.type) {
                        "complaint_created" -> Icons.Default.AddCircle
                        "status_update" -> Icons.Default.CheckCircle
                        "officer_assigned" -> Icons.Default.Engineering
                        "high_priority" -> Icons.Default.Warning
                        else -> Icons.Default.Notifications
                    }
                    AdminNotificationItem(
                        notification = AdminNotificationInfo(
                            title = notification.title,
                            description = notification.message,
                            time = notification.createdAt ?: "",
                            icon = icon,
                            iconBgColor = iconColor
                        ),
                        isRead = notification.isRead,
                        onClick = {
                            scope.launch {
                                try {
                                    RetrofitClient.instance.markNotificationRead(notification.id)
                                    notifications = notifications.map {
                                        if (it.id == notification.id) it.copy(isRead = true) else it
                                    }
                                } catch (_: Exception) { }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminNotificationItem(
    notification: AdminNotificationInfo,
    isRead: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isRead) Color.White else Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(notification.iconBgColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notification.icon,
                    contentDescription = null,
                    tint = notification.iconBgColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = notification.time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

data class AdminNotificationInfo(
    val title: String,
    val description: String,
    val time: String,
    val icon: ImageVector,
    val iconBgColor: Color
)

val adminNotifications = listOf(
    AdminNotificationInfo(
        "New Complaint Raised",
        "Vamsi Krishna has raised a new complaint regarding 'Potholes' in Jubilee Hills.",
        "Just now",
        Icons.Default.Person,
        PrimaryBlue
    ),
    AdminNotificationInfo(
        "New Report Submitted",
        "A new pothole report #CE-110 has been submitted in Main St.",
        "2m ago",
        Icons.Default.AddCircle,
        PrimaryBlue
    ),
    AdminNotificationInfo(
        "High Priority Alert",
        "AI has flagged 3 new high-priority issues for urgent inspection.",
        "1h ago",
        Icons.Default.Warning,
        Color(0xFFD32F2F)
    ),
    AdminNotificationInfo(
        "Officer Activity",
        "Officer Rajesh has started work on complaint #CE-102.",
        "3h ago",
        Icons.Default.Engineering,
        Color(0xFF757575)
    ),
    AdminNotificationInfo(
        "System Alert",
        "Scheduled server maintenance tonight at 12 AM.",
        "5h ago",
        Icons.Default.Info,
        Color(0xFFFFA000)
    ),
    AdminNotificationInfo(
        "Status Updated",
        "Complaint #CE-095 has been marked as Resolved by the crew.",
        "1d ago",
        Icons.Default.CheckCircle,
        Color(0xFF4CAF50)
    ),
    AdminNotificationInfo(
        "Location Alert",
        "A water leakage report was detected near Central Market.",
        "5h ago",
        Icons.Default.LocationOn,
        PrimaryBlue
    ),
    AdminNotificationInfo(
        "New Report Submitted",
        "Duplicate report detected for #CE-108 in Park Avenue.",
        "3h ago",
        Icons.Default.Dashboard,
        Color(0xFF757575)
    ),
    AdminNotificationInfo(
        "High Priority Alert",
        "Severe risk detected in Sector 4 infrastructure.",
        "2m ago",
        Icons.Default.PriorityHigh,
        Color(0xFFD32F2F)
    ),
    AdminNotificationInfo(
        "Officer Activity",
        "5 officers are currently active in the field.",
        "1d ago",
        Icons.Default.Group,
        Color(0xFF3F51B5)
    )
)

@Preview(showBackground = true)
@Composable
fun NotificationScreenPreview() {
    CivicIssueTheme {
        NotificationScreen()
    }
}
