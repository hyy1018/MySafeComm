// Who's currently logged in. In-memory only, so a fresh app launch always lands on Login.
package com.example.asgm.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.data.local.entity.UserRole

object UserSession {
    var currentUserId by mutableStateOf<String?>(null)
        private set
    var currentUserName by mutableStateOf<String?>(null)
        private set
    var currentUserRole by mutableStateOf<UserRole?>(null)
        private set

    val isLoggedIn: Boolean get() = currentUserId != null

    fun login(user: UserEntity) {
        currentUserId = user.id
        currentUserName = user.name
        currentUserRole = user.role
    }

    fun logout() {
        currentUserId = null
        currentUserName = null
        currentUserRole = null
    }

    // for screens behind the Login gate, where a session is guaranteed to exist
    fun requireUserId(): String = currentUserId ?: error("No user is logged in")
}
