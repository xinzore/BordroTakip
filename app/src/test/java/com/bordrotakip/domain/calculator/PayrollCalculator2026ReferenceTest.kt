package com.bordrotakip.domain.calculator

import com.bordrotakip.domain.calendar.TrHolidayCalendar
import org.junit.Assert.assertEquals
import org.junit.Test

class PayrollCalculator2026ReferenceTest {

    private val taxCalculator = TaxCalculator()
    private val holidayCalendar = TrHolidayCalendar()
    private val payrollCalculator = PayrollCalculator(
        taxCalculator = taxCalculator,
        holidayCalendar = holidayCalendar
    )

    @Test
    fun matchesPdfTable_forGross36614_noEvents_noBes() {
        val gross = 36_614.00

        val expectedNetByMonth = mapOf(
            1 to 30_637.74,
            2 to 30_637.74,
            3 to 30_637.74,
            4 to 30_637.74,
            5 to 30_637.74,
            6 to 30_637.74,
            7 to 29_571.50,
            8 to 30_485.42,
            9 to 30_485.42,
            10 to 30_485.42,
            11 to 30_485.42,
            12 to 30_485.42
        )

        val expectedIncomeTaxByMonth = mapOf(
            1 to 456.96,
            2 to 456.96,
            3 to 456.96,
            4 to 456.96,
            5 to 456.96,
            6 to 456.96,
            7 to 1_523.20,
            8 to 609.28,
            9 to 609.28,
            10 to 609.28,
            11 to 609.28,
            12 to 609.28
        )

        val expectedTaxBracketByMonth = mapOf(
            1 to 15,
            2 to 15,
            3 to 15,
            4 to 15,
            5 to 15,
            6 to 15,
            7 to 20,
            8 to 20,
            9 to 20,
            10 to 20,
            11 to 20,
            12 to 20
        )

        for (month in 1..12) {
            val estimate = payrollCalculator.estimateNetSalary(
                taxYear = 2026,
                payrollMonth = month,
                monthlyGross = gross,
                events = emptyList(),
                cumulativeTaxBaseBefore = 0.0,
                isBesEnabled = false
            )

            assertEquals(
                "sgk month=$month",
                5_125.96,
                estimate.sgkDeduction,
                0.01
            )
            assertEquals(
                "unemployment month=$month",
                366.14,
                estimate.unemploymentDeduction,
                0.01
            )
            assertEquals(
                "taxBase month=$month",
                31_121.90,
                estimate.taxBase,
                0.01
            )
            assertEquals(
                "stampTax month=$month",
                27.20,
                estimate.stampTax,
                0.01
            )
            assertEquals(
                "incomeTax month=$month",
                expectedIncomeTaxByMonth.getValue(month),
                estimate.incomeTax,
                0.01
            )
            assertEquals(
                "net month=$month",
                expectedNetByMonth.getValue(month),
                estimate.netSalary,
                0.01
            )
            assertEquals(
                "bracket month=$month",
                expectedTaxBracketByMonth.getValue(month),
                estimate.taxBracket.percentage
            )

            val expectedCumulativeBefore = 31_121.90 * (month - 1).toDouble()
            val expectedCumulativeAfter = expectedCumulativeBefore + 31_121.90

            assertEquals(
                "cumulativeBefore month=$month",
                expectedCumulativeBefore,
                estimate.cumulativeTaxBaseBefore,
                0.01
            )
            assertEquals(
                "cumulativeAfter month=$month",
                expectedCumulativeAfter,
                estimate.cumulativeTaxBaseAfter,
                0.01
            )
        }
    }

    @Test
    fun matchesPdfTable_forGross36614_noEvents_withBes() {
        val gross = 36_614.00
        val bes = gross * 0.03 // %3

        val expectedNetNoBesByMonth = mapOf(
            1 to 30_637.74,
            2 to 30_637.74,
            3 to 30_637.74,
            4 to 30_637.74,
            5 to 30_637.74,
            6 to 30_637.74,
            7 to 29_571.50,
            8 to 30_485.42,
            9 to 30_485.42,
            10 to 30_485.42,
            11 to 30_485.42,
            12 to 30_485.42
        )

        for (month in 1..12) {
            val estimate = payrollCalculator.estimateNetSalary(
                taxYear = 2026,
                payrollMonth = month,
                monthlyGross = gross,
                events = emptyList(),
                cumulativeTaxBaseBefore = 0.0,
                isBesEnabled = true,
                besRate = 0.03
            )

            assertEquals(
                "bes month=$month",
                bes,
                estimate.besDeduction,
                0.01
            )
            assertEquals(
                "netWithBes month=$month",
                expectedNetNoBesByMonth.getValue(month) - bes,
                estimate.netSalary,
                0.01
            )
        }
    }
}

