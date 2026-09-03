// One chat message. fromUserId/toUserId are generic, so this same table covers admin replies
// and resident-to-resident chat -- a reply is just another row with the two ids swapped.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

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
