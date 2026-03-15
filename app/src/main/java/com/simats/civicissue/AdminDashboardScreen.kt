package com.simats.civicissue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
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
import kotlinx.coroutines.async

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNotifications: () -> Unit = {},
    onAssignedClick: () -> Unit = {},
    onInProgressClick: () -> Unit = {},
    onCompletedClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onStatusClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfessionalDashboardClick: () -> Unit = {},
    onTaskClick: (String) -> Unit = {},
    onAIChatClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var recentComplaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var unreadCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val userName = remember { TokenManager.getUser()?.full_name ?: "Admin" }

    LaunchedEffect(Unit) {
        try {
            val api = RetrofitClient.instance
            val statsDeferred = async { api.getDashboardStats() }
            val complaintsDeferred = async { api.getComplaints(mapOf("page" to "1", "limit" to "5")) }
            val unreadDeferred = async { api.getUnreadCount() }
            stats = statsDeferred.await()
            recentComplaints = complaintsDeferred.await().items
            unreadCount = unreadDeferred.await().count
        } catch (_: Exception) { }
        finally { isLoading = false }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                onItemSelected = { item ->
                    scope.launch { drawerState.close() }
                    when (item) {
                        "Dashboard" -> { /* Already on dashboard */ }
                        "Reports" -> onReportsClick()
                        "History" -> onHistoryClick()
                        "Analytics" -> onAnalyticsClick()
                        "Settings" -> onSettingsClick()
                    }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogoutClick()
                }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Administrative Dashboard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNotifications) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "Notifications",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                                if (unreadCount > 0) {
                                    Surface(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-2).dp, y = 2.dp),
                                        shape = CircleShape,
                                        color = Color.Red,
                                        border = BorderStroke(2.dp, Color.White)
                                    ) {}
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black,
                        navigationIconContentColor = Color.Black
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAIChatClick,
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    ),
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
                        onClick = { /* Already on Dashboard */ },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", fontSize = 12.sp) },
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
                        onClick = onReportsClick,
                        icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = "Complaints") },
                        label = { Text("Complaints", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onAnalyticsClick,
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                        label = { Text("Analytics", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onSettingsClick,
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Professional Management Card (The NEW option)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProfessionalDashboardClick() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Professional Management",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Detailed assignment & analytics",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Stats Section
                item {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "ASSIGNED",
                                value = (stats?.assigned ?: 0).toString().padStart(2, '0'),
                                valueColor = Color(0xFF3F51B5),
                                onClick = onAssignedClick
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "IN PROGRESS",
                                value = (stats?.inProgress ?: 0).toString().padStart(2, '0'),
                                valueColor = StatusWarning,
                                onClick = onInProgressClick
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                label = "COMPLETED",
                                value = ((stats?.completed ?: 0) + (stats?.resolved ?: 0)).toString().padStart(2, '0'),
                                valueColor = StatusSuccess,
                                onClick = onCompletedClick
                            )
                        }
                    }
                }

                // My Active Tasks Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "My Active Tasks",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            "View All",
                            fontSize = 12.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onReportsClick() }
                        )
                    }
                }

                // Tasks List
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PrimaryBlue)
                        }
                    }
                } else {
                    items(recentComplaints) { complaint ->
                        OriginalTaskItem(
                            task = OriginalCivicTask(
                                id = complaint.complaintNumber.ifEmpty { complaint.id },
                                title = complaint.title,
                                time = complaint.createdAt ?: "",
                                priority = complaint.priority.uppercase(),
                                status = complaint.statusLabel,
                                severity = complaint.severityLevel,
                                urgency = complaint.severityLevel
                            ),
                            onClick = { onTaskClick(complaint.id) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun AdminDrawerContent(
    onItemSelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
        modifier = Modifier.width(300.dp)
    ) {
        // Drawer Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryBlue, PrimaryDark)
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CivicIssue",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Administrator Portal",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        DrawerItem(
            label = "Dashboard",
            icon = Icons.Default.GridView,
            isSelected = true,
            onClick = { onItemSelected("Dashboard") }
        )
        DrawerItem(
            label = "Reports",
            icon = Icons.Default.Warning,
            isSelected = false,
            onClick = { onItemSelected("Reports") }
        )
        DrawerItem(
            label = "Issue History",
            icon = Icons.Default.History,
            isSelected = false,
            onClick = { onItemSelected("History") }
        )
        DrawerItem(
            label = "Analytics",
            icon = Icons.Default.BarChart,
            isSelected = false,
            onClick = { onItemSelected("Analytics") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

        DrawerItem(
            label = "Settings",
            icon = Icons.Default.Settings,
            isSelected = false,
            onClick = { onItemSelected("Settings") }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Logout Footer
        DrawerItem(
            label = "Logout",
            icon = Icons.AutoMirrored.Filled.Logout,
            isSelected = false,
            textColor = Color(0xFFD32F2F),
            onClick = onLogout
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { 
            Text(
                text = label, 
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp
            ) 
        },
        selected = isSelected,
        onClick = onClick,
        icon = { 
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = if (isSelected) Color(0xFF2962FF) else Color.Gray 
            ) 
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color(0xFFE3F2FD),
            selectedIconColor = Color(0xFF2962FF),
            selectedTextColor = Color(0xFF2962FF),
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = textColor.copy(alpha = 0.7f)
        )
    )
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Colored top accent border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(valueColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
fun OriginalTaskItem(
    task: OriginalCivicTask,
    onClick: () -> Unit = {}
) {
    val priorityColor = when (task.priority) {
        "HIGH" -> StatusError
        "MEDIUM" -> StatusWarning
        else -> StatusInfo
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Priority accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(priorityColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        task.id,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    OriginalPriorityTag(task.priority)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            task.time,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    OriginalStatusIndicator(task.status)
                }
            }
        }
    }
}

@Composable
fun OriginalPriorityTag(priority: String) {
    val bgColor = when (priority) {
        "HIGH" -> Color(0xFFFFEBEE)
        "MEDIUM" -> Color(0xFFFFF9C4)
        else -> Color(0xFFE3F2FD)
    }
    val textColor = when (priority) {
        "HIGH" -> Color(0xFFC62828)
        "MEDIUM" -> Color(0xFFF9A825)
        else -> Color(0xFF1976D2)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            priority,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun OriginalStatusIndicator(status: String) {
    val color = when (status) {
        "In Progress" -> StatusInfo
        "Assigned" -> StatusInfo
        "Resolved", "Completed" -> StatusSuccess
        "Pending", "Unassigned" -> StatusWarning
        else -> Color.Gray
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            status,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

data class OriginalCivicTask(
    val id: String,
    val title: String,
    val time: String,
    val priority: String,
    val status: String,
    val severity: String = "Medium",
    val urgency: String = "Normal"
)

val originalTasks = listOf(
    OriginalCivicTask("#CE-102", "Pothole on Main St.", "2 hours ago", "HIGH", "In Progress"),
    OriginalCivicTask("#CE-098", "Broken Street Light", "Yesterday", "LOW", "Resolved"),
    OriginalCivicTask("#CE-095", "Waste Collection Delay", "2 days ago", "MEDIUM", "Pending")
)

@Preview(showBackground = true)
@Composable
fun AdminDashboardScreenPreview() {
    CivicIssueTheme {
        AdminDashboardScreen()
    }
}
