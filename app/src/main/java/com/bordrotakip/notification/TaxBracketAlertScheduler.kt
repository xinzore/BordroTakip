package com.bordrotakip.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaxBracketAlertScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleDailyTaxBracketAlert(targetTime: LocalTime = DEFAULT_TARGET_TIME) {
        val request = PeriodicWorkRequestBuilder<TaxBracketAlertWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(targetTime), TimeUnit.MILLISECONDS)
            .addTag(TAG_TAX_BRACKET_ALERT)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_TAX_BRACKET_ALERT,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyTaxBracketAlert() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_TAX_BRACKET_ALERT)
    }

    private fun calculateInitialDelay(targetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val targetToday = now.toLocalDate().atTime(targetTime)
        val nextRun = if (now.isBefore(targetToday)) targetToday else targetToday.plusDays(1)
        return Duration.between(now, nextRun).toMillis().coerceAtLeast(0)
    }

    companion object {
        const val UNIQUE_WORK_TAX_BRACKET_ALERT = "daily_tax_bracket_alert"
        const val TAG_TAX_BRACKET_ALERT = "tag_daily_tax_bracket_alert"

        // Shift prompt ile çakışmasın diye 20:05 civarı.
        val DEFAULT_TARGET_TIME: LocalTime = LocalTime.of(20, 5)
    }
}

