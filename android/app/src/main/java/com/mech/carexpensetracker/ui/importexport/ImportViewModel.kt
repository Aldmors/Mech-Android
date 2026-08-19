package com.mech.carexpensetracker.ui.importexport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.import_.ImportCoordinator
import com.mech.carexpensetracker.import_.ImportMode
import com.mech.carexpensetracker.import_.ImportPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val preview: ImportPreview? = null,
    val isImporting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importCoordinator: ImportCoordinator,
    private val carRepository: com.mech.carexpensetracker.data.repository.CarRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun preview(files: Map<String, String>) {
        val preview = importCoordinator.preview(files)
        _uiState.value = ImportUiState(preview = preview)
    }

    fun import(files: Map<String, String>, mode: ImportMode = ImportMode.Merge) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            try {
                val result = importCoordinator.import(files, mode)
                result.cars.firstOrNull()?.let { carRepository.selectCar(it.externalId) }
                _uiState.value = ImportUiState(
                    preview = result,
                    message = "Imported ${result.carCount} cars, ${result.eventCount} events",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false, error = e.message)
            }
        }
    }
}
