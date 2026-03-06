package com.bordrotakip.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bordrotakip.data.local.AppDatabase
import com.bordrotakip.data.local.dao.CalendarSyncDao
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.local.dao.GrossHistoryDao
import com.bordrotakip.data.local.dao.PayrollRecordDao
import com.bordrotakip.data.local.dao.WorkEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE company_profile ADD COLUMN isBesEnabled INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE company_profile ADD COLUMN besRate REAL NOT NULL DEFAULT 0.03")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE company_profile ADD COLUMN cumulativeTaxBase REAL NOT NULL DEFAULT 0.0")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE company_profile ADD COLUMN overtimeMultiplierNormal REAL NOT NULL DEFAULT 1.5")
            db.execSQL("ALTER TABLE company_profile ADD COLUMN overtimeMultiplierWeekend REAL NOT NULL DEFAULT 2.0")
            db.execSQL("ALTER TABLE company_profile ADD COLUMN overtimeMultiplierHoliday REAL NOT NULL DEFAULT 2.0")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_work_events_date ON work_events(date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_work_events_eventType_date ON work_events(eventType, date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payroll_records_periodYear_periodMonth ON payroll_records(periodYear, periodMonth)")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS calendar_sync (
                    workEventId INTEGER NOT NULL,
                    calendarId INTEGER NOT NULL,
                    calendarEventId INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(workEventId)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_sync_calendarId ON calendar_sync(calendarId)")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Varsayılan: Cumartesi + Pazar tatil
            db.execSQL("ALTER TABLE company_profile ADD COLUMN nonWorkingDaysMask INTEGER NOT NULL DEFAULT 96")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE company_profile ADD COLUMN employmentType TEXT NOT NULL DEFAULT 'STANDARD'")
        }
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8
        )
        .build()
    }
    
    @Provides
    fun provideWorkEventDao(database: AppDatabase): WorkEventDao {
        return database.workEventDao()
    }
    
    @Provides
    fun provideGrossHistoryDao(database: AppDatabase): GrossHistoryDao {
        return database.grossHistoryDao()
    }
    
    @Provides
    fun providePayrollRecordDao(database: AppDatabase): PayrollRecordDao {
        return database.payrollRecordDao()
    }
    
    @Provides
    fun provideCompanyProfileDao(database: AppDatabase): CompanyProfileDao {
        return database.companyProfileDao()
    }

    @Provides
    fun provideCalendarSyncDao(database: AppDatabase): CalendarSyncDao {
        return database.calendarSyncDao()
    }
}
