package com.example.asgm.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import com.example.asgm.model.MainHubItem

/** Admin dashboard entries: separate from the resident Main Hub (matches the login nav split). */
object AdminHubData {
    val modules = listOf(
        MainHubItem(
            id = "manage_reports",
            title = "Manage Reports",
            subtitle = "Review hazard reports and update their status",
            icon = Icons.Filled.ReportProblem,
            route = "admin_reports"
        ),
        MainHubItem(
            id = "manage_alerts",
            title = "Manage Alerts",
            subtitle = "Create, edit and remove community notices",
            icon = Icons.Filled.Notifications,
            route = "admin_alerts"
        ),
        MainHubItem(
            id = "manage_posts",
            title = "Manage Posts",
            subtitle = "Edit Community Feed posts",
            icon = Icons.Filled.Forum,
            route = "admin_posts"
        ),
        MainHubItem(
            id = "manage_users",
            title = "Manage Users",
            subtitle = "Add Admin accounts, reset a resident's password",
            icon = Icons.Filled.ManageAccounts,
            route = "admin_users"
        )
    )
}
