package com.bordrotakip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bordrotakip.data.local.entity.CalendarSyncEntity

@Dao
interface CalendarSyncDao {
    @Query("SELECT * FROM calendar_sync WHERE workEventId = :workEventId LIMIT 1")
    suspend fun getByWorkEventId(workEventId: Long): CalendarSyncEntity?

    @Query("SELECT * FROM calendar_sync WHERE calendarId = :calendarId")
    suspend fun getByCalendarId(calendarId: Long): List<CalendarSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CalendarSyncEntity)

    @Query("DELETE FROM calendar_sync WHERE workEventId = :workEventId")
    suspend fun deleteByWorkEventId(workEventId: Long): Int
}
