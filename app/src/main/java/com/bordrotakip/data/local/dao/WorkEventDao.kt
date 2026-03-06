package com.bordrotakip.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bordrotakip.data.local.entity.WorkEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkEventDao {
    
    @Query("SELECT * FROM work_events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<WorkEventEntity>>

    @Query("SELECT * FROM work_events ORDER BY date")
    suspend fun getAllEventsSync(): List<WorkEventEntity>
    
    @Query("SELECT * FROM work_events WHERE date = :epochDay")
    fun getEventsForDate(epochDay: Long): Flow<List<WorkEventEntity>>
    
    @Query("SELECT * FROM work_events WHERE date BETWEEN :startEpochDay AND :endEpochDay ORDER BY date")
    fun getEventsForPeriod(startEpochDay: Long, endEpochDay: Long): Flow<List<WorkEventEntity>>
    
    @Query("SELECT * FROM work_events WHERE date >= :startEpochDay AND date <= :endEpochDay ORDER BY date")
    suspend fun getEventsForPeriodSync(startEpochDay: Long, endEpochDay: Long): List<WorkEventEntity>
    
    @Query("SELECT * FROM work_events WHERE id = :id")
    suspend fun getEventById(id: Long): WorkEventEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: WorkEventEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<WorkEventEntity>)
    
    @Update
    suspend fun update(event: WorkEventEntity)
    
    @Delete
    suspend fun delete(event: WorkEventEntity)
    
    @Query("DELETE FROM work_events WHERE date = :epochDay")
    suspend fun deleteEventsForDate(epochDay: Long): Int
    
    @Query("DELETE FROM work_events WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    
    @Query("SELECT COUNT(*) FROM work_events WHERE eventType = :eventType AND date BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun countEventsByType(eventType: String, startEpochDay: Long, endEpochDay: Long): Int
    
    @Query("SELECT SUM(hours) FROM work_events WHERE eventType LIKE 'OVERTIME%' AND date BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getTotalOvertimeHours(startEpochDay: Long, endEpochDay: Long): Double?
}
