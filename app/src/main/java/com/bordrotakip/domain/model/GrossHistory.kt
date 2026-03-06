package com.bordrotakip.domain.model

import com.bordrotakip.util.formatLocalizedCurrency
import java.time.LocalDate

/**
 * Brüt maaş geçmişi
 * Zam tarihleri ile birlikte brüt maaş değişikliklerini takip eder
 */
data class GrossHistory(
    val id: Long = 0,
    val grossAmount: Double, // Brüt maaş (TL)
    val validFrom: LocalDate, // Geçerlilik başlangıç tarihi
    val validTo: LocalDate? = null, // Geçerlilik bitiş tarihi (null = hala geçerli)
    val note: String = "", // "2026 zammı" gibi
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Günlük brüt ücret (30 gün varsayımı)
     */
    val dailyGross: Double
        get() = grossAmount / 30.0
    
    /**
     * Saatlik ücret (günde 7.5 saat varsayımı)
     */
    val hourlyRate: Double
        get() = dailyGross / 7.5
    
    /**
     * Bu tarih bu brüt için geçerli mi?
     */
    fun isValidFor(date: LocalDate): Boolean {
        val afterStart = !date.isBefore(validFrom)
        val beforeEnd = validTo == null || !date.isAfter(validTo)
        return afterStart && beforeEnd
    }
    
    fun getDisplayLabel(): String {
        return formatLocalizedCurrency(grossAmount)
    }
}
