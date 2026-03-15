 package com.simats.civicissue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDashboardScreen(
    onReportIssue: () -> Unit = {},
    onViewMyIssues: () -> Unit = {},
    onActiveIssuesClick: () -> Unit = {},
    onResolvedIssuesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onAIChatClick: () -> Unit = {}
) {
    var recentComplaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var activeIssuesCount by remember { mutableStateOf("--") }
    var resolvedIssuesCount by remember { mutableStateOf("--") }
    var unreadNotifications by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var userName by remember { mutableStateOf(TokenManager.getUser()?.full_name ?: "Citizen") }

    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    LaunchedEffect(Unit) {
        val api = RetrofitClient.instance
        try {
            // Fetch recent complaints
            val response = api.getComplaints(mapOf("page" to "1", "limit" to "5"))
            recentComplaints = response.items

            // Count active vs resolved from all complaints stats
            val allResponse = api.getComplaints(mapOf("page" to "1", "limit" to "1"))
            val total = allResponse.total
            val resolvedResponse = api.getComplaints(mapOf("status" to "RESOLVED", "page" to "1", "limit" to "1"))
            val completedResponse = api.getComplaints(mapOf("status" to "COMPLETED", "page" to "1", "limit" to "1"))
            val resolvedCount = resolvedResponse.total + completedResponse.total
            val activeCount = total - resolvedCount
            activeIssuesCount = activeCount.toString().padStart(2, '0')
            resolvedIssuesCount = resolvedCount.toString().padStart(2, '0')
        } catch (e: Exception) {
            activeIssuesCount = "00"
            resolvedIssuesCount = "00"
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
                            text = "$greeting \uD83D\uDC4B",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAIChatClick,
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = "AI Assistant",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already on Home */ },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
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
                    onClick = onReportIssue,
                    icon = { Icon(Icons.Filled.AddCircle, contentDescription = "Report") },
                    label = { Text("Report", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewMyIssues,
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Main Action Card: Report New Issue
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onReportIssue() },
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
                                "Report New Issue",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Take a photo and report a civic issue",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Quick Stats Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CitizenStatCard(
                        modifier = Modifier.weight(1f).clickable { onActiveIssuesClick() },
                        label = "Active Issues",
                        value = activeIssuesCount,
                        icon = Icons.Default.ErrorOutline,
                        color = StatusWarning
                    )
                    CitizenStatCard(
                        modifier = Modifier.weight(1f).clickable { onResolvedIssuesClick() },
                        label = "Resolved",
                        value = resolvedIssuesCount,
                        icon = Icons.Default.Verified,
                        color = StatusSuccess
                    )
                }
            }

            // Recent Complaints Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "Recent Complaints",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        "View All",
                        fontSize = 14.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onViewMyIssues() }
                    )
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else if (recentComplaints.isEmpty()) {
                item {
                    Text(
                        "No complaints yet. Report your first issue!",
                        color = Color.DarkGray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(recentComplaints.take(3)) { complaint ->
                    val report = CitizenReport(
                        title = complaint.title.ifEmpty { complaint.category ?: "Issue" },
                        date = complaint.createdAt?.let { formatDate(it) } ?: "",
                        status = complaint.statusLabel,
                        icon = categoryIcon(complaint.category)
                    )
                    CitizenReportItem(report)
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

@Composable
fun CitizenStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
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
fun CitizenReportItem(report: CitizenReport) {
    val borderColor = when (report.status) {
        "Resolved", "Completed" -> StatusSuccess
        "In Progress" -> StatusInfo
        "Pending", "Unassigned" -> StatusWarning
        "Assigned" -> StatusInfo
        else -> TextSecondary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored left border strip
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(72.dp)
                    .background(borderColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = PrimaryBlue.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        report.icon,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    report.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    report.date,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
            StatusBadge(report.status)
        }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Resolved", "Completed" -> StatusSuccess
        "In Progress" -> StatusInfo
        "Pending", "Unassigned" -> StatusWarning
        "Assigned" -> StatusInfo
        else -> TextSecondary
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

data class CitizenReport(
    val title: String,
    val date: String,
    val status: String,
    val icon: ImageVector
)

fun categoryIcon(category: String?): ImageVector = when (category?.lowercase()) {
    "pothole" -> Icons.Default.ReportProblem
    "street light", "streetlight" -> Icons.Default.Lightbulb
    "waste collection", "garbage" -> Icons.Default.DeleteOutline
    "water leakage", "water" -> Icons.Default.WaterDrop
    "drainage" -> Icons.Default.WaterDrop
    else -> Icons.Default.ReportProblem
}

fun formatDate(iso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso)
        val formatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        formatter.format(date!!)
    } catch (_: Exception) { iso }
}

@Preview(showBackground = true)
@Composable
fun CitizenDashboardScreenPreview() {
    CivicIssueTheme {
        CitizenDashboardScreen()
    }
}
