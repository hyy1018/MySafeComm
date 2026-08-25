package com.example.asgm.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.EmergencyContactEntity

/** User screen: single-tap call directory. Admin's "Manage Contacts" screen is deferred until Login/roles exist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHubScreen(navController: NavHostController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).emergencyContactDao() }
    val contacts by dao.getAll().collectAsState(initial = emptyList())
    val primary = contacts.filter { !it.isSpecialized }
    val specialized = contacts.filter { it.isSpecialized }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Emergency Hub")
                        Text(
                            "Single tap to call emergency services",
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
            items(primary, key = { it.serviceId }) { contact ->
                ContactCard(contact)
            }
            if (specialized.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Specialized Services",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(
                    specialized,
                    key = { it.serviceId },
                    span = { GridItemSpan(maxLineSpan) }
                ) { contact ->
                    SpecializedContactRow(contact)
                }
            }
        }
    }
}

private fun dialContact(context: android.content.Context, phoneNo: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNo")))
}

@Composable
private fun ContactCard(contact: EmergencyContactEntity) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
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

@Composable
private fun SpecializedContactRow(contact: EmergencyContactEntity) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { dialContact(context, contact.phoneNo) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    contact.categoryEmergency,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
