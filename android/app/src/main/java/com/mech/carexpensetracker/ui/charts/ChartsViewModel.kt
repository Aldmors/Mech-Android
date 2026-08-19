package com.mech.carexpensetracker.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.data.repository.EventRepository
import com.mech.carexpensetracker.domain.model.ChartDatePreset
import com.mech.carexpensetracker.domain.model.ChartKind
import com.mech.carexpensetracker.domain.model.VehicleUnits
import com.mech.carexpensetracker.domain.service.ChartDataService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChartsUiState(
    val chartKind: ChartKind = ChartKind.MonthlySpending,
    val preset: ChartDatePreset = ChartDatePreset.TwelveMonths,
    val fuelValues: List<Double> = emptyList(),
    val repairValues: List<Double> = emptyList(),
    val papersValues: List<Double> = emptyList(),
    val lineValues: List<Double> = emptyList(),
    val hasData: Boolean = false,
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val carRepository: CarRepository,
    private val eventRepository: EventRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        load(ChartKind.MonthlySpending, ChartDatePreset.TwelveMonths)
    }

    fun load(kind: ChartKind, preset: ChartDatePreset) {
        viewModelScope.launch {
            val car = carRepository.observeSelectedCar().first()
            if (car == null) {
                _uiState.value = ChartsUiState(chartKind = kind, preset = preset, hasData = false)
                return@launch
            }
            val events = ChartDataService.filterByPreset(
                eventRepository.getEvents(car.externalId),
                preset,
            )
            val units = VehicleUnits.fromRaw(car.vehicleUnits)
            when (kind) {
                ChartKind.MonthlySpending -> {
                    val bars = ChartDataService.monthlySpending(events)
                    _uiState.value = ChartsUiState(
                        chartKind = kind,
                        preset = preset,
                        fuelValues = bars.map { it.fuel.toDouble() },
                        repairValues = bars.map { it.repair.toDouble() },
                        papersValues = bars.map { it.papers.toDouble() },
                        hasData = bars.isNotEmpty(),
                    )
                }
                ChartKind.FuelConsumption -> {
                    val points = ChartDataService.fuelConsumptionPoints(events, units)
                    _uiState.value = ChartsUiState(
                        chartKind = kind,
                        preset = preset,
                        lineValues = points.map { it.value.toDouble() },
                        hasData = points.isNotEmpty(),
                    )
                }
                ChartKind.CategoryBreakdown -> {
                    val slices = ChartDataService.categoryBreakdown(events)
                    _uiState.value = ChartsUiState(
                        chartKind = kind,
                        preset = preset,
                        lineValues = slices.map { it.amount.toDouble() },
                        hasData = slices.isNotEmpty(),
                    )
                }
                ChartKind.CumulativeCost -> {
                    val points = ChartDataService.cumulativeCost(events)
                    _uiState.value = ChartsUiState(
                        chartKind = kind,
                        preset = preset,
                        lineValues = points.map { it.total.toDouble() },
                        hasData = points.isNotEmpty(),
                    )
                }
            }
        }
    }
}
