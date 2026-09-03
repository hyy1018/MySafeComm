// #member2
// Resident's Live Alerts feed.
package com.example.asgm.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.AlertPriority
import com.example.asgm.viewmodel.AlertAckViewModel
import com.example.asgm.viewmodel.AlertAckViewModelFactory
import com.example.asgm.viewmodel.AlertViewModel
import com.example.asgm.viewmodel.AlertViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val alertDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val alertViewModel: AlertViewModel = viewModel(factory = AlertViewModelFactory(db.alertDao()))
    val alerts by alertViewModel.alerts.collectAsState()

    // Only one alert card is expanded at a time (mirrors the nullable-state dialog pattern).
    var expandedAlertId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Live Alerts & Announcements")
                        Text(
                            "Stay updated with your community",
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
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No alerts right now.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts, key = { it.alertId }) { alert ->
                    AlertCard(
                        alert = alert,
                        expanded = expandedAlertId == alert.alertId,
                        onToggleExpanded = {
                            expandedAlertId = if (expandedAlertId == alert.alertId) null else alert.alertId
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: AlertEntity,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val isUrgent = alert.priority == AlertPriority.URGENT
    // Nullable, not requireUserId(): see MyReportsScreen for why -- this must not throw during
    // a transient no-session composition (process death restoring the back stack).
    val userId = UserSession.currentUserId
    val ackViewModel: AlertAckViewModel? = if (userId != null) {
        viewModel(factory = AlertAckViewModelFactory(db.alertAcknowledgementDao(), alert.alertId, userId))
    } else {
        null
    }
    val acknowledged = ackViewModel?.acknowledged?.collectAsState()?.value ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onToggleExpanded)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityBadge(isUrgent = isUrgent)
                Spacer(Modifier.width(8.dp))
                Text(alert.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(alert.body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                NoticeMetaRow(label = "Date", value = alertDateFormat.format(Date(alert.timestamp)))
                if (alert.location.isNotBlank()) {
                    NoticeMetaRow(label = "Location", value = alert.location)
                }
                if (alert.issuedBy.isNotBlank()) {
                    NoticeMetaRow(label = "Issued by", value = alert.issuedBy)
                }
                if (isUrgent) {
                    Spacer(Modifier.height(12.dp))
                    if (acknowledged) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Acknowledged", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = { ackViewModel?.acknowledge() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Acknowledgment")
                        }
                    }
                }
            }
        }
    }
}

// a formal label/value line, e.g. "Date: Aug 26, 2026"
@Composable
private fun NoticeMetaRow(label: String, value: String) {
    Row {
        Text(
            "$label: ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PriorityBadge(isUrgent: Boolean) {
    val container = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val onContainer = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        if (isUrgent) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = onContainer,
                modifier = Modifier.height(14.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = if (isUrgent) "URGENT" else "INFO",
            style = MaterialTheme.typography.labelSmall,
            color = onContainer
        )
    }
}
