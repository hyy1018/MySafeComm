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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.viewmodel.ActivityViewModel
import com.example.asgm.viewmodel.ActivityViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ActivityType { LIKE, COMMENT }

private data class ActivityItem(
    val actorId: String,
    val postId: Long,
    val type: ActivityType,
    val commentText: String?,
    val timestamp: Long
)

private val activityDateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

/**
 * Instagram-style "activity" feed: who liked or commented on your own posts. Not a real push
 * notification -- just an in-app list, plus a red badge (Community bottom-nav tab and the heart
 * icon here) driven by PostDao.getUnseenActivityCount, cleared the moment this screen opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val userId = UserSession.requireUserId()

    val activityViewModel: ActivityViewModel =
        viewModel(factory = ActivityViewModelFactory(db.commentDao(), db.likeDao(), userId))
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val likes by activityViewModel.likes.collectAsState()
    val comments by activityViewModel.comments.collectAsState()
    val users by userViewModel.users.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.updateLastSeenActivity(userId, System.currentTimeMillis())
    }

    val items = remember(likes, comments) {
        (likes.map { ActivityItem(it.userId, it.postId, ActivityType.LIKE, null, it.timestamp) } +
            comments.map { ActivityItem(it.userId, it.postId, ActivityType.COMMENT, it.content, it.timestamp) })
            .sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No activity yet. Likes and comments on your posts will show up here.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { "${it.type}_${it.postId}_${it.actorId}_${it.timestamp}" }) { item ->
                    ActivityRow(item, users) { navController.navigate("community_post/${item.postId}") }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, users: List<UserEntity>, onClick: () -> Unit) {
    val actor = users.find { it.id == item.actorId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (actor?.avatarUri != null) {
                    AsyncImage(
                        model = actor?.avatarUri,
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
            Icon(
                imageVector = if (item.type == ActivityType.LIKE) Icons.Filled.Favorite else Icons.Filled.ChatBubble,
                contentDescription = null,
                tint = if (item.type == ActivityType.LIKE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                val actorName = actor?.name ?: item.actorId
                Text(
                    text = if (item.type == ActivityType.LIKE) {
                        "$actorName liked your post"
                    } else {
                        "$actorName commented on your post"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.type == ActivityType.COMMENT && item.commentText != null) {
                    Text(
                        "\"${item.commentText}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    activityDateFormat.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
