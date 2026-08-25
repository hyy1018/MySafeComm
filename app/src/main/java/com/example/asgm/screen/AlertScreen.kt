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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.AlertAcknowledgementEntity
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.AlertPriority
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/** User screen: community notice feed. Admin's add/edit/delete alert screen is deferred until Login/roles exist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(navController: NavHostController) {
    val context = LocalContext.current
    val alertDao = remember { AppDatabase.getInstance(context).alertDao() }
    val alerts by alertDao.getAll().collectAsState(initial = emptyList())

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
    val ackDao = remember { AppDatabase.getInstance(context).alertAcknowledgementDao() }
    val scope = rememberCoroutineScope()
    val isUrgent = alert.priority == AlertPriority.URGENT
    val userId = UserSession.currentUserId
    val acknowledged by (
        if (userId != null) ackDao.isAcknowledgedByUser(alert.alertId, userId) else emptyFlow()
    ).collectAsState(initial = false)

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
                            onClick = {
                                scope.launch {
                                    ackDao.acknowledge(
                                        AlertAcknowledgementEntity(
                                            alertId = alert.alertId,
                                            userId = UserSession.requireUserId()
                                        )
                                    )
                                }
                            },
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
