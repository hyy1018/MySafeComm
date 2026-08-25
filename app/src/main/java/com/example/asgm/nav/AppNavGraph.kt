package com.example.asgm.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.asgm.screen.AlertScreen
import com.example.asgm.screen.CommunityFeedScreen
import com.example.asgm.screen.EmergencyHubScreen
import com.example.asgm.screen.MainHubScreen
import com.example.asgm.screen.MyReportsScreen
import com.example.asgm.screen.NewPostScreen
import com.example.asgm.screen.PostDetailScreen
import com.example.asgm.screen.ReportHazardScreen
import com.example.asgm.screen.SafetyGuideDetailScreen
import com.example.asgm.screen.SafetyGuideScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "main_hub") {
        composable("main_hub") {
            MainHubScreen(
                navController = navController,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
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
    }
}
