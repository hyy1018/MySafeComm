// #member1
// Community notice shown in Live Alerts. Same class doubles as the Supabase row model.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class AlertPriority {
    INFO, URGENT
}

@Serializable
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val alertId: Long = 0,
    val title: String,
    val body: String,
    val priority: AlertPriority = AlertPriority.INFO,
    val location: String = "", // e.g. "Oakwood Sector, Blocks A-G"
    val issuedBy: String = "", // set once at creation, not user-editable
    val timestamp: Long = System.currentTimeMillis()
)
