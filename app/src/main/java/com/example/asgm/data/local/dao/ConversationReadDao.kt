// #member1
// Room queries for per-conversation unread counts: which person has how many unread messages,
// plus the grand total that drives the Home / Community / Admin mailbox badge.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.asgm.data.local.entity.ConversationReadEntity
import kotlinx.coroutines.flow.Flow

// one row of "this person has N unread"
data class PartnerUnread(val partnerId: String, val unread: Int)

@Dao
interface ConversationReadDao {
    // called when the owner opens a conversation -- stamps "read up to now"
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markRead(read: ConversationReadEntity)

    // bulk merge from the cloud pull
    @Upsert
    suspend fun upsertAll(reads: List<ConversationReadEntity>)

    // unread count grouped by sender: messages TO me, FROM someone else, newer than the last
    // time I opened that person's conversation (COALESCE -> 0 when I never opened it)
    @Query(
        "SELECT m.fromUserId AS partnerId, COUNT(*) AS unread FROM messages m " +
            "LEFT JOIN conversation_reads r ON r.ownerId = :ownerId AND r.partnerId = m.fromUserId " +
            "WHERE m.toUserId = :ownerId AND m.fromUserId != :ownerId " +
            "AND m.timestamp > COALESCE(r.lastReadAt, 0) " +
            "GROUP BY m.fromUserId"
    )
    fun unreadByPartner(ownerId: String): Flow<List<PartnerUnread>>

    // same rule, not grouped -- total unread across every conversation
    @Query(
        "SELECT COUNT(*) FROM messages m " +
            "LEFT JOIN conversation_reads r ON r.ownerId = :ownerId AND r.partnerId = m.fromUserId " +
            "WHERE m.toUserId = :ownerId AND m.fromUserId != :ownerId " +
            "AND m.timestamp > COALESCE(r.lastReadAt, 0)"
    )
    fun totalUnread(ownerId: String): Flow<Int>
}
