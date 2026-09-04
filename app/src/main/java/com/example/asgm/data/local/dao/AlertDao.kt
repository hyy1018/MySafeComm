// #member1
// Room queries for community alerts, plus the unacknowledged-urgent-count for the Alert badge.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.asgm.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Insert
    suspend fun insert(alert: AlertEntity): Long

    // bulk merge from the cloud pull
    @Upsert
    suspend fun upsertAll(alerts: List<AlertEntity>)

    @Update
    suspend fun update(alert: AlertEntity)

    @Delete
    suspend fun delete(alert: AlertEntity)

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE alertId = :alertId LIMIT 1")
    fun getById(alertId: Long): Flow<AlertEntity?>

    // urgent notices this user hasn't confirmed yet -- drives the bottom-nav Alert badge
    @Query(
        "SELECT COUNT(*) FROM alerts WHERE priority = 'URGENT' AND alertId NOT IN " +
            "(SELECT alertId FROM alert_acknowledgements WHERE userId = :userId)"
    )
    fun getUnacknowledgedUrgentCount(userId: String): Flow<Int>
}
