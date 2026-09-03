// #member1
// One conversation between two people, plus a reply box.
package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.MessageEntity
import com.example.asgm.viewmodel.MessageThreadViewModel
import com.example.asgm.viewmodel.MessageThreadViewModelFactory
import com.example.asgm.viewmodel.MessageViewModel
import com.example.asgm.viewmodel.MessageViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val messageDateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageThreadScreen(myUserId: String, otherUserId: String, navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    val threadViewModel: MessageThreadViewModel =
        viewModel(factory = MessageThreadViewModelFactory(db.messageDao(), myUserId, otherUserId))
    val messageViewModel: MessageViewModel = viewModel(factory = MessageViewModelFactory(db.messageDao()))
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val messages by threadViewModel.messages.collectAsState()
    val users by userViewModel.users.collectAsState()
    val otherName = users.find { it.id == otherUserId }?.name ?: otherUserId

    var reply by remember { mutableStateOf("") }
    var messagePendingDelete by remember { mutableStateOf<MessageEntity?>(null) }
    var messageBeingEdited by remember { mutableStateOf<MessageEntity?>(null) }
    var messageEditDraft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { messageViewModel.refreshFromCloud(myUserId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reply,
                    onValueChange = { reply = it },
                    placeholder = { Text("Reply...") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val text = reply.trim()
                        if (text.isNotEmpty()) {
                            messageViewModel.send(fromUserId = myUserId, toUserId = otherUserId, body = text)
                            reply = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply")
                }
            }
        }
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.messageId }) { message ->
                    val isOwn = message.fromUserId == myUserId
                    MessageBubble(
                        message = message,
                        isOwn = isOwn,
                        // Self-only -- the other person's message never gets these buttons, and
                        // there's no "delete for me" here: one shared row, so a delete removes it
                        // from both threads.
                        onEdit = if (isOwn) {
                            { messageBeingEdited = message; messageEditDraft = message.body }
                        } else {
                            null
                        },
                        onDelete = if (isOwn) { { messagePendingDelete = message } } else null
                    )
                }
            }
        }

        messagePendingDelete?.let { message ->
            AlertDialog(
                onDismissRequest = { messagePendingDelete = null },
                title = { Text("Delete message?") },
                text = { Text("This removes it from both sides of the conversation. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        messageViewModel.deleteMessage(message)
                        messagePendingDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { messagePendingDelete = null }) { Text("Cancel") }
                }
            )
        }

        messageBeingEdited?.let { message ->
            AlertDialog(
                onDismissRequest = { messageBeingEdited = null },
                title = { Text("Edit message") },
                text = {
                    OutlinedTextField(
                        value = messageEditDraft,
                        onValueChange = { messageEditDraft = it },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val body = messageEditDraft.trim()
                            if (body.isNotEmpty()) {
                                messageViewModel.updateMessage(message.copy(body = body))
                                messageBeingEdited = null
                            }
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { messageBeingEdited = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    isOwn: Boolean,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.75f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isOwn) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(message.body, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        messageDateFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete message",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
