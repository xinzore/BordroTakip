package com.bordrotakip.domain.model

import java.time.LocalDate

/**
 * Bordro dönemi
 * 25-24 kesim kuralına göre (örn: 25 Nisan - 24 Mayıs = Mayıs bordrosu)
 */
data class PayrollPeriod(
    val year: Int,
    val month: Int, // 1-12
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    /**
     * Dönem etiketi: "Mayıs 2026"
     */
    val label: String
        get() {
            val monthNames = listOf(
                "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
                "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
            )
            return "${monthNames[month - 1]} $year"
        }
    
    /**
     * Detaylı etiket: "Mayıs 2026 (25 Nis - 24 May)"
     */
    val detailedLabel: String
        get() {
            val shortMonthNames = listOf(
                "Oca", "Şub", "Mar", "Nis", "May", "Haz",
                "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"
            )
            val startMonth = shortMonthNames[startDate.monthValue - 1]
            val endMonth = shortMonthNames[endDate.monthValue - 1]
            return "$label (${startDate.dayOfMonth} $startMonth - ${endDate.dayOfMonth} $endMonth)"
        }
    
    /**
     * Bu tarih bu döneme ait mi?
     */
    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }
}
