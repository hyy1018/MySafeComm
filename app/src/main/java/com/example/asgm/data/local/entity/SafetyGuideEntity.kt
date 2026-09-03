// One safety guide category (Fire, Flood, etc). Same class doubles as the Supabase row model.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "safety_guides")
data class SafetyGuideEntity(
    @PrimaryKey(autoGenerate = true) val guideId: Long = 0,
    val categorySafety: String,
    val steps: String // ordered steps, one per line, each "Title||Description"
)
