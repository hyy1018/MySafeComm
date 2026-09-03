// #member2
// Admin form for adding or editing one alert.
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.AlertEntity
import com.example.asgm.data.local.entity.AlertPriority
import com.example.asgm.viewmodel.AlertDetailViewModel
import com.example.asgm.viewmodel.AlertDetailViewModelFactory
import com.example.asgm.viewmodel.AlertViewModel
import com.example.asgm.viewmodel.AlertViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAlertFormScreen(alertId: Long, navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val alertViewModel: AlertViewModel = viewModel(factory = AlertViewModelFactory(db.alertDao()))
    val isEditing = alertId != -1L

    val existingAlert: AlertEntity? = if (isEditing) {
        val detailViewModel: AlertDetailViewModel =
            viewModel(factory = AlertDetailViewModelFactory(db.alertDao(), alertId))
        detailViewModel.alert.collectAsState().value
    } else {
        null
    }

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(AlertPriority.INFO) }
    var loadedExisting by remember { mutableStateOf(false) }

    LaunchedEffect(existingAlert) {
        if (!loadedExisting) {
            existingAlert?.let {
                title = it.title
                body = it.body
                location = it.location
                priority = it.priority
                loadedExisting = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Alert" else "Add New Alert") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Body") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                placeholder = { Text("e.g., Oakwood Sector, Blocks A-G") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = priority == AlertPriority.INFO,
                    onClick = { priority = AlertPriority.INFO },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Info") }
                SegmentedButton(
                    selected = priority == AlertPriority.URGENT,
                    onClick = { priority = AlertPriority.URGENT },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Urgent") }
            }
            Button(
                onClick = {
                    if (isEditing) {
                        existingAlert?.let {
                            alertViewModel.updateAlert(
                                it.copy(title = title.trim(), body = body.trim(), location = location.trim(), priority = priority)
                            )
                        }
                    } else {
                        alertViewModel.addAlert(
                            AlertEntity(
                                title = title.trim(),
                                body = body.trim(),
                                location = location.trim(),
                                priority = priority,
                                issuedBy = UserSession.currentUserName ?: "Community Admin"
                            )
                        )
                    }
                    navController.popBackStack()
                },
                enabled = title.isNotBlank() && body.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Save Changes" else "Add Alert")
            }
        }
    }
}
