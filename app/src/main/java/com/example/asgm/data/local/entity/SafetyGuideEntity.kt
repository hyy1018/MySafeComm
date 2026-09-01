package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// @Serializable lets this same class double as the row model for the Supabase "safety_guides"
// table (Supabase talks JSON), so we don't need a second near-identical data class just for the cloud.
@Serializable
@Entity(tableName = "safety_guides")
data class SafetyGuideEntity(
    @PrimaryKey(autoGenerate = true) val guideId: Long = 0,
    val categorySafety: String,
    /** Ordered steps, one per line, each formatted as "Title||Description". */
    val steps: String
)
