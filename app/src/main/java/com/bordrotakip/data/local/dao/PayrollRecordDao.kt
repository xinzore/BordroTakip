package com.bordrotakip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bordrotakip.data.local.entity.PayrollRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayrollRecordDao {
    
    @Query("SELECT * FROM payroll_records ORDER BY periodYear DESC, periodMonth DESC")
    fun getAllRecords(): Flow<List<PayrollRecordEntity>>
    
    @Query("SELECT * FROM payroll_records WHERE periodYear = :year AND periodMonth = :month LIMIT 1")
    suspend fun getRecordForPeriod(year: Int, month: Int): PayrollRecordEntity?
    
    @Query("SELECT * FROM payroll_records WHERE id = :id")
    suspend fun getById(id: Long): PayrollRecordEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PayrollRecordEntity): Long
    
    @Update
    suspend fun update(record: PayrollRecordEntity)
    
    @Query("DELETE FROM payroll_records WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    
    @Query("SELECT SUM(taxBase) FROM payroll_records WHERE periodYear = :year AND periodMonth < :month")
    suspend fun getCumulativeTaxBase(year: Int, month: Int): Double?
    
    @Query("SELECT * FROM payroll_records WHERE periodYear = :year ORDER BY periodMonth")
    fun getRecordsForYear(year: Int): Flow<List<PayrollRecordEntity>>
}
