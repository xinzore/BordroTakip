package com.bordrotakip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bordrotakip.data.local.entity.GrossHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrossHistoryDao {
    
    @Query("SELECT * FROM gross_history ORDER BY validFromEpochDay DESC, createdAt DESC, id DESC")
    fun getAllGrossHistory(): Flow<List<GrossHistoryEntity>>
    
    @Query("SELECT * FROM gross_history WHERE validFromEpochDay <= :epochDay AND (validToEpochDay IS NULL OR validToEpochDay >= :epochDay) ORDER BY validFromEpochDay DESC, createdAt DESC, id DESC LIMIT 1")
    suspend fun getCurrentGross(epochDay: Long): GrossHistoryEntity?
    
    @Query("SELECT * FROM gross_history WHERE validFromEpochDay <= :epochDay AND (validToEpochDay IS NULL OR validToEpochDay >= :epochDay) ORDER BY validFromEpochDay DESC, createdAt DESC, id DESC LIMIT 1")
    suspend fun getGrossForDate(epochDay: Long): GrossHistoryEntity?
    
    @Query("SELECT * FROM gross_history WHERE id = :id")
    suspend fun getById(id: Long): GrossHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grossHistory: GrossHistoryEntity): Long
    
    @Update
    suspend fun update(grossHistory: GrossHistoryEntity)
    
    @Query("DELETE FROM gross_history WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    
    @Query("SELECT * FROM gross_history ORDER BY validFromEpochDay DESC, createdAt DESC, id DESC LIMIT 1")
    suspend fun getLatestGross(): GrossHistoryEntity?
}
