package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemLogsScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                        Text(
                            "System Logs",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
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
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        var logItems by remember { mutableStateOf<List<SystemLogItem>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            try {
                logItems = RetrofitClient.instance.getSystemLogs().items
            } catch (_: Exception) { }
            finally { isLoading = false }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (logItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No system logs", color = Color.Gray)
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
                items(logItems) { logItem ->
                    LogItem(LogData(
                        level = "INFO",
                        message = logItem.action,
                        user = logItem.performedByName ?: logItem.performedBy,
                        time = logItem.createdAt ?: ""
                    ))
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogData) {
    val statusColor = when (log.level) {
        "INFO" -> Color(0xFF2196F3)
        "WARNING" -> Color(0xFFFFA000)
        "ERROR" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }
    
    val icon = when (log.level) {
        "INFO" -> Icons.Default.Info
        "WARNING" -> Icons.Default.Warning
        "ERROR" -> Icons.Default.Error
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.level,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = log.time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.user,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

data class LogData(
    val level: String,
    val message: String,
    val user: String,
    val time: String
)

val logs = listOf(
    LogData("INFO", "Admin login successful", "admin_01", "10:45 AM"),
    LogData("INFO", "Report #CE-8842 status updated to 'In Progress'", "officer_v", "10:30 AM"),
    LogData("WARNING", "Multiple failed login attempts", "IP: 192.168.1.45", "09:15 AM"),
    LogData("INFO", "New department 'Waste Management' created", "admin_01", "Yesterday"),
    LogData("ERROR", "Database connection timeout", "System", "Yesterday"),
    LogData("INFO", "User 'vastr' registered successfully", "Mobile App", "2 days ago")
)

@Preview(showBackground = true)
@Composable
fun SystemLogsScreenPreview() {
    CivicIssueTheme {
        SystemLogsScreen()
    }
}
