package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// @Serializable lets this same class double as the row model for the Supabase "emergency_contacts"
// table (Supabase talks JSON), so we don't need a second near-identical data class just for the cloud.
@Serializable
@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val serviceId: Long = 0,
    val name: String,
    val phoneNo: String,
    /** Short description shown under the name, e.g. "Floods, trees, snakes". */
    val categoryEmergency: String
)
