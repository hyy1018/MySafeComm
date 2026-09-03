// #member3
// Admin form for adding or editing one safety guide category, with a numbered list of steps.
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.asgm.data.local.entity.SafetyGuideEntity
import com.example.asgm.viewmodel.SafetyGuideDetailViewModel
import com.example.asgm.viewmodel.SafetyGuideDetailViewModelFactory
import com.example.asgm.viewmodel.SafetyGuideViewModel
import com.example.asgm.viewmodel.SafetyGuideViewModelFactory

// One step being edited: title (e.g. "Drop, Cover, and Hold On") + its description.
// Backed by two independent mutableStateOf so typing in one field doesn't recompose the other.
private class StepInput(title: String = "", description: String = "") {
    var title by mutableStateOf(title)
    var description by mutableStateOf(description)
}

// Reverses the "Title||Description" per line join used when saving, so editing an existing
// guide starts from its real steps instead of one blank row.
private fun parseSteps(stepsText: String): List<StepInput> =
    stepsText.split("\n").mapNotNull { line ->
        val parts = line.split("||")
        if (parts.size == 2) StepInput(parts[0], parts[1]) else null
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddGuideScreen(guideId: Long = -1L, navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: SafetyGuideViewModel =
        viewModel(factory = SafetyGuideViewModelFactory(db.safetyGuideDao()))
    val isEditing = guideId != -1L

    val existingGuide: SafetyGuideEntity? = if (isEditing) {
        val detailViewModel: SafetyGuideDetailViewModel =
            viewModel(factory = SafetyGuideDetailViewModelFactory(db.safetyGuideDao(), guideId))
        detailViewModel.guide.collectAsState().value
    } else {
        null
    }

    var category by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf(StepInput()) }
    var loadedExisting by remember { mutableStateOf(false) }

    LaunchedEffect(existingGuide) {
        if (!loadedExisting) {
            existingGuide?.let {
                category = it.categorySafety
                val parsed = parseSteps(it.steps)
                if (parsed.isNotEmpty()) {
                    steps.clear()
                    steps.addAll(parsed)
                }
                loadedExisting = true
            }
        }
    }

    val hasCompleteStep = steps.any { it.title.isNotBlank() && it.description.isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Safety Guide" else "Add Safety Guide") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g., Earthquake") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Steps", style = MaterialTheme.typography.titleSmall)
            }
            itemsIndexed(steps) { index, step ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Step ${index + 1}", style = MaterialTheme.typography.labelLarge)
                            if (steps.size > 1) {
                                IconButton(onClick = { steps.removeAt(index) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove step")
                                }
                            }
                        }
                        OutlinedTextField(
                            value = step.title,
                            onValueChange = { step.title = it },
                            label = { Text("Step Title") },
                            placeholder = { Text("e.g., Drop, Cover, and Hold On") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = step.description,
                            onValueChange = { step.description = it },
                            label = { Text("Step Detail") },
                            placeholder = { Text("e.g., Get under sturdy furniture and hold on until the shaking stops.") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { steps.add(StepInput()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add Step")
                }
            }
            item {
                Button(
                    onClick = {
                        val stepsText = steps
                            .filter { it.title.isNotBlank() && it.description.isNotBlank() }
                            .joinToString("\n") { "${it.title.trim()}||${it.description.trim()}" }
                        if (isEditing) {
                            existingGuide?.let {
                                viewModel.updateGuide(it.copy(categorySafety = category.trim(), steps = stepsText))
                            }
                        } else {
                            viewModel.addGuide(
                                SafetyGuideEntity(categorySafety = category.trim(), steps = stepsText)
                            )
                        }
                        navController.popBackStack()
                    },
                    enabled = category.isNotBlank() && hasCompleteStep,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isEditing) "Save Changes" else "Add Safety Guide")
                }
            }
        }
    }
}
