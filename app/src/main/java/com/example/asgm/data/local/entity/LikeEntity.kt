package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * Composite primary key (postId, userId) guarantees a user can like a post at most once.
 */
// @Serializable lets this same class double as the row model for the Supabase "likes" table.
@Serializable
@Entity(
    tableName = "likes",
    primaryKeys = ["postId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["postId"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class LikeEntity(
    val postId: Long,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
