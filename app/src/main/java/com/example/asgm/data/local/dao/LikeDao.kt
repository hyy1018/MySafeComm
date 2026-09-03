// Room queries for post likes, plus likes-on-your-posts for the Activity feed.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.asgm.data.local.entity.LikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun like(like: LikeEntity)

    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun unlike(postId: Long, userId: String)

    @Query("SELECT COUNT(*) FROM likes WHERE postId = :postId")
    fun getLikeCount(postId: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE postId = :postId AND userId = :userId)")
    fun isLikedByUser(postId: Long, userId: String): Flow<Boolean>

    // likes on this person's own posts, excluding their own likes
    @Query(
        "SELECT l.* FROM likes l JOIN posts p ON l.postId = p.postId " +
            "WHERE p.userId = :postOwnerId AND l.userId != :postOwnerId ORDER BY l.timestamp DESC"
    )
    fun getLikesOnUserPosts(postOwnerId: String): Flow<List<LikeEntity>>
}
