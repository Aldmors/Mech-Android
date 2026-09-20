package com.mech.carexpensetracker.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.ui.components.AppIcons
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.CarSelector
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.EventRecordRow
import com.mech.carexpensetracker.ui.components.MetricCard
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens

@Composable
fun DashboardScreen(
    onAddFuel: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenEvents: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppLazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(DesignTokens.Spacing.md),
    ) {
        item {
            Column {
                CarSelector(
                    cars = state.cars,
                    selected = state.car,
                    onSelect = viewModel::selectCar,
                )
                ReminderBannerSlot(title = state.reminderTitle)
            }
        }
        item {
            MetricSquares {
                SquareMetric(
                    title = stringResource(R.string.monthly_spend),
                    value = state.monthlySpend,
                    icon = Icons.Default.CreditCard,
                )
                SquareMetric(
                    title = stringResource(R.string.kilometers),
                    value = state.mileage,
                    icon = Icons.Default.Speed,
                )
            }
        }
        item {
            MetricSquares {
                SquareMetric(
                    title = stringResource(R.string.avg_consumption),
                    value = state.consumption,
                    icon = Icons.Default.LocalGasStation,
                )
                SquareMetric(
                    title = stringResource(R.string.event_count),
                    value = state.eventCount,
                    icon = AppIcons.eventsTab,
                    onClick = onOpenEvents,
                )
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.ownership))
        }
        item {
            MetricSquares {
                SquareMetric(
                    title = stringResource(R.string.cost_per_km),
                    value = state.costPerKm,
                    icon = Icons.Default.AddRoad,
                    compact = true,
                )
                SquareMetric(
                    title = stringResource(R.string.total_spend),
                    value = state.totalSpend,
                    icon = Icons.Default.Functions,
                    compact = true,
                )
                SquareMetric(
                    title = stringResource(R.string.distance_traveled),
                    value = state.distance,
                    icon = Icons.Default.MultipleStop,
                    compact = true,
                )
            }
        }
        item {
            SectionHeader(
                title = stringResource(R.string.recent_events),
                trailing = {
                    AddEventButton(onAddFuel = onAddFuel, onAddExpense = onAddExpense)
                },
            )
        }
        if (state.recentEvents.isEmpty()) {
            item { EmptyStateCard(message = stringResource(R.string.no_events)) }
        } else {
            items(state.recentEvents, key = { it.externalId }) { event ->
                EventRecordRow(
                    event = event,
                    previousFuelEvent = state.previousFuelById[event.externalId],
                    units = state.vehicleUnits,
                )
            }
        }
    }
}

@Composable
private fun ReminderBannerSlot(title: String?) {
    AnimatedVisibility(visible = title != null) {
        title?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DesignTokens.Spacing.md),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(DesignTokens.Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.reminders),
                    )
                    Text(text = it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MetricSquares(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        content = content,
    )
}

@Composable
private fun RowScope.SquareMetric(
    title: String,
    value: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    MetricCard(
        title = title,
        value = value,
        icon = icon,
        onClick = onClick,
        centered = true,
        compact = compact,
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
    )
}

@Composable
private fun AddEventButton(
    onAddFuel: () -> Unit,
    onAddExpense: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_event),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_fuel)) },
                onClick = {
                    menuOpen = false
                    onAddFuel()
                },
                leadingIcon = {
                    Icon(AppIcons.eventType(EventType.Fuel), contentDescription = null)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_expense)) },
                onClick = {
                    menuOpen = false
                    onAddExpense()
                },
                leadingIcon = {
                    Icon(AppIcons.eventType(EventType.Repair), contentDescription = null)
                },
            )
        }
    }
}
