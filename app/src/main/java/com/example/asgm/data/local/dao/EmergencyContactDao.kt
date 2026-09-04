// #member1
// Room queries for the SOS emergency contacts list.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.asgm.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Insert
    suspend fun insert(contact: EmergencyContactEntity): Long

    // bulk merge from the cloud pull
    @Upsert
    suspend fun upsertAll(contacts: List<EmergencyContactEntity>)

    @Update
    suspend fun update(contact: EmergencyContactEntity)

    @Delete
    suspend fun delete(contact: EmergencyContactEntity)

    @Query("SELECT * FROM emergency_contacts ORDER BY serviceId ASC")
    fun getAll(): Flow<List<EmergencyContactEntity>>

    // the shared list: admin-managed and seeded contacts, no private owner
    @Query("SELECT * FROM emergency_contacts WHERE ownerId IS NULL ORDER BY serviceId ASC")
    fun getCommunity(): Flow<List<EmergencyContactEntity>>

    // one resident's own private contacts
    @Query("SELECT * FROM emergency_contacts WHERE ownerId = :ownerId ORDER BY serviceId ASC")
    fun getByOwner(ownerId: String): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts WHERE serviceId = :serviceId LIMIT 1")
    fun getById(serviceId: Long): Flow<EmergencyContactEntity?>
}
