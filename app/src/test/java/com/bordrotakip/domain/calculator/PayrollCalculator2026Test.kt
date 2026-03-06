package com.bordrotakip.domain.calculator

import com.bordrotakip.domain.calendar.TrHolidayCalendar
import com.bordrotakip.domain.model.EmploymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollCalculator2026Test {

    private val calculator = PayrollCalculator(
        taxCalculator = TaxCalculator(),
        holidayCalendar = TrHolidayCalendar()
    )

    @Test
    fun `matches 2026 January reference (gross 36614)`() {
        val estimate = calculator.estimateNetSalary(
            taxYear = 2026,
            payrollMonth = 1,
            monthlyGross = 36_614.0,
            events = emptyList(),
            cumulativeTaxBaseBefore = 0.0,
            isBesEnabled = false
        )

        assertEquals(5_125.96, estimate.sgkDeduction, 0.01)
        assertEquals(366.14, estimate.unemploymentDeduction, 0.01)
        assertEquals(31_121.90, estimate.taxBase, 0.01)
        assertEquals(456.96, estimate.incomeTax, 0.02)
        assertEquals(27.20, estimate.stampTax, 0.01)
        assertEquals(30_637.74, estimate.netSalary, 0.02)
        assertEquals(15, estimate.taxBracket.percentage)
    }

    @Test
    fun `matches 2026 July reference (crosses 15 to 20)`() {
        val estimate = calculator.estimateNetSalary(
            taxYear = 2026,
            payrollMonth = 7,
            monthlyGross = 36_614.0,
            events = emptyList(),
            cumulativeTaxBaseBefore = 186_731.40,
            isBesEnabled = false
        )

        assertEquals(31_121.90, estimate.taxBase, 0.01)
        assertEquals(1_523.20, estimate.incomeTax, 0.02)
        assertEquals(27.20, estimate.stampTax, 0.01)
        assertEquals(29_571.50, estimate.netSalary, 0.02)
        assertEquals(20, estimate.taxBracket.percentage)
    }

    @Test
    fun `estimates 2026 July correctly when cumulative is missing`() {
        val estimate = calculator.estimateNetSalary(
            taxYear = 2026,
            payrollMonth = 7,
            monthlyGross = 36_614.0,
            events = emptyList(),
            cumulativeTaxBaseBefore = 0.0,
            isBesEnabled = false
        )

        assertEquals(29_571.50, estimate.netSalary, 0.02)
        assertEquals(20, estimate.taxBracket.percentage)
    }

    @Test
    fun `matches 2026 August reference (20 bracket)`() {
        val estimate = calculator.estimateNetSalary(
            taxYear = 2026,
            payrollMonth = 8,
            monthlyGross = 36_614.0,
            events = emptyList(),
            cumulativeTaxBaseBefore = 217_853.30,
            isBesEnabled = false
        )

        assertEquals(31_121.90, estimate.taxBase, 0.01)
        assertEquals(609.28, estimate.incomeTax, 0.02)
        assertEquals(27.20, estimate.stampTax, 0.01)
        assertEquals(30_485.42, estimate.netSalary, 0.02)
        assertEquals(20, estimate.taxBracket.percentage)
    }

    @Test
    fun `estimates 2026 August correctly when cumulative is missing`() {
        val estimate = calculator.estimateNetSalary(
            taxYear = 2026,
            payrollMonth = 8,
            monthlyGross = 36_614.0,
            events = emptyList(),
            cumulativeTaxBaseBefore = 0.0,
            isBesEnabled = false
        )

        assertEquals(30_485.42, estimate.netSalary, 0.02)
        assertEquals(20, estimate.taxBracket.percentage)
    }

    @Test
    fun `uses SGDP for retired employees`() {
        val standard = calculator.estimateNetSalary(
            taxYear = 2026,
            payrollMonth = 1,
            monthlyGross = 36_614.0,
            events = emptyList(),
            cumulativeTaxBaseBefore = 0.0,
            employmentType = EmploymentType.STANDARD,
            isBesEnabled = false
        )

        val retired = calculator.estimateNetSalary(
            taxYear = 2026,
            payrollMonth = 1,
            monthlyGross = 36_614.0,
            events = emptyList(),
            cumulativeTaxBaseBefore = 0.0,
            employmentType = EmploymentType.RETIRED,
            isBesEnabled = false
        )

        assertEquals(2_746.05, retired.sgkDeduction, 0.01)
        assertEquals(0.0, retired.unemploymentDeduction, 0.01)
        assertEquals(retired.totalEarnings - retired.sgkDeduction, retired.taxBase, 0.01)
        assertTrue(retired.netSalary > standard.netSalary)
    }
}
