// #member3
// Admin screen: add emergency contacts and safety guides shown in resident SOS.
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.viewmodel.EmergencyContactViewModel
import com.example.asgm.viewmodel.EmergencyContactViewModelFactory
import com.example.asgm.viewmodel.SafetyGuideViewModel
import com.example.asgm.viewmodel.SafetyGuideViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSosScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val contactViewModel: EmergencyContactViewModel =
        viewModel(factory = EmergencyContactViewModelFactory(db.emergencyContactDao()))
    val guideViewModel: SafetyGuideViewModel =
        viewModel(factory = SafetyGuideViewModelFactory(db.safetyGuideDao()))
    val contacts by contactViewModel.contacts.collectAsState()
    val guides by guideViewModel.guides.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage SOS") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Emergency Contacts", style = MaterialTheme.typography.titleMedium)
            }
            items(contacts, key = { "contact_${it.serviceId}" }) { contact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(contact.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            contact.categoryEmergency,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(contact.phoneNo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { navController.navigate("admin_add_contact") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Contact")
                }
            }

            item {
                Text(
                    "Safety Guides",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            items(guides, key = { "guide_${it.guideId}" }) { guide ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(guide.categorySafety, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${guide.steps.split("\n").size} steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { navController.navigate("admin_add_guide") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Safety Guide")
                }
            }
        }
    }
}
