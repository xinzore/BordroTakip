package com.bordrotakip.domain.model

/**
 * Vardiya türleri
 * Fabrikada kullanılan 3 standart vardiya
 */
enum class ShiftType(
    val startHour: Int,
    val endHour: Int,
    val displayName: String,
    val emoji: String
) {
    SHIFT_08_16(8, 16, "08-16", "🌅"),
    SHIFT_16_24(16, 24, "16-24", "🌇"),
    SHIFT_24_08(0, 8, "24-08", "🌙");
    
    val durationHours: Int
        get() = if (endHour > startHour) endHour - startHour else 24 - startHour + endHour
    
    fun getDisplayLabel(): String = "$emoji $displayName"
    
    companion object {
        fun fromHours(start: Int, end: Int): ShiftType? {
            return entries.find { it.startHour == start && it.endHour == end }
        }
    }
}
