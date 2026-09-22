package com.mech.carexpensetracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.MainViewModel
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.importexport.ImportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    onCars: () -> Unit,
    onImport: () -> Unit,
    onReminders: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    importViewModel: ImportViewModel = hiltViewModel(),
) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val importState by importViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val exportSuccessMessage = stringResource(R.string.export_success)
    val exportFailedMessage = stringResource(R.string.export_failed)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) importViewModel.exportTo(uri)
    }

    LaunchedEffect(importState.exportOk) {
        if (importState.exportOk) {
            onMessage(exportSuccessMessage)
            importViewModel.consumeAlerts()
        }
    }
    LaunchedEffect(importState.exportFailed) {
        if (importState.exportFailed) {
            onMessage(exportFailedMessage)
            importViewModel.consumeAlerts()
        }
    }

    AppLazyColumn(modifier = modifier) {
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.cars)) },
                supportingContent = {
                    Text(pluralStringResource(R.plurals.car_count, cars.size, cars.size))
                },
                modifier = Modifier.clickable(onClick = onCars),
            )
            HorizontalDivider()
        }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.import_label)) },
                modifier = Modifier.clickable(
                    enabled = !importState.isExporting,
                    onClick = onImport,
                ),
            )
            HorizontalDivider()
        }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.export)) },
                modifier = Modifier.clickable(enabled = !importState.isExporting) {
                    val name = "carnotes_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.zip"
                    exportLauncher.launch(name)
                },
            )
            HorizontalDivider()
        }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.reminders)) },
                modifier = Modifier.clickable(onClick = onReminders),
            )
            HorizontalDivider()
        }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.version)) },
                supportingContent = { Text(versionName) },
            )
        }
    }
}
