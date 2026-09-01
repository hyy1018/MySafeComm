package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.MessageDao
import com.example.asgm.data.local.entity.MessageEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// UI -> ViewModel -> Dao (local Room) + Supabase (cloud) -> Database.
// Backs sending a message (from ContactAdminScreen or a reply in MessageThreadScreen) and
// pulling in anything the cloud has that this device doesn't -- see refreshFromCloud.
class MessageViewModel(private val dao: MessageDao) : ViewModel() {

    fun send(fromUserId: String, toUserId: String, body: String) = viewModelScope.launch {
        val message = MessageEntity(fromUserId = fromUserId, toUserId = toUserId, body = body)
        val newId = dao.insert(message)
        if (isSupabaseConfigured) {
            try {
                supabase.from("messages").insert(message.copy(messageId = newId))
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    /**
     * "Refresh" button per the teacher's suggestion that manual refresh is an acceptable
     * substitute for real-time chat. Locally, Room's Flow already updates instantly -- this
     * button exists for the case a manual poll actually matters: the other party sent/replied
     * from a different device/install, so it only exists in Supabase, not yet in this device's
     * local Room copy. Uses two plain eq() queries rather than one OR'd query, since that's the
     * filter style Practical 9 actually demonstrates.
     */
    fun refreshFromCloud(userId: String) = viewModelScope.launch {
        if (!isSupabaseConfigured) return@launch
        try {
            val sent = supabase.from("messages")
                .select { filter { eq("fromUserId", userId) } }
                .decodeList<MessageEntity>()
            val received = supabase.from("messages")
                .select { filter { eq("toUserId", userId) } }
                .decodeList<MessageEntity>()
            dao.insertAll(sent + received)
        } catch (e: Exception) {
            // Offline, or not configured yet -- local data just stays as it is.
        }
    }
}

class MessageViewModelFactory(private val dao: MessageDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/** Backs one thread view: every message between two specific people. */
class MessageThreadViewModel(dao: MessageDao, userA: String, userB: String) : ViewModel() {
    val messages: StateFlow<List<MessageEntity>> = dao.getThread(userA, userB)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class MessageThreadViewModelFactory(
    private val dao: MessageDao,
    private val userA: String,
    private val userB: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageThreadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageThreadViewModel(dao, userA, userB) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/** Backs AdminMessagesScreen's inbox list: who has messaged this admin. */
class AdminInboxViewModel(dao: MessageDao, adminId: String) : ViewModel() {
    val partnerIds: StateFlow<List<String>> = dao.getConversationPartnerIds(adminId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class AdminInboxViewModelFactory(
    private val dao: MessageDao,
    private val adminId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminInboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminInboxViewModel(dao, adminId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
