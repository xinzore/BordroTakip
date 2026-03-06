package com.bordrotakip.domain.calculator

import com.bordrotakip.domain.model.PayrollPeriod
import java.time.LocalDate
import javax.inject.Inject

/**
 * Bordro dönemi hesaplama motoru
 * 25-24 kesim kuralına göre çalışır
 */
class PayrollPeriodEngine @Inject constructor() {
    
    companion object {
        const val DEFAULT_CUTOFF_DAY = 24
    }
    
    private var cutoffDay: Int = DEFAULT_CUTOFF_DAY
    
    /**
     * Kesim gününü ayarla
     */
    fun setCutoffDay(day: Int) {
        require(day in 1..28) { "Kesim günü 1-28 arasında olmalıdır" }
        cutoffDay = day
    }
    
    /**
     * Verilen tarih hangi bordro dönemine ait?
     */
    fun getPeriodForDate(date: LocalDate): PayrollPeriod {
        val dayOfMonth = date.dayOfMonth
        
        // Eğer gün kesim gününden büyükse, sonraki ayın bordrosuna gider
        return if (dayOfMonth > cutoffDay) {
            // Sonraki ay
            val nextMonth = date.plusMonths(1)
            PayrollPeriod(
                year = nextMonth.year,
                month = nextMonth.monthValue,
                startDate = date.withDayOfMonth(cutoffDay + 1),
                endDate = nextMonth.withDayOfMonth(cutoffDay)
            )
        } else {
            // Bu ay
            val previousMonth = date.minusMonths(1)
            PayrollPeriod(
                year = date.year,
                month = date.monthValue,
                startDate = previousMonth.withDayOfMonth(cutoffDay + 1),
                endDate = date.withDayOfMonth(cutoffDay)
            )
        }
    }
    
    /**
     * Belirli bir ay ve yıl için bordro dönemini getir
     */
    fun getPeriodForMonth(year: Int, month: Int): PayrollPeriod {
        val targetDate = LocalDate.of(year, month, 1)
        val previousMonth = targetDate.minusMonths(1)
        
        return PayrollPeriod(
            year = year,
            month = month,
            startDate = previousMonth.withDayOfMonth(cutoffDay + 1),
            endDate = targetDate.withDayOfMonth(cutoffDay)
        )
    }
    
    /**
     * Mevcut bordro dönemini getir
     */
    fun getCurrentPeriod(): PayrollPeriod {
        return getPeriodForDate(LocalDate.now())
    }
    
    /**
     * Verilen tarih mevcut bordro döneminde mi?
     */
    fun isDateInCurrentPeriod(date: LocalDate): Boolean {
        return getCurrentPeriod().contains(date)
    }
    
    /**
     * Tarih, bordro döneminin hangi tarafında?
     * Yıl kırılımlarında önemli (Aralık sonu vs Ocak başı)
     */
    fun getYearForTaxPurpose(date: LocalDate): Int {
        val period = getPeriodForDate(date)
        // Vergi yılı bordro ayına göre belirlenir
        return period.year
    }
    
    /**
     * İki tarih arasındaki brüt hesaplama için dönem kırılımı
     * Zam dönemlerinde eski/yeni brüt ayırımı için
     */
    fun splitDatesByGrossValidity(
        periodStartDate: LocalDate,
        periodEndDate: LocalDate,
        grossChangeDate: LocalDate
    ): Pair<IntRange, IntRange> {
        val oldGrossDays = if (grossChangeDate.isAfter(periodStartDate)) {
            val daysWithOldGross = java.time.temporal.ChronoUnit.DAYS
                .between(periodStartDate, grossChangeDate.minusDays(1)).toInt() + 1
            1..daysWithOldGross
        } else {
            IntRange.EMPTY
        }
        
        val newGrossDays = if (grossChangeDate.isBefore(periodEndDate) || 
                               grossChangeDate.isEqual(periodEndDate)) {
            val startDay = if (oldGrossDays.isEmpty()) 1 else oldGrossDays.last + 1
            val totalDays = java.time.temporal.ChronoUnit.DAYS
                .between(periodStartDate, periodEndDate).toInt() + 1
            startDay..totalDays
        } else {
            IntRange.EMPTY
        }
        
        return Pair(oldGrossDays, newGrossDays)
    }
}
