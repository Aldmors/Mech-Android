package com.mech.carexpensetracker.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.data.repository.ReminderRepository
import com.mech.carexpensetracker.domain.model.VehicleUnits
import com.mech.carexpensetracker.domain.service.EventSummaryService
import com.mech.carexpensetracker.domain.service.ReminderAlertService
import com.mech.carexpensetracker.domain.service.ReminderSchedule
import com.mech.carexpensetracker.reminders.CalendarSyncResult
import com.mech.carexpensetracker.reminders.ReminderCalendarSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class ReminderItemUi(
    val externalId: String,
    val title: String,
    val colorHex: String,
    val dueDateMillis: Long?,
    val dueMileage: Int?,
    val daysLeft: Long?,
    val remainingKm: Int?,
    val isDue: Boolean,
    val isApproaching: Boolean,
    val isCompleted: Boolean,
)

data class RemindersUiState(
    val carExternalId: String? = null,
    val currentMileage: Int? = null,
    val vehicleUnits: VehicleUnits = VehicleUnits.Km,
    val reminders: List<ReminderItemUi> = emptyList(),
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    carRepository: CarRepository,
    private val eventRepository: EventRepository,
    private val reminderRepository: ReminderRepository,
    private val calendarSync: ReminderCalendarSync,
) : ViewModel() {
    private val messagesFlow = MutableSharedFlow<CalendarSyncResult>(extraBufferCapacity = 1)
    val messages: SharedFlow<CalendarSyncResult> = messagesFlow.asSharedFlow()

    val uiState: StateFlow<RemindersUiState> = carRepository.observeSelectedCar()
        .flatMapLatest { car ->
            if (car == null) {
                flowOf(RemindersUiState())
            } else {
                combine(
                    reminderRepository.observeReminders(car.externalId),
                    eventRepository.observeEvents(car.externalId),
                ) { reminders, events ->
                    val mileage = EventSummaryService.currentMileage(events)
                    RemindersUiState(
                        carExternalId = car.externalId,
                        currentMileage = mileage,
                        vehicleUnits = VehicleUnits.fromRaw(car.vehicleUnits),
                        reminders = reminders
                            .filter { it.dueDateMillis != null || it.dueMileage != null }
                            .map { reminder ->
                                val status = ReminderAlertService.status(reminder, mileage)
                                ReminderItemUi(
                                    externalId = reminder.externalId,
                                    title = reminder.title,
                                    colorHex = reminder.colorHex,
                                    dueDateMillis = reminder.dueDateMillis,
                                    dueMileage = reminder.dueMileage,
                                    daysLeft = status.daysLeft,
                                    remainingKm = status.remainingKm,
                                    isDue = status.isDue,
                                    isApproaching = status.isApproaching,
                                    isCompleted = reminder.isCompleted,
                                ) to reminder
                            }
                            .sortedWith(
                                compareBy<Pair<ReminderItemUi, CarReminderEntity>> { it.first.isCompleted }
                                    .thenBy { !it.first.isApproaching }
                                    .thenBy { it.first.dueDateMillis ?: Long.MAX_VALUE }
                                    .thenBy { it.first.dueMileage ?: Int.MAX_VALUE },
                            )
                            .map { it.first },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RemindersUiState())

    fun saveReminder(
        title: String,
        colorHex: String,
        dueDateMillis: Long?,
        intervalKm: Int?,
        reminderId: String? = null,
    ) {
        val trimmed = title.trim()
        val carId = uiState.value.carExternalId ?: return
        if (trimmed.isEmpty()) return
        if (dueDateMillis == null && intervalKm == null) return
        if (intervalKm != null && intervalKm < 1) return
        viewModelScope.launch {
            withContext(NonCancellable) {
                val existing = reminderId?.let { reminderRepository.get(it) }
                val now = System.currentTimeMillis()
                val createdAt = existing?.createdAtMillis?.takeIf { it > 0 } ?: now
                val originMileage = existing?.dueMileage?.let { due ->
                    existing.intervalKm?.let { due - it }
                } ?: uiState.value.currentMileage
                val base = existing ?: CarReminderEntity(
                    externalId = UUID.randomUUID().toString(),
                    carExternalId = carId,
                    title = trimmed,
                )
                reminderRepository.upsert(
                    base.copy(
                        title = trimmed,
                        colorHex = colorHex,
                        createdAtMillis = createdAt,
                        intervalDays = dueDateMillis?.let { ReminderSchedule.intervalDaysBetween(createdAt, it) },
                        intervalKm = intervalKm,
                        dueDateMillis = dueDateMillis,
                        dueMileage = intervalKm?.let {
                            ReminderSchedule.dueMileageAfterInterval(originMileage, it)
                        },
                    ),
                )
            }
        }
    }

    suspend fun getReminder(externalId: String): CarReminderEntity? = reminderRepository.get(externalId)

    fun complete(externalId: String, repeat: Boolean) {
        viewModelScope.launch {
            val reminder = reminderRepository.get(externalId) ?: return@launch
            reminderRepository.upsert(reminder.copy(isCompleted = true))
            if (repeat) {
                ReminderSchedule.nextReminder(
                    reminder,
                    System.currentTimeMillis(),
                    uiState.value.currentMileage,
                    UUID.randomUUID().toString(),
                )?.let { reminderRepository.upsert(it) }
            }
        }
    }

    fun deleteReminder(externalId: String) {
        viewModelScope.launch { reminderRepository.delete(externalId) }
    }

    fun syncCalendar() {
        viewModelScope.launch {
            val carId = uiState.value.carExternalId ?: return@launch
            val reminders = reminderRepository.getForCar(carId)
                .filter { !it.isCompleted && (it.dueDateMillis != null || it.dueMileage != null) }
            val events = eventRepository.getEvents(carId)
            val result = calendarSync.sync(
                reminders = reminders,
                currentMileage = EventSummaryService.currentMileage(events),
                avgDailyDistance = ReminderSchedule.averageDailyDistance(events),
                onUpdated = { reminderRepository.upsert(it) },
            )
            messagesFlow.emit(result)
        }
    }
}
