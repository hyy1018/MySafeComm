// Community directory of every account, with a chat icon to message someone directly.
package com.example.asgm.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val users by userViewModel.users.collectAsState()
    val currentUserId = UserSession.currentUserId
    val members = remember(users, currentUserId) {
        val (me, others) = users.partition { it.id == currentUserId }
        me + others
    }

    // Members is where a resident actually reaches per-person chat, so opening it clears the
    // unread-messages badge on Community Feed's People icon -- same pattern as ActivityScreen
    // stamping lastSeenActivityAt for the heart-icon badge.
    LaunchedEffect(currentUserId) {
        currentUserId?.let { userViewModel.updateLastSeenMessages(it, System.currentTimeMillis()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Members") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (members.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No members yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members, key = { it.id }) { member ->
                    val isSelf = member.id == currentUserId
                    MemberRow(
                        member = member,
                        isSelf = isSelf,
                        onClick = { if (!isSelf) navController.navigate("profile/${member.id}") },
                        onChatClick = {
                            navController.navigate("message_thread/${UserSession.requireUserId()}/${member.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: UserEntity,
    isSelf: Boolean,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSelf, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUri != null) {
                    AsyncImage(
                        model = member.avatarUri,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(member.name, style = MaterialTheme.typography.titleSmall)
                    if (isSelf) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "ME",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    member.role.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isSelf) {
                IconButton(onClick = onChatClick) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Message ${member.name}")
                }
            }
        }
    }
}
