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

    /** Admin edits a post's content; marks it as edited and records who edited it. */
    @Query(
        "UPDATE posts SET content = :content, isEdited = 1, editedByAdminId = :adminId " +
            "WHERE postId = :postId"
    )
    suspend fun editByAdmin(postId: Long, content: String, adminId: String)

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE postId = :postId LIMIT 1")
    fun getById(postId: Long): Flow<PostEntity?>
}
