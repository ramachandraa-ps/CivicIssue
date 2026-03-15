package com.simats.civicissue

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface CivicApiService {
    // Auth
    @POST("api/auth/signup") suspend fun signup(@Body body: SignupRequest): AuthResponse
    @POST("api/auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("api/auth/verify-email") suspend fun verifyEmail(@Body body: VerifyEmailRequest): Map<String, String>
    @POST("api/auth/resend-otp") suspend fun resendOtp(@Body body: ForgotPasswordRequest): Map<String, String>
    @POST("api/auth/forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Map<String, String>
    @POST("api/auth/verify-otp") suspend fun verifyOtp(@Body body: VerifyOtpRequest): Map<String, String>
    @POST("api/auth/reset-password") suspend fun resetPassword(@Body body: ResetPasswordRequest): Map<String, String>
    @PUT("api/auth/change-password") suspend fun changePassword(@Body body: ChangePasswordRequest): Map<String, String>

    // Complaints
    @GET("api/complaints") suspend fun getComplaints(@QueryMap filters: Map<String, String> = emptyMap()): PaginatedResponse<Complaint>
    @Multipart
    @POST("api/complaints") suspend fun createComplaint(@Part images: List<MultipartBody.Part>, @Part("data") data: RequestBody): Complaint
    @GET("api/complaints/stats") suspend fun getComplaintStats(): DashboardStats
    @GET("api/complaints/map-data") suspend fun getMapData(): List<MapMarker>
    @GET("api/complaints/{id}") suspend fun getComplaint(@Path("id") id: String): Complaint
    @PUT("api/complaints/{id}/status") suspend fun updateComplaintStatus(@Path("id") id: String, @Body body: StatusUpdateRequest): Complaint
    @PUT("api/complaints/{id}/assign") suspend fun assignOfficer(@Path("id") id: String, @Body body: AssignOfficerRequest): Complaint
    @PUT("api/complaints/{id}/resolve") suspend fun resolveComplaint(@Path("id") id: String, @Body body: ResolveRequest): Complaint
    @GET("api/complaints/{id}/history") suspend fun getComplaintHistory(@Path("id") id: String): List<StatusHistoryItem>
    @GET("api/complaints/{id}/similar") suspend fun getSimilarComplaints(@Path("id") id: String): List<Complaint>

    // Images & AI
    @Multipart
    @POST("api/images/upload") suspend fun uploadImage(@Part image: MultipartBody.Part): Map<String, String>
    @Multipart
    @POST("api/images/analyze") suspend fun analyzeImage(@Part image: MultipartBody.Part): ImageAnalysisResult
    @POST("api/ai/analyze-text") suspend fun analyzeText(@Body body: TextAnalysisRequest): TextAnalysisResult
    @POST("api/ai/chatbot") suspend fun chat(@Body body: ChatRequest): ChatResponse
    @GET("api/ai/chatbot/history") suspend fun getChatHistory(): List<Map<String, Any>>

    // Notifications
    @GET("api/notifications") suspend fun getNotifications(): List<CivicNotification>
    @GET("api/notifications/unread-count") suspend fun getUnreadCount(): UnreadCountResponse
    @PUT("api/notifications/{id}/read") suspend fun markNotificationRead(@Path("id") id: String): Map<String, String>
    @PUT("api/notifications/read-all") suspend fun markAllNotificationsRead(): Map<String, String>

    // Users
    @GET("api/users/me") suspend fun getProfile(): UserProfile
    @PUT("api/users/me") suspend fun updateProfile(@Body body: UpdateProfileRequest): UserProfile
    @Multipart
    @PUT("api/users/me/avatar") suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): UserProfile

    // Admin
    @GET("api/admin/categories") suspend fun getCategories(): List<CategoryItem>
    @POST("api/admin/categories") suspend fun createCategory(@Body body: CategoryCreate): CategoryItem
    @PUT("api/admin/categories/{id}") suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryCreate): CategoryItem
    @DELETE("api/admin/categories/{id}") suspend fun deleteCategory(@Path("id") id: String): Map<String, String>
    @GET("api/admin/departments") suspend fun getDepartments(): List<DepartmentItem>
    @POST("api/admin/departments") suspend fun createDepartment(@Body body: DepartmentCreate): DepartmentItem
    @PUT("api/admin/departments/{id}") suspend fun updateDepartment(@Path("id") id: String, @Body body: DepartmentCreate): DepartmentItem
    @DELETE("api/admin/departments/{id}") suspend fun deleteDepartment(@Path("id") id: String): Map<String, String>
    @GET("api/admin/officers") suspend fun getOfficers(): List<Officer>
    @POST("api/admin/officers") suspend fun createOfficer(@Body body: OfficerCreateRequest): Officer
    @GET("api/admin/system-logs") suspend fun getSystemLogs(@QueryMap params: Map<String, String> = emptyMap()): PaginatedResponse<SystemLogItem>
    @GET("api/admin/dashboard-stats") suspend fun getDashboardStats(): DashboardStats

    // Groups
    @GET("api/groups") suspend fun getIssueGroups(): List<IssueGroupItem>
    @GET("api/groups/{id}") suspend fun getIssueGroup(@Path("id") id: String): Map<String, Any>

    // Geocoding
    @GET("api/geo/reverse") suspend fun reverseGeocode(@Query("lat") lat: Double, @Query("lng") lng: Double): GeoResponse
}
