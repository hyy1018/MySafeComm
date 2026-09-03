// #member4
// ViewModels for Community Feed: posts, comments/replies, likes, and the Activity feed.
package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.CommentDao
import com.example.asgm.data.local.dao.LikeDao
import com.example.asgm.data.local.dao.PostDao
import com.example.asgm.data.local.entity.CommentEntity
import com.example.asgm.data.local.entity.LikeEntity
import com.example.asgm.data.local.entity.PostEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Backs the Community Feed post list, new-post submission, and Admin's post editing.
class PostViewModel(private val dao: PostDao) : ViewModel() {

    val posts: StateFlow<List<PostEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // suspend so NewPostScreen can await the new id before navigating back
    suspend fun submit(post: PostEntity): Long {
        val newId = dao.insert(post)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("posts").insert(post.copy(postId = newId))
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
        return newId
    }

    fun editByAdmin(postId: Long, content: String, adminId: String) = viewModelScope.launch {
        dao.editByAdmin(postId, content, adminId)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("posts").update({
                        set("content", content)
                        set("isEdited", true)
                        set("editedByAdminId", adminId)
                    }) {
                        filter { eq("postId", postId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }
}

class PostViewModelFactory(private val dao: PostDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs PostDetailScreen: one post, its comments, and its like count. userId is passed per call
// (addComment/like/unlike) rather than in the constructor, so building this never needs a session.
class PostDetailViewModel(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val likeDao: LikeDao,
    private val postId: Long
) : ViewModel() {

    val post: StateFlow<PostEntity?> = postDao.getById(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val comments: StateFlow<List<CommentEntity>> = commentDao.getByPost(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likeCount: StateFlow<Int> = likeDao.getLikeCount(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addComment(userId: String, content: String, parentCommentId: Long? = null) = viewModelScope.launch {
        val comment = CommentEntity(
            postId = postId,
            userId = userId,
            content = content,
            parentCommentId = parentCommentId
        )
        val newId = commentDao.insert(comment)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("comments").insert(comment.copy(commentId = newId))
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun deleteComment(comment: CommentEntity) = viewModelScope.launch {
        commentDao.delete(comment)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("comments").delete { filter { eq("commentId", comment.commentId) } }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
            }
        }
    }

    fun deletePost(post: PostEntity) = viewModelScope.launch {
        postDao.delete(post)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("posts").delete { filter { eq("postId", post.postId) } }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
            }
        }
    }

    fun like(userId: String) = viewModelScope.launch {
        val like = LikeEntity(postId = postId, userId = userId)
        likeDao.like(like)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("likes").insert(like)
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    fun unlike(userId: String) = viewModelScope.launch {
        likeDao.unlike(postId, userId)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("likes").delete {
                        filter {
                            eq("postId", postId)
                            eq("userId", userId)
                        }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
            }
        }
    }
}

class PostDetailViewModelFactory(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val likeDao: LikeDao,
    private val postId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostDetailViewModel(postDao, commentDao, likeDao, postId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs one post row's "did I like this" state.
class PostLikeViewModel(
    private val dao: LikeDao,
    private val postId: Long,
    private val userId: String
) : ViewModel() {

    val liked: StateFlow<Boolean> = dao.isLikedByUser(postId, userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}

class PostLikeViewModelFactory(
    private val dao: LikeDao,
    private val postId: Long,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostLikeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostLikeViewModel(dao, postId, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs ActivityScreen: likes and comments on the signed-in user's own posts.
class ActivityViewModel(
    commentDao: CommentDao,
    likeDao: LikeDao,
    userId: String
) : ViewModel() {

    val likes: StateFlow<List<LikeEntity>> = likeDao.getLikesOnUserPosts(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val comments: StateFlow<List<CommentEntity>> = commentDao.getCommentsOnUserPosts(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class ActivityViewModelFactory(
    private val commentDao: CommentDao,
    private val likeDao: LikeDao,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(commentDao, likeDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
