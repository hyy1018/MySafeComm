// List of who a person has exchanged messages with -- shared by Admin's Messages, a resident's
// My Messages, and the signed-out Check Messages flow.
package com.example.asgm.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.viewmodel.MessageViewModel
import com.example.asgm.viewmodel.MessageViewModelFactory
import com.example.asgm.viewmodel.MessagesInboxViewModel
import com.example.asgm.viewmodel.MessagesInboxViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesInboxScreen(userId: String, navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    val inboxViewModel: MessagesInboxViewModel =
        viewModel(factory = MessagesInboxViewModelFactory(db.messageDao(), userId))
    val messageViewModel: MessageViewModel = viewModel(factory = MessageViewModelFactory(db.messageDao()))
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val partnerIds by inboxViewModel.partnerIds.collectAsState()
    val users by userViewModel.users.collectAsState()

    // Check the cloud once when the inbox opens, in addition to the manual Refresh button, and
    // mark "seen" so the unread badge (wherever this userId's Messages entry point shows one)
    // clears -- same pattern as ActivityScreen's lastSeenActivityAt.
    LaunchedEffect(Unit) {
        messageViewModel.refreshFromCloud(userId)
        userViewModel.updateLastSeenMessages(userId, System.currentTimeMillis())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { messageViewModel.refreshFromCloud(userId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (partnerIds.isEmpty()) {
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
                items(partnerIds, key = { it }) { partnerId ->
                    val partnerName = users.find { it.id == partnerId }?.name ?: partnerId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("message_thread/$userId/$partnerId") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(partnerName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    partnerId,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
