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
    val timestamp: Long = System.currentTimeMillis()
)
