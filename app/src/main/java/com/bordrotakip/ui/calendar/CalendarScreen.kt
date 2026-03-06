package com.bordrotakip.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CurrencyLira
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bordrotakip.domain.model.EventType
import com.bordrotakip.domain.model.PayrollPeriod
import com.bordrotakip.domain.model.WorkEvent
import com.bordrotakip.ui.theme.*
import com.bordrotakip.util.formatLocalizedCurrency
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToSalary: () -> Unit,
    onNavigateToPayrolls: () -> Unit,
    onNavigateToSettings: () -> Unit,
    openDayDetailEpochDay: Long? = null,
    onOpenDayDetailConsumed: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val eventsByDate = remember(uiState.events) { uiState.events.groupBy { it.date } }

    LaunchedEffect(openDayDetailEpochDay) {
        if (openDayDetailEpochDay == null) return@LaunchedEffect
        val date = runCatching { LocalDate.ofEpochDay(openDayDetailEpochDay) }.getOrNull()
            ?: return@LaunchedEffect

        viewModel.showPeriodForDate(date)
        selectedDate = date
        showBottomSheet = true
        onOpenDayDetailConsumed()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bordro Takip",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSalary) {
                        Icon(Icons.Default.CurrencyLira, "Maaş Tahmini")
                    }
                    IconButton(onClick = onNavigateToPayrolls) {
                        Icon(Icons.Default.Description, "Bordro Kayıtları")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Ayarlar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ay Seçici
            MonthSelector(
                currentMonth = uiState.currentMonth,
                onPreviousMonth = { viewModel.goToPreviousMonth() },
                onNextMonth = { viewModel.goToNextMonth() }
            )
            
            // Bordro Dönemi Etiketi
            PayrollPeriodLabel(
                periodLabel = uiState.currentPeriodLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Takvim
            uiState.currentPeriod?.let { period ->
                CalendarGrid(
                    period = period,
                    labelMonth = uiState.currentMonth,
                    eventsByDate = eventsByDate,
                    officialHolidayEpochDays = uiState.officialHolidayEpochDays,
                    onDayClick = { date ->
                        selectedDate = date
                        showBottomSheet = true
                    }
                )
            }
            
            // Ay Özeti
            MonthlySummary(
                workDays = uiState.workDays,
                overtimeHours = uiState.overtimeHours,
                estimatedNet = uiState.estimatedNet,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
    
    // Gün Detay Alt Sheet
    if (showBottomSheet && selectedDate != null) {
        DayDetailBottomSheet(
            date = selectedDate!!,
            events = selectedDate?.let { eventsByDate[it] }.orEmpty(),
            defaultShiftHours = uiState.defaultShiftHours,
            onDismiss = { showBottomSheet = false },
            onAddEvent = { eventType, shiftType, hours, note ->
                viewModel.addEvent(selectedDate!!, eventType, shiftType, hoursOverride = hours, note = note)
                showBottomSheet = false
            },
            onUpdateEvent = { event ->
                viewModel.updateEvent(event)
            },
            onDeleteEvent = { event ->
                viewModel.deleteEvent(event)
            }
        )
    }
}

@Composable
fun MonthSelector(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthNames = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Önceki Ay")
        }
        
        Text(
            text = "${monthNames[currentMonth.monthValue - 1]} ${currentMonth.year}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Sonraki Ay")
        }
    }
}

@Composable
fun PayrollPeriodLabel(
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = "🗓️ Bordro Dönemi: $periodLabel",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CalendarGrid(
    period: PayrollPeriod,
    labelMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<WorkEvent>>,
    officialHolidayEpochDays: Set<Long>,
    onDayClick: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        // Gün başlıkları
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Takvim günleri
        val days = remember(period) {
            val firstDay = period.startDate
            val lastDay = period.endDate
            val firstDayOfWeek = firstDay.dayOfWeek.value // 1=Pazartesi, 7=Pazar

            val result = mutableListOf<LocalDate?>()

            // Haftayı hizalamak için boşluk
            repeat(firstDayOfWeek - 1) {
                result.add(null)
            }

            // Dönem günleri (25-24)
            var date = firstDay
            while (!date.isAfter(lastDay)) {
                result.add(date)
                date = date.plusDays(1)
            }

            // Haftayı tamamla
            while (result.size % 7 != 0) {
                result.add(null)
            }

            result
        }
        val today = LocalDate.now()
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(((days.size / 7) * 60).dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(
                items = days,
                key = { index, day -> day?.toEpochDay() ?: "empty-$index" }
            ) { _, day ->
                CalendarDayCell(
                    date = day,
                    events = day?.let { d -> eventsByDate[d] }.orEmpty(),
                    isToday = day == today,
                    isOutsideLabelMonth = day?.let { it.year != labelMonth.year || it.monthValue != labelMonth.monthValue } ?: false,
                    isOfficialHoliday = day?.toEpochDay()?.let { it in officialHolidayEpochDays } ?: false,
                    onClick = { day?.let(onDayClick) }
                )
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    date: LocalDate?,
    events: List<WorkEvent>,
    isToday: Boolean,
    isOutsideLabelMonth: Boolean,
    isOfficialHoliday: Boolean,
    onClick: () -> Unit
) {
    val hasShift = events.any { it.eventType == EventType.SHIFT }
    val hasOvertime = events.any { it.eventType in EventType.overtimeTypes }
    val leaveDotColor = events.firstOrNull { it.eventType in EventType.leaveTypes }?.eventType?.color
    val hasSickLeave = events.any { it.eventType == EventType.SICK_LEAVE }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    date != null && isOutsideLabelMonth -> MaterialTheme.colorScheme.surfaceVariant
                    date != null -> MaterialTheme.colorScheme.surface
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = date != null, onClick = onClick),
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 6.dp),
                fontSize = 14.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.primary
                    isOutsideLabelMonth -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (isOfficialHoliday) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp, top = 6.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(HolidayColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "T",
                        modifier = Modifier.offset(y = (-3).dp),
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Event göstergeleri
            if (events.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                ) {
                    if (hasShift) {
                        EventDot(color = ShiftColor)
                    }
                    if (hasOvertime) {
                        EventDot(color = OvertimeColor)
                    }
                    leaveDotColor?.let { EventDot(color = it) }
                    if (hasSickLeave) {
                        EventDot(color = SickLeaveColor)
                    }
                }
            }
        }
    }
}

@Composable
fun EventDot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun MonthlySummary(
    workDays: Int,
    overtimeHours: Double,
    estimatedNet: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Bu Dönem Özeti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    label = "Çalışma Günü",
                    value = "$workDays gün",
                    emoji = "🗓️"
                )
                SummaryItem(
                    label = "Mesai",
                    value = "${overtimeHours.toInt()} saat",
                    emoji = "⏰"
                )
                SummaryItem(
                    label = "Tahmini Net",
                    value = formatLocalizedCurrency(estimatedNet),
                    emoji = "₺"
                )
            }
        }
    }
}

@Composable
fun SummaryItem(
    label: String,
    value: String,
    emoji: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
