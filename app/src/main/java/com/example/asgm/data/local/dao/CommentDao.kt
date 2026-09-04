// #member1
// Room queries for post comments/replies, plus comments-on-your-posts for the Activity feed.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.asgm.data.local.entity.CommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Insert
    suspend fun insert(comment: CommentEntity): Long

    // bulk merge from the cloud pull
    @Upsert
    suspend fun upsertAll(comments: List<CommentEntity>)

    @Update
    suspend fun update(comment: CommentEntity)

    @Delete
    suspend fun delete(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getByPost(postId: Long): Flow<List<CommentEntity>>

    // comments on this person's own posts, excluding their own comments
    @Query(
        "SELECT c.* FROM comments c JOIN posts p ON c.postId = p.postId " +
            "WHERE p.userId = :postOwnerId AND c.userId != :postOwnerId ORDER BY c.timestamp DESC"
    )
    fun getCommentsOnUserPosts(postOwnerId: String): Flow<List<CommentEntity>>
}
