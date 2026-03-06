package com.bordrotakip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bordrotakip.data.local.entity.CompanyProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyProfileDao {
    
    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<CompanyProfileEntity?>
    
    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): CompanyProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CompanyProfileEntity)
    
    @Update
    suspend fun update(profile: CompanyProfileEntity)
    
    @Query("UPDATE company_profile SET cutoffDay = :cutoffDay, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateCutoffDay(cutoffDay: Int, updatedAt: Long = System.currentTimeMillis()): Int
    
    @Query("UPDATE company_profile SET companyName = :companyName, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateCompanyName(companyName: String, updatedAt: Long = System.currentTimeMillis()): Int
}
