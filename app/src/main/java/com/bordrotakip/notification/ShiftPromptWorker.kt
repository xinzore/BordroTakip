package com.bordrotakip.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bordrotakip.MainActivity
import com.bordrotakip.R
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.repository.WorkEventRepository
import com.bordrotakip.domain.calendar.TrHolidayCalendar
import com.bordrotakip.domain.model.EventType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

class ShiftPromptWorker @Inject constructor(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun companyProfileDao(): CompanyProfileDao
        fun workEventRepository(): WorkEventRepository
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefsRepo = NotificationPrefsRepository(context)

        val prefs = prefsRepo.prefsFlow.first()
        if (!prefs.isShiftPromptEnabled) return Result.success()

        if (!hasNotificationPermission(context)) return Result.success()

        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()

        if (prefs.lastShiftPromptEpochDay == todayEpoch) return Result.success()

        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        val profile = entryPoint.companyProfileDao().getProfileSync()
        val nonWorkingDaysMask = profile?.nonWorkingDaysMask ?: TrHolidayCalendar.DEFAULT_NON_WORKING_DAYS_MASK
        val isTodayNonWorkingDay = isNonWorkingDay(today, nonWorkingDaysMask)

        NotificationChannels.ensureCreated(context)

        if (isTodayNonWorkingDay) {
            val weekStartEpochDay = startOfWeek(today).toEpochDay()
            if (prefs.lastWeeklyOvertimePromptWeekStartEpochDay == weekStartEpochDay) {
                return Result.success()
            }

            val weekStartDate = LocalDate.ofEpochDay(weekStartEpochDay)
            val weekEndDate = weekStartDate.plusDays(6)
            val eventsThisWeek = entryPoint.workEventRepository().getEventsForPeriodSync(weekStartDate, weekEndDate)

            val hasOvertimeThisWeek = eventsThisWeek.any { it.eventType in EventType.overtimeTypes && it.hours > 0.0 }
            if (hasOvertimeThisWeek) return Result.success()

            showWeeklyOvertimePromptNotification(context)
            prefsRepo.setLastShiftPromptEpochDay(todayEpoch)
            prefsRepo.setLastWeeklyOvertimePromptWeekStartEpochDay(weekStartEpochDay)
            return Result.success()
        }

        if (prefs.lastShiftLoggedEpochDay == todayEpoch) return Result.success()

        showShiftPromptNotification(context, todayEpoch)
        prefsRepo.setLastShiftPromptEpochDay(todayEpoch)
        return Result.success()
    }

    private fun showShiftPromptNotification(context: Context, epochDay: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationIntents.EXTRA_OPEN_SHIFT_PROMPT_EPOCH_DAY, epochDay)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_SHIFT_PROMPT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Vardiya kaydı")
            .setContentText("Bugün hangi vardiyadaydın?")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID_SHIFT_PROMPT, notification)
    }

    private fun showWeeklyOvertimePromptNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_WEEKLY_OVERTIME_PROMPT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mesai kontrolü")
            .setContentText("Bu hafta hiç mesai yaptın mı?")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID_WEEKLY_OVERTIME_PROMPT, notification)
    }

    private fun isNonWorkingDay(date: LocalDate, nonWorkingDaysMask: Int): Boolean {
        val bit = 1 shl (date.dayOfWeek.value - 1)
        return (nonWorkingDaysMask and bit) != 0
    }

    private fun startOfWeek(date: LocalDate): LocalDate {
        val firstDayOfWeek = WeekFields.of(Locale.forLanguageTag("tr-TR")).firstDayOfWeek
        return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private companion object {
        const val NOTIFICATION_ID_SHIFT_PROMPT = 1001
        const val NOTIFICATION_ID_WEEKLY_OVERTIME_PROMPT = 1002
        const val REQUEST_CODE_OPEN_SHIFT_PROMPT = 2001
        const val REQUEST_CODE_OPEN_WEEKLY_OVERTIME_PROMPT = 2002
    }
}
