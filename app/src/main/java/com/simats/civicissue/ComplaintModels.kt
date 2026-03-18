package com.simats.civicissue

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName

// ===== Complaint Models =====
data class Complaint(
    val id: String = "",
    @SerializedName("complaint_number") val complaintNumber: String = "",
    @SerializedName("citizen_id") val citizenId: String = "",
    @SerializedName("citizen_name") val citizenName: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String? = null,
    @SerializedName("ai_detected_category") val aiDetectedCategory: String? = null,
    @SerializedName("ai_text_category") val aiTextCategory: String? = null,
    @SerializedName("location_text") val locationText: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val priority: String = "MEDIUM",
    @SerializedName("severity_level") val severityLevel: String = "MEDIUM",
    val status: String = "UNASSIGNED",
    @SerializedName("assigned_officer_id") val assignedOfficerId: String? = null,
    @SerializedName("assigned_officer_name") val assignedOfficerName: String? = null,
    @SerializedName("group_id") val groupId: String? = null,
    @SerializedName("ai_confidence") val aiConfidence: Float? = null,
    @SerializedName("ai_keywords") val aiKeywords: List<String>? = null,
    @SerializedName("resolution_notes") val resolutionNotes: String? = null,
    @SerializedName("resolution_image") val resolutionImage: String? = null,
    val images: List<String> = emptyList(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val updates: List<ComplaintUpdate> = emptyList()
)

// Helper extensions for UI color mapping
val Complaint.priorityColor: Color get() = when (priority) {
    "HIGH" -> Color(0xFFD32F2F)
    "MEDIUM" -> Color(0xFFF9A825)
    "LOW" -> Color(0xFF388E3C)
    else -> Color(0xFF9E9E9E)
}

val Complaint.statusColor: Color get() = when (status) {
    "UNASSIGNED" -> Color(0xFF9E9E9E)
    "ASSIGNED" -> Color(0xFF1976D2)
    "IN_PROGRESS" -> Color(0xFFF9A825)
    "COMPLETED" -> Color(0xFF388E3C)
    "RESOLVED" -> Color(0xFF2E7D32)
    "REWORK" -> Color(0xFFD32F2F)
    else -> Color(0xFF9E9E9E)
}

val Complaint.statusLabel: String get() = when (status) {
    "UNASSIGNED" -> "Unassigned"
    "ASSIGNED" -> "Assigned"
    "IN_PROGRESS" -> "In Progress"
    "COMPLETED" -> "Completed"
    "RESOLVED" -> "Resolved"
    "REWORK" -> "Rework Required"
    else -> status
}

val Complaint.priorityLabel: String get() = when (priority) {
    "HIGH" -> "High"
    "MEDIUM" -> "Medium"
    "LOW" -> "Low"
    else -> priority
}

// Keep these enums for backward compatibility with screens that reference them
enum class Priority(val label: String, val color: Color) {
    HIGH("High", Color(0xFFD32F2F)),
    MEDIUM("Medium", Color(0xFFF9A825)),
    LOW("Low", Color(0xFF388E3C))
}

enum class ComplaintStatus(val label: String) {
    UNASSIGNED("Unassigned"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    RESOLVED("Resolved"),
    REWORK("Rework Required")
}

// ===== Paginated Response =====
data class PaginatedResponse<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20,
    val pages: Int = 1
)

// ===== Officer =====
data class Officer(
    val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    val name: String = "",
    @SerializedName("full_name") val fullName: String = "",
    val email: String = "",
    val department: String? = null,
    val designation: String? = null,
    @SerializedName("workload_count") val workloadCount: Int = 0,
    @SerializedName("is_available") val isAvailable: Boolean = true
)

// ===== Notification =====
data class CivicNotification(
    val id: String = "",
    @SerializedName("complaint_id") val complaintId: String? = null,
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val priority: String = "MEDIUM",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class UnreadCountResponse(val count: Int = 0)

// ===== Auth Models =====
data class SignupRequest(
    val full_name: String,
    val email: String,
    val password: String,
    val phone_number: String? = null,
    val country_code: String? = null,
    val role: String = "citizen"
)

data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val access_token: String,
    val token_type: String = "bearer",
    val user: UserProfile
)

data class UserProfile(
    val id: String = "",
    val full_name: String = "",
    val email: String = "",
    val phone_number: String? = null,
    val country_code: String? = null,
    val role: String = "",
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val department: String? = null,
    val designation: String? = null
)

data class VerifyEmailRequest(val email: String, val otp_code: String)
data class ForgotPasswordRequest(val email: String)
data class VerifyOtpRequest(val email: String, val otp_code: String)
data class ResetPasswordRequest(val email: String, val otp_code: String, val new_password: String)
data class ChangePasswordRequest(val current_password: String, val new_password: String)
data class UpdateProfileRequest(val full_name: String? = null, val phone_number: String? = null, val country_code: String? = null)

// ===== AI Models =====
data class ImageAnalysisResult(
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("detected_category") val detectedCategory: String = "",
    @SerializedName("severity_level") val severityLevel: String = "",
    @SerializedName("confidence_score") val confidenceScore: Float = 0f,
    val tags: List<String> = emptyList(),
    @SerializedName("description_suggestion") val descriptionSuggestion: String = ""
)

data class TextAnalysisRequest(val description: String)
data class TextAnalysisResult(
    @SerializedName("detected_category") val detectedCategory: String = "",
    val keywords: List<String> = emptyList(),
    @SerializedName("suggested_priority") val suggestedPriority: String = "",
    @SerializedName("urgency_indicator") val urgencyIndicator: String = ""
)

data class ChatRequest(val message: String, val session_id: String? = null)
data class ChatResponse(val response: String, val session_id: String)

// ===== Admin Models =====
data class CategoryItem(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val icon: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class DepartmentItem(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class DashboardStats(
    @SerializedName("total_complaints") val totalComplaints: Int = 0,
    @SerializedName("total_citizens") val totalCitizens: Int = 0,
    @SerializedName("total_officers") val totalOfficers: Int = 0,
    val unassigned: Int = 0,
    val assigned: Int = 0,
    @SerializedName("in_progress") val inProgress: Int = 0,
    val resolved: Int = 0,
    val completed: Int = 0,
    val rework: Int = 0,
    @SerializedName("by_category") val byCategory: Map<String, Int> = emptyMap(),
    @SerializedName("by_severity") val bySeverity: Map<String, Int> = emptyMap(),
    @SerializedName("recent_7_days") val recent7Days: Int = 0,
    @SerializedName("resolution_rate") val resolutionRate: Float = 0f
)

data class StatusHistoryItem(
    val id: String = "",
    @SerializedName("complaint_id") val complaintId: String = "",
    @SerializedName("old_status") val oldStatus: String? = null,
    @SerializedName("new_status") val newStatus: String = "",
    @SerializedName("changed_by") val changedBy: String = "",
    @SerializedName("changed_by_name") val changedByName: String? = null,
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class MapMarker(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val category: String? = null,
    @SerializedName("severity_level") val severityLevel: String = "",
    val status: String = "",
    val title: String = ""
)

data class GeoResponse(
    @SerializedName("display_name") val displayName: String = "",
    val address: Map<String, String> = emptyMap()
)

data class SystemLogItem(
    val id: String = "",
    val action: String = "",
    @SerializedName("entity_type") val entityType: String? = null,
    @SerializedName("entity_id") val entityId: String? = null,
    @SerializedName("performed_by") val performedBy: String = "",
    @SerializedName("performed_by_name") val performedByName: String? = null,
    val details: Map<String, Any>? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class IssueGroupItem(
    val id: String = "",
    val category: String = "",
    @SerializedName("center_lat") val centerLat: Double = 0.0,
    @SerializedName("center_lng") val centerLng: Double = 0.0,
    @SerializedName("complaint_count") val complaintCount: Int = 0,
    @SerializedName("avg_severity") val avgSeverity: String? = null,
    val status: String = ""
)

// ===== Complaint Updates (Officer Progress) =====
data class ComplaintUpdate(
    val id: String = "",
    @SerializedName("complaint_id") val complaintId: String = "",
    @SerializedName("officer_id") val officerId: String = "",
    @SerializedName("officer_name") val officerName: String? = null,
    val message: String = "",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// Request models for officer actions
data class CompleteComplaintRequest(val notes: String, val resolution_image: String? = null)
data class ReworkRequest(val reason: String)

// Officer stats
data class OfficerStats(
    @SerializedName("officer_id") val officerId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("full_name") val fullName: String = "",
    val email: String = "",
    val department: String? = null,
    val designation: String? = null,
    @SerializedName("workload_count") val workloadCount: Int = 0,
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("total_assigned") val totalAssigned: Int = 0,
    @SerializedName("total_completed") val totalCompleted: Int = 0,
    @SerializedName("total_rework") val totalRework: Int = 0,
    @SerializedName("avg_resolution_hours") val avgResolutionHours: Float? = null
)

data class OfficerUpdate(
    val department: String? = null,
    val designation: String? = null,
    val is_available: Boolean? = null
)

// Request models for admin actions
data class StatusUpdateRequest(val status: String, val notes: String? = null)
data class AssignOfficerRequest(val officer_id: String)
data class ResolveRequest(val resolution_notes: String, val resolution_image: String? = null)
data class CategoryCreate(val name: String, val description: String? = null, val icon: String? = null)
data class DepartmentCreate(val name: String, val description: String? = null)
data class OfficerCreateRequest(
    val full_name: String,
    val email: String,
    val password: String,
    val phone_number: String? = null,
    val department: String? = null,
    val designation: String? = null
)

// ===== Keep CitizenReportDto for backward compat =====
data class CitizenReportDto(
    val title: String = "",
    val date: String = "",
    val status: String = "",
    val icon: String = ""
)

// ===== Remove allComplaints mock data =====
// Mock data has been removed - all data comes from backend API
