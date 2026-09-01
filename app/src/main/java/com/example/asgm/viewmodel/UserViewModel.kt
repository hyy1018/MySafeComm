package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.UserDao
import com.example.asgm.data.local.entity.UserEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// UI -> ViewModel -> Dao (local Room) + Supabase (cloud) -> Database.
// Shared across every screen that needs the user directory or an auth/profile write: login,
// sign up, profile edits, admin account management, the members directory.
class UserViewModel(private val dao: UserDao) : ViewModel() {

    val users: StateFlow<List<UserEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Plain suspend functions (not wrapped in viewModelScope.launch) so the calling screen's own
    // coroutine can await the result before deciding what happens next -- show an error, navigate,
    // etc. -- the same way Practical 9's suspend fetchUsers()/addUser() are awaited from the UI's
    // own scope.launch, instead of firing detached like EmergencyContactViewModel's addContact.
    suspend fun login(id: String, password: String): UserEntity? = dao.login(id, password)

    suspend fun getById(id: String): UserEntity? = dao.getById(id)

    suspend fun signUp(user: UserEntity) {
        dao.insert(user)
        pushInsertToCloud(user)
    }

    suspend fun updateUser(user: UserEntity) {
        dao.update(user)
        pushUpdateToCloud(user)
    }

    suspend fun updateLastSeenActivity(userId: String, timestamp: Long) {
        dao.updateLastSeenActivity(userId, timestamp)
        if (isSupabaseConfigured) {
            try {
                supabase.from("users").update({
                    set("lastSeenActivityAt", timestamp)
                }) {
                    filter { eq("id", userId) }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    suspend fun updateLastSeenMessages(userId: String, timestamp: Long) {
        dao.updateLastSeenMessages(userId, timestamp)
        if (isSupabaseConfigured) {
            try {
                supabase.from("users").update({
                    set("lastSeenMessagesAt", timestamp)
                }) {
                    filter { eq("id", userId) }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }
}

class UserViewModelFactory(private val dao: UserDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs a single user's live profile (own or someone else's) -- ProfileScreen. Takes userId up
// front via its Factory, the same reasoning as SafetyGuideDetailViewModel: the StateFlow for that
// one user is set up once in the ViewModel's body, not re-created on every recomposition.
class UserDetailViewModel(private val dao: UserDao, userId: String) : ViewModel() {

    val user: StateFlow<UserEntity?> = dao.observeById(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(user: UserEntity) = viewModelScope.launch {
        dao.update(user)
        pushUpdateToCloud(user)
    }
}

class UserDetailViewModelFactory(
    private val dao: UserDao,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserDetailViewModel(dao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Shared by UserViewModel and UserDetailViewModel above -- both push the same shape of write to
// Supabase, just triggered from different screens.
private suspend fun pushInsertToCloud(user: UserEntity) {
    if (isSupabaseConfigured) {
        try {
            supabase.from("users").insert(user)
        } catch (e: Exception) {
            // Cloud copy failed -- local Room copy already saved.
        }
    }
}

private suspend fun pushUpdateToCloud(user: UserEntity) {
    if (isSupabaseConfigured) {
        try {
            supabase.from("users").update({
                set("password", user.password)
                set("name", user.name)
                set("role", user.role.name)
                set("phone", user.phone)
                set("address", user.address)
                set("email", user.email)
                set("avatarUri", user.avatarUri)
                set("lastSeenActivityAt", user.lastSeenActivityAt)
                set("lastSeenMessagesAt", user.lastSeenMessagesAt)
            }) {
                filter { eq("id", user.id) }
            }
        } catch (e: Exception) {
            // Cloud copy failed -- local Room copy already saved.
        }
    }
}
