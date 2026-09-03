// Hazard report submitted by a resident. Same class doubles as the Supabase row model.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class ReportStatus {
    PENDING, IN_PROGRESS, SOLVED
}

@Serializable
@Entity(
    tableName = "reports",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val reportId: Long = 0,
    val userId: String,
    val title: String,
    val location: String,
    val description: String,
    val status: ReportStatus = ReportStatus.PENDING,
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
