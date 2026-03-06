package com.bordrotakip.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bordrotakip.domain.model.EventType
import com.bordrotakip.domain.model.ShiftType
import com.bordrotakip.domain.model.WorkEvent
import java.time.LocalDate
import kotlin.math.roundToInt
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    date: LocalDate,
    events: List<WorkEvent>,
    defaultShiftHours: Double,
    onDismiss: () -> Unit,
    onAddEvent: (eventType: EventType, shiftType: ShiftType?, hours: Double, note: String) -> Unit,
    onUpdateEvent: (WorkEvent) -> Unit,
    onDeleteEvent: (WorkEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var addDialogState by remember { mutableStateOf<AddDialogState?>(null) }
    var editEvent by remember { mutableStateOf<WorkEvent?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tarih başlığı
            Text(
                text = formatDateTurkish(date),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mevcut eventler
            if (events.isEmpty()) {
                Text(
                    text = "Bu gün için kayıt yok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(
                        items = events,
                        key = { it.id }
                    ) { event ->
                        EventListItem(
                            event = event,
                            onEdit = { editEvent = event },
                            onDelete = { onDeleteEvent(event) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Hızlı ekleme butonları
            Text(
                text = "Hızlı Ekle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vardiya butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShiftType.entries.forEach { shift ->
                    OutlinedButton(
                        onClick = { addDialogState = AddDialogState.shift(shift, defaultShiftHours) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(shift.displayName, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mesai butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { addDialogState = AddDialogState.overtime() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🟥 Mesai", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = { addDialogState = AddDialogState.leave() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🟩 İzin", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalButton(
                    onClick = { onAddEvent(EventType.SICK_LEAVE, null, 0.0, "") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🟨 Rapor", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    addDialogState?.let { state ->
        WorkEventDialog(
            title = state.title,
            initialEventType = state.initialEventType,
            initialShiftType = state.initialShiftType,
            initialHours = state.initialHours,
            initialNote = "",
            onDismiss = { addDialogState = null },
            onConfirm = { eventType, shiftType, hours, note ->
                onAddEvent(eventType, shiftType, hours, note)
                addDialogState = null
            }
        )
    }

    editEvent?.let { event ->
        WorkEventDialog(
            title = "Düzenle",
            initialEventType = event.eventType,
            initialShiftType = event.shiftType,
            initialHours = event.hours,
            initialNote = event.note,
            onDismiss = { editEvent = null },
            onConfirm = { eventType, shiftType, hours, note ->
                onUpdateEvent(
                    event.copy(
                        eventType = eventType,
                        shiftType = shiftType,
                        hours = hours,
                        note = note
                    )
                )
                editEvent = null
            }
        )
    }
}

@Composable
fun EventListItem(
    event: WorkEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = event.eventType.color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = event.getDisplayLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (event.hours > 0) {
                    Text(
                        text = "${formatHours(event.hours)} saat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.note.isNotBlank()) {
                    Text(
                        text = event.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Düzenle")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private data class AddDialogState(
    val title: String,
    val initialEventType: EventType,
    val initialShiftType: ShiftType?,
    val initialHours: Double
) {
    companion object {
        fun shift(shiftType: ShiftType, defaultShiftHours: Double) = AddDialogState(
            title = "Vardiya Ekle",
            initialEventType = EventType.SHIFT,
            initialShiftType = shiftType,
            initialHours = defaultShiftHours
        )

        fun overtime() = AddDialogState(
            title = "Mesai Ekle",
            initialEventType = EventType.OVERTIME_NORMAL,
            initialShiftType = null,
            initialHours = 8.0
        )

        fun leave() = AddDialogState(
            title = "İzin Ekle",
            initialEventType = EventType.LEAVE_ANNUAL,
            initialShiftType = null,
            initialHours = 0.0
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkEventDialog(
    title: String,
    initialEventType: EventType,
    initialShiftType: ShiftType?,
    initialHours: Double,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (eventType: EventType, shiftType: ShiftType?, hours: Double, note: String) -> Unit
) {
    var selectedEventType by remember { mutableStateOf(initialEventType) }
    var selectedShiftType by remember {
        mutableStateOf(
            initialShiftType ?: ShiftType.entries.first()
        )
    }

    val shouldShowHours = selectedEventType == EventType.SHIFT || selectedEventType in EventType.overtimeTypes
    val hourOptions = if (selectedEventType == EventType.SHIFT) SHIFT_HOUR_OPTIONS else OVERTIME_HOUR_OPTIONS
    var hoursIndexValue by remember(selectedEventType) {
        val coerced = initialHours.coerceIn(1.0, 12.0)
        mutableStateOf(findClosestIndex(hourOptions, coerced).toFloat())
    }
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (initialEventType in EventType.leaveTypes || initialEventType == EventType.LEAVE_ANNUAL) {
                    Text("İzin türü", style = MaterialTheme.typography.labelMedium)
                    EventType.leaveTypes.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedEventType == type,
                                onClick = { selectedEventType = type }
                            )
                            Text(text = type.displayName)
                        }
                    }
                }

                if (initialEventType == EventType.SHIFT) {
                    Text("Vardiya", style = MaterialTheme.typography.labelMedium)
                    ShiftType.entries.forEach { shiftType ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedShiftType == shiftType,
                                onClick = { selectedShiftType = shiftType }
                            )
                            Text(text = shiftType.getDisplayLabel())
                        }
                    }
                }

                if (shouldShowHours) {
                    val index = hoursIndexValue.roundToInt().coerceIn(0, hourOptions.lastIndex)
                    val selectedHours = hourOptions[index]
                    Text("Süre: ${formatHours(selectedHours)} saat", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = hoursIndexValue,
                        onValueChange = { hoursIndexValue = it },
                        valueRange = 0f..hourOptions.lastIndex.toFloat(),
                        steps = (hourOptions.size - 2).coerceAtLeast(0)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not (opsiyonel)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val resolvedShiftType = if (initialEventType == EventType.SHIFT) selectedShiftType else null
                    val resolvedHours = if (shouldShowHours) {
                        val index = hoursIndexValue.roundToInt().coerceIn(0, hourOptions.lastIndex)
                        hourOptions[index]
                    } else 0.0
                    onConfirm(selectedEventType, resolvedShiftType, resolvedHours, note.trim())
                }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

private fun formatDateTurkish(date: LocalDate): String {
    val dayNames = listOf(
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
    )
    val monthNames = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    
    val dayName = dayNames[date.dayOfWeek.value - 1]
    val monthName = monthNames[date.monthValue - 1]
    
    return "${date.dayOfMonth} $monthName $dayName"
}

private val SHIFT_HOUR_OPTIONS: List<Double> = listOf(
    1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 7.5, 8.0, 9.0, 10.0, 11.0, 12.0
)

private val OVERTIME_HOUR_OPTIONS: List<Double> = (1..12).map { it.toDouble() }

private fun findClosestIndex(options: List<Double>, value: Double): Int {
    var bestIndex = 0
    var bestDistance = Double.MAX_VALUE
    options.forEachIndexed { index, option ->
        val distance = abs(option - value)
        if (distance < bestDistance) {
            bestIndex = index
            bestDistance = distance
        }
    }
    return bestIndex
}

private fun formatHours(hours: Double): String {
    val rounded = (hours * 2.0).roundToInt() / 2.0
    val isInt = abs(rounded - rounded.toLong().toDouble()) < 1e-9
    return if (isInt) rounded.toLong().toString() else rounded.toString().replace('.', ',')
}
