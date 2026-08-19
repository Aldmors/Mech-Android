package com.mech.carexpensetracker.ui.importexport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mech.carexpensetracker.ui.components.PrimaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens

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
                if (CarnotesDtos.ALL_TABLES.contains(name)) {
                    files[name] = content
                }
            }
            if (files.isNotEmpty()) {
                viewModel.preview(files)
                viewModel.import(files)
            }
        },
    )

    LazyColumn(modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md)) {
        item { SectionHeader(title = stringResource(R.string.import_label)) }
        item {
            PrimaryButton(
                text = stringResource(R.string.import_json),
                onClick = { launcher.launch(arrayOf("application/json")) },
            )
        }
        state.preview?.let { preview ->
            item { Text("Cars: ${preview.carCount}, Events: ${preview.eventCount}") }
            item { Text("Reminders: ${preview.reminderCount}, Notes: ${preview.noteCount}") }
        }
        state.message?.let { item { Text(it) } }
        state.error?.let { item { Text(it) } }
        item {
            PrimaryButton(text = stringResource(R.string.save), onClick = onDone)
        }
    }
}
