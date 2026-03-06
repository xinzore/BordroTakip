package com.bordrotakip.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalculatorTest {

    @Test
    fun `getCurrentBracket - boundary uses next bracket`() {
        val calculator = TaxCalculator()

        val b1 = calculator.getCurrentBracket(year = 2026, cumulativeBase = 0.0)
        assertEquals(15, b1.percentage)

        val b2 = calculator.getCurrentBracket(year = 2026, cumulativeBase = 190_000.0)
        assertEquals(20, b2.percentage)
    }

    @Test
    fun `calculateIncomeTax - splits month base across brackets`() {
        val calculator = TaxCalculator()

        // 2026 varsayılan değerlerle:
        // 0-190k => %15, 190k-400k => %20
        val result = calculator.calculateIncomeTax(
            year = 2026,
            cumulativeBase = 189_000.0,
            monthBase = 2_000.0,
            isExempt = false
        )

        // 1.000 TL * %15 + 1.000 TL * %20 = 350 TL
        assertEquals(350.0, result.taxAmount, 0.0001)
        assertEquals(20, result.bracket.percentage)
        assertEquals(2_000.0, result.base, 0.0001)
    }

    @Test
    fun `predictNextBracketEntry - returns a positive month count when near boundary`() {
        val calculator = TaxCalculator()

        val months = calculator.predictNextBracketEntry(
            year = 2026,
            monthlyBase = 10_000.0,
            currentCumulative = 185_000.0
        )

        assertTrue(months != null && months > 0)
    }
}

