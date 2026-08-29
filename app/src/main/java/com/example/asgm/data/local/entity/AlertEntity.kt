package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertPriority {
    INFO, URGENT
}

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val alertId: Long = 0,
    val title: String,
    val body: String,
    val priority: AlertPriority = AlertPriority.INFO,
    /** Where the notice applies, e.g. "Oakwood Sector, Blocks A-G". */
    val location: String = "",
    /** Who posted it, e.g. an admin's display name. Set once at creation; not user-editable. */
    val issuedBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
