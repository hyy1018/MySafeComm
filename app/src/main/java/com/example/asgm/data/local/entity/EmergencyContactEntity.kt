package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val serviceId: Long = 0,
    val name: String,
    val phoneNo: String,
    val categoryEmergency: String
)
