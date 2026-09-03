// One emergency contact shown in SOS. Same class doubles as the Supabase row model.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val serviceId: Long = 0,
    val name: String,
    val phoneNo: String,
    val categoryEmergency: String // short line shown under the name, e.g. "Floods, trees, snakes"
)
