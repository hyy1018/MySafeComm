// #member1
// How far a person has read one conversation. Local only -- read state doesn't need to sync.
// Same "who did what" shape as AlertAcknowledgementEntity.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "conversation_reads",
    primaryKeys = ["ownerId", "partnerId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["partnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partnerId")]
)
data class ConversationReadEntity(
    val ownerId: String,   // whose inbox this read-marker belongs to
    val partnerId: String, // the other person in the conversation
    val lastReadAt: Long   // messages from partnerId newer than this count as unread
)
