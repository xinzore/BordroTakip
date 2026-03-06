package com.bordrotakip.data.repository

import com.bordrotakip.data.local.dao.PayrollRecordDao
import com.bordrotakip.data.local.entity.PayrollRecordEntity
import com.bordrotakip.domain.model.PayrollPeriod
import com.bordrotakip.domain.model.PayrollRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayrollRecordRepository @Inject constructor(
    private val payrollRecordDao: PayrollRecordDao
) {
    fun getAllRecords(): Flow<List<PayrollRecord>> {
        return payrollRecordDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getById(id: Long): PayrollRecord? {
        return payrollRecordDao.getById(id)?.toDomain()
    }

    suspend fun getRecordForPeriod(year: Int, month: Int): PayrollRecord? {
        return payrollRecordDao.getRecordForPeriod(year, month)?.toDomain()
    }

    suspend fun insert(record: PayrollRecord): Long {
        return payrollRecordDao.insert(PayrollRecordEntity.fromDomain(record))
    }

    suspend fun update(record: PayrollRecord) {
        payrollRecordDao.update(PayrollRecordEntity.fromDomain(record))
    }

    suspend fun deleteById(id: Long) {
        payrollRecordDao.deleteById(id)
    }

    suspend fun getCumulativeTaxBaseBeforePeriod(period: PayrollPeriod): Double {
        return payrollRecordDao.getCumulativeTaxBase(period.year, period.month) ?: 0.0
    }

    fun getRecordsForYear(year: Int): Flow<List<PayrollRecord>> {
        return payrollRecordDao.getRecordsForYear(year).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

