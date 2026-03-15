package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
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
                            "Status Tracking",
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
        containerColor = BackgroundLight
    ) { paddingValues ->
        var stats by remember { mutableStateOf<DashboardStats?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            try {
                stats = RetrofitClient.instance.getDashboardStats()
            } catch (_: Exception) { }
            finally { isLoading = false }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            val s = stats
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Overall Status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatusMetricItem(label = "Total", value = "${s?.totalComplaints ?: 0}", color = PrimaryBlue)
                                StatusMetricItem(label = "Pending", value = "${(s?.unassigned ?: 0) + (s?.assigned ?: 0) + (s?.inProgress ?: 0)}", color = StatusWarning)
                                StatusMetricItem(label = "Closed", value = "${(s?.resolved ?: 0) + (s?.completed ?: 0)}", color = StatusSuccess)
                            }
                        }
                    }
                }

                item {
                    Text(text = "Status Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    StatusDetailCard(label = "Assigned Issues", count = "${s?.assigned ?: 0}", icon = Icons.Default.Assignment, color = Color(0xFF673AB7))
                }

                item {
                    StatusDetailCard(label = "In Progress Issues", count = "${s?.inProgress ?: 0}", icon = Icons.Default.HourglassEmpty, color = StatusInfo)
                }

                item {
                    StatusDetailCard(label = "Resolved Issues", count = "${(s?.resolved ?: 0) + (s?.completed ?: 0)}", icon = Icons.Default.CheckCircle, color = StatusSuccess)
                }

                item {
                    StatusDetailCard(label = "Unassigned Issues", count = "${s?.unassigned ?: 0}", icon = Icons.Default.Assignment, color = StatusWarning)
                }
            }
        }
    }
}

@Composable
fun StatusMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StatusDetailCard(
    label: String,
    count: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Text(text = "$count Reports", fontSize = 12.sp, color = Color.Gray)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Using as a sort of "view" indicator if needed, but rotated
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StatusScreenPreview() {
    CivicIssueTheme {
        StatusScreen()
    }
}
