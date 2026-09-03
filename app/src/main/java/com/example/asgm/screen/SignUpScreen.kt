// #member1
// Self-service account creation (resident accounts only).
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.data.local.entity.UserRole
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val scope = rememberCoroutineScope()

    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Up") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Choose an ID and password to sign in with.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it; errorMessage = null },
                label = { Text("User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PasswordField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = "Password",
                modifier = Modifier.fillMaxWidth()
            )
            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = null },
                label = "Confirm Password",
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                PasswordRules.REQUIREMENT_MESSAGE,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val id = userId.trim()
                    if (id.isEmpty() || password.isEmpty()) {
                        errorMessage = "Enter an ID and password"
                        return@Button
                    }
                    if (!PasswordRules.isValid(password)) {
                        errorMessage = PasswordRules.REQUIREMENT_MESSAGE
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords don't match"
                        return@Button
                    }
                    isSubmitting = true
                    scope.launch {
                        val existing = userViewModel.getById(id)
                        if (existing != null) {
                            errorMessage = "That ID is already taken"
                            isSubmitting = false
                        } else {
                            val newUser = UserEntity(
                                id = id,
                                password = password,
                                name = id,
                                role = UserRole.RESIDENT
                            )
                            userViewModel.signUp(newUser)
                            UserSession.login(newUser)
                            isSubmitting = false
                            navController.navigate("complete_profile") {
                                popUpTo("signup") { inclusive = true }
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Up")
            }
        }
    }
}
