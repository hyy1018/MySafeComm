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

    /** Distinct other-party ids this user has exchanged messages with, for the inbox list. */
    @Query(
        "SELECT DISTINCT CASE WHEN fromUserId = :userId THEN toUserId ELSE fromUserId END " +
            "FROM messages WHERE fromUserId = :userId OR toUserId = :userId"
    )
    fun getConversationPartnerIds(userId: String): Flow<List<String>>

    /** Drives the Messages unread badge: messages sent TO this user (not by themselves) since
     * they last opened their inbox -- same shape as PostDao.getUnseenActivityCount. */
    @Query(
        "SELECT COUNT(*) FROM messages WHERE toUserId = :userId AND fromUserId != :userId " +
            "AND timestamp > :since"
    )
    fun getUnseenMessageCount(userId: String, since: Long): Flow<Int>
}
