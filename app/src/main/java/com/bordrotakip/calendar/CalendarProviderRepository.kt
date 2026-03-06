package com.bordrotakip.calendar

import android.content.Context
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarProviderRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun listWritableCalendars(): List<DeviceCalendar> {
        val resolver = context.contentResolver
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE
        )

        val selection =
            "(${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?) AND (${CalendarContract.Calendars.VISIBLE} = 1)"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        val result = mutableListOf<DeviceCalendar>()
        val cursor = resolver.query(uri, projection, selection, selectionArgs, null) ?: return emptyList()
        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountNameIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val displayName = it.getString(nameIndex) ?: "Takvim"
                val accountName = it.getString(accountNameIndex)
                val accountType = it.getString(accountTypeIndex)
                result.add(
                    DeviceCalendar(
                        id = id,
                        displayName = displayName,
                        accountName = accountName,
                        accountType = accountType
                    )
                )
            }
        }

        val google = result.filter { it.accountType?.contains("google", ignoreCase = true) == true }
        return (google.ifEmpty { result }).sortedBy { it.displayName.lowercase() }
    }
}

