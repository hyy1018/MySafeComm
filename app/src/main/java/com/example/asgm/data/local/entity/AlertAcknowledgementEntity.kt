// #member1
// Tracks which resident confirmed which urgent alert. Composite key = one ack per user per alert.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "alert_acknowledgements",
    primaryKeys = ["alertId", "userId"],
    foreignKeys = [
        ForeignKey(
            entity = AlertEntity::class,
            parentColumns = ["alertId"],
            childColumns = ["alertId"],
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
data class AlertAcknowledgementEntity(
    val alertId: Long,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
