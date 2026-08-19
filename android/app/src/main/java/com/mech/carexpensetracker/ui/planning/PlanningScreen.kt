package com.mech.carexpensetracker.ui.planning

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.components.AppCard
import com.mech.carexpensetracker.ui.components.MetricCard
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens

@Composable
fun PlanningScreen(
    modifier: Modifier = Modifier,
    viewModel: PlanningViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md),
    ) {
        item {
            SectionHeader(title = stringResource(R.string.planned_expenses))
        }
        item {
            MetricCard(
                title = stringResource(R.string.savings_target),
                value = state.monthlyTarget,
            )
        }
        items(state.plannedItems) { item ->
            AppCard {
                Text(text = item.name, modifier = Modifier.padding(DesignTokens.Spacing.md))
                Text(text = item.cost, modifier = Modifier.padding(DesignTokens.Spacing.md))
            }
        }
    }
}
