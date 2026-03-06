package com.bordrotakip.domain.model

import androidx.compose.ui.graphics.Color
import com.bordrotakip.ui.theme.AnnualLeaveColor
import com.bordrotakip.ui.theme.HolidayColor
import com.bordrotakip.ui.theme.OvertimeColor
import com.bordrotakip.ui.theme.ShiftColor
import com.bordrotakip.ui.theme.SickLeaveColor
import com.bordrotakip.ui.theme.UnpaidLeaveColor

/**
 * Çalışma olayı türleri
 */
enum class EventType(
    val displayName: String,
    val emoji: String,
    val color: Color,
    val affectsGross: Boolean,
    val affectsWorkDays: Boolean
) {
    // Vardiya (normal çalışma)
    SHIFT(
        displayName = "Vardiya",
        emoji = "🟦",
        color = ShiftColor,
        affectsGross = true,
        affectsWorkDays = true
    ),
    
    // Fazla mesai türleri
    OVERTIME_NORMAL(
        displayName = "Fazla Mesai",
        emoji = "🟥",
        color = OvertimeColor,
        affectsGross = true,
        affectsWorkDays = false
    ),
    OVERTIME_WEEKEND(
        displayName = "Hafta Sonu Mesai",
        emoji = "🟥",
        color = OvertimeColor,
        affectsGross = true,
        affectsWorkDays = false
    ),
    OVERTIME_HOLIDAY(
        displayName = "Tatil Mesaisi",
        emoji = "🟪",
        color = HolidayColor,
        affectsGross = true,
        affectsWorkDays = false
    ),
    
    // İzin türleri
    LEAVE_ANNUAL(
        displayName = "Yıllık İzin",
        emoji = "🟩",
        color = AnnualLeaveColor,
        affectsGross = false, // Yıllık izin brütü etkilemez
        affectsWorkDays = false
    ),
    LEAVE_UNPAID(
        displayName = "Ücretsiz İzin",
        emoji = "🟧",
        color = UnpaidLeaveColor,
        affectsGross = true, // Brütten düşer
        affectsWorkDays = false
    ),
    LEAVE_PAID(
        displayName = "Ücretli İzin",
        emoji = "🟩",
        color = AnnualLeaveColor,
        affectsGross = false,
        affectsWorkDays = false
    ),
    
    // Rapor
    SICK_LEAVE(
        displayName = "Rapor",
        emoji = "🟨",
        color = SickLeaveColor,
        affectsGross = true, // SGK'dan ödenir (ilk 2 gün işverenden)
        affectsWorkDays = false
    ),
    
    // Resmi tatil
    HOLIDAY(
        displayName = "Resmi Tatil",
        emoji = "🟪",
        color = HolidayColor,
        affectsGross = false,
        affectsWorkDays = false
    );
    
    fun getDisplayLabel(): String = "$emoji $displayName"
    
    companion object {
        val overtimeTypes = listOf(OVERTIME_NORMAL, OVERTIME_WEEKEND, OVERTIME_HOLIDAY)
        val leaveTypes = listOf(LEAVE_ANNUAL, LEAVE_UNPAID, LEAVE_PAID)
    }
}
