package com.bordrotakip.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Gün bazlı çalışma olayı kaydı
 * Takvimde bir güne ait bir event (vardiya, mesai, izin, rapor vb.)
 */
data class WorkEvent(
    val id: Long = 0,
    val date: LocalDate,
    val eventType: EventType,
    val shiftType: ShiftType? = null, // Vardiya veya mesai için
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val hours: Double = 0.0, // Toplam saat
    val note: String = "",
    val payrollPeriodLabel: String = "", // "Mayıs 2026"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Bu event brütü artırıyor mu?
     */
    val increasesGross: Boolean
        get() = when (eventType) {
            EventType.SHIFT -> true
            EventType.OVERTIME_NORMAL -> true
            EventType.OVERTIME_WEEKEND -> true
            EventType.OVERTIME_HOLIDAY -> true
            else -> false
        }
    
    /**
     * Bu event brütü azaltıyor mu?
     */
    val decreasesGross: Boolean
        get() = when (eventType) {
            EventType.LEAVE_UNPAID -> true
            EventType.SICK_LEAVE -> true // İlk 2 gün hariç SGK'dan
            else -> false
        }
    
    /**
     * Mesai çarpanı (normal=1.5, hafta sonu=2, bayram=2 veya 3)
     */
    val overtimeMultiplier: Double
        get() = when (eventType) {
            EventType.OVERTIME_NORMAL -> 1.5
            EventType.OVERTIME_WEEKEND -> 2.0
            EventType.OVERTIME_HOLIDAY -> 2.0 // Bazı firmalarda 3.0
            else -> 1.0
        }
    
    fun getDisplayLabel(): String {
        val shiftLabel = shiftType?.displayName?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return when (eventType) {
            EventType.SHIFT -> "Vardiya$shiftLabel"
            EventType.OVERTIME_NORMAL,
            EventType.OVERTIME_WEEKEND,
            EventType.OVERTIME_HOLIDAY -> "${eventType.displayName}$shiftLabel"
            else -> eventType.displayName
        }
    }
}
