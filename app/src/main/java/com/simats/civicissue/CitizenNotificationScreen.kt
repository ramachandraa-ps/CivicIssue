package com.simats.civicissue

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.CivicIssueTheme
import com.simats.civicissue.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenNotificationScreen(
    onBack: () -> Unit = {}
) {
    var notifications by remember { mutableStateOf<List<CivicNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedNotification by remember { mutableStateOf<CivicNotification?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            notifications = RetrofitClient.instance.getNotifications()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load notifications"
            Log.e("Notifications", "Load failed", e)
        } finally {
            isLoading = false
        }
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
                            } catch (_: Exception) {}
                        }
                    }) {
                        Text("Mark All Read", color = PrimaryBlue, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = Color.Red)
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No notifications yet.", color = Color.DarkGray)
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
                    val info = notification.toNotificationInfo()
                    CitizenNotificationItem(notification = info, isRead = notification.isRead) {
                        selectedNotification = notification
                        showSheet = true
                        // Mark as read
                        if (!notification.isRead) {
                            scope.launch {
                                try {
                                    RetrofitClient.instance.markNotificationRead(notification.id)
                                    notifications = notifications.map {
                                        if (it.id == notification.id) it.copy(isRead = true) else it
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            }
        }

        if (showSheet && selectedNotification != null) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                selectedNotification?.let { NotificationDetailContent(it.toNotificationInfo()) }
            }
        }
    }
}

@Composable
fun CitizenNotificationItem(
    notification: CitizenNotificationInfo,
    isRead: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) Color.White else PrimaryBlue.copy(alpha = 0.03f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color indicator strip
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(72.dp)
                    .background(notification.color, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(notification.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = notification.icon,
                        contentDescription = null,
                        tint = notification.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            fontSize = 15.sp,
                            fontWeight = if (isRead) FontWeight.Bold else FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Text(
                            text = notification.time,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        fontSize = 13.sp,
                        color = if (isRead) Color.Gray else Color.DarkGray,
                        lineHeight = 18.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationDetailContent(notification: CitizenNotificationInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(notification.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notification.icon,
                contentDescription = null,
                tint = notification.color,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = notification.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = notification.time,
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = notification.message,
            fontSize = 15.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        if (notification.image != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Attached Proof:",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                bitmap = notification.image.asImageBitmap(),
                contentDescription = "Resolution Proof",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* Could mark as read or navigate */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Acknowledge", fontWeight = FontWeight.Bold)
        }
    }
}

data class CitizenNotificationInfo(
    val title: String,
    val message: String,
    val time: String,
    val icon: ImageVector,
    val color: Color,
    val image: Bitmap? = null
)

fun CivicNotification.toNotificationInfo(): CitizenNotificationInfo {
    val iconAndColor = when (type.lowercase()) {
        "status_update" -> Icons.Default.Refresh to PrimaryBlue
        "complaint_created" -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        "complaint_resolved", "resolved" -> Icons.Default.TaskAlt to Color(0xFF4CAF50)
        "assignment" -> Icons.Default.PersonAdd to PrimaryBlue
        "comment" -> Icons.Default.Comment to Color(0xFFFFA000)
        "alert", "warning" -> Icons.Default.Warning to Color(0xFFD32F2F)
        else -> Icons.Default.Notifications to PrimaryBlue
    }
    return CitizenNotificationInfo(
        title = title,
        message = message,
        time = createdAt?.let { formatDate(it) } ?: "",
        icon = iconAndColor.first,
        color = iconAndColor.second
    )
}

// Keep for backward compat but no longer used for data
object CitizenNotificationStore {
    val citizenNotifications = mutableStateListOf<CitizenNotificationInfo>()
}

@Preview(showBackground = true)
@Composable
fun CitizenNotificationScreenPreview() {
    CivicIssueTheme {
        CitizenNotificationScreen()
    }
}
