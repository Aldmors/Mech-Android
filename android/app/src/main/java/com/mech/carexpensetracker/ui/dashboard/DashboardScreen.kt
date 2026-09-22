package com.mech.carexpensetracker.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.domain.model.FuelType
import com.mech.carexpensetracker.ui.components.AppIcons
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.CarSelector
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.EventRecordRow
import com.mech.carexpensetracker.ui.components.MetricCard
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.components.fuelTypeStringRes
import com.mech.carexpensetracker.ui.components.parseCategoryColor
import com.mech.carexpensetracker.ui.theme.DesignTokens

@Composable
fun DashboardScreen(
    onOpenEvents: () -> Unit,
    onReminderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppLazyColumn(
        modifier = modifier,
        clearFab = true,
    ) {
        item {
            CarSelector(
                cars = state.cars,
                selected = state.car,
                onSelect = viewModel::selectCar,
            )
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
            val altFuel = state.car?.alternativeFuelTypeRaw?.takeIf { it.isNotBlank() }
            MetricSquares {
                SquareMetric(
                    title = if (altFuel == null) {
                        stringResource(R.string.avg_consumption)
                    } else {
                        stringResource(
                            R.string.avg_consumption_fuel,
                            stringResource(fuelTypeStringRes(FuelType.fromRaw(state.car?.primaryFuelTypeRaw))),
                        )
                    },
                    value = state.consumption,
                    icon = Icons.Default.LocalGasStation,
                    compact = altFuel != null,
                )
                if (altFuel != null) {
                    SquareMetric(
                        title = stringResource(
                            R.string.avg_consumption_fuel,
                            stringResource(fuelTypeStringRes(FuelType.fromRaw(altFuel))),
                        ),
                        value = state.alternativeConsumption,
                        icon = Icons.Default.LocalGasStation,
                        compact = true,
                    )
                }
                SquareMetric(
                    title = stringResource(R.string.event_count),
                    value = state.eventCount,
                    icon = AppIcons.eventsTab,
                    onClick = onOpenEvents,
                    compact = altFuel != null,
                )
            }
        }
        if (state.approachingReminders.isNotEmpty()) {
            item { SectionHeader(title = stringResource(R.string.reminder_approaching)) }
            items(state.approachingReminders, key = { it.externalId }) { reminder ->
                DashboardReminderRow(
                    reminder = reminder,
                    unit = state.vehicleUnits.raw,
                    onClick = { onReminderClick(reminder.externalId) },
                )
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.ownership))
        }
        item {
            MetricSquares {
                SquareMetric(
                    title = stringResource(
                        if (state.vehicleUnits == com.mech.carexpensetracker.domain.model.VehicleUnits.Mi) {
                            R.string.cost_per_mi
                        } else {
                            R.string.cost_per_km
                        },
                    ),
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
                    title = stringResource(R.string.avg_monthly_cost),
                    value = state.avgMonthlyCost,
                    icon = Icons.Default.CalendarMonth,
                    compact = true,
                )
            }
        }
        item {
            SectionHeader(title = stringResource(R.string.recent_events))
        }
        if (state.recentEvents.isEmpty()) {
            item { EmptyStateCard(message = stringResource(R.string.no_events)) }
        } else {
            items(state.recentEvents, key = { it.externalId }) { event ->
                EventRecordRow(
                    event = event,
                    consumption = state.consumptionById[event.externalId],
                    units = state.vehicleUnits,
                )
            }
        }
    }
}

@Composable
private fun DashboardReminderRow(
    reminder: DashboardReminderUi,
    unit: String,
    onClick: () -> Unit,
) {
    val color = remember(reminder.colorHex) { parseCategoryColor(reminder.colorHex) }
    val supportingColor = if (reminder.isDue) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListItem(
        headlineContent = { Text(reminder.title) },
        supportingContent = {
            Column {
                if (reminder.isDue) {
                    Text(
                        text = stringResource(R.string.reminder_overdue),
                        color = supportingColor,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                reminder.daysLeft?.let { days ->
                    val text = when {
                        days < 0 -> null
                        days == 0L -> stringResource(R.string.reminder_due_today)
                        else -> pluralStringResource(R.plurals.reminder_days_left, days.toInt(), days.toInt())
                    }
                    if (text != null) {
                        Text(text = text, color = supportingColor, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                reminder.remainingKm?.let { remaining ->
                    if (remaining > 0) {
                        Text(
                            text = stringResource(R.string.reminder_remaining_km, remaining, unit),
                            color = supportingColor,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = color,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun MetricSquares(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
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
        compact = compact,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
    )
}
