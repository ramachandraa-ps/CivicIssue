package com.simats.civicissue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.PrimaryBlue
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveIssuesScreen(
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onIssuesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Issues", fontWeight = FontWeight.Bold, color = Color.Black) },
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
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = false,
                    onClick = onHomeClick,
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onReportClick,
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
                    selected = true,
                    onClick = onIssuesClick,
                    icon = { Icon(Icons.Filled.Assignment, contentDescription = "Issues") },
                    label = { Text("Issues", fontSize = 11.sp) },
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
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                // Fetch complaints that are not resolved/completed
                val unassigned = RetrofitClient.instance.getComplaints(mapOf("status" to "UNASSIGNED"))
                val assigned = RetrofitClient.instance.getComplaints(mapOf("status" to "ASSIGNED"))
                val inProgress = RetrofitClient.instance.getComplaints(mapOf("status" to "IN_PROGRESS"))
                complaints = unassigned.items + assigned.items + inProgress.items
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load active issues"
                Log.e("ActiveIssues", "Load failed", e)
            } finally {
                isLoading = false
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            } else if (errorMessage != null) {
                item {
                    Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            } else if (complaints.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active issues found.", color = Color.DarkGray)
                    }
                }
            } else {
                items(complaints) { complaint ->
                    ComplaintHistoryCard(complaint.toCitizenComplaintDetailed())
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
