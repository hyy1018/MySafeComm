package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * "Contact Admin" messaging addon: a simple inbox, not real-time chat, replacing the old
 * "Forgot password" snackbar with an actual way to reach someone. [fromUserId]/[toUserId] are
 * generic (not fixed to "resident -> admin") so an admin's reply is just another row with the
 * two ids swapped -- the same table backs both directions of a conversation.
 */
// @Serializable lets this same class double as the row model for the Supabase "messages" table.
@Serializable
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromUserId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["toUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fromUserId"), Index("toUserId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val fromUserId: String,
    val toUserId: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)
