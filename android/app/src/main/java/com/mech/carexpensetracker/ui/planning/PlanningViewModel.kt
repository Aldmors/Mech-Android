package com.mech.carexpensetracker.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.PlannedExpenseDao
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.domain.service.PlanningSavingsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PlannedItemUi(val name: String, val cost: String)

data class PlanningUiState(
    val monthlyTarget: String = "—",
    val plannedItems: List<PlannedItemUi> = emptyList(),
)

@HiltViewModel
class PlanningViewModel @Inject constructor(
    carRepository: CarRepository,
    plannedExpenseDao: PlannedExpenseDao,
) : ViewModel() {
    val uiState: StateFlow<PlanningUiState> = carRepository.observeSelectedCar()
        .flatMapLatest { car ->
            if (car == null) {
                flowOf(PlanningUiState())
            } else {
                plannedExpenseDao.observeForCar(car.externalId).map { planned ->
                    val summary = PlanningSavingsService.summarize(planned)
                    PlanningUiState(
                        monthlyTarget = CurrencyFormatter.formatOrDash(summary.monthlyTarget),
                        plannedItems = planned.map {
                            PlannedItemUi(it.name, CurrencyFormatter.formatOrDash(it.cost.toBigDecimalOrNull()))
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlanningUiState())
}
