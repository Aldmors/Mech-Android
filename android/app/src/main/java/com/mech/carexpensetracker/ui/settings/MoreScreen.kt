package com.mech.carexpensetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mech.carexpensetracker.ui.theme.DesignTokens

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

    LazyColumn(modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md)) {
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.cars)) },
                supportingContent = { Text("${cars.size} cars") },
                modifier = Modifier.clickable(onClick = onCars),
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_label)) },
                modifier = Modifier.clickable(onClick = onImport),
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.reminders)) },
                modifier = Modifier.clickable(onClick = onReminders),
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.categories)) },
                modifier = Modifier.clickable(onClick = onCategories),
            )
        }
        item {
            ListItem(headlineContent = { Text(stringResource(R.string.version)) }, supportingContent = { Text("1.0.0") })
        }
    }
}
