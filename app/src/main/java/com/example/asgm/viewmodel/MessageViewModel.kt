// #member1
// ViewModels for chat messages: sending, thread view, and the inbox list.
package com.example.asgm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.asgm.data.local.dao.ConversationReadDao
import com.example.asgm.data.local.dao.MessageDao
import com.example.asgm.data.local.entity.ConversationReadEntity
import com.example.asgm.data.local.entity.MessageEntity
import com.example.asgm.data.remote.isSupabaseConfigured
import com.example.asgm.data.remote.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    // Self-only: the caller (MessageThreadScreen) only ever shows this action on a message the
    // signed-in user sent themselves. One shared row per message, so both sides see the edit.
    fun updateMessage(message: MessageEntity) = viewModelScope.launch {
        dao.update(message)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("messages").update({
                        set("body", message.body)
                    }) {
                        filter { eq("messageId", message.messageId) }
                    }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }

    // Self-only, same reasoning as updateMessage above. No "delete for me only" -- removing it
    // here removes it from both people's thread, since there's just the one row.
    fun deleteMessage(message: MessageEntity) = viewModelScope.launch {
        dao.delete(message)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("messages").delete { filter { eq("messageId", message.messageId) } }
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already deleted.
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
class MessageThreadViewModel(
    messageDao: MessageDao,
    private val conversationReadDao: ConversationReadDao,
    private val myUserId: String,
    private val otherUserId: String
) : ViewModel() {
    val messages: StateFlow<List<MessageEntity>> = messageDao.getThread(myUserId, otherUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // stamp this conversation "read up to now" -- called on open and when a new message
    // lands while the thread is on screen, so its inbox badge clears.
    fun markRead() = viewModelScope.launch {
        val read = ConversationReadEntity(
            ownerId = myUserId,
            partnerId = otherUserId,
            lastReadAt = System.currentTimeMillis()
        )
        conversationReadDao.markRead(read)
        if (isSupabaseConfigured) {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("conversation_reads").upsert(read)
                }
            } catch (e: Exception) {
                // Cloud copy failed -- local Room copy already saved.
            }
        }
    }
}

class MessageThreadViewModelFactory(
    private val messageDao: MessageDao,
    private val conversationReadDao: ConversationReadDao,
    private val myUserId: String,
    private val otherUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageThreadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageThreadViewModel(messageDao, conversationReadDao, myUserId, otherUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// Backs MessagesInboxScreen: who this person has exchanged messages with, plus how many unread
// each one has. Same class for an Admin checking resident messages and a resident checking theirs.
class MessagesInboxViewModel(
    messageDao: MessageDao,
    conversationReadDao: ConversationReadDao,
    userId: String
) : ViewModel() {
    val partnerIds: StateFlow<List<String>> = messageDao.getConversationPartnerIds(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadByPartner: StateFlow<Map<String, Int>> = conversationReadDao.unreadByPartner(userId)
        .map { rows -> rows.associate { it.partnerId to it.unread } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}

class MessagesInboxViewModelFactory(
    private val messageDao: MessageDao,
    private val conversationReadDao: ConversationReadDao,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessagesInboxViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessagesInboxViewModel(messageDao, conversationReadDao, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
