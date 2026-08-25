package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.UserRole
import kotlinx.coroutines.launch

private enum class LoginTab { USER, ADMIN }

/** Login gate: everything else in the app (Main Hub and its modules) sits behind this. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val userDao = remember { AppDatabase.getInstance(context).userDao() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(LoginTab.USER) }
    var userId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Guardian Sync",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                "Community safety, streamlined. Secure your neighborhood with real-time response.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Text("Welcome Back", style = MaterialTheme.typography.titleLarge)
            Text(
                "Please sign in to your secure account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == LoginTab.USER,
                    onClick = {
                        selectedTab = LoginTab.USER
                        errorMessage = null
                        userId = ""
                        password = ""
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("User")
                }
                SegmentedButton(
                    selected = selectedTab == LoginTab.ADMIN,
                    onClick = {
                        selectedTab = LoginTab.ADMIN
                        errorMessage = null
                        userId = ""
                        password = ""
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Admin")
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it; errorMessage = null },
                label = { Text("User ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { navController.navigate("signup") }) {
                    Text("Sign Up")
                }
                TextButton(
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Contact your community admin to reset your password") }
                    }
                ) {
                    Text("Forgot?")
                }
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val id = userId.trim()
                    if (id.isEmpty() || password.isEmpty()) {
                        errorMessage = "Enter your ID and password"
                        return@Button
                    }
                    isLoggingIn = true
                    scope.launch {
                        val user = userDao.login(id, password)
                        isLoggingIn = false
                        val tabMatchesRole = (selectedTab == LoginTab.ADMIN) == (user?.role == UserRole.ADMIN)
                        when {
                            user == null || !tabMatchesRole -> errorMessage = "Invalid ID or password"
                            else -> {
                                UserSession.login(user)
                                navController.navigate("main_hub") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    }
                },
                enabled = !isLoggingIn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedTab == LoginTab.ADMIN) "Login as Admin" else "Login as User")
            }
            Spacer(Modifier.height(16.dp))

            Row {
                Text("New to Guardian Sync? ", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { navController.navigate("signup") }) {
                    Text("Request access")
                }
            }
        }
    }
}
