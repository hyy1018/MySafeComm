// #member3
// Admin form for adding or editing one emergency contact in SOS.
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.EmergencyContactEntity
import com.example.asgm.viewmodel.EmergencyContactDetailViewModel
import com.example.asgm.viewmodel.EmergencyContactDetailViewModelFactory
import com.example.asgm.viewmodel.EmergencyContactViewModel
import com.example.asgm.viewmodel.EmergencyContactViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddContactScreen(serviceId: Long = -1L, navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: EmergencyContactViewModel =
        viewModel(factory = EmergencyContactViewModelFactory(db.emergencyContactDao()))
    val isEditing = serviceId != -1L

    val existingContact: EmergencyContactEntity? = if (isEditing) {
        val detailViewModel: EmergencyContactDetailViewModel =
            viewModel(factory = EmergencyContactDetailViewModelFactory(db.emergencyContactDao(), serviceId))
        detailViewModel.contact.collectAsState().value
    } else {
        null
    }

    var name by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var loadedExisting by remember { mutableStateOf(false) }

    LaunchedEffect(existingContact) {
        if (!loadedExisting) {
            existingContact?.let {
                name = it.name
                detail = it.categoryEmergency
                phone = it.phoneNo
                loadedExisting = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Emergency Contact" else "Add Emergency Contact") },
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g., General Emergency") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                label = { Text("Detail") },
                placeholder = { Text("e.g., Police and ambulance") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { input -> phone = input.filter { it.isDigit() || it == '-' } },
                label = { Text("Phone Number") },
                placeholder = { Text("e.g., 999") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (isEditing) {
                        existingContact?.let {
                            viewModel.updateContact(
                                it.copy(name = name.trim(), categoryEmergency = detail.trim(), phoneNo = phone.trim())
                            )
                        }
                    } else {
                        viewModel.addContact(
                            EmergencyContactEntity(
                                name = name.trim(),
                                categoryEmergency = detail.trim(),
                                phoneNo = phone.trim()
                            )
                        )
                    }
                    navController.popBackStack()
                },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Save Changes" else "Add Contact")
            }
        }
    }
}
