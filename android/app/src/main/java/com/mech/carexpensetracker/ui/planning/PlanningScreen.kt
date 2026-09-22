package com.mech.carexpensetracker.ui.planning

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.MetricCard
import com.mech.carexpensetracker.ui.components.SectionHeader

@Composable
fun PlanningScreen(
    modifier: Modifier = Modifier,
    viewModel: PlanningViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppLazyColumn(modifier = modifier) {
        item {
            SectionHeader(title = stringResource(R.string.planned_expenses))
        }
        item {
            MetricCard(
                title = stringResource(R.string.savings_target),
                value = state.monthlyTarget,
                icon = Icons.Default.Savings,
            )
        }
        if (state.plannedItems.isEmpty()) {
            item {
                EmptyStateCard(message = stringResource(R.string.no_planned_expenses))
            }
        } else {
            items(state.plannedItems) { item ->
                ListItem(
                    headlineContent = { Text(item.name) },
                    trailingContent = { Text(item.cost) },
                )
            }
        }
    }
}
