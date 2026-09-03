// ViewModels for chat messages: sending, thread view, and the inbox list.
package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.MessageDao
import com.example.asgm.data.local.entity.MessageEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageViewModel(private val dao: MessageDao) : ViewModel() {

    fun send(fromUserId: String, toUserId: String, body: String) = viewModelScope.launch {
        val message = MessageEntity(fromUserId = fromUserId, toUserId = toUserId, body = body)
        val newId = dao.insert(message)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("messages").insert(message.copy(messageId = newId))
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    // manual "Refresh" -- Room's Flow already updates live locally, this just pulls in a message
    // that only exists in Supabase because it came from a different device/install
    fun refreshFromCloud(userId: String) = viewModelScope.launch {
        if (!isSupabaseConfigured) return@launch
        try {
            withContext(Dispatchers.IO) {
                val sent = supabase.from("messages")
                    .select { filter { eq("fromUserId", userId) } }
                    .decodeList<MessageEntity>()
                val received = supabase.from("messages")
                    .select { filter { eq("toUserId", userId) } }
                    .decodeList<MessageEntity>()
                dao.insertAll(sent + received)
            }
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

// Backs one thread view: every message between two specific people.
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

// Backs MessagesInboxScreen: who this person has exchanged messages with. Same class for an
// Admin checking resident messages and a resident checking theirs -- it's just a user id.
class MessagesInboxViewModel(dao: MessageDao, userId: String) : ViewModel() {
    val partnerIds: StateFlow<List<String>> = dao.getConversationPartnerIds(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class MessagesInboxViewModelFactory(
    private val dao: MessageDao,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessagesInboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessagesInboxViewModel(dao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
