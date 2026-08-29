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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.PostEntity
import kotlinx.coroutines.flow.emptyFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** User screen: the community's Reddit/Facebook-style feed. Admin edit is deferred until Login/roles exist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val postDao = db.postDao()
    val posts by postDao.getAll().collectAsState(initial = emptyList())

    val userId = UserSession.currentUserId
    val currentUser by (
        if (userId != null) db.userDao().observeById(userId) else emptyFlow()
    ).collectAsState(initial = null)
    val unseenActivityCount by (
        if (userId != null) {
            postDao.getUnseenActivityCount(userId, currentUser?.lastSeenActivityAt ?: 0)
        } else {
            emptyFlow()
        }
    ).collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Feed") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("activity") }) {
                        if (unseenActivityCount > 0) {
                            BadgedBox(badge = { Badge { Text("$unseenActivityCount") } }) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Activity")
                            }
                        } else {
                            Icon(Icons.Filled.FavoriteBorder, contentDescription = "Activity")
                        }
                    }
                    IconButton(onClick = { navController.navigate("members") }) {
                        Icon(Icons.Filled.People, contentDescription = "Community Members")
                    }
                }
            )
        },
        bottomBar = { AppBottomBar(navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("community_new") }) {
                Icon(Icons.Filled.Add, contentDescription = "New post")
            }
        }
    ) { innerPadding ->
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No posts yet. Tap + to share something with your neighbours.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts, key = { it.postId }) { post ->
                    PostCard(
                        post = post,
                        onClick = { navController.navigate("community_post/${post.postId}") },
                        onAuthorClick = { navController.navigate("profile/${post.userId}") }
                    )
                }
            }
        }
    }
}

private val postDateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

@Composable
private fun PostCard(post: PostEntity, onClick: () -> Unit, onAuthorClick: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val likeCount by db.likeDao().getLikeCount(post.postId).collectAsState(initial = 0)
    val comments by db.commentDao().getByPost(post.postId).collectAsState(initial = emptyList())
    val author by db.userDao().observeById(post.userId).collectAsState(initial = null)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    author?.name ?: post.userId,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.clickable(onClick = onAuthorClick)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    postDateFormat.format(Date(post.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            if (post.isEdited) {
                Text(
                    "(edited by admin)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text("$likeCount", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Filled.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text("${comments.size}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
