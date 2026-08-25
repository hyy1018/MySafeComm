package com.example.asgm.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.data.local.entity.UserRole

/**
 * Who is currently logged in, for the lifetime of the app process. In-memory only (no
 * "remember me" persistence) -- the app always starts back at Login after a fresh process
 * start. Backed by [mutableStateOf] so any composable reading these properties recomposes
 * when Login/Logout changes them, the same way it would react to a database Flow.
 */
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

    /** The logged-in user's id. Screens behind the Login gate can rely on this being non-null. */
    fun requireUserId(): String = currentUserId ?: error("No user is logged in")
}
