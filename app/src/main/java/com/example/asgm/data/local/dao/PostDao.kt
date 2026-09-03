// Room queries for Community Feed posts, plus the unseen-activity-count for the Activity badge.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.asgm.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert
    suspend fun insert(post: PostEntity): Long

    @Delete
    suspend fun delete(post: PostEntity)

    // admin edits a post's content, marking it edited and recording who did it
    @Query(
        "UPDATE posts SET content = :content, isEdited = 1, editedByAdminId = :adminId " +
            "WHERE postId = :postId"
    )
    suspend fun editByAdmin(postId: Long, content: String, adminId: String)

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE postId = :postId LIMIT 1")
    fun getById(postId: Long): Flow<PostEntity?>

    // likes+comments on this person's own posts (excluding self-actions) since they last checked
    @Query(
        "SELECT COUNT(*) FROM (" +
            "SELECT l.timestamp AS ts FROM likes l JOIN posts p ON l.postId = p.postId " +
            "WHERE p.userId = :postOwnerId AND l.userId != :postOwnerId AND l.timestamp > :since " +
            "UNION ALL " +
            "SELECT c.timestamp AS ts FROM comments c JOIN posts p ON c.postId = p.postId " +
            "WHERE p.userId = :postOwnerId AND c.userId != :postOwnerId AND c.timestamp > :since" +
            ")"
    )
    fun getUnseenActivityCount(postOwnerId: String, since: Long): Flow<Int>
}
