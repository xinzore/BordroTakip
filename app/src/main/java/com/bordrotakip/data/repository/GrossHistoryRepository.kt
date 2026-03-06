package com.bordrotakip.data.repository

import com.bordrotakip.data.local.dao.GrossHistoryDao
import com.bordrotakip.data.local.entity.GrossHistoryEntity
import com.bordrotakip.domain.model.GrossHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrossHistoryRepository @Inject constructor(
    private val grossHistoryDao: GrossHistoryDao
) {
    fun getAllGrossHistory(): Flow<List<GrossHistory>> {
        return grossHistoryDao.getAllGrossHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getCurrentGross(): GrossHistory? {
        return grossHistoryDao.getCurrentGross(LocalDate.now().toEpochDay())?.toDomain()
    }
    
    suspend fun getGrossForDate(date: LocalDate): GrossHistory? {
        return grossHistoryDao.getGrossForDate(date.toEpochDay())?.toDomain()
    }
    
    suspend fun getLatestGross(): GrossHistory? {
        return grossHistoryDao.getLatestGross()?.toDomain()
    }
    
    suspend fun insert(grossHistory: GrossHistory): Long {
        // Önceki güncel kaydın bitiş tarihini ayarla
        val current = grossHistoryDao.getLatestGross()
        if (current != null && current.validToEpochDay == null) {
            val newFrom = grossHistory.validFrom.toEpochDay()
            if (newFrom <= current.validFromEpochDay) {
                grossHistoryDao.update(
                    current.copy(
                        grossAmount = grossHistory.grossAmount,
                        note = grossHistory.note.ifBlank { current.note }
                    )
                )
                return current.id
            } else {
                grossHistoryDao.update(
                    current.copy(
                        validToEpochDay = grossHistory.validFrom.minusDays(1).toEpochDay()
                    )
                )
            }
        }
        
        return grossHistoryDao.insert(GrossHistoryEntity.fromDomain(grossHistory))
    }
    
    suspend fun update(grossHistory: GrossHistory) {
        grossHistoryDao.update(GrossHistoryEntity.fromDomain(grossHistory))
    }
    
    suspend fun deleteById(id: Long) {
        grossHistoryDao.deleteById(id)
    }
}
