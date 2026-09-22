package com.mech.carexpensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.data.repository.ReminderRepository
import com.mech.carexpensetracker.domain.model.FuelSlot
import com.mech.carexpensetracker.domain.model.VehicleUnits
import com.mech.carexpensetracker.domain.service.ConsumptionCalculator
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.domain.service.EventSummaryService
import com.mech.carexpensetracker.domain.service.OwnershipAnalyticsService
import com.mech.carexpensetracker.domain.service.ReminderAlertService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar
import javax.inject.Inject

data class DashboardReminderUi(
    val externalId: String,
    val title: String,
    val colorHex: String,
    val dueDateMillis: Long?,
    val dueMileage: Int?,
    val daysLeft: Long?,
    val remainingKm: Int?,
    val isDue: Boolean,
)

data class DashboardUiState(
    val cars: List<CarEntity> = emptyList(),
    val car: CarEntity? = null,
    val monthlySpend: String = "—",
    val mileage: String = "—",
    val consumption: String = "—",
    val alternativeConsumption: String = "—",
    val eventCount: String = "—",
    val costPerKm: String = "—",
    val totalSpend: String = "—",
    val avgMonthlyCost: String = "—",
    val approachingReminders: List<DashboardReminderUi> = emptyList(),
    val recentEvents: List<CarEventEntity> = emptyList(),
    val consumptionById: Map<String, java.math.BigDecimal> = emptyMap(),
    val vehicleUnits: VehicleUnits = VehicleUnits.Km,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val carRepository: CarRepository,
    eventRepository: EventRepository,
    reminderRepository: ReminderRepository,
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = combine(
        carRepository.observeCars(),
        carRepository.observeSelectedCar(),
    ) { cars, selected -> cars to selected }
        .flatMapLatest { (cars, car) ->
            if (car == null) {
                flowOf(DashboardUiState(cars = cars))
            } else {
                combine(
                    eventRepository.observeEvents(car.externalId),
                    reminderRepository.observeReminders(car.externalId),
                ) { events, reminders ->
                    buildState(cars, car, events, reminders)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun selectCar(externalId: String) {
        viewModelScope.launch { carRepository.selectCar(externalId) }
    }

    private fun buildState(
        cars: List<CarEntity>,
        car: CarEntity,
        events: List<CarEventEntity>,
        reminders: List<CarReminderEntity>,
    ): DashboardUiState {
        val cal = Calendar.getInstance()
        val summary = EventSummaryService.monthlySummary(events, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val units = VehicleUnits.fromRaw(car.vehicleUnits)
        val ownership = OwnershipAnalyticsService.compute(events, units)
        val consumptionById = ConsumptionCalculator.displayedConsumptionByEventId(events, units)
        val mileage = EventSummaryService.currentMileage(events)
        val unit = units.raw
        val consumption = ConsumptionCalculator.averageConsumption(events, units)
        val alternativeConsumption = ConsumptionCalculator.averageConsumption(events, units, FuelSlot.Secondary)
        val approaching = reminders.map { ReminderAlertService.status(it, mileage) }
            .filter { it.isApproaching }
            .map { status ->
                DashboardReminderUi(
                    externalId = status.reminder.externalId,
                    title = status.reminder.title,
                    colorHex = status.reminder.colorHex,
                    dueDateMillis = status.reminder.dueDateMillis,
                    dueMileage = status.reminder.dueMileage,
                    daysLeft = status.daysLeft,
                    remainingKm = status.remainingKm,
                    isDue = status.isDue,
                )
            }
        return DashboardUiState(
            cars = cars,
            car = car,
            monthlySpend = CurrencyFormatter.formatOrDash(summary.totalSpend),
            mileage = mileage?.let { "$it $unit" } ?: "—",
            consumption = formatConsumption(consumption, units),
            alternativeConsumption = formatConsumption(alternativeConsumption, units),
            eventCount = events.size.toString(),
            costPerKm = CurrencyFormatter.formatOrDash(ownership.costPerKm),
            totalSpend = CurrencyFormatter.formatOrDash(ownership.totalCost),
            avgMonthlyCost = CurrencyFormatter.formatOrDash(ownership.costPerMonth),
            approachingReminders = approaching,
            recentEvents = events.take(5),
            consumptionById = consumptionById,
            vehicleUnits = units,
        )
    }

    private fun formatConsumption(
        value: BigDecimal?,
        units: VehicleUnits,
    ): String {
        if (value == null) return "—"
        return when (units) {
            VehicleUnits.Km -> "$value L/100km"
            VehicleUnits.Mi -> "$value mpg"
        }
    }
}
