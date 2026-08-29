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
    val phone: String = "",
    val address: String = "",
    val email: String = "",
    val avatarUri: String? = null,
    /** When this user last opened the Community activity feed -- drives the unread badge. */
    val lastSeenActivityAt: Long = 0
)
