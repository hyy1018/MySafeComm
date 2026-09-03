// A Community Feed post. isEdited/editedByAdminId track if an admin changed the content.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

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
