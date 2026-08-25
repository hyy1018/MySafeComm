package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.asgm.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Insert
    suspend fun insert(contact: EmergencyContactEntity): Long

    @Query("SELECT * FROM emergency_contacts ORDER BY categoryEmergency ASC")
    fun getAll(): Flow<List<EmergencyContactEntity>>
}
