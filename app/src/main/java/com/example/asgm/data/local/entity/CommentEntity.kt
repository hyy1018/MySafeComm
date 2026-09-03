package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// @Serializable lets this same class double as the row model for the Supabase "comments" table.
@Serializable
@Entity(
    tableName = "comments",
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
    indices = [Index("postId"), Index("userId")]
)
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val commentId: Long = 0,
    val postId: Long,
    val userId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** Null for a top-level comment; set to the parent's commentId for an IG-style reply.
     * One level deep only -- a reply can't itself be replied to. */
    val parentCommentId: Long? = null
)
