package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.data.local.entity.UserRole
import kotlinx.coroutines.launch

/** Admin screen: create new Admin accounts, and reset any resident's password. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(navController: NavHostController) {
    val context = LocalContext.current
    val userDao = remember { AppDatabase.getInstance(context).userDao() }
    val scope = rememberCoroutineScope()

    var newAdminId by remember { mutableStateOf("") }
    var newAdminName by remember { mutableStateOf("") }
    var newAdminPassword by remember { mutableStateOf("") }
    var addAdminMessage by remember { mutableStateOf<String?>(null) }
    var addAdminIsError by remember { mutableStateOf(false) }

    var resetId by remember { mutableStateOf("") }
    var resetPassword by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var resetIsError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Admin Account", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = newAdminName,
                        onValueChange = { newAdminName = it; addAdminMessage = null },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAdminId,
                        onValueChange = { newAdminId = it; addAdminMessage = null },
                        label = { Text("New Admin ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAdminPassword,
                        onValueChange = { newAdminPassword = it; addAdminMessage = null },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    addAdminMessage?.let {
                        Text(
                            it,
                            color = if (addAdminIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = {
                            val id = newAdminId.trim()
                            val name = newAdminName.trim()
                            if (id.isEmpty() || name.isEmpty() || newAdminPassword.isEmpty()) {
                                addAdminIsError = true
                                addAdminMessage = "Fill in name, ID and password"
                                return@Button
                            }
                            scope.launch {
                                val existing = userDao.getById(id)
                                if (existing != null) {
                                    addAdminIsError = true
                                    addAdminMessage = "That ID is already taken"
                                } else {
                                    userDao.insert(
                                        UserEntity(
                                            id = id,
                                            password = newAdminPassword,
                                            name = name,
                                            role = UserRole.ADMIN,
                                            contact = ""
                                        )
                                    )
                                    addAdminIsError = false
                                    addAdminMessage = "Admin account \"$id\" created"
                                    newAdminId = ""
                                    newAdminName = ""
                                    newAdminPassword = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Admin")
                    }
                }
            }

            HorizontalDivider()

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Reset User Password", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = resetId,
                        onValueChange = { resetId = it; resetMessage = null },
                        label = { Text("User ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = resetPassword,
                        onValueChange = { resetPassword = it; resetMessage = null },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    resetMessage?.let {
                        Text(
                            it,
                            color = if (resetIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = {
                            val id = resetId.trim()
                            if (id.isEmpty() || resetPassword.isEmpty()) {
                                resetIsError = true
                                resetMessage = "Enter a user ID and new password"
                                return@Button
                            }
                            scope.launch {
                                val existing = userDao.getById(id)
                                if (existing == null) {
                                    resetIsError = true
                                    resetMessage = "No user found with that ID"
                                } else {
                                    userDao.update(existing.copy(password = resetPassword))
                                    resetIsError = false
                                    resetMessage = "Password reset for \"$id\""
                                    resetId = ""
                                    resetPassword = ""
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
    }
}
