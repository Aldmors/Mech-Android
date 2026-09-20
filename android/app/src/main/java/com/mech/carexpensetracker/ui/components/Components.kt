package com.mech.carexpensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.domain.model.FuelType
import com.mech.carexpensetracker.domain.model.VehicleUnits
import com.mech.carexpensetracker.domain.service.ConsumptionCalculator
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.domain.service.RecordCostService
import com.mech.carexpensetracker.ui.theme.DesignTokens
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(DesignTokens.Spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
        content = content,
    )
}

@Composable
fun AppLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(DesignTokens.Spacing.md),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(DesignTokens.Spacing.md),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier, content = content)
    } else {
        Card(modifier = cardModifier, content = content)
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    centered: Boolean = false,
    compact: Boolean = false,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        if (centered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) DesignTokens.Spacing.sm else DesignTokens.Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = title,
                        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = value,
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Column(modifier = Modifier.padding(DesignTokens.Spacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        ButtonLabel(text = text, icon = icon)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        ButtonLabel(text = text, icon = icon)
    }
}

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(text)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    if (trailing == null) {
        Text(
            text = title,
            modifier = modifier,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        trailing()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarSelector(
    cars: List<CarEntity>,
    selected: CarEntity?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = selected ?: cars.firstOrNull()
    val canExpand = cars.size > 1
    ExposedDropdownMenuBox(
        expanded = expanded && canExpand,
        onExpandedChange = { if (canExpand) expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        AppCard(
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = canExpand)
                .fillMaxWidth(),
        ) {
            CarSelectorRow(
                car = current,
                expanded = expanded && canExpand,
                showChevron = canExpand,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(DesignTokens.Spacing.md),
            )
        }
        ExposedDropdownMenu(
            expanded = expanded && canExpand,
            onDismissRequest = { expanded = false },
        ) {
            cars.filter { it.externalId != current?.externalId }.forEach { car ->
                DropdownMenuItem(
                    text = {
                        CarSelectorRow(car = car, modifier = Modifier.fillMaxWidth())
                    },
                    onClick = {
                        onSelect(car.externalId)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun CarSelectorRow(
    car: CarEntity?,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    showChevron: Boolean = false,
) {
    if (car == null) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Text(
                text = car.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            car.plateNumber?.takeIf { it.isNotBlank() }?.let { plate ->
                Text(
                    text = plate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs)) {
                FuelTypeLabel(typeRaw = car.primaryFuelTypeRaw)
                car.alternativeFuelTypeRaw?.takeIf { it.isNotBlank() }?.let { FuelTypeLabel(typeRaw = it) }
            }
        }
        if (showChevron) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.select_car),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FuelTypeLabel(typeRaw: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(fuelTypeStringRes(FuelType.fromRaw(typeRaw))),
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.sm, vertical = DesignTokens.Spacing.xs),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun fuelTypeStringRes(type: FuelType): Int = when (type) {
    FuelType.Gasoline -> R.string.fuel_gasoline
    FuelType.Diesel -> R.string.fuel_diesel
    FuelType.Lpg -> R.string.fuel_lpg
    FuelType.Electric -> R.string.fuel_electric
}

fun eventTypeStringRes(type: EventType): Int = when (type) {
    EventType.Fuel -> R.string.event_type_fuel
    EventType.Repair -> R.string.event_type_service
    EventType.Papers -> R.string.event_type_documents
}

fun eventTypeColor(type: EventType): Color = when (type) {
    EventType.Fuel -> DesignTokens.Palette.fuel
    EventType.Repair -> DesignTokens.Palette.repair
    EventType.Papers -> DesignTokens.Palette.papers
}

@Composable
fun EventTypeBadge(type: EventType, modifier: Modifier = Modifier) {
    val color = eventTypeColor(type)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.sm, vertical = DesignTokens.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Icon(
                imageVector = AppIcons.eventType(type),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color,
            )
            Text(
                text = stringResource(eventTypeStringRes(type)),
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun EventRecordRow(
    event: CarEventEntity,
    modifier: Modifier = Modifier,
    previousFuelEvent: CarEventEntity? = null,
    units: VehicleUnits = VehicleUnits.Km,
) {
    val type = EventType.fromRaw(event.typeRaw)
    val isFuel = type == EventType.Fuel
    val fullTank = event.fuelFullTank || event.secondaryFuelFullTank
    val dateText = remember(event.dateMillis) { formatEventDate(event.dateMillis) }
    val timeText = remember(event.dateMillis) { formatEventTime(event.dateMillis) }
    val namedTitle = event.categoryName?.takeIf { it.isNotBlank() }
        ?: event.comment?.takeIf { it.isNotBlank() }
    val title = namedTitle ?: dateText
    val consumption = if (isFuel) {
        ConsumptionCalculator.consumptionIfFullTank(event, previousFuelEvent, units)
    } else {
        null
    }
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.md),
            verticalAlignment = if (isFuel) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        ) {
            EventTypeIcon(type = type, fullTank = isFuel && fullTank)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isFuel) {
                    FuelSubtitle(
                        event = event,
                        timeText = timeText,
                        dateText = dateText,
                        showDate = namedTitle != null,
                    )
                    event.mileage?.let { mileage ->
                        Text(
                            text = stringResource(R.string.mileage_with_unit, formatGroupedInt(mileage), units.raw),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                    ) {
                        EventTypeBadge(type = type)
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    event.comment?.takeIf { it.isNotBlank() && it != title }?.let { comment ->
                        Text(
                            text = comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatOrDash(event.totalCost?.toBigDecimalOrNull()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFuel) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
                if (isFuel) {
                    FuelCostExtras(event = event, consumption = consumption, units = units)
                }
            }
        }
    }
}

@Composable
private fun EventTypeIcon(type: EventType, fullTank: Boolean, modifier: Modifier = Modifier) {
    val color = eventTypeColor(type)
    Surface(
        modifier = modifier.size(48.dp),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.12f),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = AppIcons.eventType(type, fullTank = fullTank),
                contentDescription = stringResource(eventTypeStringRes(type)),
                modifier = Modifier.size(24.dp),
                tint = color,
            )
        }
    }
}

@Composable
private fun FuelSubtitle(
    event: CarEventEntity,
    timeText: String,
    dateText: String,
    showDate: Boolean,
) {
    val types = listOfNotNull(
        event.fuelTypeRaw?.takeIf { it.isNotBlank() }?.let { stringResource(fuelTypeStringRes(FuelType.fromRaw(it))) },
        event.secondaryFuelTypeRaw?.takeIf { it.isNotBlank() }?.let { stringResource(fuelTypeStringRes(FuelType.fromRaw(it))) },
    )
    val parts = buildList {
        add(timeText)
        addAll(types)
        if (showDate) add(dateText)
    }
    Text(
        text = parts.joinToString("  "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FuelCostExtras(
    event: CarEventEntity,
    consumption: BigDecimal?,
    units: VehicleUnits,
) {
    eventUnitPriceText(event)?.let { FuelStat(icon = Icons.Default.AttachMoney, text = it) }
    fuelAmountText(event)?.let { FuelStat(icon = Icons.Default.WaterDrop, text = it) }
    consumption?.let { value ->
        val label = when (units) {
            VehicleUnits.Km -> stringResource(R.string.consumption_l_100km, formatDecimal(value))
            VehicleUnits.Mi -> stringResource(R.string.consumption_mpg, formatDecimal(value))
        }
        FuelStat(icon = Icons.Default.Speed, text = label)
    }
}

@Composable
private fun FuelStat(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun fuelAmountText(event: CarEventEntity): String? {
    val amounts = listOfNotNull(
        event.fuelAmount?.takeIf { it.isNotBlank() }?.let { displayAmount(it) },
        event.secondaryFuelAmount?.takeIf { it.isNotBlank() }?.let { displayAmount(it) },
    )
    if (amounts.isEmpty()) return null
    return stringResource(R.string.fuel_liters, amounts.joinToString(" + "))
}

@Composable
private fun eventUnitPriceText(event: CarEventEntity): String? {
    if (!event.secondaryFuelAmount.isNullOrBlank() || !event.secondaryFuelCost.isNullOrBlank()) return null
    val unit = RecordCostService.unitPrice(
        event.fuelCost?.toBigDecimalOrNull() ?: event.totalCost?.toBigDecimalOrNull(),
        event.fuelAmount?.toBigDecimalOrNull(),
    ) ?: return null
    return stringResource(R.string.price_per_liter, CurrencyFormatter.format(unit))
}

private val eventDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val eventTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatEventDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(eventDateFormatter)

private fun formatEventTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(eventTimeFormatter)

private fun formatGroupedInt(value: Int): String = NumberFormat.getIntegerInstance().format(value)

private fun displayAmount(raw: String): String =
    raw.toBigDecimalOrNull()?.let { formatDecimal(it) } ?: raw

private fun formatDecimal(value: BigDecimal): String =
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)

@Composable
fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = message,
            modifier = Modifier.padding(DesignTokens.Spacing.lg),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
