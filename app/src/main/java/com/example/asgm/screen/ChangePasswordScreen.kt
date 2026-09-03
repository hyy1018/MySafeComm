// #member1
// Self-service password change, separate from Admin's reset (this one needs the old password).
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
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
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
            PasswordField(
                value = currentPassword,
                onValueChange = { currentPassword = it; message = null },
                label = "Current Password",
                modifier = Modifier.fillMaxWidth()
            )
            PasswordField(
                value = newPassword,
                onValueChange = { newPassword = it; message = null },
                label = "New Password",
                modifier = Modifier.fillMaxWidth()
            )
            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; message = null },
                label = "Confirm New Password",
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                PasswordRules.REQUIREMENT_MESSAGE,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        isError = true
                        message = "Fill in all three fields"
                        return@Button
                    }
                    if (!PasswordRules.isValid(newPassword)) {
                        isError = true
                        message = PasswordRules.REQUIREMENT_MESSAGE
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        isError = true
                        message = "New password and confirmation don't match"
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val userId = UserSession.requireUserId()
                        val user = userViewModel.getById(userId)
                        when {
                            user == null || user.password != currentPassword -> {
                                isError = true
                                message = "Current password is incorrect"
                            }
                            newPassword == currentPassword -> {
                                isError = true
                                message = "New password can't be the same as the old password"
                            }
                            else -> {
                                userViewModel.updateUser(user.copy(password = newPassword))
                                isError = false
                                message = "Password changed successfully"
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            }
                        }
                        isSubmitting = false
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }
        }
    }
}
