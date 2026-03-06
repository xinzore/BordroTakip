package com.bordrotakip.domain.calculator

import com.bordrotakip.domain.calendar.TrHolidayCalendar
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.domain.model.EventType
import com.bordrotakip.domain.model.WorkEvent
import javax.inject.Inject

/**
 * Maaş hesaplama sonucu
 */
data class SalaryEstimate(
    val grossSalary: Double,         // Aylık brüt
    val overtimeAmount: Double,      // Mesai tutarı
    val totalEarnings: Double,       // Toplam kazanç
    val sgkDeduction: Double,        // SGK kesintisi
    val unemploymentDeduction: Double, // İşsizlik kesintisi
    val taxBase: Double,             // Gelir vergisi matrahı (aylık)
    val incomeTax: Double,           // Gelir vergisi
    val stampTax: Double,            // Damga vergisi
    val besDeduction: Double = 0.0,  // BES kesintisi
    val totalDeductions: Double,     // Toplam kesinti
    val netSalary: Double,           // Net maaş
    val taxBracket: TaxBracket,      // Vergi dilimi
    val cumulativeTaxBaseBefore: Double, // Ay başı kümülatif
    val cumulativeTaxBaseAfter: Double,  // Ay sonu (tahmini) kümülatif
    val confidenceRate: Double = 0.95 // Güven oranı (%95)
)

/**
 * Bordro hesaplayıcı
 * SGK, işsizlik, vergi hesaplamaları
 */
class PayrollCalculator @Inject constructor(
    private val taxCalculator: TaxCalculator,
    private val holidayCalendar: TrHolidayCalendar
) {
    companion object {
        // SGK oranları
        const val SGK_EMPLOYEE_RATE = 0.14      // %14 işçi payı
        const val SGK_EMPLOYER_RATE = 0.205     // %20.5 işveren payı
        const val UNEMPLOYMENT_EMPLOYEE_RATE = 0.01 // %1 işsizlik işçi payı
        const val SGDP_EMPLOYEE_RATE = 0.075    // %7.5 emekli çalışan (SGDP) işçi payı
        
        // Damga vergisi
        const val STAMP_TAX_RATE = 0.00759      // Binde 7.59
        
        // Mesai çarpanları
        const val OVERTIME_MULTIPLIER_NORMAL = 1.5
        const val OVERTIME_MULTIPLIER_WEEKEND = 2.0
        const val OVERTIME_MULTIPLIER_HOLIDAY = 2.0
    }
    
    /**
     * SGK/SGDP ve işsizlik kesintilerini hesapla
     */
    fun calculateSgkDeductions(
        taxYear: Int,
        grossAmount: Double,
        employmentType: EmploymentType = EmploymentType.STANDARD
    ): Pair<Double, Double> {
        // SGK tavanı kontrolü
        val sgkCeiling = taxCalculator.getMinWageGross(taxYear) * 7.5
        val sgkBase = minOf(grossAmount, sgkCeiling)

        val sgkEmployee = when (employmentType) {
            EmploymentType.RETIRED -> sgkBase * SGDP_EMPLOYEE_RATE
            EmploymentType.STANDARD -> sgkBase * SGK_EMPLOYEE_RATE
        }
        val unemployment = when (employmentType) {
            EmploymentType.RETIRED -> 0.0
            EmploymentType.STANDARD -> sgkBase * UNEMPLOYMENT_EMPLOYEE_RATE
        }
        
        return Pair(sgkEmployee, unemployment)
    }
    
    /**
     * Damga vergisi hesapla
     */
    fun calculateStampTax(grossAmount: Double): Double {
        return grossAmount * STAMP_TAX_RATE
    }
    
    /**
     * Gelir vergisi matrahını hesapla
     * Brüt - SGK/SGDP - İşsizlik
     */
    fun calculateTaxBase(
        taxYear: Int,
        grossAmount: Double,
        employmentType: EmploymentType = EmploymentType.STANDARD
    ): Double {
        val (sgk, unemployment) = calculateSgkDeductions(taxYear, grossAmount, employmentType)
        return grossAmount - sgk - unemployment
    }
    
    /**
     * WorkEvent listesinden mesai saatlerini çıkar
     */
    fun extractOvertimeHours(
        events: List<WorkEvent>,
        nonWorkingDaysMask: Int = TrHolidayCalendar.DEFAULT_NON_WORKING_DAYS_MASK
    ): Triple<Double, Double, Double> {
        var normal = 0.0
        var weekend = 0.0
        var holiday = 0.0
        
        for (event in events) {
            val type = if (event.eventType in EventType.overtimeTypes) {
                holidayCalendar.classifyOvertime(event.date, nonWorkingDaysMask)
            } else {
                event.eventType
            }

            when (type) {
                EventType.OVERTIME_NORMAL -> normal += event.hours
                EventType.OVERTIME_WEEKEND -> weekend += event.hours
                EventType.OVERTIME_HOLIDAY -> holiday += event.hours
                else -> {}
            }
        }
        
        return Triple(normal, weekend, holiday)
    }

    /**
     * Fazla mesai ücretini hesapla
     */
    fun calculateOvertimeAmount(
        hourlyRate: Double,
        normalHours: Double,
        weekendHours: Double,
        holidayHours: Double,
        normalMultiplier: Double = OVERTIME_MULTIPLIER_NORMAL,
        weekendMultiplier: Double = OVERTIME_MULTIPLIER_WEEKEND,
        holidayMultiplier: Double = OVERTIME_MULTIPLIER_HOLIDAY
    ): Double {
        return (hourlyRate * normalHours * normalMultiplier) +
               (hourlyRate * weekendHours * weekendMultiplier) +
               (hourlyRate * holidayHours * holidayMultiplier)
    }
    
    /**
     * Tahmini net maaş hesapla
     */
    fun estimateNetSalary(
        taxYear: Int,
        payrollMonth: Int,
        monthlyGross: Double,
        events: List<WorkEvent>,
        nonWorkingDaysMask: Int = TrHolidayCalendar.DEFAULT_NON_WORKING_DAYS_MASK,
        cumulativeTaxBaseBefore: Double,
        overtimeMultiplierNormal: Double = OVERTIME_MULTIPLIER_NORMAL,
        overtimeMultiplierWeekend: Double = OVERTIME_MULTIPLIER_WEEKEND,
        overtimeMultiplierHoliday: Double = OVERTIME_MULTIPLIER_HOLIDAY,
        employmentType: EmploymentType = EmploymentType.STANDARD,
        isBesEnabled: Boolean = false,
        besRate: Double = 0.03
    ): SalaryEstimate {
        // Saatlik ücret (Bordro örneğindeki gibi 225 saat üzerinden)
        val hourlyRate = monthlyGross / 225.0 
        
        val (normalHours, weekendHours, holidayHours) = extractOvertimeHours(events, nonWorkingDaysMask)
        
        val overtimeAmount = calculateOvertimeAmount(
            hourlyRate = hourlyRate,
            normalHours = normalHours,
            weekendHours = weekendHours,
            holidayHours = holidayHours,
            normalMultiplier = overtimeMultiplierNormal,
            weekendMultiplier = overtimeMultiplierWeekend,
            holidayMultiplier = overtimeMultiplierHoliday
        )
        
        val unpaidLeaveHours = events
            .filter { it.eventType == EventType.LEAVE_UNPAID }
            .sumOf { event -> resolveEventHours(event) }

        val sickLeaveHours = events
            .filter { it.eventType == EventType.SICK_LEAVE }
            .sumOf { event -> resolveEventHours(event) }

        // Basit model: Ücretsiz izin/rapor brütten düşer.
        val leaveDeductionAmount = hourlyRate * (unpaidLeaveHours + sickLeaveHours)

        val totalEarnings = (monthlyGross - leaveDeductionAmount + overtimeAmount).coerceAtLeast(0.0)
        
        // 1. SGK/SGDP ve İşsizlik
        val (sgk, unemp) = calculateSgkDeductions(taxYear, totalEarnings, employmentType)
        
        // 2. Gelir Vergisi Matrahı
        val taxBase = totalEarnings - sgk - unemp

        val defaultMonthTaxBase = calculateTaxBase(taxYear, monthlyGross, employmentType)
        val effectiveCumulativeBefore = if (cumulativeTaxBaseBefore == 0.0 && payrollMonth > 1) {
            defaultMonthTaxBase * (payrollMonth - 1).toDouble()
        } else {
            cumulativeTaxBaseBefore
        }
        
        // 3. Gelir Vergisi (İstisnalı)
        val taxResult = taxCalculator.calculateIncomeTax(
            year = taxYear,
            cumulativeBase = effectiveCumulativeBefore,
            monthBase = taxBase,
            monthIndexInYear = payrollMonth,
            isExempt = true
        )
        
        // 4. Damga Vergisi (İstisnalı)
        val rawStampTax = totalEarnings * STAMP_TAX_RATE
        val exemptStampTax = taxCalculator.calculateStampTaxExemption(taxYear)
        val stampTax = (rawStampTax - exemptStampTax).coerceAtLeast(0.0)
        
        // 5. BES Kesintisi
        val besAmount = if (isBesEnabled) totalEarnings * besRate else 0.0
        
        // Toplam Kesinti
        val totalDeductions = sgk + unemp + taxResult.taxAmount + stampTax + besAmount
        
        val netSalary = totalEarnings - totalDeductions
        val cumulativeAfter = effectiveCumulativeBefore + taxBase
        
        return SalaryEstimate(
            grossSalary = monthlyGross,
            overtimeAmount = overtimeAmount,
            totalEarnings = totalEarnings,
            sgkDeduction = sgk,
            unemploymentDeduction = unemp,
            taxBase = taxBase,
            incomeTax = taxResult.taxAmount,
            stampTax = stampTax,
            besDeduction = besAmount,
            totalDeductions = totalDeductions,
            netSalary = netSalary,
            taxBracket = taxResult.bracket,
            cumulativeTaxBaseBefore = effectiveCumulativeBefore,
            cumulativeTaxBaseAfter = cumulativeAfter
        )
    }

    private fun resolveEventHours(event: WorkEvent): Double {
        return when {
            event.hours > 0 -> event.hours
            event.shiftType != null -> event.shiftType.durationHours.toDouble()
            else -> 8.0
        }
    }
    
    /**
     * Hızlı tahmin (sadece data inputlarla)
     */
    fun quickEstimate(
        taxYear: Int,
        payrollMonth: Int,
        monthlyGross: Double,
        overtimeHours: Double,
        cumulativeTaxBaseBefore: Double,
        overtimeMultiplierNormal: Double = OVERTIME_MULTIPLIER_NORMAL,
        employmentType: EmploymentType = EmploymentType.STANDARD
    ): SalaryEstimate {
        val hourlyRate = monthlyGross / 225.0
        
        val overtimeAmount = hourlyRate * overtimeHours * overtimeMultiplierNormal
        val totalEarnings = monthlyGross + overtimeAmount
        
        val (sgk, unemp) = calculateSgkDeductions(taxYear, totalEarnings, employmentType)
        val taxBase = totalEarnings - sgk - unemp

        val defaultMonthTaxBase = calculateTaxBase(taxYear, monthlyGross, employmentType)
        val effectiveCumulativeBefore = if (cumulativeTaxBaseBefore == 0.0 && payrollMonth > 1) {
            defaultMonthTaxBase * (payrollMonth - 1).toDouble()
        } else {
            cumulativeTaxBaseBefore
        }
        
        val taxResult = taxCalculator.calculateIncomeTax(
            year = taxYear,
            cumulativeBase = effectiveCumulativeBefore,
            monthBase = taxBase,
            monthIndexInYear = payrollMonth,
            isExempt = true
        )
        
        val rawStampTax = totalEarnings * STAMP_TAX_RATE
        val exemptStampTax = taxCalculator.calculateStampTaxExemption(taxYear)
        val stampTax = (rawStampTax - exemptStampTax).coerceAtLeast(0.0)
        
        val totalDeductions = sgk + unemp + taxResult.taxAmount + stampTax
        val netSalary = totalEarnings - totalDeductions
        val cumulativeAfter = effectiveCumulativeBefore + taxBase
        
        return SalaryEstimate(
            grossSalary = monthlyGross,
            overtimeAmount = overtimeAmount,
            totalEarnings = totalEarnings,
            sgkDeduction = sgk,
            unemploymentDeduction = unemp,
            taxBase = taxBase,
            incomeTax = taxResult.taxAmount,
            stampTax = stampTax,
            besDeduction = 0.0,
            totalDeductions = totalDeductions,
            netSalary = netSalary,
            taxBracket = taxResult.bracket,
            cumulativeTaxBaseBefore = effectiveCumulativeBefore,
            cumulativeTaxBaseAfter = cumulativeAfter
        )
    }
}
