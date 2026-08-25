package com.example.asgm.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.SafetyGuideEntity

/** User screen: safety guide categories. Admin's "Manage Guides" screen is deferred until Login/roles exist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyGuideScreen(navController: NavHostController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).safetyGuideDao() }
    val guides by dao.getAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Safety Guide") }) },
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(guides, key = { it.guideId }) { guide ->
                GuideCard(guide) { navController.navigate("guide_detail/${guide.guideId}") }
            }
        }
    }
}

private fun iconFor(category: String): ImageVector = when (category) {
    "Fire" -> Icons.Filled.LocalFireDepartment
    "Flood" -> Icons.Filled.Water
    "Power Outage" -> Icons.Filled.PowerOff
    "Earthquake" -> Icons.Filled.Vibration
    else -> Icons.AutoMirrored.Filled.MenuBook
}

@Composable
private fun GuideCard(guide: SafetyGuideEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = iconFor(guide.categorySafety),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(guide.categorySafety, style = MaterialTheme.typography.titleMedium)
            Text("Procedures", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
