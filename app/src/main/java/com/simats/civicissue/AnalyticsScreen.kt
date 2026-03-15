package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
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
                            "Analytics",
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
            val total = (s?.totalComplaints ?: 1).coerceAtLeast(1)
            val categoryColors = listOf(PrimaryBlue, Color(0xFF673AB7), Color(0xFFFFA000), Color(0xFF4CAF50), Color.Gray, Color(0xFF2196F3), Color(0xFFD32F2F), Color(0xFF009688))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Reports",
                            value = "${s?.totalComplaints ?: 0}",
                            icon = Icons.Default.BarChart,
                            color = PrimaryBlue
                        )
                        AnalyticsMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Resolution Rate",
                            value = "${"%.0f".format((s?.resolutionRate ?: 0f) * 100)}%",
                            icon = Icons.Default.Timeline,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                item {
                    AnalyticsSectionCard(
                        title = "Category Distribution",
                        content = {
                            Column {
                                val categoryEntries = s?.byCategory?.entries?.toList() ?: emptyList()
                                if (categoryEntries.isEmpty()) {
                                    Text("No category data available", color = Color.Gray, fontSize = 14.sp)
                                } else {
                                    categoryEntries.forEachIndexed { index, (name, count) ->
                                        val percentage = count.toFloat() / total
                                        CategoryRow(
                                            name = name,
                                            percentage = percentage,
                                            color = categoryColors[index % categoryColors.size]
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    AnalyticsSectionCard(
                        title = "Severity Distribution",
                        content = {
                            Column {
                                val severityEntries = s?.bySeverity?.entries?.toList() ?: emptyList()
                                if (severityEntries.isEmpty()) {
                                    Text("No severity data available", color = Color.Gray, fontSize = 14.sp)
                                } else {
                                    severityEntries.forEachIndexed { index, (name, count) ->
                                        val percentage = count.toFloat() / total
                                        CategoryRow(
                                            name = name,
                                            percentage = percentage,
                                            color = categoryColors[index % categoryColors.size]
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    AnalyticsSectionCard(
                        title = "Performance Overview",
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PerformanceItem(label = "Recent 7 Days", value = "${s?.recent7Days ?: 0} complaints")
                                PerformanceItem(label = "Total Officers", value = "${s?.totalOfficers ?: 0}")
                                PerformanceItem(label = "Total Citizens", value = "${s?.totalCitizens ?: 0}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.05f),
                            Color.White
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                Text(text = label, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun AnalyticsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun CategoryRow(name: String, percentage: Float, color: Color) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 800),
        label = "progressAnim"
    )
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Text(text = "${(percentage * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun PerformanceItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsScreenPreview() {
    CivicIssueTheme {
        AnalyticsScreen()
    }
}
