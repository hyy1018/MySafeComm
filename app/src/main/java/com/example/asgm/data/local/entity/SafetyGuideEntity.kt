package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_guides")
data class SafetyGuideEntity(
    @PrimaryKey(autoGenerate = true) val guideId: Long = 0,
    val categorySafety: String,
    /** Ordered guide steps, one step per line. */
    val steps: String
)
