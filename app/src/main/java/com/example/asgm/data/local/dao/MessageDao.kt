// #member1
// Room queries for chat messages: threads, inbox list, and the unread-messages badge.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.asgm.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity): Long

    // bulk merge from the cloud pull
    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    // for merging messages pulled from Supabase (MessageViewModel.refreshFromCloud) -- IGNORE
    // skips a message this device already has instead of erroring on the duplicate id
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<MessageEntity>)

    // caller enforces sender-only editing -- the DAO itself doesn't know who's asking
    @Update
    suspend fun update(message: MessageEntity)

    // one shared row per message, not a per-user copy -- deleting it removes it for both sides
    @Delete
    suspend fun delete(message: MessageEntity)

    // every message between these two people, either direction, oldest first
    @Query(
        "SELECT * FROM messages WHERE (fromUserId = :userA AND toUserId = :userB) " +
            "OR (fromUserId = :userB AND toUserId = :userA) ORDER BY timestamp ASC"
    )
    fun getThread(userA: String, userB: String): Flow<List<MessageEntity>>

    // distinct other-party ids this user has messaged, for the inbox list
    @Query(
        "SELECT DISTINCT CASE WHEN fromUserId = :userId THEN toUserId ELSE fromUserId END " +
            "FROM messages WHERE fromUserId = :userId OR toUserId = :userId"
    )
    fun getConversationPartnerIds(userId: String): Flow<List<String>>

    // messages sent TO this user since they last opened Messages -- drives the unread badge
    @Query(
        "SELECT COUNT(*) FROM messages WHERE toUserId = :userId AND fromUserId != :userId " +
            "AND timestamp > :since"
    )
    fun getUnseenMessageCount(userId: String, since: Long): Flow<Int>
}
