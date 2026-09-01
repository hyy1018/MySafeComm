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
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

/** Admin screen: set a new password for an existing account. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminResetPasswordScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val scope = rememberCoroutineScope()

    var id by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset User Password") },
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
                value = id,
                onValueChange = { id = it; message = null },
                label = { Text("User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PasswordField(
                value = newPassword,
                onValueChange = { newPassword = it; message = null },
                label = "New Password",
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
                    if (trimmedId.isEmpty() || newPassword.isEmpty()) {
                        isError = true
                        message = "Enter a user ID and new password"
                        return@Button
                    }
                    if (!PasswordRules.isValid(newPassword)) {
                        isError = true
                        message = PasswordRules.REQUIREMENT_MESSAGE
                        return@Button
                    }
                    scope.launch {
                        val existing = userViewModel.getById(trimmedId)
                        when {
                            existing == null -> {
                                isError = true
                                message = "No user found with that ID"
                            }
                            existing.password == newPassword -> {
                                isError = true
                                message = "New password can't be the same as the old password"
                            }
                            else -> {
                                userViewModel.updateUser(existing.copy(password = newPassword))
                                isError = false
                                message = "Password reset for \"$trimmedId\""
                                id = ""
                                newPassword = ""
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Password")
            }
        }
    }
}
