package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.asgm.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    // Used to merge messages pulled from Supabase (see MessageViewModel.refreshFromCloud):
    // IGNORE means a message this device already has (same messageId) is silently skipped
    // instead of erroring, while one that only exists in the cloud (e.g. sent from another
    // device/install) gets added.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<MessageEntity>)

    /** Every message between these two people, either direction, oldest first. */
    @Query(
        "SELECT * FROM messages WHERE (fromUserId = :userA AND toUserId = :userB) " +
            "OR (fromUserId = :userB AND toUserId = :userA) ORDER BY timestamp ASC"
    )
    fun getThread(userA: String, userB: String): Flow<List<MessageEntity>>

    /** Distinct other-party ids this admin has exchanged messages with, for the inbox list. */
    @Query(
        "SELECT DISTINCT CASE WHEN fromUserId = :adminId THEN toUserId ELSE fromUserId END " +
            "FROM messages WHERE fromUserId = :adminId OR toUserId = :adminId"
    )
    fun getConversationPartnerIds(adminId: String): Flow<List<String>>
}
