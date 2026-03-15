package com.simats.civicissue

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.civicissue.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignOfficerScreen(
    complaintId: String,
    onBack: () -> Unit,
    onAssignComplete: (Officer) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var officers by remember { mutableStateOf<List<Officer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isAssigning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            officers = RetrofitClient.instance.getOfficers()
        } catch (_: Exception) { }
        finally { isLoading = false }
    }

    val filteredOfficers = officers.filter {
        val displayName = it.fullName.ifEmpty { it.name }
        displayName.contains(searchQuery, ignoreCase = true) || (it.department ?: "").contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Officer", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF8F9FE)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlue)
                    .padding(16.dp)
            ) {
                Column {
                    Text("Assigning for Ticket:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Text(complaintId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search Officer or Department...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                "Available Officers",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading || isAssigning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOfficers) { officer ->
                        OfficerItem(officer = officer, onClick = {
                            isAssigning = true
                            scope.launch {
                                try {
                                    RetrofitClient.instance.assignOfficer(
                                        complaintId,
                                        AssignOfficerRequest(officer_id = officer.userId.ifEmpty { officer.id })
                                    )
                                    Toast.makeText(context, "Officer assigned successfully", Toast.LENGTH_SHORT).show()
                                    onAssignComplete(officer)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to assign: ${e.message}", Toast.LENGTH_SHORT).show()
                                    isAssigning = false
                                }
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun OfficerItem(officer: Officer, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (officer.fullName.ifEmpty { officer.name }).take(1),
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(officer.fullName.ifEmpty { officer.name }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(officer.department ?: "No department", color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Workload", fontSize = 10.sp, color = Color.Gray)
                Text("${officer.workloadCount} Active", fontWeight = FontWeight.Bold, color = if (officer.workloadCount > 3) Color.Red else Color(0xFF4CAF50))
            }
        }
    }
}
