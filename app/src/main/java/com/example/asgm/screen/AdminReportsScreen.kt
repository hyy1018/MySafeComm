// #member2
// Admin screen listing every hazard report: tap a card for the full detail (photo included),
// or use the status chips to move it and auto-notify the reporter.
package com.example.asgm.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.ReportEntity
import com.example.asgm.data.local.entity.ReportStatus
import com.example.asgm.viewmodel.MessageViewModel
import com.example.asgm.viewmodel.MessageViewModelFactory
import com.example.asgm.viewmodel.ReportViewModel
import com.example.asgm.viewmodel.ReportViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val reportViewModel: ReportViewModel = viewModel(factory = ReportViewModelFactory(db.reportDao()))
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val messageViewModel: MessageViewModel = viewModel(factory = MessageViewModelFactory(db.messageDao()))
    val reports by reportViewModel.reports.collectAsState()
    val users by userViewModel.users.collectAsState()

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
                Text("No reports submitted yet.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports, key = { it.reportId }) { report ->
                    val reporterName = users.find { it.id == report.userId }?.name ?: report.userId
                    AdminReportCard(
                        report = report,
                        reporterName = reporterName,
                        viewModel = reportViewModel,
                        messageViewModel = messageViewModel,
                        onOpen = { navController.navigate("report_detail/${report.reportId}") }
                    )
                }
            }
        }
    }
}

// The message the reporter gets whenever an admin moves their report to a new status.
private fun reportStatusMessage(reportTitle: String, status: ReportStatus): String {
    val label = when (status) {
        ReportStatus.PENDING -> "Pending"
        ReportStatus.IN_PROGRESS -> "In Progress"
        ReportStatus.SOLVED -> "Solved"
        ReportStatus.REJECTED -> "Rejected"
    }
    return "Update on your report \"$reportTitle\": status is now $label."
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AdminReportCard(
    report: ReportEntity,
    reporterName: String,
    viewModel: ReportViewModel,
    messageViewModel: MessageViewModel,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
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
            report.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "Report photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                "Reported by $reporterName - ${dateFormat.format(Date(report.timestamp))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportStatus.entries.forEach { status ->
                    FilterChip(
                        selected = report.status == status,
                        onClick = {
                            // only act on a real change -- re-tapping the current status does nothing
                            if (report.status != status) {
                                viewModel.updateStatus(report.reportId, status)
                                val adminId = UserSession.currentUserId
                                if (adminId != null) {
                                    messageViewModel.send(
                                        fromUserId = adminId,
                                        toUserId = report.userId,
                                        body = reportStatusMessage(report.title, status)
                                    )
                                }
                            }
                        },
                        label = { Text(status.name.replace('_', ' ')) }
                    )
                }
            }
        }
    }
}
