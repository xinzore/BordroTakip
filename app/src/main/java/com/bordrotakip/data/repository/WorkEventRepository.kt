package com.bordrotakip.data.repository

import com.bordrotakip.data.local.dao.WorkEventDao
import com.bordrotakip.data.local.entity.WorkEventEntity
import com.bordrotakip.domain.model.WorkEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkEventRepository @Inject constructor(
    private val workEventDao: WorkEventDao
) {
    fun getAllEvents(): Flow<List<WorkEvent>> {
        return workEventDao.getAllEvents().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getEventsForDate(date: LocalDate): Flow<List<WorkEvent>> {
        return workEventDao.getEventsForDate(date.toEpochDay()).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getEventsForPeriod(startDate: LocalDate, endDate: LocalDate): Flow<List<WorkEvent>> {
        return workEventDao.getEventsForPeriod(
            startDate.toEpochDay(),
            endDate.toEpochDay()
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAllEventsSync(): List<WorkEvent> {
        return workEventDao.getAllEventsSync().map { it.toDomain() }
    }
    
    suspend fun getEventsForPeriodSync(startDate: LocalDate, endDate: LocalDate): List<WorkEvent> {
        return workEventDao.getEventsForPeriodSync(
            startDate.toEpochDay(),
            endDate.toEpochDay()
        ).map { it.toDomain() }
    }
    
    suspend fun getEventById(id: Long): WorkEvent? {
        return workEventDao.getEventById(id)?.toDomain()
    }
    
    suspend fun insert(event: WorkEvent): Long {
        return workEventDao.insert(WorkEventEntity.fromDomain(event))
    }
    
    suspend fun update(event: WorkEvent) {
        workEventDao.update(WorkEventEntity.fromDomain(event))
    }
    
    suspend fun delete(event: WorkEvent) {
        workEventDao.delete(WorkEventEntity.fromDomain(event))
    }
    
    suspend fun deleteById(id: Long) {
        workEventDao.deleteById(id)
    }
    
    suspend fun getTotalOvertimeHours(startDate: LocalDate, endDate: LocalDate): Double {
        return workEventDao.getTotalOvertimeHours(
            startDate.toEpochDay(),
            endDate.toEpochDay()
        ) ?: 0.0
    }
}
