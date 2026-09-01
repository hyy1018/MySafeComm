package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Community Feed addon module: user-uploaded posts (Reddit/Facebook-style).
 * Admins may edit a post's content; [isEdited]/[editedByAdminId] record that.
 */
// @Serializable lets this same class double as the row model for the Supabase "posts" table.
@Serializable
@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["editedByAdminId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("editedByAdminId")]
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val postId: Long = 0,
    val userId: String,
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val editedByAdminId: String? = null
)
