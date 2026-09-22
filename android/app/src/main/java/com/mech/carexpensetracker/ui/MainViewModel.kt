package com.mech.carexpensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.repository.CarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val carRepository: CarRepository,
) : ViewModel() {
    private val ready = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = ready.asStateFlow()

    val cars: StateFlow<List<CarEntity>> = carRepository.observeCars()
        .onEach { ready.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedCar: StateFlow<CarEntity?> = carRepository.observeSelectedCar()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectCar(externalId: String) {
        viewModelScope.launch { carRepository.selectCar(externalId) }
    }

    suspend fun saveCar(
        externalId: String?,
        name: String,
        plateNumber: String?,
        vehicleUnits: String,
        primaryFuelType: String,
        alternativeFuelType: String?,
        iconName: String,
        buyDateMillis: Long? = null,
    ) {
        if (externalId == null) {
            carRepository.createCar(
                name,
                plateNumber,
                vehicleUnits,
                primaryFuelType,
                alternativeFuelType,
                iconName,
                buyDateMillis,
            )
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
                        iconName = iconName,
                        buyDateMillis = buyDateMillis,
                    ),
                )
            }
        }
    }

    suspend fun deleteCar(externalId: String) = carRepository.deleteCar(externalId)
}
