package com.bordrotakip.domain.calendar

import com.bordrotakip.domain.model.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrHolidayCalendarTest {

    private val calendar = TrHolidayCalendar()

    @Test
    fun `recognizes official holidays`() {
        assertTrue(calendar.isOfficialHoliday(LocalDate.of(2025, 3, 29))) // Ramazan Arefe
        assertTrue(calendar.isOfficialHoliday(LocalDate.of(2025, 3, 30))) // Ramazan Bayramı
        assertTrue(calendar.isOfficialHoliday(LocalDate.of(2026, 5, 1))) // 1 Mayıs
        assertFalse(calendar.isOfficialHoliday(LocalDate.of(2026, 2, 2))) // normal weekday
    }

    @Test
    fun `classifies overtime by holiday then weekend`() {
        // 2025-03-30 is both Sunday and official holiday -> holiday wins
        assertEquals(EventType.OVERTIME_HOLIDAY, calendar.classifyOvertime(LocalDate.of(2025, 3, 30)))

        // 2026-02-01 is Sunday (not official holiday)
        assertEquals(EventType.OVERTIME_WEEKEND, calendar.classifyOvertime(LocalDate.of(2026, 2, 1)))

        // Normal weekday
        assertEquals(EventType.OVERTIME_NORMAL, calendar.classifyOvertime(LocalDate.of(2026, 2, 2)))
    }

    @Test
    fun `falls back to fixed holidays for unknown years`() {
        assertTrue(calendar.isOfficialHoliday(LocalDate.of(2040, 10, 29)))
    }
}

