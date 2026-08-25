package com.example.asgm.model

import androidx.compose.ui.graphics.vector.ImageVector

data class MainHubItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val isLive: Boolean = false
)
