package com.bordrotakip.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bordrotakip.calendar.CalendarSyncManager
import com.bordrotakip.calendar.CalendarSyncPrefsRepository
import com.bordrotakip.data.local.dao.CompanyProfileDao
import com.bordrotakip.data.repository.GrossHistoryRepository
import com.bordrotakip.data.repository.PayrollRecordRepository
import com.bordrotakip.data.repository.WorkEventRepository
import com.bordrotakip.domain.calendar.TrHolidayCalendar
import com.bordrotakip.domain.calculator.PayrollCalculator
import com.bordrotakip.domain.calculator.PayrollPeriodEngine
import com.bordrotakip.domain.model.EmploymentType
import com.bordrotakip.domain.model.EventType
import com.bordrotakip.domain.model.PayrollPeriod
import com.bordrotakip.domain.model.ShiftType
import com.bordrotakip.domain.model.WorkEvent
import com.bordrotakip.notification.NotificationPrefsRepository
import com.bordrotakip.prefs.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val currentPeriod: PayrollPeriod? = null,
    val events: List<WorkEvent> = emptyList(),
    val currentPeriodLabel: String = "",
    val officialHolidayEpochDays: Set<Long> = emptySet(),
    val workDays: Int = 0,
    val overtimeHours: Double = 0.0,
    val estimatedNet: Double = 0.0,
    val defaultShiftHours: Double = 7.5,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val workEventRepository: WorkEventRepository,
    private val grossHistoryRepository: GrossHistoryRepository,
    private val payrollRecordRepository: PayrollRecordRepository,
    private val companyProfileDao: CompanyProfileDao,
    private val payrollPeriodEngine: PayrollPeriodEngine,
    private val payrollCalculator: PayrollCalculator,
    private val holidayCalendar: TrHolidayCalendar,
    private val notificationPrefsRepository: NotificationPrefsRepository,
    private val calendarSyncPrefsRepository: CalendarSyncPrefsRepository,
    private val calendarSyncManager: CalendarSyncManager,
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var eventsJob: Job? = null
    private var nonWorkingDaysMask: Int = TrHolidayCalendar.DEFAULT_NON_WORKING_DAYS_MASK

    private data class PeriodInputs(
        val period: PayrollPeriod,
        val baselineCumulative: Double,
        val isBesEnabled: Boolean,
        val employmentType: EmploymentType,
        val overtimeMultiplierNormal: Double,
        val overtimeMultiplierWeekend: Double,
        val overtimeMultiplierHoliday: Double
    )
    
    init {
        viewModelScope.launch {
            userPrefsRepository.prefsFlow.collect { prefs ->
                _uiState.update { it.copy(defaultShiftHours = prefs.defaultShiftHours) }
            }
        }
        loadCurrentMonth()
    }
    
    private fun loadCurrentMonth() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            val currentMonth = _uiState.value.currentMonth

            var periodEventsJob: Job? = null

            companyProfileDao.getProfile().collect { profile ->
                periodEventsJob?.cancel()

                payrollPeriodEngine.setCutoffDay(profile?.cutoffDay ?: PayrollPeriodEngine.DEFAULT_CUTOFF_DAY)
                val period = payrollPeriodEngine.getPeriodForMonth(currentMonth.year, currentMonth.monthValue)
                nonWorkingDaysMask = profile?.nonWorkingDaysMask ?: TrHolidayCalendar.DEFAULT_NON_WORKING_DAYS_MASK
                val officialHolidayEpochDays = buildSet {
                    var date = period.startDate
                    while (!date.isAfter(period.endDate)) {
                        if (holidayCalendar.isOfficialHoliday(date)) add(date.toEpochDay())
                        date = date.plusDays(1)
                    }
                }

                val inputs = PeriodInputs(
                    period = period,
                    baselineCumulative = profile?.cumulativeTaxBase ?: 0.0,
                    isBesEnabled = profile?.isBesEnabled ?: false,
                    employmentType = EmploymentType.fromStorageValue(profile?.employmentType),
                    overtimeMultiplierNormal = profile?.overtimeMultiplierNormal ?: PayrollCalculator.OVERTIME_MULTIPLIER_NORMAL,
                    overtimeMultiplierWeekend = profile?.overtimeMultiplierWeekend ?: PayrollCalculator.OVERTIME_MULTIPLIER_WEEKEND,
                    overtimeMultiplierHoliday = profile?.overtimeMultiplierHoliday ?: PayrollCalculator.OVERTIME_MULTIPLIER_HOLIDAY
                )

                _uiState.update {
                    it.copy(
                        currentPeriod = period,
                        currentPeriodLabel = period.detailedLabel,
                        officialHolidayEpochDays = officialHolidayEpochDays,
                        isLoading = true
                    )
                }

                periodEventsJob = launch {
                    workEventRepository.getEventsForPeriod(period.startDate, period.endDate).collect { events ->
                        val classifiedEvents = events.map { event ->
                            if (event.eventType in EventType.overtimeTypes) {
                                val classifiedType = holidayCalendar.classifyOvertime(event.date, nonWorkingDaysMask)
                                if (classifiedType != event.eventType) event.copy(eventType = classifiedType) else event
                            } else {
                                event
                            }
                        }

                        val workDays = classifiedEvents.count { it.eventType == EventType.SHIFT }
                        val overtimeHours = classifiedEvents
                            .filter { it.eventType in EventType.overtimeTypes }
                            .sumOf { it.hours }

                        val grossHistory = grossHistoryRepository.getGrossForDate(period.endDate)
                            ?: grossHistoryRepository.getLatestGross()

                        val estimatedNet = if (grossHistory != null) {
                            val recordedCumulative = payrollRecordRepository.getCumulativeTaxBaseBeforePeriod(period)
                            val cumulativeBefore = inputs.baselineCumulative + recordedCumulative

                            payrollCalculator.estimateNetSalary(
                                taxYear = period.year,
                                payrollMonth = period.month,
                                monthlyGross = grossHistory.grossAmount,
                                events = classifiedEvents,
                                nonWorkingDaysMask = nonWorkingDaysMask,
                                cumulativeTaxBaseBefore = cumulativeBefore,
                                overtimeMultiplierNormal = inputs.overtimeMultiplierNormal,
                                overtimeMultiplierWeekend = inputs.overtimeMultiplierWeekend,
                                overtimeMultiplierHoliday = inputs.overtimeMultiplierHoliday,
                                employmentType = inputs.employmentType,
                                isBesEnabled = inputs.isBesEnabled
                            ).netSalary
                        } else {
                            0.0
                        }

                        _uiState.update {
                            it.copy(
                                events = classifiedEvents,
                                workDays = workDays,
                                overtimeHours = overtimeHours,
                                estimatedNet = estimatedNet,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun goToPreviousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
        loadCurrentMonth()
    }
    
    fun goToNextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
        loadCurrentMonth()
    }

    fun showPeriodForDate(date: LocalDate) {
        val period = payrollPeriodEngine.getPeriodForDate(date)
        val targetMonth = YearMonth.of(period.year, period.month)
        if (_uiState.value.currentMonth == targetMonth) return

        _uiState.update { it.copy(currentMonth = targetMonth) }
        loadCurrentMonth()
    }
    
    fun addEvent(
        date: LocalDate,
        eventType: EventType,
        shiftType: ShiftType?,
        hoursOverride: Double? = null,
        note: String = ""
    ) {
        viewModelScope.launch {
            val period = payrollPeriodEngine.getPeriodForDate(date)

            val resolvedEventType = if (eventType == EventType.OVERTIME_NORMAL) {
                holidayCalendar.classifyOvertime(date, nonWorkingDaysMask)
            } else {
                eventType
            }
            
            val defaultHours = when {
                resolvedEventType == EventType.SHIFT -> shiftType?.durationHours?.toDouble() ?: 8.0
                resolvedEventType in EventType.overtimeTypes -> 8.0
                else -> 0.0
            }
            val hours = hoursOverride ?: defaultHours

            val resolvedStartTime = if (resolvedEventType == EventType.SHIFT && shiftType != null) {
                LocalTime.of(shiftType.startHour, 0)
            } else null

            val resolvedEndTime = if (resolvedEventType == EventType.SHIFT && shiftType != null) {
                LocalTime.of(shiftType.endHour % 24, 0)
            } else null
            
            val event = WorkEvent(
                date = date,
                eventType = resolvedEventType,
                shiftType = shiftType,
                startTime = resolvedStartTime,
                endTime = resolvedEndTime,
                hours = hours,
                note = note,
                payrollPeriodLabel = period.label
            )
            
            val insertedId = workEventRepository.insert(event)
            val persistedEvent = event.copy(id = insertedId)
            if (resolvedEventType == EventType.SHIFT) {
                notificationPrefsRepository.setLastShiftLoggedEpochDay(date.toEpochDay())
            }

            val syncPrefs = calendarSyncPrefsRepository.getPrefsOnce()
            val calendarId = syncPrefs.selectedCalendarId
            if (syncPrefs.isEnabled && calendarId != null) {
                runCatching { calendarSyncManager.upsertWorkEvent(persistedEvent, calendarId) }
            }
        }
    }

    fun updateEvent(event: WorkEvent) {
        viewModelScope.launch {
            val normalized = if (event.eventType == EventType.SHIFT && event.shiftType != null) {
                event.copy(
                    startTime = LocalTime.of(event.shiftType.startHour, 0),
                    endTime = LocalTime.of(event.shiftType.endHour % 24, 0)
                )
            } else {
                event
            }

            val updated = normalized.copy(updatedAt = System.currentTimeMillis())
            workEventRepository.update(updated)

            val syncPrefs = calendarSyncPrefsRepository.getPrefsOnce()
            val calendarId = syncPrefs.selectedCalendarId
            if (syncPrefs.isEnabled && calendarId != null) {
                runCatching { calendarSyncManager.upsertWorkEvent(updated, calendarId) }
            }
        }
    }
    
    fun deleteEvent(event: WorkEvent) {
        viewModelScope.launch {
            workEventRepository.deleteById(event.id)

            val syncPrefs = calendarSyncPrefsRepository.getPrefsOnce()
            if (syncPrefs.isEnabled) {
                runCatching { calendarSyncManager.deleteWorkEvent(event.id) }
            }
        }
    }
}
