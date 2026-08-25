package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Records that a specific resident tapped "Confirm Acknowledgment" on an urgent alert.
 * Composite primary key (alertId, userId) mirrors [LikeEntity]: one acknowledgement per
 * user per alert, so one resident confirming doesn't mark it confirmed for everyone else.
 */
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
