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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.CommentEntity
import com.example.asgm.data.local.entity.UserRole
import com.example.asgm.viewmodel.PostDetailViewModel
import com.example.asgm.viewmodel.PostDetailViewModelFactory
import com.example.asgm.viewmodel.PostLikeViewModel
import com.example.asgm.viewmodel.PostLikeViewModelFactory
import com.example.asgm.viewmodel.PostViewModel
import com.example.asgm.viewmodel.PostViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

/** User screen: a single post with its comments and like button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(postId: Long, navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    val detailViewModel: PostDetailViewModel = viewModel(
        factory = PostDetailViewModelFactory(db.postDao(), db.commentDao(), db.likeDao(), postId)
    )
    val postViewModel: PostViewModel = viewModel(factory = PostViewModelFactory(db.postDao()))
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))

    val post by detailViewModel.post.collectAsState()
    val comments by detailViewModel.comments.collectAsState()
    val likeCount by detailViewModel.likeCount.collectAsState()
    val users by userViewModel.users.collectAsState()
    // Nullable, not requireUserId(): see MyReportsScreen for why -- must not throw during a
    // transient no-session composition.
    val currentUserId = UserSession.currentUserId
    val likeViewModel: PostLikeViewModel? = if (currentUserId != null) {
        viewModel(factory = PostLikeViewModelFactory(db.likeDao(), postId, currentUserId))
    } else {
        null
    }
    val liked = likeViewModel?.liked?.collectAsState()?.value ?: false
    val author = users.find { it.id == post?.userId }

    var commentText by remember { mutableStateOf("") }
    var showDeletePostConfirm by remember { mutableStateOf(false) }
    var commentPendingDelete by remember { mutableStateOf<CommentEntity?>(null) }
    var editedContent by remember { mutableStateOf<String?>(null) }
    var replyingTo by remember { mutableStateOf<CommentEntity?>(null) }

    val topLevelComments = comments.filter { it.parentCommentId == null }
    val repliesByParent = comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId }

    val isOwnPost = post?.userId == UserSession.currentUserId
    val isAdmin = UserSession.currentUserRole == UserRole.ADMIN
    // Admin gets the same delete-post/delete-comment powers as the post's own author, plus the
    // ability to edit content -- same "feel" as viewing your own post, per the brief.
    val canManage = isOwnPost || isAdmin

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { editedContent = post?.content ?: "" }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit post")
                        }
                    }
                    if (canManage) {
                        IconButton(onClick = { showDeletePostConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete post")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                replyingTo?.let { target ->
                    val targetName = users.find { it.id == target.userId }?.name ?: target.userId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Replying to $targetName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { replyingTo = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel reply")
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(if (replyingTo != null) "Add a reply..." else "Add a comment...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val text = commentText.trim()
                            if (text.isNotEmpty()) {
                                detailViewModel.addComment(
                                    UserSession.requireUserId(),
                                    text,
                                    parentCommentId = replyingTo?.commentId
                                )
                                commentText = ""
                                replyingTo = null
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
                    }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                author?.name ?: currentPost.userId,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.clickable {
                                    navController.navigate("profile/${currentPost.userId}")
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                dateFormat.format(Date(currentPost.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                    val userId = UserSession.requireUserId()
                                    if (liked) detailViewModel.unlike(userId) else detailViewModel.like(userId)
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
                items(topLevelComments, key = { it.commentId }) { comment ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommentRow(
                            comment = comment,
                            authorName = users.find { it.id == comment.userId }?.name ?: comment.userId,
                            canDelete = canManage,
                            onDelete = { commentPendingDelete = comment },
                            onAuthorClick = { navController.navigate("profile/${comment.userId}") },
                            onReply = { replyingTo = comment }
                        )
                        repliesByParent[comment.commentId]?.forEach { reply ->
                            Box(modifier = Modifier.padding(start = 32.dp)) {
                                CommentRow(
                                    comment = reply,
                                    authorName = users.find { it.id == reply.userId }?.name ?: reply.userId,
                                    canDelete = canManage,
                                    onDelete = { commentPendingDelete = reply },
                                    onAuthorClick = { navController.navigate("profile/${reply.userId}") },
                                    onReply = null
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeletePostConfirm && currentPost != null) {
            AlertDialog(
                onDismissRequest = { showDeletePostConfirm = false },
                title = { Text("Delete post?") },
                text = { Text("This removes the post and its comments and likes. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        detailViewModel.deletePost(currentPost)
                        showDeletePostConfirm = false
                        navController.popBackStack()
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePostConfirm = false }) { Text("Cancel") }
                }
            )
        }

        commentPendingDelete?.let { comment ->
            AlertDialog(
                onDismissRequest = { commentPendingDelete = null },
                title = { Text("Delete comment?") },
                text = { Text("This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        detailViewModel.deleteComment(comment)
                        commentPendingDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { commentPendingDelete = null }) { Text("Cancel") }
                }
            )
        }

        editedContent?.let { draft ->
            AlertDialog(
                onDismissRequest = { editedContent = null },
                title = { Text("Edit post") },
                text = {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { editedContent = it },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val content = draft.trim()
                            if (content.isNotEmpty()) {
                                postViewModel.editByAdmin(postId, content, UserSession.requireUserId())
                                editedContent = null
                            }
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { editedContent = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun CommentRow(
    comment: CommentEntity,
    authorName: String,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onAuthorClick: () -> Unit,
    onReply: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (comment.userId == UserSession.currentUserId) "You" else authorName,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable(onClick = onAuthorClick)
            )
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            if (onReply != null) {
                Text(
                    "Reply",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onReply)
                )
            }
        }
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete comment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
