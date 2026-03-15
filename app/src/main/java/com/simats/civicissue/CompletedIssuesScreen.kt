package com.simats.civicissue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.async

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedIssuesScreen(
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Completed Issues",
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
                val api = RetrofitClient.instance
                val completedDeferred = async { api.getComplaints(mapOf("status" to "COMPLETED")).items }
                val resolvedDeferred = async { api.getComplaints(mapOf("status" to "RESOLVED")).items }
                complaints = completedDeferred.await() + resolvedDeferred.await()
            } catch (_: Exception) { }
            finally { isLoading = false }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (complaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No completed issues", color = Color.Gray)
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
                items(complaints) { complaint ->
                    CompletedIssueItem(
                        CompletedIssueData(
                            id = complaint.complaintNumber.ifEmpty { complaint.id },
                            title = complaint.title,
                            priority = complaint.priority,
                            time = complaint.updatedAt ?: complaint.createdAt ?: ""
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun CompletedIssueItem(issue: CompletedIssueData) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resolved",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
                
                Text(
                    text = issue.time,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

data class CompletedIssueData(
    val id: String,
    val title: String,
    val priority: String,
    val time: String
)

val completedIssues = listOf(
    CompletedIssueData("#CE-8802", "Pothole on Main St.", "HIGH", "2h ago"),
    CompletedIssueData("#CE-8795", "Street Light Out", "MEDIUM", "5h ago"),
    CompletedIssueData("#CE-8750", "Illegal Waste Dumping", "LOW", "1d ago"),
    CompletedIssueData("#CE-8742", "Broken Sidewalk", "MEDIUM", "2d ago"),
    CompletedIssueData("#CE-8738", "Graffiti Removal", "LOW", "3d ago"),
    CompletedIssueData("#CE-8720", "Water Leakage", "HIGH", "4d ago")
)

@Preview(showBackground = true)
@Composable
fun CompletedIssuesScreenPreview() {
    CivicIssueTheme {
        CompletedIssuesScreen()
    }
}
