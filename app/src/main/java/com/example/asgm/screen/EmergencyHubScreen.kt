package com.example.asgm.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.local.entity.SafetyGuideEntity

/**
 * User screen: SOS -- single-tap emergency contacts plus safety guides in one place, since "who
 * to call" and "what to do" in an emergency are the same mental model. One continuous grid with
 * a section header between the two, rather than tabs, so it reads as one page, not two stitched
 * together. Admin management of either is deferred until Login/roles exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHubScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val contacts by db.emergencyContactDao().getAll().collectAsState(initial = emptyList())
    val guides by db.safetyGuideDao().getAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SOS")
                        Text(
                            "Emergency contacts & safety guides",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // An odd count leaves one card without a row partner; center that last one on its
            // own row instead of letting the grid strand it in the left column.
            val gridContacts = if (contacts.size % 2 == 1) contacts.dropLast(1) else contacts
            val centeredContact = if (contacts.size % 2 == 1) contacts.lastOrNull() else null

            items(gridContacts, key = { "contact_${it.serviceId}" }) { contact ->
                ContactCard(contact)
            }
            if (centeredContact != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ContactCard(centeredContact, modifier = Modifier.fillMaxWidth(0.5f))
                    }
                }
            }

            if (guides.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Safety Guides",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(guides, key = { "guide_${it.guideId}" }) { guide ->
                    GuideCard(guide) { navController.navigate("guide_detail/${guide.guideId}") }
                }
            }
        }
    }
}

private fun dialContact(context: android.content.Context, phoneNo: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNo")))
}

@Composable
private fun ContactCard(contact: EmergencyContactEntity, modifier: Modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .clickable { dialContact(context, contact.phoneNo) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(contact.name, style = MaterialTheme.typography.titleSmall)
            Text(
                contact.categoryEmergency,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                contact.phoneNo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun iconForGuide(category: String): ImageVector = when (category) {
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
                imageVector = iconForGuide(guide.categorySafety),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(guide.categorySafety, style = MaterialTheme.typography.titleMedium)
            Text(
                "Procedures",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
