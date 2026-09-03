// Shared bottom nav bar for Main Hub and its sibling screens.
package com.example.asgm.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import kotlinx.coroutines.flow.emptyFlow

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("main_hub", "Home", Icons.Filled.Home),
    BottomNavItem("report", "Report", Icons.Filled.ReportProblem),
    BottomNavItem("alert", "Alert", Icons.Filled.Notifications),
    BottomNavItem("community", "Comm", Icons.Filled.Forum),
    BottomNavItem("sos", "SOS", Icons.Filled.Emergency)
)

@Composable
fun AppBottomBar(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val context = LocalContext.current
    val userId = UserSession.currentUserId
    // Gives residents a reason to check Alert: a red badge when there's an urgent notice they
    // haven't confirmed yet, instead of relying on them to remember to look.
    val unacknowledgedUrgentCount by (
        if (userId != null) {
            AppDatabase.getInstance(context).alertDao().getUnacknowledgedUrgentCount(userId)
        } else {
            emptyFlow()
        }
    ).collectAsState(initial = 0)

    // Same idea for Community: an Instagram-style badge for unseen likes/comments on your posts.
    val userViewModel: UserViewModel =
        viewModel(factory = UserViewModelFactory(AppDatabase.getInstance(context).userDao()))
    val users by userViewModel.users.collectAsState()
    val currentUser = users.find { it.id == userId }
    val unseenActivityCount by (
        if (userId != null) {
            AppDatabase.getInstance(context).postDao()
                .getUnseenActivityCount(userId, currentUser?.lastSeenActivityAt ?: 0)
        } else {
            emptyFlow()
        }
    ).collectAsState(initial = 0)

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        if (item.route == "main_hub") {
                            // Home is the post-login "base" screen itself: pop straight back to
                            // it instead of the saveState/restoreState dance below, which is only
                            // meant for navigating between sibling destinations.
                            val poppedToHub = navController.popBackStack("main_hub", inclusive = false)
                            if (!poppedToHub) {
                                navController.navigate("main_hub") { launchSingleTop = true }
                            }
                        } else {
                            navController.navigate(item.route) {
                                // Anchor on "main_hub" explicitly, not graph.findStartDestination():
                                // the NavHost's actual start destination is "login", which is no
                                // longer on the back stack after a successful sign-in.
                                popUpTo("main_hub") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                icon = {
                    val badgeCount = when (item.route) {
                        "alert" -> unacknowledgedUrgentCount
                        "community" -> unseenActivityCount
                        else -> 0
                    }
                    if (badgeCount > 0) {
                        BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
                            Icon(item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
