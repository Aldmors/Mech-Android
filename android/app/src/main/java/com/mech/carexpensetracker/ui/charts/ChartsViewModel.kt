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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChartsUiState(
    val chartKind: ChartKind = ChartKind.MonthlySpending,
    val preset: ChartDatePreset = ChartDatePreset.TwelveMonths,
    val axisLabels: List<String> = emptyList(),
    val fuelValues: List<Double> = emptyList(),
    val repairValues: List<Double> = emptyList(),
    val papersValues: List<Double> = emptyList(),
    val careValues: List<Double> = emptyList(),
    val lineValues: List<Double> = emptyList(),
    val hasData: Boolean = false,
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    carRepository: CarRepository,
    eventRepository: EventRepository,
) : ViewModel() {
    private val kind = MutableStateFlow(ChartKind.MonthlySpending)
    private val preset = MutableStateFlow(ChartDatePreset.TwelveMonths)

    val uiState: StateFlow<ChartsUiState> = combine(
        kind,
        preset,
        carRepository.observeSelectedCar(),
    ) { currentKind, currentPreset, car -> Triple(currentKind, currentPreset, car) }
        .flatMapLatest { (currentKind, currentPreset, car) ->
            if (car == null) {
                flowOf(ChartsUiState(chartKind = currentKind, preset = currentPreset, hasData = false))
            } else {
                val units = VehicleUnits.fromRaw(car.vehicleUnits)
                eventRepository.observeEvents(car.externalId).map { events ->
                    buildState(currentKind, currentPreset, events, units)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChartsUiState())

    fun load(chartKind: ChartKind, datePreset: ChartDatePreset) {
        kind.value = chartKind
        preset.value = datePreset
    }

    private fun buildState(
        chartKind: ChartKind,
        datePreset: ChartDatePreset,
        events: List<com.mech.carexpensetracker.data.db.entity.CarEventEntity>,
        units: VehicleUnits,
    ): ChartsUiState {
        val filtered = ChartDataService.filterByPreset(events, datePreset)
        return when (chartKind) {
            ChartKind.MonthlySpending -> {
                val bars = ChartDataService.monthlySpending(filtered)
                ChartsUiState(
                    chartKind = chartKind,
                    preset = datePreset,
                    axisLabels = bars.map { it.label },
                    fuelValues = bars.map { it.fuel.toDouble() },
                    repairValues = bars.map { it.repair.toDouble() },
                    papersValues = bars.map { it.papers.toDouble() },
                    careValues = bars.map { it.care.toDouble() },
                    hasData = bars.isNotEmpty(),
                )
            }
            ChartKind.FuelConsumption -> {
                val points = ChartDataService.fuelConsumptionPoints(events, units, filtered)
                ChartsUiState(
                    chartKind = chartKind,
                    preset = datePreset,
                    axisLabels = points.map { it.label },
                    lineValues = points.map { it.value.toDouble() },
                    hasData = points.isNotEmpty(),
                )
            }
            ChartKind.CategoryBreakdown -> {
                val slices = ChartDataService.categoryBreakdown(filtered)
                ChartsUiState(
                    chartKind = chartKind,
                    preset = datePreset,
                    axisLabels = slices.map { it.name },
                    lineValues = slices.map { it.amount.toDouble() },
                    hasData = slices.isNotEmpty(),
                )
            }
            ChartKind.CumulativeCost -> {
                val points = ChartDataService.cumulativeCost(filtered)
                ChartsUiState(
                    chartKind = chartKind,
                    preset = datePreset,
                    axisLabels = points.map { it.label },
                    lineValues = points.map { it.total.toDouble() },
                    hasData = points.isNotEmpty(),
                )
            }
        }
    }
}
