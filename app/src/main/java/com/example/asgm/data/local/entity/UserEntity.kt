package com.example.asgm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    RESIDENT, ADMIN, RESPONDER
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val password: String,
    val name: String,
    val role: UserRole,
    /** Address / contact info shown on the user's profile. */
    val contact: String,
    val avatarUri: String? = null
)
