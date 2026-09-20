package com.mech.carexpensetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.MainViewModel
import com.mech.carexpensetracker.ui.components.AppLazyColumn

@Composable
fun MoreScreen(
    onCars: () -> Unit,
    onImport: () -> Unit,
    onReminders: () -> Unit,
    onCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()

    AppLazyColumn(modifier = modifier) {
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.cars)) },
                supportingContent = { Text("${cars.size}") },
                modifier = Modifier.clickable(onClick = onCars),
            )
            HorizontalDivider()
        }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.import_label)) },
                modifier = Modifier.clickable(onClick = onImport),
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
                leadingContent = { Icon(Icons.Default.Category, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.categories)) },
                modifier = Modifier.clickable(onClick = onCategories),
            )
            HorizontalDivider()
        }
        item {
            ListItem(
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.version)) },
                supportingContent = { Text("1.0.0") },
            )
        }
    }
}
