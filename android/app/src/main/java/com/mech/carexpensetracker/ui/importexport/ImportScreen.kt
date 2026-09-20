package com.mech.carexpensetracker.ui.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.import_.CarnotesDtos
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.PrimaryButton

@Composable
fun ImportScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            val files = mutableMapOf<String, String>()
            uris.forEach { uri ->
                val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                } ?: return@forEach
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@forEach
                val tableKey = CarnotesDtos.resolveTableKey(name, content) ?: return@forEach
                files[tableKey] = content
            }
            if (files.isNotEmpty()) {
                viewModel.stageFiles(files)
            }
        },
    )

    AppLazyColumn(modifier = modifier) {
        item {
            PrimaryButton(
                text = stringResource(R.string.import_json),
                onClick = { launcher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                icon = Icons.Default.FolderOpen,
            )
        }
        state.preview?.let { preview ->
            item { Text("Cars: ${preview.carCount}, Events: ${preview.eventCount}") }
            item { Text("Reminders: ${preview.reminderCount}, Notes: ${preview.noteCount}") }
        }
        state.message?.let { item { Text(it) } }
        state.error?.let { item { Text(it) } }
        if (state.preview != null) {
            item {
                PrimaryButton(
                    text = stringResource(R.string.import_label),
                    onClick = { viewModel.importStaged() },
                    icon = Icons.Default.CloudUpload,
                )
            }
        }
        item {
            PrimaryButton(text = stringResource(R.string.save), onClick = onDone, icon = Icons.Default.Check)
        }
    }
}
