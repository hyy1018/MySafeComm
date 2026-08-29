package com.example.asgm.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import com.example.asgm.model.MainHubItem

/**
 * Static menu entries shown as cards on the Main Hub. The Community Feed entry is the
 * addon module (Reddit/Facebook-style posts) and is its own top-level entry.
 * Safety Guide used to be its own fifth entry; it's now folded into SOS (EmergencyHubScreen)
 * since "who to call" and "what to do" in an emergency are the same mental model, so Main Hub
 * stays at four entries instead of growing back to five.
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
            id = "community",
            title = "Community Feed",
            subtitle = "Share posts, comment & like with neighbours",
            icon = Icons.Filled.Forum,
            route = "community"
        ),
        MainHubItem(
            id = "sos",
            title = "SOS",
            subtitle = "Emergency contacts & safety guides",
            icon = Icons.Filled.Emergency,
            route = "sos"
        )
    )
}
