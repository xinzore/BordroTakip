package com.bordrotakip.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PayrollPeriodEngineTest {

    @Test
    fun `getPeriodForDate - 25 Nisan belongs to May period (25-24)`() {
        val engine = PayrollPeriodEngine().apply { setCutoffDay(24) }

        val period = engine.getPeriodForDate(LocalDate.of(2024, 4, 25))

        assertEquals(2024, period.year)
        assertEquals(5, period.month)
        assertEquals(LocalDate.of(2024, 4, 25), period.startDate)
        assertEquals(LocalDate.of(2024, 5, 24), period.endDate)
    }

    @Test
    fun `getPeriodForDate - 24 Nisan belongs to April period (25-24)`() {
        val engine = PayrollPeriodEngine().apply { setCutoffDay(24) }

        val period = engine.getPeriodForDate(LocalDate.of(2024, 4, 24))

        assertEquals(2024, period.year)
        assertEquals(4, period.month)
        assertEquals(LocalDate.of(2024, 3, 25), period.startDate)
        assertEquals(LocalDate.of(2024, 4, 24), period.endDate)
    }

    @Test
    fun `getPeriodForDate - year rollover 25 Aralik 2025 belongs to January 2026 period`() {
        val engine = PayrollPeriodEngine().apply { setCutoffDay(24) }

        val period = engine.getPeriodForDate(LocalDate.of(2025, 12, 25))

        assertEquals(2026, period.year)
        assertEquals(1, period.month)
        assertEquals(LocalDate.of(2025, 12, 25), period.startDate)
        assertEquals(LocalDate.of(2026, 1, 24), period.endDate)
    }
}

