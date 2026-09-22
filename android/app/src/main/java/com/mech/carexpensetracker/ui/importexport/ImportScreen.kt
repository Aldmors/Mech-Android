package com.mech.carexpensetracker.ui.importexport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.import_.CarnotesDtos
import com.mech.carexpensetracker.ui.components.AppCard
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.PrimaryButton
import com.mech.carexpensetracker.ui.components.SecondaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val zipMimeTypes = arrayOf(
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
    "*/*",
)

private val jsonMimeTypes = arrayOf(
    "application/json",
    "text/plain",
    "application/octet-stream",
    "*/*",
)

private val jsonSlots = listOf(
    CarnotesDtos.GARAGE_TABLE to R.string.import_file_garage,
    CarnotesDtos.CAR_EVENTS_TABLE to R.string.import_file_events,
    CarnotesDtos.CAR_REMINDERS_TABLE to R.string.import_file_reminders,
    CarnotesDtos.NOTES_TABLE to R.string.import_file_notes,
)

@Composable
fun ImportScreen(
    onDone: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val importSuccessMessage = stringResource(R.string.import_success)
    val importFailedMessage = stringResource(R.string.import_failed)
    val importNoFilesMessage = stringResource(R.string.import_no_files)
    val exportSuccessMessage = stringResource(R.string.export_success)
    val exportFailedMessage = stringResource(R.string.export_failed)
    val busy = state.isImporting || state.isExporting

    LaunchedEffect(state.message) {
        if (state.message != null) {
            onMessage(importSuccessMessage)
            viewModel.consumeAlerts()
            onDone()
        }
    }
    LaunchedEffect(state.error) {
        if (state.error != null) {
            onMessage(importFailedMessage)
            viewModel.consumeAlerts()
        }
    }
    LaunchedEffect(state.noFiles) {
        if (state.noFiles) {
            onMessage(importNoFilesMessage)
            viewModel.consumeAlerts()
        }
    }
    LaunchedEffect(state.exportOk) {
        if (state.exportOk) {
            onMessage(exportSuccessMessage)
            viewModel.consumeAlerts()
        }
    }
    LaunchedEffect(state.exportFailed) {
        if (state.exportFailed) {
            onMessage(exportFailedMessage)
            viewModel.consumeAlerts()
        }
    }

    val zipLauncher = rememberNamedFileLauncher(context) { name, bytes ->
        viewModel.stageZip(name, bytes)
    }
    val jsonLauncher = rememberNamedFileLauncher(context) { name, bytes ->
        viewModel.stageJson(name, bytes)
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }

    AppLazyColumn(modifier = modifier) {
        item { SectionHeader(title = stringResource(R.string.import_zip)) }
        item {
            PrimaryButton(
                text = stringResource(R.string.import_zip_action),
                onClick = { zipLauncher.launch(zipMimeTypes) },
                icon = Icons.Default.FolderOpen,
                enabled = !busy,
            )
        }
        item { SectionHeader(title = stringResource(R.string.import_json_files)) }
        jsonSlots.forEachIndexed { index, (tableKey, labelRes) ->
            item(key = tableKey) {
                ImportFileSlot(
                    labelRes = labelRes,
                    fileName = state.selectedFiles[tableKey],
                    enabled = !busy,
                    onClick = { jsonLauncher.launch(jsonMimeTypes) },
                    showDivider = index < jsonSlots.lastIndex,
                )
            }
        }
        state.preview?.let { preview ->
            item {
                AppCard {
                    Column(
                        modifier = Modifier.padding(DesignTokens.Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                    ) {
                        Text(
                            text = stringResource(R.string.import_summary),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(stringResource(R.string.import_cars_count, preview.carCount))
                        Text(stringResource(R.string.import_events_count, preview.eventCount))
                        Text(stringResource(R.string.import_reminders_count, preview.reminderCount))
                        Text(stringResource(R.string.import_notes_count, preview.noteCount))
                        Text(stringResource(R.string.import_photos_count, preview.photoCount))
                    }
                }
            }
        }
        if (state.preview != null) {
            item {
                PrimaryButton(
                    text = stringResource(R.string.import_label),
                    onClick = { viewModel.importStaged() },
                    icon = Icons.Default.CloudUpload,
                    loading = state.isImporting,
                )
            }
        }
        item { SectionHeader(title = stringResource(R.string.export)) }
        item {
            PrimaryButton(
                text = stringResource(R.string.export),
                onClick = {
                    val name = "carnotes_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.zip"
                    exportLauncher.launch(name)
                },
                icon = Icons.Default.Download,
                enabled = !busy,
                loading = state.isExporting,
            )
        }
        item {
            SecondaryButton(
                text = stringResource(R.string.done),
                onClick = onDone,
                icon = Icons.Default.Check,
                enabled = !busy,
            )
        }
    }
}

@Composable
private fun ImportFileSlot(
    @StringRes labelRes: Int,
    fileName: String?,
    enabled: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    ListItem(
        headlineContent = { Text(stringResource(labelRes)) },
        supportingContent = {
            Text(fileName ?: stringResource(R.string.import_file_not_selected))
        },
        trailingContent = {
            Icon(Icons.Default.FolderOpen, contentDescription = stringResource(labelRes))
        },
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
    if (showDivider) HorizontalDivider()
}

@Composable
private fun rememberNamedFileLauncher(
    context: Context,
    onPicked: (String, ByteArray) -> Unit,
) = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri == null) return@rememberLauncherForActivityResult
    val file = readNamedBytes(context, uri) ?: return@rememberLauncherForActivityResult
    onPicked(file.first, file.second)
}

private fun readNamedBytes(context: Context, uri: Uri): Pair<String, ByteArray>? {
    val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: uri.lastPathSegment ?: "file"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return name to bytes
}
