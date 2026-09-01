package com.example.asgm.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.example.asgm.data.UserSession
import com.example.asgm.data.local.AppDatabase
import com.example.asgm.data.local.entity.PostEntity
import com.example.asgm.viewmodel.PostViewModel
import com.example.asgm.viewmodel.PostViewModelFactory
import com.example.asgm.viewmodel.UserViewModel
import com.example.asgm.viewmodel.UserViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

/** Admin screen: edit any Community Feed post's content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPostsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val postViewModel: PostViewModel = viewModel(factory = PostViewModelFactory(db.postDao()))
    val userViewModel: UserViewModel = viewModel(factory = UserViewModelFactory(db.userDao()))
    val posts by postViewModel.posts.collectAsState()
    val users by userViewModel.users.collectAsState()

    var postPendingEdit by remember { mutableStateOf<PostEntity?>(null) }
    var editedContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Posts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No posts yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts, key = { it.postId }) { post ->
                    val authorName = users.find { it.id == post.userId }?.name ?: post.userId
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "$authorName - ${dateFormat.format(Date(post.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(post.content, style = MaterialTheme.typography.bodyMedium)
                            if (post.isEdited) {
                                Text(
                                    "(edited by admin)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    postPendingEdit = post
                                    editedContent = post.content
                                }
                            ) {
                                Text("Edit")
                            }
                        }
                    }
                }
            }
        }

        postPendingEdit?.let { post ->
            AlertDialog(
                onDismissRequest = { postPendingEdit = null },
                title = { Text("Edit post") },
                text = {
                    OutlinedTextField(
                        value = editedContent,
                        onValueChange = { editedContent = it },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val content = editedContent.trim()
                            if (content.isNotEmpty()) {
                                postViewModel.editByAdmin(post.postId, content, UserSession.requireUserId())
                                postPendingEdit = null
                            }
                        }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { postPendingEdit = null }) { Text("Cancel") }
                }
            )
        }
    }
}
