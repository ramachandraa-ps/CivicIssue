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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.CivicIssueTheme
import com.simats.civicissue.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignedIssuesScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Assigned Issues",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { paddingValues ->
        var complaints by remember { mutableStateOf<List<Complaint>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            try {
                complaints = RetrofitClient.instance.getComplaints(mapOf("status" to "ASSIGNED")).items
            } catch (_: Exception) { }
            finally { isLoading = false }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (complaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No assigned issues", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(complaints) { complaint ->
                    AssignedIssueItem(
                        AssignedIssueData(
                            id = complaint.complaintNumber.ifEmpty { complaint.id },
                            title = complaint.title,
                            category = complaint.category ?: "N/A",
                            priority = complaint.priority
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AssignedIssueItem(issue: AssignedIssueData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = issue.id,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                PriorityTag(issue.priority)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = issue.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = issue.category,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Assigned",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

data class AssignedIssueData(
    val id: String,
    val title: String,
    val category: String,
    val priority: String
)

val assignedIssues = listOf(
    AssignedIssueData("#CE-8902", "Pothole Repair", "Street & Maintenance", "HIGH"),
    AssignedIssueData("#CE-8841", "Street Light Outage", "Public Safety", "MEDIUM"),
    AssignedIssueData("#CE-8790", "Waste Collection", "Sanitation", "LOW"),
    AssignedIssueData("#CE-8755", "Graffiti Removal", "Environment", "LOW"),
    AssignedIssueData("#CE-8612", "Broken Bench", "Parks & Recreation", "MEDIUM"),
    AssignedIssueData("#CE-8540", "Water Leakage", "Utilities", "HIGH")
)

@Preview(showBackground = true)
@Composable
fun AssignedIssuesScreenPreview() {
    CivicIssueTheme {
        AssignedIssuesScreen()
    }
}
