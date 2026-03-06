package com.bordrotakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bordrotakip.domain.model.GrossHistory
import java.time.LocalDate

/**
 * GrossHistory Room Entity
 */
@Entity(tableName = "gross_history")
data class GrossHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grossAmount: Double,
    val validFromEpochDay: Long,
    val validToEpochDay: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): GrossHistory {
        return GrossHistory(
            id = id,
            grossAmount = grossAmount,
            validFrom = LocalDate.ofEpochDay(validFromEpochDay),
            validTo = validToEpochDay?.let { LocalDate.ofEpochDay(it) },
            note = note,
            createdAt = createdAt
        )
    }
    
    companion object {
        fun fromDomain(grossHistory: GrossHistory): GrossHistoryEntity {
            return GrossHistoryEntity(
                id = grossHistory.id,
                grossAmount = grossHistory.grossAmount,
                validFromEpochDay = grossHistory.validFrom.toEpochDay(),
                validToEpochDay = grossHistory.validTo?.toEpochDay(),
                note = grossHistory.note,
                createdAt = grossHistory.createdAt
            )
        }
    }
}
