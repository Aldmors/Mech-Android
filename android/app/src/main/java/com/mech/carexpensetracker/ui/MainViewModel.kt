package com.mech.carexpensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val carRepository: CarRepository,
) : ViewModel() {
    val cars: StateFlow<List<CarEntity>> = carRepository.observeCars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCar: StateFlow<CarEntity?> = carRepository.observeSelectedCar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectCar(externalId: String) {
        viewModelScope.launch { carRepository.selectCar(externalId) }
    }

    fun saveCar(
        externalId: String?,
        name: String,
        plateNumber: String?,
        vehicleUnits: String,
        primaryFuelType: String,
        alternativeFuelType: String?,
    ) {
        viewModelScope.launch {
            if (externalId == null) {
                carRepository.createCar(name, plateNumber, vehicleUnits, primaryFuelType, alternativeFuelType)
            } else {
                val existing = carRepository.getCar(externalId)
                if (existing != null) {
                    carRepository.upsertCar(
                        existing.copy(
                            name = name,
                            plateNumber = plateNumber,
                            vehicleUnits = vehicleUnits,
                            primaryFuelTypeRaw = primaryFuelType,
                            alternativeFuelTypeRaw = alternativeFuelType,
                        ),
                    )
                }
            }
        }
    }

    fun deleteCar(externalId: String) {
        viewModelScope.launch { carRepository.deleteCar(externalId) }
    }
}
