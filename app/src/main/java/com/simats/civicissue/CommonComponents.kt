package com.simats.civicissue

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusBadge(status: ComplaintStatus) {
    val color = when (status) {
        ComplaintStatus.RESOLVED, ComplaintStatus.COMPLETED -> Color(0xFF4CAF50)
        ComplaintStatus.IN_PROGRESS, ComplaintStatus.ASSIGNED -> Color(0xFF2196F3)
        ComplaintStatus.UNASSIGNED -> Color(0xFFFFA000)
        ComplaintStatus.REWORK -> Color(0xFFE53935)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun PriorityBadge(priority: Priority) {
    Surface(
        color = priority.color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = priority.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = priority.color
        )
    }
}

@Composable
fun PriorityTag(priority: String) {
    val bgColor = when (priority.uppercase()) {
        "HIGH" -> Color(0xFFFFEBEE)
        "MEDIUM" -> Color(0xFFFFF9C4)
        else -> Color(0xFFE3F2FD)
    }
    val textColor = when (priority.uppercase()) {
        "HIGH" -> Color(0xFFC62828)
        "MEDIUM" -> Color(0xFFF9A825)
        else -> Color(0xFF1976D2)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = priority,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
