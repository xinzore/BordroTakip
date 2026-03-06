package com.bordrotakip.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.compose.ui.graphics.toArgb
import com.bordrotakip.data.local.entity.CalendarSyncEntity
import com.bordrotakip.data.repository.CalendarSyncRepository
import com.bordrotakip.data.repository.WorkEventRepository
import com.bordrotakip.domain.model.EventType
import com.bordrotakip.domain.model.WorkEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarSyncResult(
    val syncedCount: Int,
    val removedCount: Int,
    val failedCount: Int
)

@Singleton
class CalendarSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workEventRepository: WorkEventRepository,
    private val calendarSyncRepository: CalendarSyncRepository
) {
    private data class EventColorOption(
        val colorKey: String,
        val argb: Int
    )

    private val colorOptionsCache = mutableMapOf<Long, List<EventColorOption>>()

    suspend fun syncAll(calendarId: Long): CalendarSyncResult = withContext(Dispatchers.IO) {
        val events = workEventRepository.getAllEventsSync().filter { it.id != 0L }
        val eventIds = events.map { it.id }.toSet()

        var synced = 0
        var failed = 0
        for (event in events) {
            try {
                upsertWorkEventInternal(event, calendarId)
                synced += 1
            } catch (_: Throwable) {
                failed += 1
            }
        }

        val mappings = calendarSyncRepository.getByCalendarId(calendarId)
        var removed = 0
        for (mapping in mappings) {
            if (mapping.workEventId in eventIds) continue
            runCatching {
                deleteCalendarEvent(mapping.calendarEventId)
            }
            calendarSyncRepository.deleteByWorkEventId(mapping.workEventId)
            removed += 1
        }

        CalendarSyncResult(
            syncedCount = synced,
            removedCount = removed,
            failedCount = failed
        )
    }

    suspend fun upsertWorkEvent(workEvent: WorkEvent, calendarId: Long) = withContext(Dispatchers.IO) {
        upsertWorkEventInternal(workEvent, calendarId)
    }

    suspend fun deleteWorkEvent(workEventId: Long) = withContext(Dispatchers.IO) {
        val mapping = calendarSyncRepository.getByWorkEventId(workEventId) ?: return@withContext
        runCatching { deleteCalendarEvent(mapping.calendarEventId) }
        calendarSyncRepository.deleteByWorkEventId(workEventId)
    }

    private suspend fun upsertWorkEventInternal(workEvent: WorkEvent, calendarId: Long) {
        if (workEvent.id == 0L) return
        val resolver = context.contentResolver

        val existing = try {
            calendarSyncRepository.getByWorkEventId(workEvent.id)
        } catch (_: Throwable) {
            null
        }
        if (existing != null && existing.calendarId != calendarId) {
            runCatching { deleteCalendarEvent(existing.calendarEventId) }
            try {
                calendarSyncRepository.deleteByWorkEventId(workEvent.id)
            } catch (_: Throwable) {
                // ignore
            }
        }

        val values = buildEventValues(workEvent, calendarId)

        if (existing != null && existing.calendarId == calendarId) {
            val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existing.calendarEventId)
            val updatedRows = runCatching { resolver.update(eventUri, values, null, null) }.getOrDefault(0)
            if (updatedRows > 0) {
                try {
                    calendarSyncRepository.upsert(
                        CalendarSyncEntity(
                            workEventId = workEvent.id,
                            calendarId = calendarId,
                            calendarEventId = existing.calendarEventId,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } catch (_: Throwable) {
                    // ignore
                }
                return
            }
        }

        val insertedUri = resolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return
        val calendarEventId = ContentUris.parseId(insertedUri)
        calendarSyncRepository.upsert(
            CalendarSyncEntity(
                workEventId = workEvent.id,
                calendarId = calendarId,
                calendarEventId = calendarEventId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun deleteCalendarEvent(calendarEventId: Long) {
        val resolver = context.contentResolver
        val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
        resolver.delete(eventUri, null, null)
    }

    private data class ResolvedTime(
        val startMillis: Long,
        val endMillis: Long,
        val timeZone: String,
        val isAllDay: Boolean
    )

    private fun buildEventValues(workEvent: WorkEvent, calendarId: Long): ContentValues {
        val values = ContentValues()
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId)

        val title = buildTitle(workEvent)
        val description = buildDescription(workEvent)

        val time = resolveTime(workEvent)
        values.put(CalendarContract.Events.TITLE, title)
        values.put(CalendarContract.Events.DESCRIPTION, description)
        values.put(CalendarContract.Events.DTSTART, time.startMillis)
        values.put(CalendarContract.Events.DTEND, time.endMillis)
        values.put(CalendarContract.Events.EVENT_TIMEZONE, time.timeZone)
        values.put(CalendarContract.Events.ALL_DAY, if (time.isAllDay) 1 else 0)
        val requestedColor = resolveEventColor(workEvent)
        val option = resolveNearestColorOption(calendarId, requestedColor)
        if (option != null) {
            values.put(CalendarContract.Events.EVENT_COLOR_KEY, option.colorKey)
            values.put(CalendarContract.Events.EVENT_COLOR, option.argb)
        } else {
            values.put(CalendarContract.Events.EVENT_COLOR, requestedColor)
            values.putNull(CalendarContract.Events.EVENT_COLOR_KEY)
        }

        return values
    }

    private fun resolveTime(workEvent: WorkEvent): ResolvedTime {
        val date = workEvent.date

        val startTime: LocalTime? = workEvent.startTime
            ?: workEvent.shiftType?.let { LocalTime.of(it.startHour, 0) }
            ?: if (workEvent.eventType == EventType.SHIFT) LocalTime.of(8, 0) else null

        val endTime: LocalTime? = workEvent.endTime
            ?: workEvent.shiftType?.let { LocalTime.of(it.endHour % 24, 0) }
            ?: if (workEvent.eventType == EventType.SHIFT && startTime != null && workEvent.hours > 0) {
                startTime.plusMinutes((workEvent.hours * 60).toLong())
            } else null

        if (workEvent.eventType == EventType.SHIFT && startTime != null && endTime != null) {
            val zone = ZoneId.systemDefault()
            val endDate = if (!endTime.isAfter(startTime)) date.plusDays(1) else date
            val startMillis = ZonedDateTime.of(date, startTime, zone).toInstant().toEpochMilli()
            val endMillis = ZonedDateTime.of(endDate, endTime, zone).toInstant().toEpochMilli()
            return ResolvedTime(startMillis, endMillis, zone.id, isAllDay = false)
        }

        // Diğer eventler: all-day (UTC) – Google Takvim'de daha stabil görünür.
        val startMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        return ResolvedTime(startMillis, endMillis, "UTC", isAllDay = true)
    }

    private fun buildTitle(workEvent: WorkEvent): String {
        val base = workEvent.getDisplayLabel()
        val hours = workEvent.hours.takeIf { it > 0 }?.toInt()?.let { " (${it}s)" } ?: ""
        return "$base$hours <BordroTakip"
    }

    private fun buildDescription(workEvent: WorkEvent): String {
        val parts = mutableListOf<String>()
        parts.add("Bordro Takip")
        parts.add("Tür: ${workEvent.eventType.displayName}")
        if (workEvent.hours > 0) parts.add("Süre: ${workEvent.hours.toInt()} saat")
        if (workEvent.note.isNotBlank()) parts.add("Not: ${workEvent.note}")
        parts.add("WorkEventId: ${workEvent.id}")
        return parts.joinToString("\n")
    }

    private fun resolveEventColor(workEvent: WorkEvent): Int {
        return workEvent.eventType.color.toArgb()
    }

    private fun resolveNearestColorOption(calendarId: Long, requestedColor: Int): EventColorOption? {
        val options = colorOptionsCache.getOrPut(calendarId) { loadEventColorOptions(calendarId) }
        if (options.isEmpty()) return null
        return options.minByOrNull { colorDistanceSquared(it.argb, requestedColor) }
    }

    private fun loadEventColorOptions(calendarId: Long): List<EventColorOption> {
        val resolver = context.contentResolver

        val calendarUri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId)
        val calendarProjection = arrayOf(
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val (accountName, accountType) = resolver.query(calendarUri, calendarProjection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return emptyList()
            val accountNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
            cursor.getString(accountNameIndex) to cursor.getString(accountTypeIndex)
        } ?: return emptyList()

        val selectionBuilder = StringBuilder()
        val args = mutableListOf<String>()
        selectionBuilder.append("${CalendarContract.Colors.COLOR_TYPE} = ?")
        args += CalendarContract.Colors.TYPE_EVENT.toString()

        if (!accountName.isNullOrBlank() && !accountType.isNullOrBlank()) {
            selectionBuilder.append(" AND ${CalendarContract.Colors.ACCOUNT_NAME} = ?")
            selectionBuilder.append(" AND ${CalendarContract.Colors.ACCOUNT_TYPE} = ?")
            args += accountName
            args += accountType
        }

        val projection = arrayOf(
            CalendarContract.Colors.COLOR_KEY,
            CalendarContract.Colors.COLOR
        )

        return resolver.query(
            CalendarContract.Colors.CONTENT_URI,
            projection,
            selectionBuilder.toString(),
            args.toTypedArray(),
            null
        )?.use { cursor ->
            val colorKeyIndex = cursor.getColumnIndexOrThrow(CalendarContract.Colors.COLOR_KEY)
            val colorIndex = cursor.getColumnIndexOrThrow(CalendarContract.Colors.COLOR)
            buildList {
                while (cursor.moveToNext()) {
                    val key = cursor.getString(colorKeyIndex) ?: continue
                    val color = cursor.getInt(colorIndex)
                    add(EventColorOption(colorKey = key, argb = color))
                }
            }
        }.orEmpty()
    }

    private fun colorDistanceSquared(c1: Int, c2: Int): Int {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF

        val dr = abs(r1 - r2)
        val dg = abs(g1 - g2)
        val db = abs(b1 - b2)
        return (dr * dr) + (dg * dg) + (db * db)
    }
}
