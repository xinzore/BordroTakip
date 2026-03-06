package com.bordrotakip.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_sync",
    indices = [
        Index(value = ["calendarId"])
    ]
)
data class CalendarSyncEntity(
    @PrimaryKey
    val workEventId: Long,
    val calendarId: Long,
    val calendarEventId: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

