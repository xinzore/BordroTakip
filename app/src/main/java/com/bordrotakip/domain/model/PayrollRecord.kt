package com.bordrotakip.domain.model

import com.bordrotakip.util.formatLocalizedCurrency

/**
 * Bordro kaydı
 * Ay sonunda bordro fotoğrafından veya manuel girişten oluşturulan kayıt
 */
data class PayrollRecord(
    val id: Long = 0,
    val period: PayrollPeriod,
    
    // Kazançlar
    val grossSalary: Double,        // Aylık brüt ücret
    val overtimeAmount: Double = 0.0, // Fazla mesai tutarı
    val bonusAmount: Double = 0.0,    // Prim/ikramiye
    val totalEarnings: Double = 0.0,  // Toplam brüt kazanç
    
    // Kesintiler
    val sgkEmployee: Double = 0.0,    // SGK işçi payı (%14)
    val unemployment: Double = 0.0,    // İşsizlik sigortası (%1)
    val incomeTax: Double = 0.0,      // Gelir vergisi
    val stampTax: Double = 0.0,       // Damga vergisi
    val bes: Double = 0.0,            // BES kesintisi
    val otherDeductions: Double = 0.0, // Diğer kesintiler
    val totalDeductions: Double = 0.0, // Toplam kesinti
    
    // Net ödeme
    val netSalary: Double = 0.0,      // Net maaş
    
    // Matrahlar
    val sgkBase: Double = 0.0,        // SGK matrahı
    val taxBase: Double = 0.0,        // Gelir vergisi matrahı
    val cumulativeTaxBase: Double = 0.0, // Kümülatif vergi matrahı
    
    // Çalışma bilgileri
    val workDays: Int = 0,            // Çalışılan gün
    val workHours: Double = 0.0,      // Çalışılan saat
    val overtimeHours: Double = 0.0,  // Fazla mesai saati
    val leaveDays: Int = 0,           // İzin günleri
    val sickLeaveDays: Int = 0,       // Rapor günleri
    
    // Meta bilgiler
    val photoPath: String? = null,    // Bordro fotoğrafı yolu
    val note: String = "",
    val isManualEntry: Boolean = true, // Manuel mi yoksa OCR mi
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Kesinti oranı
     */
    val deductionRate: Double
        get() = if (totalEarnings > 0) (totalDeductions / totalEarnings) * 100 else 0.0
    
    /**
     * Net/brüt oranı
     */
    val netToGrossRate: Double
        get() = if (totalEarnings > 0) (netSalary / totalEarnings) * 100 else 0.0
    
    fun getDisplayNetSalary(): String {
        return formatLocalizedCurrency(netSalary)
    }
    
    fun getDisplayGrossSalary(): String {
        return formatLocalizedCurrency(totalEarnings)
    }
}
