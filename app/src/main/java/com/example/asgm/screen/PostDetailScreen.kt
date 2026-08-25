package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.CommentEntity
import com.example.asgm.data.local.entity.LikeEntity
import kotlinx.coroutines.launch

/** User screen: a single post with its comments and like button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(postId: Long, navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    val post by db.postDao().getById(postId).collectAsState(initial = null)
    val comments by db.commentDao().getByPost(postId).collectAsState(initial = emptyList())
    val likeCount by db.likeDao().getLikeCount(postId).collectAsState(initial = 0)
    val liked by db.likeDao().isLikedByUser(postId, UserSession.requireUserId())
        .collectAsState(initial = false)

    var commentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val text = commentText.trim()
                        if (text.isNotEmpty()) {
                            scope.launch {
                                db.commentDao().insert(
                                    CommentEntity(
                                        postId = postId,
                                        userId = UserSession.requireUserId(),
                                        content = text
                                    )
                                )
                                commentText = ""
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
                }
            }
        }
    ) { innerPadding ->
        val currentPost = post
        if (currentPost == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(currentPost.content, style = MaterialTheme.typography.bodyLarge)
                        if (currentPost.isEdited) {
                            Text(
                                "(edited by admin)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (liked) {
                                            db.likeDao().unlike(postId, UserSession.requireUserId())
                                        } else {
                                            db.likeDao().like(
                                                LikeEntity(postId = postId, userId = UserSession.requireUserId())
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text("$likeCount likes")
                        }
                        HorizontalDivider()
                        Text("Comments", style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(comments, key = { it.commentId }) { comment ->
                    CommentRow(comment)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentEntity) {
    Column {
        Text(
            text = if (comment.userId == UserSession.currentUserId) "You" else comment.userId,
            style = MaterialTheme.typography.labelMedium
        )
        Text(comment.content, style = MaterialTheme.typography.bodyMedium)
    }
}
