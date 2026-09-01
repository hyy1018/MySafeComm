package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.data.local.entity.UserRole
import com.example.asgm.viewmodel.MessageViewModel
import com.example.asgm.viewmodel.MessageViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory

/**
 * Reached from Login's "Forgot password?" -- replaces the old snackbar (which just told you to
 * "contact your admin" with no actual way to do it) with a real inbox message: type your ID
 * (since you're not signed in), pick which admin to send it to (the list grows automatically as
 * more admin accounts are added, since it's read straight from UserViewModel.users), and write
 * what you need. Not real-time chat -- the admin sees it in their Manage Messages inbox and
 * replies there, with a manual Refresh button to check for replies later.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactAdminScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val messageViewModel: MessageViewModel = viewModel(factory = MessageViewModelFactory(db.messageDao()))
    val users by userViewModel.users.collectAsState()
    val admins = users.filter { it.role == UserRole.ADMIN }

    var userId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedAdmin by remember { mutableStateOf<UserEntity?>(null) }
    var body by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Admin") },
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
            Text(
                "Forgot your password, or need help with something else? Send a message to " +
                    "one of the community admins below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it; message = null },
                label = { Text("Your User ID") },
                singleLine = true,
                enabled = !sent,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (!sent) expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedAdmin?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !sent,
                    label = { Text("To (Admin)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (admins.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No admins available") },
                            onClick = { expanded = false }
                        )
                    }
                    admins.forEach { admin ->
                        DropdownMenuItem(
                            text = { Text("${admin.name} (${admin.id})") },
                            onClick = {
                                selectedAdmin = admin
                                expanded = false
                                message = null
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it; message = null },
                label = { Text("Message") },
                placeholder = { Text("e.g. I forgot my password, can you reset it for me?") },
                minLines = 4,
                enabled = !sent,
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
                    val id = userId.trim()
                    val admin = selectedAdmin
                    val text = body.trim()
                    when {
                        id.isEmpty() -> {
                            isError = true
                            message = "Enter your User ID"
                        }
                        users.none { it.id == id } -> {
                            isError = true
                            message = "No account found with that ID"
                        }
                        admin == null -> {
                            isError = true
                            message = "Choose which admin to send this to"
                        }
                        text.isEmpty() -> {
                            isError = true
                            message = "Write a message"
                        }
                        else -> {
                            messageViewModel.send(fromUserId = id, toUserId = admin.id, body = text)
                            isError = false
                            sent = true
                            message = "Message sent to ${admin.name}. They'll get back to you."
                        }
                    }
                },
                enabled = !sent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (sent) "Sent" else "Send Message")
            }
        }
    }
}
