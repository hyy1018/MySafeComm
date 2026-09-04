// #member3
// Resident form for adding or editing one of their own private emergency contacts (name + phone).
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.PHONE_LENGTH_MESSAGE
import com.example.asgm.data.isValidPhone
import com.example.asgm.data.sanitizePhone
import com.example.asgm.viewmodel.EmergencyContactDetailViewModel
import com.example.asgm.viewmodel.EmergencyContactDetailViewModelFactory
import com.example.asgm.viewmodel.PersonalContactViewModel
import com.example.asgm.viewmodel.PersonalContactViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalContactFormScreen(contactId: Long = -1L, navController: NavHostController) {
    // no session (e.g. restored after process death) -> the nav guard sends us to login
    val userId = UserSession.currentUserId ?: return
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewModel: PersonalContactViewModel =
        viewModel(factory = PersonalContactViewModelFactory(db.emergencyContactDao(), userId))
    val isEditing = contactId != -1L

    val existing = if (isEditing) {
        val detailViewModel: EmergencyContactDetailViewModel =
            viewModel(factory = EmergencyContactDetailViewModelFactory(db.emergencyContactDao(), contactId))
        detailViewModel.contact.collectAsState().value
    } else {
        null
    }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loadedExisting by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!loadedExisting) {
            existing?.let {
                name = it.name
                phone = it.phoneNo
                loadedExisting = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Contact" else "Add Your Emergency Contact") },
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g., Mum") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { input -> phone = sanitizePhone(input); error = null },
                label = { Text("Phone Number") },
                placeholder = { Text("e.g., 012-345-6789") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (!isValidPhone(phone)) {
                        error = PHONE_LENGTH_MESSAGE
                        return@Button
                    }
                    if (isEditing) {
                        existing?.let {
                            viewModel.update(it.copy(name = name.trim(), phoneNo = phone.trim()))
                        }
                    } else {
                        viewModel.add(name.trim(), phone.trim())
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
