package com.mech.carexpensetracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.domain.model.VehicleUnits
import com.mech.carexpensetracker.domain.service.ConsumptionCalculator
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.domain.service.EventSummaryService
import com.mech.carexpensetracker.domain.service.OwnershipAnalyticsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val car: CarEntity? = null,
    val monthlySpend: String = "—",
    val mileage: String = "—",
    val consumption: String = "—",
    val ownershipCost: String = "—",
    val recentEvents: List<CarEventEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    carRepository: CarRepository,
    eventRepository: EventRepository,
) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> = carRepository.observeSelectedCar()
        .flatMapLatest { car ->
            if (car == null) {
                flowOf(DashboardUiState())
            } else {
                eventRepository.observeEvents(car.externalId).map { events ->
                    buildState(car, events)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun buildState(car: CarEntity, events: List<CarEventEntity>): DashboardUiState {
        val cal = Calendar.getInstance()
        val summary = EventSummaryService.monthlySummary(events, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        val units = VehicleUnits.fromRaw(car.vehicleUnits)
        val consumption = ConsumptionCalculator.averageConsumption(events, units)
        val ownership = OwnershipAnalyticsService.compute(events, car.buyDateMillis, units)
        val consumptionLabel = when (units) {
            VehicleUnits.Km -> "L/100km"
            VehicleUnits.Mi -> "MPG"
        }
        return DashboardUiState(
            car = car,
            monthlySpend = CurrencyFormatter.formatOrDash(summary.totalSpend),
            mileage = EventSummaryService.currentMileage(events)?.toString() ?: "—",
            consumption = consumption?.let { "$it $consumptionLabel" } ?: "—",
            ownershipCost = CurrencyFormatter.formatOrDash(ownership.costPerMonth) + "/mo",
            recentEvents = events.take(5),
        )
    }
}
