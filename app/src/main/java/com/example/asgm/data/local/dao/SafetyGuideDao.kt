// Room queries for the SOS safety guide categories and steps.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.asgm.data.local.entity.SafetyGuideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyGuideDao {
    @Insert
    suspend fun insert(guide: SafetyGuideEntity): Long

    @Query("SELECT * FROM safety_guides ORDER BY categorySafety ASC")
    fun getAll(): Flow<List<SafetyGuideEntity>>

    @Query("SELECT * FROM safety_guides WHERE categorySafety = :category LIMIT 1")
    suspend fun getByCategory(category: String): SafetyGuideEntity?

    @Query("SELECT * FROM safety_guides WHERE guideId = :guideId LIMIT 1")
    fun getById(guideId: Long): Flow<SafetyGuideEntity?>
}
