// #member3
// Admin screen: permanently delete a user account and everything tied to it.
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDeleteUserScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val scope = rememberCoroutineScope()

    var id by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var userPendingDelete by remember { mutableStateOf<UserEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete User Account") },
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
                value = id,
                onValueChange = { id = it; message = null },
                label = { Text("User ID") },
                singleLine = true,
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
                    if (trimmedId.isEmpty()) {
                        isError = true
                        message = "Enter a user ID"
                        return@Button
                    }
                    if (trimmedId == UserSession.currentUserId) {
                        isError = true
                        message = "Use \"Delete My Account\" in your own Profile instead"
                        return@Button
                    }
                    scope.launch {
                        val existing = userViewModel.getById(trimmedId)
                        if (existing == null) {
                            isError = true
                            message = "No user found with that ID"
                        } else {
                            isError = false
                            message = null
                            userPendingDelete = existing
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find Account")
            }
        }

        userPendingDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { userPendingDelete = null },
                title = { Text("Delete \"${target.id}\"?") },
                text = {
                    Text(
                        "This permanently deletes ${target.name} (${target.id}) and everything tied " +
                            "to their account -- reports, posts, comments, likes, and messages. " +
                            "This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            userViewModel.deleteUser(target)
                            isError = false
                            message = "Deleted \"${target.id}\""
                            id = ""
                            userPendingDelete = null
                        }
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { userPendingDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}
