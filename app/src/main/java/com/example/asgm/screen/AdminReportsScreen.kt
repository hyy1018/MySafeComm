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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.ReportEntity
import com.example.asgm.data.local.entity.ReportStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

/** Admin screen: every resident's hazard reports, with a status control on each. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val reports by db.reportDao().getAll().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Reports") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No reports submitted yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports, key = { it.reportId }) { report ->
                    AdminReportCard(report)
                }
            }
        }
    }
}

@Composable
private fun AdminReportCard(report: ReportEntity) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val reporter by db.userDao().observeById(report.userId).collectAsState(initial = null)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(report.title, style = MaterialTheme.typography.titleMedium)
            Text(
                report.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(report.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Reported by ${reporter?.name ?: report.userId} - ${dateFormat.format(Date(report.timestamp))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportStatus.entries.forEach { status ->
                    FilterChip(
                        selected = report.status == status,
                        onClick = { scope.launch { db.reportDao().updateStatus(report.reportId, status) } },
                        label = { Text(status.name.replace('_', ' ')) }
                    )
                }
            }
        }
    }
}
