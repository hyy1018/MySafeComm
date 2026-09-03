// Room queries for who has confirmed which urgent alert.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.asgm.data.local.entity.AlertAcknowledgementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertAcknowledgementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun acknowledge(ack: AlertAcknowledgementEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM alert_acknowledgements WHERE alertId = :alertId AND userId = :userId)")
    fun isAcknowledgedByUser(alertId: Long, userId: String): Flow<Boolean>
}
