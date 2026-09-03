// A comment on a post, or a reply to one (see parentCommentId). Doubles as the Supabase row model.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

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
    val parentCommentId: Long? = null // null = top-level comment, set = reply (one level deep)
)
