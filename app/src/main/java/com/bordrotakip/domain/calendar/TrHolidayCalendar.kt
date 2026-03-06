package com.bordrotakip.domain.calendar

import com.bordrotakip.domain.model.EventType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Türkiye resmi tatil takvimi (tarih bazlı).
 *
 * Not: Arefe ve 28 Ekim (1/2 gün) günleri de “tatil” sayılır.
 * Kapsam: 2024–2035 (Diyanet “Resmi Tatil Günleri” listesi) + fallback olarak sabit tatiller.
 */
@Singleton
class TrHolidayCalendar @Inject constructor() {

    fun isOfficialHoliday(date: LocalDate): Boolean {
        val year = date.year
        val known = OFFICIAL_HOLIDAYS_BY_YEAR[year]
        return if (known != null) {
            date in known
        } else {
            date in fixedHolidaysForYear(year)
        }
    }

    fun classifyOvertime(date: LocalDate): EventType {
        return classifyOvertime(date, DEFAULT_NON_WORKING_DAYS_MASK)
    }

    fun classifyOvertime(date: LocalDate, nonWorkingDaysMask: Int): EventType {
        return when {
            isOfficialHoliday(date) -> EventType.OVERTIME_HOLIDAY
            isNonWorkingDay(date, nonWorkingDaysMask) -> EventType.OVERTIME_WEEKEND
            else -> EventType.OVERTIME_NORMAL
        }
    }

    fun isNonWorkingDay(date: LocalDate, nonWorkingDaysMask: Int): Boolean {
        val bit = 1 shl (date.dayOfWeek.value - 1)
        return (nonWorkingDaysMask and bit) != 0
    }

    private fun fixedHolidaysForYear(year: Int): Set<LocalDate> {
        return setOf(
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 4, 23),
            LocalDate.of(year, 5, 1),
            LocalDate.of(year, 5, 19),
            LocalDate.of(year, 7, 15),
            LocalDate.of(year, 8, 30),
            LocalDate.of(year, 10, 28),
            LocalDate.of(year, 10, 29)
        )
    }

    companion object {
        // Varsayılan: Cumartesi + Pazar tatil
        // Bitmask: DayOfWeek.value (1..7) -> (value - 1) bit.
        // Cumartesi (6) + Pazar (7) => bit 5 + bit 6 => 32 + 64 = 96
        const val DEFAULT_NON_WORKING_DAYS_MASK: Int = 96

        private val OFFICIAL_HOLIDAYS_BY_YEAR: Map<Int, Set<LocalDate>> = mapOf(
            2024 to setOf(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 4, 9),
                LocalDate.of(2024, 4, 10),
                LocalDate.of(2024, 4, 11),
                LocalDate.of(2024, 4, 12),
                LocalDate.of(2024, 4, 23),
                LocalDate.of(2024, 5, 1),
                LocalDate.of(2024, 5, 19),
                LocalDate.of(2024, 6, 15),
                LocalDate.of(2024, 6, 16),
                LocalDate.of(2024, 6, 17),
                LocalDate.of(2024, 6, 18),
                LocalDate.of(2024, 6, 19),
                LocalDate.of(2024, 7, 15),
                LocalDate.of(2024, 8, 30),
                LocalDate.of(2024, 10, 28),
                LocalDate.of(2024, 10, 29)
            ),
            2025 to setOf(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 3, 29),
                LocalDate.of(2025, 3, 30),
                LocalDate.of(2025, 3, 31),
                LocalDate.of(2025, 4, 1),
                LocalDate.of(2025, 4, 23),
                LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 5, 19),
                LocalDate.of(2025, 6, 5),
                LocalDate.of(2025, 6, 6),
                LocalDate.of(2025, 6, 7),
                LocalDate.of(2025, 6, 8),
                LocalDate.of(2025, 6, 9),
                LocalDate.of(2025, 7, 15),
                LocalDate.of(2025, 8, 30),
                LocalDate.of(2025, 10, 28),
                LocalDate.of(2025, 10, 29)
            ),
            2026 to setOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 19),
                LocalDate.of(2026, 3, 20),
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 22),
                LocalDate.of(2026, 4, 23),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 19),
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 5, 28),
                LocalDate.of(2026, 5, 29),
                LocalDate.of(2026, 5, 30),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 8, 30),
                LocalDate.of(2026, 10, 28),
                LocalDate.of(2026, 10, 29)
            ),
            2027 to setOf(
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 3, 8),
                LocalDate.of(2027, 3, 9),
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 11),
                LocalDate.of(2027, 4, 23),
                LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 5, 15),
                LocalDate.of(2027, 5, 16),
                LocalDate.of(2027, 5, 17),
                LocalDate.of(2027, 5, 18),
                LocalDate.of(2027, 5, 19),
                LocalDate.of(2027, 7, 15),
                LocalDate.of(2027, 8, 30),
                LocalDate.of(2027, 10, 28),
                LocalDate.of(2027, 10, 29)
            ),
            2028 to setOf(
                LocalDate.of(2028, 1, 1),
                LocalDate.of(2028, 2, 25),
                LocalDate.of(2028, 2, 26),
                LocalDate.of(2028, 2, 27),
                LocalDate.of(2028, 2, 28),
                LocalDate.of(2028, 4, 23),
                LocalDate.of(2028, 5, 1),
                LocalDate.of(2028, 5, 4),
                LocalDate.of(2028, 5, 5),
                LocalDate.of(2028, 5, 6),
                LocalDate.of(2028, 5, 7),
                LocalDate.of(2028, 5, 8),
                LocalDate.of(2028, 5, 19),
                LocalDate.of(2028, 7, 15),
                LocalDate.of(2028, 8, 30),
                LocalDate.of(2028, 10, 28),
                LocalDate.of(2028, 10, 29)
            ),
            2029 to setOf(
                LocalDate.of(2029, 1, 1),
                LocalDate.of(2029, 2, 13),
                LocalDate.of(2029, 2, 14),
                LocalDate.of(2029, 2, 15),
                LocalDate.of(2029, 2, 16),
                LocalDate.of(2029, 4, 23),
                LocalDate.of(2029, 4, 24),
                LocalDate.of(2029, 4, 25),
                LocalDate.of(2029, 4, 26),
                LocalDate.of(2029, 4, 27),
                LocalDate.of(2029, 5, 1),
                LocalDate.of(2029, 5, 19),
                LocalDate.of(2029, 7, 15),
                LocalDate.of(2029, 8, 30),
                LocalDate.of(2029, 10, 28),
                LocalDate.of(2029, 10, 29)
            ),
            2030 to setOf(
                LocalDate.of(2030, 1, 1),
                LocalDate.of(2030, 2, 3),
                LocalDate.of(2030, 2, 4),
                LocalDate.of(2030, 2, 5),
                LocalDate.of(2030, 2, 6),
                LocalDate.of(2030, 4, 12),
                LocalDate.of(2030, 4, 13),
                LocalDate.of(2030, 4, 14),
                LocalDate.of(2030, 4, 15),
                LocalDate.of(2030, 4, 16),
                LocalDate.of(2030, 4, 23),
                LocalDate.of(2030, 5, 1),
                LocalDate.of(2030, 5, 19),
                LocalDate.of(2030, 7, 15),
                LocalDate.of(2030, 8, 30),
                LocalDate.of(2030, 10, 28),
                LocalDate.of(2030, 10, 29)
            ),
            2031 to setOf(
                LocalDate.of(2031, 1, 1),
                LocalDate.of(2031, 1, 23),
                LocalDate.of(2031, 1, 24),
                LocalDate.of(2031, 1, 25),
                LocalDate.of(2031, 1, 26),
                LocalDate.of(2031, 4, 1),
                LocalDate.of(2031, 4, 2),
                LocalDate.of(2031, 4, 3),
                LocalDate.of(2031, 4, 4),
                LocalDate.of(2031, 4, 5),
                LocalDate.of(2031, 4, 23),
                LocalDate.of(2031, 5, 1),
                LocalDate.of(2031, 5, 19),
                LocalDate.of(2031, 7, 15),
                LocalDate.of(2031, 8, 30),
                LocalDate.of(2031, 10, 28),
                LocalDate.of(2031, 10, 29)
            ),
            2032 to setOf(
                LocalDate.of(2032, 1, 1),
                LocalDate.of(2032, 1, 13),
                LocalDate.of(2032, 1, 14),
                LocalDate.of(2032, 1, 15),
                LocalDate.of(2032, 1, 16),
                LocalDate.of(2032, 3, 21),
                LocalDate.of(2032, 3, 22),
                LocalDate.of(2032, 3, 23),
                LocalDate.of(2032, 3, 24),
                LocalDate.of(2032, 3, 25),
                LocalDate.of(2032, 4, 23),
                LocalDate.of(2032, 5, 1),
                LocalDate.of(2032, 5, 19),
                LocalDate.of(2032, 7, 15),
                LocalDate.of(2032, 8, 30),
                LocalDate.of(2032, 10, 28),
                LocalDate.of(2032, 10, 29)
            ),
            2033 to setOf(
                LocalDate.of(2033, 1, 1),
                LocalDate.of(2033, 1, 2),
                LocalDate.of(2033, 1, 3),
                LocalDate.of(2033, 1, 4),
                LocalDate.of(2033, 3, 10),
                LocalDate.of(2033, 3, 11),
                LocalDate.of(2033, 3, 12),
                LocalDate.of(2033, 3, 13),
                LocalDate.of(2033, 3, 14),
                LocalDate.of(2033, 4, 23),
                LocalDate.of(2033, 5, 1),
                LocalDate.of(2033, 5, 19),
                LocalDate.of(2033, 7, 15),
                LocalDate.of(2033, 8, 30),
                LocalDate.of(2033, 10, 28),
                LocalDate.of(2033, 10, 29),
                LocalDate.of(2033, 12, 22),
                LocalDate.of(2033, 12, 23),
                LocalDate.of(2033, 12, 24),
                LocalDate.of(2033, 12, 25)
            ),
            2034 to setOf(
                LocalDate.of(2034, 1, 1),
                LocalDate.of(2034, 2, 28),
                LocalDate.of(2034, 3, 1),
                LocalDate.of(2034, 3, 2),
                LocalDate.of(2034, 3, 3),
                LocalDate.of(2034, 3, 4),
                LocalDate.of(2034, 4, 23),
                LocalDate.of(2034, 5, 1),
                LocalDate.of(2034, 5, 19),
                LocalDate.of(2034, 7, 15),
                LocalDate.of(2034, 8, 30),
                LocalDate.of(2034, 10, 28),
                LocalDate.of(2034, 10, 29),
                LocalDate.of(2034, 12, 11),
                LocalDate.of(2034, 12, 12),
                LocalDate.of(2034, 12, 13),
                LocalDate.of(2034, 12, 14)
            ),
            2035 to setOf(
                LocalDate.of(2035, 1, 1),
                LocalDate.of(2035, 2, 17),
                LocalDate.of(2035, 2, 18),
                LocalDate.of(2035, 2, 19),
                LocalDate.of(2035, 2, 20),
                LocalDate.of(2035, 2, 21),
                LocalDate.of(2035, 4, 23),
                LocalDate.of(2035, 5, 1),
                LocalDate.of(2035, 5, 19),
                LocalDate.of(2035, 7, 15),
                LocalDate.of(2035, 8, 30),
                LocalDate.of(2035, 10, 28),
                LocalDate.of(2035, 10, 29),
                LocalDate.of(2035, 11, 30),
                LocalDate.of(2035, 12, 1),
                LocalDate.of(2035, 12, 2),
                LocalDate.of(2035, 12, 3)
            )
        )
    }
}
