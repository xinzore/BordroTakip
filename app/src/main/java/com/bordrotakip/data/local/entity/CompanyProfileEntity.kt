package com.bordrotakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bordrotakip.domain.model.EmploymentType
import java.time.DayOfWeek

/**
 * Şirket profili Room Entity
 */
@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey
    val id: Long = 1, // Sadece 1 profil olacak
    val companyName: String = "",
    val cutoffDay: Int = 24, // Bordro kesim günü
    val shiftDefinitions: String = "", // JSON olarak vardiya tanımları
    val isBesEnabled: Boolean = false, // BES Otomatik Katılım
    val besRate: Double = 0.03, // BES Oranı (Varsayılan %3)
    val overtimeMultiplierNormal: Double = 1.5, // Normal mesai katsayısı
    val overtimeMultiplierWeekend: Double = 2.0, // Hafta sonu mesai katsayısı
    val overtimeMultiplierHoliday: Double = 2.0, // Resmi tatil mesai katsayısı
    val employmentType: String = EmploymentType.STANDARD.name, // Çalışma statüsü (normal / emekli)
    val nonWorkingDaysMask: Int = DEFAULT_NON_WORKING_DAYS_MASK, // Haftalık tatil günleri (bitmask)
    val cumulativeTaxBase: Double = 0.0, // Kümülatif Vergi Matrahı
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        // Varsayılan: Cumartesi + Pazar tatil
        val DEFAULT_NON_WORKING_DAYS_MASK: Int =
            (1 shl (DayOfWeek.SATURDAY.value - 1)) or (1 shl (DayOfWeek.SUNDAY.value - 1))
    }
}
