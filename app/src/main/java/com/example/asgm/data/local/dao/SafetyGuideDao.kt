// #member1
// Room queries for the SOS safety guide categories and steps.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.asgm.data.local.entity.SafetyGuideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyGuideDao {
    @Insert
    suspend fun insert(guide: SafetyGuideEntity): Long

    // bulk merge from the cloud pull
    @Upsert
    suspend fun upsertAll(guides: List<SafetyGuideEntity>)

    @Update
    suspend fun update(guide: SafetyGuideEntity)

    @Delete
    suspend fun delete(guide: SafetyGuideEntity)

    @Query("SELECT * FROM safety_guides ORDER BY categorySafety ASC")
    fun getAll(): Flow<List<SafetyGuideEntity>>

    @Query("SELECT * FROM safety_guides WHERE categorySafety = :category LIMIT 1")
    suspend fun getByCategory(category: String): SafetyGuideEntity?

    @Query("SELECT * FROM safety_guides WHERE guideId = :guideId LIMIT 1")
    fun getById(guideId: Long): Flow<SafetyGuideEntity?>
}
