package com.simats.civicissue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.*
import kotlinx.coroutines.async

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAdminDashboardScreen(
    onComplaintClick: (String) -> Unit,
    onBack: () -> Unit,
    onReportsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var recentComplaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val api = RetrofitClient.instance
            val statsDeferred = async { api.getDashboardStats() }
            val complaintsDeferred = async { api.getComplaints(mapOf("page" to "1", "limit" to "5")) }
            stats = statsDeferred.await()
            recentComplaints = complaintsDeferred.await().items
        } catch (_: Exception) { }
        finally { isLoading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Professional Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview stats
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats?.totalComplaints ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Text("Total", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats?.unassigned ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StatusWarning)
                                Text("Unassigned", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(stats?.resolved ?: 0) + (stats?.completed ?: 0)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                                Text("Resolved", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats?.totalOfficers ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF673AB7))
                                Text("Officers", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats?.totalCitizens ?: 0}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                Text("Citizens", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${"%.0f".format((stats?.resolutionRate ?: 0f) * 100)}%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                Text("Resolution", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Recent complaints
                item {
                    Text("Recent Complaints", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                items(recentComplaints) { complaint ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onComplaintClick(complaint.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(complaint.complaintNumber.ifEmpty { complaint.id }, fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                Text(complaint.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(complaint.statusLabel, fontSize = 12.sp, color = Color.Gray)
                            }
                            PriorityTag(complaint.priority)
                        }
                    }
                }

                // Quick actions
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onReportsClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                            Text("All Reports")
                        }
                        Button(onClick = onProfileClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                            Text("My Profile")
                        }
                    }
                }
            }
        }
    }
}
