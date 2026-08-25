package com.example.asgm.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import com.example.asgm.model.MainHubItem

/**
 * Static menu entries shown as cards on the Main Hub. The Community Feed entry is the
 * addon module (Reddit/Facebook-style posts) and is its own top-level entry, matching
 * Report / Alert / SOS / Guide rather than being folded into any existing screen.
 */
object MainHubData {
    val modules = listOf(
        MainHubItem(
            id = "report",
            title = "Hazard Reporting",
            subtitle = "Report issues in your area",
            icon = Icons.Filled.ReportProblem,
            route = "report"
        ),
        MainHubItem(
            id = "alert",
            title = "Live Alerts",
            subtitle = "Real-time safety updates",
            icon = Icons.Filled.Notifications,
            route = "alert",
            isLive = true
        ),
        MainHubItem(
            id = "sos",
            title = "Emergency Hub",
            subtitle = "Direct access to help",
            icon = Icons.Filled.Emergency,
            route = "sos"
        ),
        MainHubItem(
            id = "guide",
            title = "Safety Guide",
            subtitle = "Tips & emergency protocols",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            route = "guide"
        ),
        MainHubItem(
            id = "community",
            title = "Community Feed",
            subtitle = "Share posts, comment & like with neighbours",
            icon = Icons.Filled.Forum,
            route = "community"
        )
    )
}
