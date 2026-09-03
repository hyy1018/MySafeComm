// #member1
// Room queries for hazard reports: create, edit, and admin status updates.
package com.example.asgm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.asgm.data.local.entity.ReportEntity
import com.example.asgm.data.local.entity.ReportStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: ReportEntity): Long

    @Update
    suspend fun update(report: ReportEntity)

    @Delete
    suspend fun delete(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE reportId = :reportId")
    suspend fun updateStatus(reportId: Long, status: ReportStatus)

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY timestamp DESC")
    fun getByUser(userId: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE reportId = :reportId LIMIT 1")
    fun getById(reportId: Long): Flow<ReportEntity?>
}
