package com.example.asgm.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.asgm.screen.AdminAddAdminScreen
import com.example.asgm.screen.AdminAlertFormScreen
import com.example.asgm.screen.AdminAlertsScreen
import com.example.asgm.screen.AdminHubScreen
import com.example.asgm.screen.AdminResetPasswordScreen
import com.example.asgm.screen.AdminPostsScreen
import com.example.asgm.screen.AdminReportsScreen
import com.example.asgm.screen.AdminUsersScreen
import com.example.asgm.screen.AlertScreen
import com.example.asgm.screen.CommunityFeedScreen
import com.example.asgm.screen.EmergencyHubScreen
import com.example.asgm.screen.LoginScreen
import com.example.asgm.screen.MainHubScreen
import com.example.asgm.screen.MyReportsScreen
import com.example.asgm.screen.NewPostScreen
import com.example.asgm.screen.PostDetailScreen
import com.example.asgm.screen.ProfileScreen
import com.example.asgm.screen.ReportHazardScreen
import com.example.asgm.screen.SafetyGuideDetailScreen
import com.example.asgm.screen.SafetyGuideScreen
import com.example.asgm.screen.SearchScreen
import com.example.asgm.screen.SignUpScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignUpScreen(navController) }
        composable("main_hub") {
            MainHubScreen(
                navController = navController,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable("search") { SearchScreen(navController) }
        composable("report") { ReportHazardScreen(navController) }
        composable("my_reports") { MyReportsScreen(navController) }
        composable("alert") { AlertScreen(navController) }
        composable("sos") { EmergencyHubScreen(navController) }
        composable("guide") { SafetyGuideScreen(navController) }
        composable(
            route = "guide_detail/{guideId}",
            arguments = listOf(navArgument("guideId") { type = NavType.LongType })
        ) { backStackEntry ->
            val guideId = backStackEntry.arguments?.getLong("guideId") ?: 0L
            SafetyGuideDetailScreen(guideId = guideId, navController = navController)
        }
        composable("community") { CommunityFeedScreen(navController) }
        composable("community_new") { NewPostScreen(navController) }
        composable(
            route = "community_post/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.LongType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getLong("postId") ?: 0L
            PostDetailScreen(postId = postId, navController = navController)
        }
        composable("admin_hub") { AdminHubScreen(navController) }
        composable("admin_reports") { AdminReportsScreen(navController) }
        composable("admin_alerts") { AdminAlertsScreen(navController) }
        composable(
            route = "admin_alert_form?alertId={alertId}",
            arguments = listOf(navArgument("alertId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getLong("alertId") ?: -1L
            AdminAlertFormScreen(alertId = alertId, navController = navController)
        }
        composable("admin_posts") { AdminPostsScreen(navController) }
        composable("admin_users") { AdminUsersScreen(navController) }
        composable("admin_add_admin") { AdminAddAdminScreen(navController) }
        composable("admin_reset_password") { AdminResetPasswordScreen(navController) }
        composable(
            route = "profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ProfileScreen(userId = userId, navController = navController)
        }
    }
}
