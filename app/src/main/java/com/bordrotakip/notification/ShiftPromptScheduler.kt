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
class ShiftPromptScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleDailyShiftPrompt(targetTime: LocalTime = DEFAULT_TARGET_TIME) {
        val workManager = WorkManager.getInstance(context)
        val request = PeriodicWorkRequestBuilder<ShiftPromptWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(targetTime), TimeUnit.MILLISECONDS)
            .addTag(TAG_SHIFT_PROMPT)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_SHIFT_PROMPT,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDailyShiftPrompt() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_SHIFT_PROMPT)
    }

    private fun calculateInitialDelay(targetTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val targetToday = now.toLocalDate().atTime(targetTime)
        val nextRun = if (now.isBefore(targetToday)) targetToday else targetToday.plusDays(1)
        return Duration.between(now, nextRun).toMillis().coerceAtLeast(0)
    }

    companion object {
        const val UNIQUE_WORK_SHIFT_PROMPT = "daily_shift_prompt"
        const val TAG_SHIFT_PROMPT = "tag_daily_shift_prompt"

        val DEFAULT_TARGET_TIME: LocalTime = LocalTime.of(20, 0)
    }
}

