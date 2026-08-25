package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_guides")
data class SafetyGuideEntity(
    @PrimaryKey(autoGenerate = true) val guideId: Long = 0,
    val categorySafety: String,
    /** Ordered steps, one per line, each formatted as "Title||Description". */
    val steps: String
)
