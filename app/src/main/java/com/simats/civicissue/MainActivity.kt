package com.simats.civicissue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simats.civicissue.ui.theme.CivicIssueTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CivicIssueTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(onNavigate = {
                            navController.navigate("role_selection") {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }
                    composable("role_selection") {
                        RoleSelectionScreen(onRoleSelected = { role ->
                            navController.navigate("login/$role")
                        })
                    }
                    composable("login/{role}") { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "Citizen"
                        LoginScreen(
                            role = role,
                            onBack = { navController.popBackStack() },
                            onSignUp = { navController.navigate("signup") },
                            onForgotPassword = { navController.navigate("reset_password/$role") },
                            onLoginSuccess = { userRole ->
                                if (userRole == "admin") {
                                    navController.navigate("admin_dashboard") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("citizen_dashboard") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    composable("signup") {
                        SignUpScreen(
                            onBack = { navController.popBackStack() },
                            onVerifyAccount = { navController.navigate("verify_account") },
                            onLogin = { navController.popBackStack() }
                        )
                    }
                    composable("verify_account") {
                        VerifyAccountScreen(
                            onBack = { navController.popBackStack() },
                            onVerify = { 
                                navController.navigate("account_created") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("account_created") {
                        AccountCreatedScreen(
                            onProceedToLogin = {
                                navController.navigate("role_selection") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("admin_dashboard") {
                        AdminDashboardScreen(
                            onNotifications = { navController.navigate("notifications") },
                            onAssignedClick = { navController.navigate("assigned_issues") },
                            onInProgressClick = { navController.navigate("in_progress_issues") },
                            onCompletedClick = { navController.navigate("completed_issues") },
                            onSettingsClick = { navController.navigate("settings") },
                            onLogoutClick = { navController.navigate("logout") },
                            onStatusClick = { navController.navigate("status_tracking") },
                            onReportsClick = { navController.navigate("all_reports") },
                            onProfileClick = { navController.navigate("admin_profile") },
                            onHistoryClick = { navController.navigate("issue_history") },
                            onProfessionalDashboardClick = { navController.navigate("modern_admin_dashboard") },
                            onTaskClick = { complaintId ->
                                navController.navigate("complaint_detail/$complaintId")
                            },
                            onAIChatClick = { navController.navigate("admin_ai_chatbot") },
                            onAnalyticsClick = { navController.navigate("analytics") }
                        )
                    }
                    composable("admin_ai_chatbot") {
                        AdminAIChatbotScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("issue_history") {
                        IssueHistoryScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("modern_admin_dashboard") {
                        ModernAdminDashboardScreen(
                            onComplaintClick = { complaintId ->
                                navController.navigate("complaint_detail/$complaintId")
                            },
                            onBack = { navController.popBackStack() },
                            onReportsClick = { navController.navigate("all_reports") },
                            onProfileClick = { navController.navigate("admin_profile") }
                        )
                    }
                    composable("citizen_dashboard") {
                        CitizenDashboardScreen(
                            onReportIssue = { navController.navigate("report_issue") },
                            onViewMyIssues = { navController.navigate("citizen_issues") },
                            onActiveIssuesClick = { navController.navigate("active_issues") },
                            onResolvedIssuesClick = { navController.navigate("resolved_issues") },
                            onNotificationsClick = { navController.navigate("citizen_notifications") },
                            onProfileClick = { navController.navigate("citizen_profile") },
                            onLogoutClick = {
                                navController.navigate("logout")
                            },
                            onAIChatClick = { navController.navigate("ai_chatbot") }
                        )
                    }
                    composable("report_issue") {
                        ReportIssueScreen(
                            onBack = {
                                navController.navigate("citizen_dashboard") {
                                    popUpTo("citizen_dashboard") { inclusive = true }
                                }
                            },
                            onViewComplaints = {
                                navController.navigate("citizen_issues") {
                                    popUpTo("citizen_dashboard") { inclusive = false }
                                }
                            },
                            onProfileClick = { navController.navigate("citizen_profile") }
                        )
                    }
                    composable("citizen_notifications") {
                        CitizenNotificationScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("ai_chatbot") {
                        AIChatbotScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("citizen_issues") {
                        CitizenIssuesScreen(
                            onBack = { navController.popBackStack() },
                            onHomeClick = {
                                navController.navigate("citizen_dashboard") {
                                    popUpTo("citizen_dashboard") { inclusive = true }
                                }
                            },
                            onReportClick = { navController.navigate("report_issue") },
                            onProfileClick = { navController.navigate("citizen_profile") }
                        )
                    }
                    composable("active_issues") {
                        ActiveIssuesScreen(
                            onBack = { navController.popBackStack() },
                            onHomeClick = { navController.navigate("citizen_dashboard") },
                            onReportClick = { navController.navigate("report_issue") },
                            onIssuesClick = { navController.navigate("citizen_issues") },
                            onProfileClick = { navController.navigate("citizen_profile") }
                        )
                    }
                    composable("resolved_issues") {
                        ResolvedIssuesScreen(
                            onBack = { navController.popBackStack() },
                            onHomeClick = { navController.navigate("citizen_dashboard") },
                            onReportClick = { navController.navigate("report_issue") },
                            onIssuesClick = { navController.navigate("citizen_issues") },
                            onProfileClick = { navController.navigate("citizen_profile") }
                        )
                    }
                    composable("citizen_profile") {
                        CitizenProfileScreen(
                            onBack = { navController.popBackStack() },
                            onHomeClick = { navController.navigate("citizen_dashboard") },
                            onReportClick = { navController.navigate("report_issue") },
                            onIssuesClick = { navController.navigate("citizen_issues") },
                            onEditProfile = { navController.navigate("edit_profile") },
                            onChangePassword = { navController.navigate("citizen_change_password") },
                            onLogoutClick = {
                                TokenManager.clear()
                                navController.navigate("role_selection") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("edit_profile") {
                        EditProfileScreen(
                            onBack = { navController.popBackStack() },
                            onSave = { navController.popBackStack() }
                        )
                    }
                    composable("citizen_change_password") {
                        ChangePasswordScreen(
                            onBack = { navController.popBackStack() },
                            onUpdatePassword = {
                                navController.navigate("password_updated/Citizen") {
                                    popUpTo("citizen_profile") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("complaint_detail/{complaintId}") { backStackEntry ->
                        val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                        ComplaintDetailScreen(
                            complaintId = complaintId,
                            onBack = { navController.popBackStack() },
                            onAssignOfficer = { 
                                navController.navigate("assign_officer/$complaintId") 
                            },
                            onUpdateStatus = { status -> /* update status */ },
                            onResolveClick = { id -> 
                                navController.navigate("admin_resolve_issue/$id")
                            }
                        )
                    }

                    composable("assign_officer/{complaintId}") { backStackEntry ->
                        val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                        AssignOfficerScreen(
                            complaintId = complaintId,
                            onBack = { navController.popBackStack() },
                            onAssignComplete = { officer ->
                                // Logic for assignment completion
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("admin_resolve_issue/{complaintId}") { backStackEntry ->
                        val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                        AdminResolveIssueScreen(
                            complaintId = complaintId,
                            onBack = { navController.popBackStack() },
                            onResolveSuccess = { 
                                navController.navigate("admin_dashboard") {
                                    popUpTo("admin_dashboard") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("notifications") {
                        NotificationScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("assigned_issues") {
                        AssignedIssuesScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("in_progress_issues") {
                        InProgressIssuesScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("completed_issues") {
                        CompletedIssuesScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onLogout = { navController.navigate("logout") },
                            onManageCategories = { navController.navigate("manage_categories") },
                            onManageDepartments = { navController.navigate("manage_departments") },
                            onSystemLogs = { navController.navigate("system_logs") }
                        )
                    }
                    composable("manage_categories") {
                        ManageCategoriesScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("manage_departments") {
                        ManageDepartmentsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("system_logs") {
                        SystemLogsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("all_reports") {
                        AllReportsScreen(
                            onBack = { navController.popBackStack() },
                            onComplaintClick = { complaintId ->
                                navController.navigate("complaint_detail/$complaintId")
                            }
                        )
                    }
                    composable("status_tracking") {
                        StatusScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("analytics") {
                        AnalyticsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("admin_profile") {
                        AdminProfileScreen(
                            onBack = { navController.popBackStack() },
                            onChangePassword = { navController.navigate("change_password") },
                            onLogoutClick = {
                                TokenManager.clear()
                                navController.navigate("role_selection") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("change_password") {
                        ChangePasswordScreen(
                            onBack = { navController.popBackStack() },
                            onUpdatePassword = {
                                navController.navigate("password_updated/Admin") {
                                    popUpTo("admin_profile") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("logout") {
                        LogoutScreen(
                            onConfirm = {
                                TokenManager.clear()
                                navController.navigate("role_selection") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    composable("reset_password/{role}") { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "Citizen"
                        ResetPasswordScreen(
                            role = role,
                            onBack = { navController.popBackStack() },
                            onSendOTP = { navController.navigate("verify_otp/$role") }
                        )
                    }
                    composable("verify_otp/{role}") { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "Citizen"
                        VerifyOTPScreen(
                            role = role,
                            onBack = { navController.popBackStack() },
                            onContinue = { navController.navigate("create_new_password/$role") }
                        )
                    }
                    composable("create_new_password/{role}") { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "Citizen"
                        CreateNewPasswordScreen(
                            role = role,
                            onBack = { navController.popBackStack() },
                            onUpdatePassword = { navController.navigate("password_updated/$role") }
                        )
                    }
                    composable("password_updated/{role}") { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "Citizen"
                        PasswordUpdatedScreen(
                            role = role,
                            onBackToLogin = {
                                navController.navigate("login/$role") {
                                    popUpTo("login/$role") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}