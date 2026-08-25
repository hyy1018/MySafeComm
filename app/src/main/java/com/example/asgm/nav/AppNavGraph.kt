package com.example.asgm.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.asgm.screen.MainHubScreen
import com.example.asgm.screen.MyReportsScreen
import com.example.asgm.screen.PlaceholderScreen
import com.example.asgm.screen.ReportHazardScreen

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
        composable("alert") { PlaceholderScreen("Live Alerts", navController) }
        composable("sos") { PlaceholderScreen("Emergency Hub", navController) }
        composable("guide") { PlaceholderScreen("Safety Guide", navController) }
        composable("community") { PlaceholderScreen("Community Feed", navController) }
    }
}
