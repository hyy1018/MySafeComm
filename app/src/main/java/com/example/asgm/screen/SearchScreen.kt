// #member1
// Search across reports, alerts, posts, guides, and emergency contacts.
package com.example.asgm.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.data.local.entity.PostEntity
import com.example.asgm.data.local.entity.ReportEntity
import com.example.asgm.data.local.entity.SafetyGuideEntity
import com.example.asgm.viewmodel.AlertViewModel
import com.example.asgm.viewmodel.AlertViewModelFactory
import com.example.asgm.viewmodel.EmergencyContactViewModel
import com.example.asgm.viewmodel.EmergencyContactViewModelFactory
import com.example.asgm.viewmodel.MyReportsViewModel
import com.example.asgm.viewmodel.MyReportsViewModelFactory
import com.example.asgm.viewmodel.PostViewModel
import com.example.asgm.viewmodel.PostViewModelFactory
import com.example.asgm.viewmodel.SafetyGuideViewModel
import com.example.asgm.viewmodel.SafetyGuideViewModelFactory

private data class SearchResult(val category: String, val title: String, val subtitle: String, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    var query by remember { mutableStateOf("") }

    val posts by viewModel<PostViewModel>(factory = PostViewModelFactory(db.postDao()))
        .posts.collectAsState()
    val alerts by viewModel<AlertViewModel>(factory = AlertViewModelFactory(db.alertDao()))
        .alerts.collectAsState()
    // Nullable, not requireUserId(): see MyReportsScreen for why -- must not throw during a
    // transient no-session composition.
    val userId = UserSession.currentUserId
    val reports: List<ReportEntity> = if (userId != null) {
        viewModel<MyReportsViewModel>(factory = MyReportsViewModelFactory(db.reportDao(), userId))
            .reports.collectAsState().value
    } else {
        emptyList()
    }
    val guides by viewModel<SafetyGuideViewModel>(factory = SafetyGuideViewModelFactory(db.safetyGuideDao()))
        .guides.collectAsState()
    val contacts by viewModel<EmergencyContactViewModel>(
        factory = EmergencyContactViewModelFactory(db.emergencyContactDao())
    ).contacts.collectAsState()

    val results = remember(query, posts, alerts, reports, guides, contacts) {
        if (query.isBlank()) emptyList() else buildSearchResults(query, posts, alerts, reports, guides, contacts)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search reports, alerts, posts, guides...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            query.isBlank() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Search across Reports, Alerts, Community posts, Safety Guides and Emergency contacts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            results.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No results for \"$query\"")
            }
            else -> {
                val grouped = results.groupBy { it.category }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                category,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(items) { result ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { navController.navigate(result.route) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(result.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(
                                        result.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildSearchResults(
    query: String,
    posts: List<PostEntity>,
    alerts: List<AlertEntity>,
    reports: List<ReportEntity>,
    guides: List<SafetyGuideEntity>,
    contacts: List<EmergencyContactEntity>
): List<SearchResult> {
    val results = mutableListOf<SearchResult>()
    posts.filter { it.content.contains(query, ignoreCase = true) }.forEach {
        results += SearchResult("Community Feed", it.content.take(60), "Post", "community_post/${it.postId}")
    }
    alerts.filter { it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true) }
        .forEach { results += SearchResult("Live Alerts", it.title, it.body.take(60), "alert") }
    reports.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
    }.forEach { results += SearchResult("My Reports", it.title, it.location, "my_reports") }
    guides.filter { it.categorySafety.contains(query, ignoreCase = true) }
        .forEach { results += SearchResult("Safety Guide", it.categorySafety, "Procedures", "guide_detail/${it.guideId}") }
    contacts.filter {
        it.name.contains(query, ignoreCase = true) || it.categoryEmergency.contains(query, ignoreCase = true)
    }.forEach { results += SearchResult("Emergency Hub", it.name, it.categoryEmergency, "sos") }
    return results
}
