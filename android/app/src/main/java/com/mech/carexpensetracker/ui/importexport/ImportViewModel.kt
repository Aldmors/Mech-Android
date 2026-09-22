package com.mech.carexpensetracker.ui.importexport

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mech.carexpensetracker.data.repository.CarRepository
import com.mech.carexpensetracker.import_.CarnotesExporter
import com.mech.carexpensetracker.import_.ImportCoordinator
import com.mech.carexpensetracker.import_.ImportFileReader
import com.mech.carexpensetracker.import_.ImportMode
import com.mech.carexpensetracker.import_.ImportPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ImportUiState(
    val preview: ImportPreview? = null,
    val selectedFiles: Map<String, String> = emptyMap(),
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val noFiles: Boolean = false,
    val exportOk: Boolean = false,
    val exportFailed: Boolean = false,
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importCoordinator: ImportCoordinator,
    private val carRepository: CarRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val stagedFiles = mutableMapOf<String, String>()
    private val selectedNames = mutableMapOf<String, String>()
    private var stagedBinaries = emptyMap<String, ByteArray>()

    fun stageZip(fileName: String, bytes: ByteArray) {
        stageNamedFiles(listOf(fileName to bytes), replace = true)
    }

    fun stageJson(fileName: String, bytes: ByteArray) {
        stageNamedFiles(listOf(fileName to bytes), replace = false)
    }

    fun stageNamedFiles(files: List<Pair<String, ByteArray>>, replace: Boolean = true) {
        viewModelScope.launch {
            try {
                val archive = withContext(Dispatchers.IO) {
                    ImportFileReader.archiveFromNamedContents(files)
                }
                if (archive.tables.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = null,
                        noFiles = true,
                        message = null,
                    )
                    return@launch
                }
                applyTables(
                    archive.tables,
                    sourceName = files.firstOrNull()?.first,
                    replace = replace,
                    binaries = archive.binaries,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isImporting = false,
                    noFiles = false,
                )
            }
        }
    }

    fun stageFiles(files: Map<String, String>) {
        applyTables(files, sourceName = null, replace = true)
    }

    private fun applyTables(
        tables: Map<String, String>,
        sourceName: String?,
        replace: Boolean,
        binaries: Map<String, ByteArray> = emptyMap(),
    ) {
        if (replace) {
            stagedFiles.clear()
            selectedNames.clear()
            stagedBinaries = emptyMap()
        }
        stagedFiles.putAll(tables)
        stagedBinaries = if (replace) binaries else stagedBinaries + binaries
        val label = sourceName?.substringAfterLast('/')?.substringAfterLast('\\') ?: ""
        tables.keys.forEach { key ->
            selectedNames[key] = label.ifBlank { key }
        }
        preview(stagedFiles.toMap())
    }

    fun preview(files: Map<String, String>) {
        try {
            val preview = importCoordinator.preview(files)
            _uiState.value = _uiState.value.copy(
                preview = preview,
                selectedFiles = selectedNames.toMap(),
                error = null,
                noFiles = false,
                message = null,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                preview = null,
                error = e.message,
                noFiles = false,
            )
        }
    }

    fun consumeAlerts() {
        _uiState.value = _uiState.value.copy(
            error = null,
            noFiles = false,
            message = null,
            exportOk = false,
            exportFailed = false,
        )
    }

    fun importStaged(mode: ImportMode = ImportMode.Merge) {
        import(stagedFiles.toMap(), mode)
    }

    fun import(files: Map<String, String>, mode: ImportMode = ImportMode.Merge) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null, message = null)
            try {
                val result = importCoordinator.import(files, mode, binaries = stagedBinaries)
                result.cars.firstOrNull()?.let { carRepository.selectCar(it.externalId) }
                _uiState.value = ImportUiState(
                    preview = result,
                    selectedFiles = selectedNames.toMap(),
                    message = "ok",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false, error = e.message)
            }
        }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                exportOk = false,
                exportFailed = false,
            )
            try {
                val ok = withContext(Dispatchers.IO) {
                    val archive = importCoordinator.exportAll()
                    val bytes = CarnotesExporter.toZip(archive.tables, archive.binaries)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                        true
                    } ?: false
                }
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportOk = ok,
                    exportFailed = !ok,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportFailed = true,
                )
            }
        }
    }
}
