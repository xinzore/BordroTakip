package com.bordrotakip.data.repository

import com.bordrotakip.data.local.dao.CalendarSyncDao
import com.bordrotakip.data.local.entity.CalendarSyncEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncRepository @Inject constructor(
    private val calendarSyncDao: CalendarSyncDao
) {
    suspend fun getByWorkEventId(workEventId: Long): CalendarSyncEntity? {
        return calendarSyncDao.getByWorkEventId(workEventId)
    }

    suspend fun getByCalendarId(calendarId: Long): List<CalendarSyncEntity> {
        return calendarSyncDao.getByCalendarId(calendarId)
    }

    suspend fun upsert(entity: CalendarSyncEntity) {
        calendarSyncDao.upsert(entity)
    }

    suspend fun deleteByWorkEventId(workEventId: Long) {
        calendarSyncDao.deleteByWorkEventId(workEventId)
    }
}

