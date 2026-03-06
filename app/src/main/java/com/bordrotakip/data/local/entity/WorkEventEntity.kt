package com.bordrotakip.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bordrotakip.domain.model.EventType
import com.bordrotakip.domain.model.ShiftType
import com.bordrotakip.domain.model.WorkEvent
import java.time.LocalDate
import java.time.LocalTime

/**
 * WorkEvent Room Entity
 */
@Entity(
    tableName = "work_events",
    indices = [
        Index(value = ["date"]),
        Index(value = ["eventType", "date"])
    ]
)
data class WorkEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long, // LocalDate as epoch day
    val eventType: String, // EventType.name
    val shiftType: String? = null, // ShiftType.name
    val startTimeHour: Int? = null,
    val startTimeMinute: Int? = null,
    val endTimeHour: Int? = null,
    val endTimeMinute: Int? = null,
    val hours: Double = 0.0,
    val note: String = "",
    val payrollPeriodLabel: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): WorkEvent {
        return WorkEvent(
            id = id,
            date = LocalDate.ofEpochDay(date),
            eventType = EventType.valueOf(eventType),
            shiftType = shiftType?.let { ShiftType.valueOf(it) },
            startTime = if (startTimeHour != null && startTimeMinute != null) {
                LocalTime.of(startTimeHour, startTimeMinute)
            } else null,
            endTime = if (endTimeHour != null && endTimeMinute != null) {
                LocalTime.of(endTimeHour, endTimeMinute)
            } else null,
            hours = hours,
            note = note,
            payrollPeriodLabel = payrollPeriodLabel,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomain(workEvent: WorkEvent): WorkEventEntity {
            return WorkEventEntity(
                id = workEvent.id,
                date = workEvent.date.toEpochDay(),
                eventType = workEvent.eventType.name,
                shiftType = workEvent.shiftType?.name,
                startTimeHour = workEvent.startTime?.hour,
                startTimeMinute = workEvent.startTime?.minute,
                endTimeHour = workEvent.endTime?.hour,
                endTimeMinute = workEvent.endTime?.minute,
                hours = workEvent.hours,
                note = workEvent.note,
                payrollPeriodLabel = workEvent.payrollPeriodLabel,
                createdAt = workEvent.createdAt,
                updatedAt = workEvent.updatedAt
            )
        }
    }
}
