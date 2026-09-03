// #member2
// Admin screen: create, edit, and remove alerts.
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.AlertPriority
import com.example.asgm.viewmodel.AlertViewModel
import com.example.asgm.viewmodel.AlertViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val adminAlertDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAlertsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: AlertViewModel = viewModel(factory = AlertViewModelFactory(db.alertDao()))
    val alerts by viewModel.alerts.collectAsState()

    var alertPendingDelete by remember { mutableStateOf<AlertEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Alerts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("admin_alert_form") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new alert")
            }
        }
    ) { innerPadding ->
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No alerts yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts, key = { it.alertId }) { alert ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                if (alert.priority == AlertPriority.URGENT) "URGENT" else "INFO",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (alert.priority == AlertPriority.URGENT)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(alert.title, style = MaterialTheme.typography.titleMedium)
                            Text(alert.body, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                buildString {
                                    append(adminAlertDateFormat.format(Date(alert.timestamp)))
                                    if (alert.location.isNotBlank()) append(" - ${alert.location}")
                                    if (alert.issuedBy.isNotBlank()) append(" - by ${alert.issuedBy}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { navController.navigate("admin_alert_form?alertId=${alert.alertId}") }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                    Text("Edit")
                                }
                                TextButton(onClick = { alertPendingDelete = alert }) {
                                    Icon(Icons.Filled.Delete, contentDescription = null)
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        alertPendingDelete?.let { alert ->
            AlertDialog(
                onDismissRequest = { alertPendingDelete = null },
                title = { Text("Delete alert?") },
                text = { Text("Residents will no longer see \"${alert.title}\" in Live Alerts.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAlert(alert)
                        alertPendingDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { alertPendingDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}
