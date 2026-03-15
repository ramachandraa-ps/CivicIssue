package com.simats.civicissue

object TokenManager {
    private var token: String? = null
    private var currentUser: UserProfile? = null

    // Temporary storage for password reset flow
    var pendingEmail: String? = null
    var pendingOtp: String? = null

    fun saveToken(t: String) { token = t }
    fun getToken(): String? = token
    fun saveUser(u: UserProfile) { currentUser = u }
    fun getUser(): UserProfile? = currentUser
    fun clear() { token = null; currentUser = null; pendingEmail = null; pendingOtp = null }
    fun isLoggedIn(): Boolean = token != null
    fun getUserRole(): String = currentUser?.role ?: "citizen"
}
