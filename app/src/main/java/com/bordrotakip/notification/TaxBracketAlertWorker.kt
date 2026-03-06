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
import com.bordrotakip.data.repository.GrossHistoryRepository
import com.bordrotakip.data.repository.PayrollRecordRepository
import com.bordrotakip.data.repository.WorkEventRepository
import com.bordrotakip.domain.calendar.TrHolidayCalendar
import com.bordrotakip.domain.calculator.PayrollCalculator
import com.bordrotakip.domain.calculator.PayrollPeriodEngine
import com.bordrotakip.domain.calculator.TaxCalculator
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.util.formatLocalizedCurrency
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class TaxBracketAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun taxCalculator(): TaxCalculator
        fun payrollCalculator(): PayrollCalculator
        fun payrollPeriodEngine(): PayrollPeriodEngine
        fun workEventRepository(): WorkEventRepository
        fun grossHistoryRepository(): GrossHistoryRepository
        fun payrollRecordRepository(): PayrollRecordRepository
        fun companyProfileDao(): CompanyProfileDao
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefsRepo = NotificationPrefsRepository(context)
        val prefs = prefsRepo.prefsFlow.first()
        if (!prefs.isTaxBracketAlertsEnabled) return Result.success()

        if (!hasNotificationPermission(context)) return Result.success()

        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        val companyProfileDao = entryPoint.companyProfileDao()
        val payrollPeriodEngine = entryPoint.payrollPeriodEngine()
        val workEventRepository = entryPoint.workEventRepository()
        val grossHistoryRepository = entryPoint.grossHistoryRepository()
        val payrollRecordRepository = entryPoint.payrollRecordRepository()
        val payrollCalculator = entryPoint.payrollCalculator()
        val taxCalculator = entryPoint.taxCalculator()

        val profile = companyProfileDao.getProfileSync()
        payrollPeriodEngine.setCutoffDay(profile?.cutoffDay ?: PayrollPeriodEngine.DEFAULT_CUTOFF_DAY)

        val period = payrollPeriodEngine.getCurrentPeriod()
        val events = workEventRepository.getEventsForPeriodSync(period.startDate, period.endDate)
        val grossHistory = grossHistoryRepository.getGrossForDate(period.endDate)
            ?: grossHistoryRepository.getLatestGross()
            ?: return Result.success()

        val baselineCumulative = profile?.cumulativeTaxBase ?: 0.0
        val recordedCumulative = payrollRecordRepository.getCumulativeTaxBaseBeforePeriod(period)
        val cumulativeBefore = baselineCumulative + recordedCumulative
        val nonWorkingDaysMask = profile?.nonWorkingDaysMask ?: TrHolidayCalendar.DEFAULT_NON_WORKING_DAYS_MASK
        val employmentType = EmploymentType.fromStorageValue(profile?.employmentType)

        val estimate = payrollCalculator.estimateNetSalary(
            taxYear = period.year,
            payrollMonth = period.month,
            monthlyGross = grossHistory.grossAmount,
            events = events,
            nonWorkingDaysMask = nonWorkingDaysMask,
            cumulativeTaxBaseBefore = cumulativeBefore,
            overtimeMultiplierNormal = profile?.overtimeMultiplierNormal ?: PayrollCalculator.OVERTIME_MULTIPLIER_NORMAL,
            overtimeMultiplierWeekend = profile?.overtimeMultiplierWeekend ?: PayrollCalculator.OVERTIME_MULTIPLIER_WEEKEND,
            overtimeMultiplierHoliday = profile?.overtimeMultiplierHoliday ?: PayrollCalculator.OVERTIME_MULTIPLIER_HOLIDAY,
            employmentType = employmentType,
            isBesEnabled = profile?.isBesEnabled ?: false
        )

        val bracketBefore = taxCalculator.getCurrentBracket(period.year, estimate.cumulativeTaxBaseBefore)
        val bracketAfter = taxCalculator.getCurrentBracket(period.year, estimate.cumulativeTaxBaseAfter)

        if (bracketAfter.upperBound == Double.MAX_VALUE) return Result.success()

        val tier = resolveTier(bracketBefore.percentage, bracketAfter.percentage, estimate, bracketAfter)
            ?: return Result.success()

        val todayEpoch = LocalDate.now().toEpochDay()
        val bracketPercentageForDedup = bracketAfter.percentage

        val alreadySentSameAlert =
            prefs.lastTaxAlertYear == period.year &&
                prefs.lastTaxAlertBracketPercentage == bracketPercentageForDedup &&
                prefs.lastTaxAlertTier == tier

        if (alreadySentSameAlert) return Result.success()

        NotificationChannels.ensureCreated(context)
        showNotification(
            context = context,
            tier = tier,
            bracketPercentage = bracketPercentageForDedup,
            remainingToUpper = (bracketAfter.upperBound - estimate.cumulativeTaxBaseAfter).coerceAtLeast(0.0)
        )

        prefsRepo.setLastTaxAlert(
            epochDay = todayEpoch,
            tier = tier,
            bracketPercentage = bracketPercentageForDedup,
            year = period.year
        )

        return Result.success()
    }

    private fun resolveTier(
        beforePercentage: Int,
        afterPercentage: Int,
        estimate: com.bordrotakip.domain.calculator.SalaryEstimate,
        bracketAfter: com.bordrotakip.domain.calculator.TaxBracket
    ): Int? {
        if (afterPercentage > beforePercentage) return TIER_ENTERED

        val denom = bracketAfter.upperBound - bracketAfter.lowerBound
        if (denom <= 0) return null
        val progress =
            ((estimate.cumulativeTaxBaseAfter - bracketAfter.lowerBound) / denom).coerceIn(0.0, 1.0)

        return when {
            progress >= 0.95 -> TIER_VERY_CLOSE
            progress >= 0.80 -> TIER_CLOSE
            else -> null
        }
    }

    private fun showNotification(
        context: Context,
        tier: Int,
        bracketPercentage: Int,
        remainingToUpper: Double
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationIntents.EXTRA_OPEN_SALARY_PREDICTION, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_SALARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        val remainingText = formatCurrencyTry(remainingToUpper)

        when (tier) {
            TIER_ENTERED -> {
                title = "Vergi dilimi"
                text = "%$bracketPercentage dilimine girdin."
            }
            TIER_VERY_CLOSE -> {
                title = "Vergi dilimi uyarısı"
                text = "%$bracketPercentage diliminde çok yaklaştın (kalan: $remainingText)."
            }
            else -> {
                title = "Vergi dilimi uyarısı"
                text = "%$bracketPercentage dilimine yaklaştın (kalan: $remainingText)."
            }
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID_TAX_BRACKET, notification)
    }

    private fun formatCurrencyTry(amount: Double): String {
        return formatLocalizedCurrency(amount)
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
        const val NOTIFICATION_ID_TAX_BRACKET = 1101
        const val REQUEST_CODE_OPEN_SALARY = 2101

        const val TIER_CLOSE = 0
        const val TIER_VERY_CLOSE = 1
        const val TIER_ENTERED = 2
    }
}
