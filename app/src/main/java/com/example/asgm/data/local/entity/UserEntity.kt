// #member1
// A resident/admin account. Same class doubles as the Supabase row model.
package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    RESIDENT, ADMIN, RESPONDER
}

@Serializable
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val password: String,
    val name: String,
    val role: UserRole,
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val avatarUri: String? = null,
    val lastSeenActivityAt: Long = 0, // last time they opened Activity -- drives its unread badge
    val lastSeenMessagesAt: Long = 0 // last time they opened Messages -- drives its unread badge
)
