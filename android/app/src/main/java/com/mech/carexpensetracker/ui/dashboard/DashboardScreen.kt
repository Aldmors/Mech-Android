package com.mech.carexpensetracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.ui.components.AppCard
import com.mech.carexpensetracker.ui.components.CarHeader
import com.mech.carexpensetracker.ui.components.EventTypeBadge
import com.mech.carexpensetracker.ui.components.MetricCard
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens

@Composable
fun DashboardScreen(
    onAddFuel: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
    ) {
        item {
            CarHeader(car = state.car, modifier = Modifier.padding(DesignTokens.Spacing.md))
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                MetricCard(
                    title = stringResource(R.string.monthly_spend),
                    value = state.monthlySpend,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = stringResource(R.string.mileage),
                    value = state.mileage,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                MetricCard(
                    title = stringResource(R.string.consumption),
                    value = state.consumption,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = stringResource(R.string.ownership),
                    value = state.ownershipCost,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SectionHeader(
                title = stringResource(R.string.recent_events),
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md),
            )
        }
        items(state.recentEvents) { event ->
            RecentEventRow(event = event, modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md))
        }
    }
}

@Composable
private fun RecentEventRow(event: CarEventEntity, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            EventTypeBadge(type = EventType.fromRaw(event.typeRaw))
            Text(text = CurrencyFormatter.formatOrDash(event.totalCost?.toBigDecimalOrNull()))
        }
        event.comment?.let {
            Text(
                text = it,
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md, vertical = 4.dp),
            )
        }
    }
}
