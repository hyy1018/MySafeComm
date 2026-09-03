// #member1
// One emergency contact shown in SOS. Same class doubles as the Supabase row model.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "emergency_contacts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ownerId")]
)
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val serviceId: Long = 0,
    val name: String,
    val phoneNo: String,
    val categoryEmergency: String, // short line shown under the name, e.g. "Floods, trees, snakes"
    // null = community contact (admin-managed / seeded, everyone sees it);
    // a user id = a private contact only that resident sees. Capped at 4 per resident in the UI.
    val ownerId: String? = null
)
