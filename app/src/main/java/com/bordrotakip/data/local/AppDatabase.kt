package com.bordrotakip.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bordrotakip.data.local.dao.CalendarSyncDao
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.local.dao.GrossHistoryDao
import com.bordrotakip.data.local.dao.PayrollRecordDao
import com.bordrotakip.data.local.dao.WorkEventDao
import com.bordrotakip.data.local.entity.CalendarSyncEntity
import com.bordrotakip.data.local.entity.CompanyProfileEntity
import com.bordrotakip.data.local.entity.GrossHistoryEntity
import com.bordrotakip.data.local.entity.PayrollRecordEntity
import com.bordrotakip.data.local.entity.WorkEventEntity

@Database(
    entities = [
        WorkEventEntity::class,
        GrossHistoryEntity::class,
        PayrollRecordEntity::class,
        CompanyProfileEntity::class,
        CalendarSyncEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workEventDao(): WorkEventDao
    abstract fun grossHistoryDao(): GrossHistoryDao
    abstract fun payrollRecordDao(): PayrollRecordDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun calendarSyncDao(): CalendarSyncDao
    
    companion object {
        const val DATABASE_NAME = "bordro_takip_db"
    }
}
