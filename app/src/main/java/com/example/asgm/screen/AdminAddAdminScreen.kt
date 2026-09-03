// Admin screen: create another Admin account.
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.PasswordRules
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.data.local.entity.UserRole
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddAdminScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Admin Account") },
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
                onValueChange = { name = it; message = null },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = id,
                onValueChange = { id = it; message = null },
                label = { Text("New Admin ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PasswordField(
                value = password,
                onValueChange = { password = it; message = null },
                label = "Password",
                modifier = Modifier.fillMaxWidth()
            )
            message?.let {
                Text(
                    it,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = {
                    val trimmedId = id.trim()
                    val trimmedName = name.trim()
                    if (trimmedId.isEmpty() || trimmedName.isEmpty() || password.isEmpty()) {
                        isError = true
                        message = "Fill in name, ID and password"
                        return@Button
                    }
                    if (!PasswordRules.isValid(password)) {
                        isError = true
                        message = PasswordRules.REQUIREMENT_MESSAGE
                        return@Button
                    }
                    scope.launch {
                        val existing = userViewModel.getById(trimmedId)
                        if (existing != null) {
                            isError = true
                            message = "That ID is already taken"
                        } else {
                            userViewModel.signUp(
                                UserEntity(
                                    id = trimmedId,
                                    password = password,
                                    name = trimmedName,
                                    role = UserRole.ADMIN
                                )
                            )
                            isError = false
                            message = "Admin account \"$trimmedId\" created"
                            name = ""
                            id = ""
                            password = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Admin")
            }
        }
    }
}
