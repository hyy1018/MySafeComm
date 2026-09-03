// Step-by-step procedure for one safety guide category.
package com.example.asgm.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.viewmodel.SafetyGuideDetailViewModel
import com.example.asgm.viewmodel.SafetyGuideDetailViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyGuideDetailScreen(guideId: Long, navController: NavHostController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getInstance(context).safetyGuideDao() }
    val viewModel: SafetyGuideDetailViewModel =
        viewModel(factory = SafetyGuideDetailViewModelFactory(dao, guideId))
    val guide by viewModel.guide.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(guide?.categorySafety?.let { "$it Safety Guide" } ?: "Safety Guide") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        val currentGuide = guide
        if (currentGuide == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        } else {
            val steps = currentGuide.steps.split("\n").mapIndexedNotNull { index, line ->
                val parts = line.split("||")
                if (parts.size == 2) Triple(index + 1, parts[0], parts[1]) else null
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "How to Handle a ${currentGuide.categorySafety} Emergency",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(steps, key = { it.first }) { (number, title, description) ->
                    StepCard(number, title, description)
                }
            }
        }
    }
}

@Composable
private fun StepCard(number: Int, title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$number",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
