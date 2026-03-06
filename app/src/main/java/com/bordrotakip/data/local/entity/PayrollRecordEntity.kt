package com.bordrotakip.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bordrotakip.domain.model.PayrollPeriod
import com.bordrotakip.domain.model.PayrollRecord
import java.time.LocalDate

/**
 * PayrollRecord Room Entity
 */
@Entity(
    tableName = "payroll_records",
    indices = [
        Index(value = ["periodYear", "periodMonth"])
    ]
)
data class PayrollRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Dönem bilgisi
    val periodYear: Int,
    val periodMonth: Int,
    val periodStartEpochDay: Long,
    val periodEndEpochDay: Long,
    
    // Kazançlar
    val grossSalary: Double,
    val overtimeAmount: Double = 0.0,
    val bonusAmount: Double = 0.0,
    val totalEarnings: Double = 0.0,
    
    // Kesintiler
    val sgkEmployee: Double = 0.0,
    val unemployment: Double = 0.0,
    val incomeTax: Double = 0.0,
    val stampTax: Double = 0.0,
    val bes: Double = 0.0,
    val otherDeductions: Double = 0.0,
    val totalDeductions: Double = 0.0,
    
    // Net
    val netSalary: Double = 0.0,
    
    // Matrahlar
    val sgkBase: Double = 0.0,
    val taxBase: Double = 0.0,
    val cumulativeTaxBase: Double = 0.0,
    
    // Çalışma bilgileri
    val workDays: Int = 0,
    val workHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val leaveDays: Int = 0,
    val sickLeaveDays: Int = 0,
    
    // Meta
    val photoPath: String? = null,
    val note: String = "",
    val isManualEntry: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): PayrollRecord {
        val period = PayrollPeriod(
            year = periodYear,
            month = periodMonth,
            startDate = LocalDate.ofEpochDay(periodStartEpochDay),
            endDate = LocalDate.ofEpochDay(periodEndEpochDay)
        )

        val safeTotalEarnings = if (totalEarnings > 0.0) totalEarnings else (grossSalary + overtimeAmount + bonusAmount)
        val safeTotalDeductions = if (totalDeductions > 0.0) {
            totalDeductions
        } else {
            (sgkEmployee + unemployment + incomeTax + stampTax + bes + otherDeductions)
        }

        return PayrollRecord(
            id = id,
            period = period,
            grossSalary = grossSalary,
            overtimeAmount = overtimeAmount,
            bonusAmount = bonusAmount,
            totalEarnings = safeTotalEarnings,
            sgkEmployee = sgkEmployee,
            unemployment = unemployment,
            incomeTax = incomeTax,
            stampTax = stampTax,
            bes = bes,
            otherDeductions = otherDeductions,
            totalDeductions = safeTotalDeductions,
            netSalary = netSalary,
            sgkBase = sgkBase,
            taxBase = taxBase,
            cumulativeTaxBase = cumulativeTaxBase,
            workDays = workDays,
            workHours = workHours,
            overtimeHours = overtimeHours,
            leaveDays = leaveDays,
            sickLeaveDays = sickLeaveDays,
            photoPath = photoPath,
            note = note,
            isManualEntry = isManualEntry,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(record: PayrollRecord): PayrollRecordEntity {
            val safeTotalEarnings = if (record.totalEarnings > 0.0) {
                record.totalEarnings
            } else {
                record.grossSalary + record.overtimeAmount + record.bonusAmount
            }

            val safeTotalDeductions = if (record.totalDeductions > 0.0) {
                record.totalDeductions
            } else {
                record.sgkEmployee + record.unemployment + record.incomeTax + record.stampTax + record.bes + record.otherDeductions
            }

            return PayrollRecordEntity(
                id = record.id,
                periodYear = record.period.year,
                periodMonth = record.period.month,
                periodStartEpochDay = record.period.startDate.toEpochDay(),
                periodEndEpochDay = record.period.endDate.toEpochDay(),
                grossSalary = record.grossSalary,
                overtimeAmount = record.overtimeAmount,
                bonusAmount = record.bonusAmount,
                totalEarnings = safeTotalEarnings,
                sgkEmployee = record.sgkEmployee,
                unemployment = record.unemployment,
                incomeTax = record.incomeTax,
                stampTax = record.stampTax,
                bes = record.bes,
                otherDeductions = record.otherDeductions,
                totalDeductions = safeTotalDeductions,
                netSalary = record.netSalary,
                sgkBase = record.sgkBase,
                taxBase = record.taxBase,
                cumulativeTaxBase = record.cumulativeTaxBase,
                workDays = record.workDays,
                workHours = record.workHours,
                overtimeHours = record.overtimeHours,
                leaveDays = record.leaveDays,
                sickLeaveDays = record.sickLeaveDays,
                photoPath = record.photoPath,
                note = record.note,
                isManualEntry = record.isManualEntry,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt
            )
        }
    }
}
