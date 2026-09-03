// #member1
// Resident's home screen: quick-post bar plus the module cards (Report, Alert, Comm, SOS).
package com.example.asgm.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.asgm.data.MainHubData
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.PostEntity
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.model.MainHubItem
import com.example.asgm.viewmodel.PostViewModel
import com.example.asgm.viewmodel.PostViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    navController: NavHostController,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userId = UserSession.currentUserId
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val postViewModel: PostViewModel = viewModel(factory = PostViewModelFactory(db.postDao()))
    val users by userViewModel.users.collectAsState()
    val currentUser = users.find { it.id == userId }
    val scope = rememberCoroutineScope()
    // Same badge computation as Community Feed's People icon -- unread direct messages.
    val unseenMessageCount by (
        if (userId != null) {
            db.messageDao().getUnseenMessageCount(userId, currentUser?.lastSeenMessagesAt ?: 0)
        } else {
            emptyFlow()
        }
    ).collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Safe Community") },
                actions = {
                    IconButton(onClick = { navController.navigate("search") }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(
                        onClick = { userId?.let { navController.navigate("messages_inbox/$it") } }
                    ) {
                        if (unseenMessageCount > 0) {
                            BadgedBox(badge = { Badge { Text("$unseenMessageCount") } }) {
                                Icon(Icons.Filled.MailOutline, contentDescription = "My Messages")
                            }
                        } else {
                            Icon(Icons.Filled.MailOutline, contentDescription = "My Messages")
                        }
                    }
                    IconButton(
                        onClick = {
                            userId?.let { navController.navigate("profile/$it") }
                        }
                    ) {
                        val avatarUri = currentUser?.avatarUri
                        if (avatarUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            ) {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                        }
                    }
                }
            )
        },
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "NEIGHBORHOOD WATCH",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Building safer, resilient neighborhoods",
                style = MaterialTheme.typography.headlineSmall
            )
            UserSession.currentUserName?.let { name ->
                Text(
                    text = "Welcome, $name",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Stay informed, report hazards, and connect with your local " +
                    "emergency responders in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
            QuickPostCard(
                currentUser = currentUser,
                onPost = { content ->
                    scope.launch {
                        // submit() suspends until the post is saved -- only then open the feed
                        postViewModel.submit(
                            PostEntity(userId = UserSession.requireUserId(), content = content)
                        )
                        onNavigate("community")
                    }
                }
            )
            Spacer(Modifier.size(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(MainHubData.modules, key = { it.id }) { module ->
                    MainHubModuleCard(module = module, onClick = { onNavigate(module.route) })
                }
            }
        }
    }
}

// type + Send posts to the Community Feed and then opens it; text-only, a photo still goes
// through the full New Post screen. Send is disabled and does nothing until there's some text.
@Composable
private fun QuickPostCard(currentUser: UserEntity?, onPost: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val avatarUri = currentUser?.avatarUri
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Your avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Your avatar",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("What's happening in your community?") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val content = text.trim()
                    if (content.isNotEmpty()) {
                        onPost(content)
                        text = ""
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post")
            }
        }
    }
}

// each module gets its own accent color so the list reads as distinct sections
@Composable
private fun moduleAccent(moduleId: String): Pair<Color, Color> =
    when (moduleId) {
        "report" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        "alert" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "community" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "sos" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun MainHubModuleCard(module: MainHubItem, onClick: () -> Unit) {
    val (containerColor, contentColor) = moduleAccent(module.id)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(module.title, style = MaterialTheme.typography.titleMedium)
                    if (module.isLive) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
