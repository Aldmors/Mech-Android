package com.mech.carexpensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    clearFab: Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val bottom = contentPadding.calculateBottomPadding() +
        if (clearFab) DesignTokens.Spacing.xl + DesignTokens.Spacing.lg else 0.dp
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = contentPadding.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = bottom,
        ),
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
    val colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        OutlinedCard(onClick = onClick, modifier = cardModifier, colors = colors, content = content)
    } else {
        OutlinedCard(modifier = cardModifier, colors = colors, content = content)
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(if (compact) DesignTokens.Spacing.sm else DesignTokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = if (compact) {
                        MaterialTheme.typography.labelSmall
                    } else {
                        MaterialTheme.typography.labelMedium
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = DesignTokens.Spacing.xs)
                            .size(DesignTokens.Spacing.lg),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = value,
                style = if (compact) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !loading,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            ButtonLabel(text = text, icon = icon)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth(), enabled = enabled) {
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
fun InputUnitSuffix(unit: String) {
    Row(
        modifier = Modifier.padding(start = DesignTokens.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = DesignTokens.Spacing.sm)
                .width(1.dp)
                .height(DesignTokens.Spacing.lg)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = MaterialTheme.shapes.extraSmall,
                )
                .padding(
                    horizontal = DesignTokens.Spacing.sm,
                    vertical = DesignTokens.Spacing.xs,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
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

@Composable
fun CarSelector(
    cars: List<CarEntity>,
    selected: CarEntity?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = selected ?: cars.firstOrNull() ?: return
    val canExpand = cars.size > 1
    val description = stringResource(R.string.select_car) + ": " + current.name
    Box(modifier = modifier.fillMaxWidth()) {
        CarSelectorPill(
            car = current,
            onClick = if (canExpand) {
                { expanded = true }
            } else {
                null
            },
            modifier = Modifier.semantics { contentDescription = description },
        )
        DropdownMenu(
            expanded = expanded && canExpand,
            onDismissRequest = { expanded = false },
        ) {
            cars.filter { it.externalId != current.externalId }.forEach { car ->
                DropdownMenuItem(
                    text = { CarIdentityTexts(car = car) },
                    leadingIcon = { CarAvatar(iconName = car.iconName) },
                    onClick = {
                        onSelect(car.externalId)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun CarAvatar(
    iconName: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.car(iconName),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun CarIdentityTexts(
    car: CarEntity,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        CarMetaRow(car = car)
        Text(
            text = car.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CarSelectorPill(
    car: CarEntity,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = DesignTokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        ) {
            CarAvatar(iconName = car.iconName)
            CarIdentityTexts(
                car = car,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = Color.Transparent,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = Color.Transparent,
            content = content,
        )
    }
}

@Composable
fun CarMetaRow(
    car: CarEntity,
    modifier: Modifier = Modifier,
) {
    val plate = car.plateNumber?.takeIf { it.isNotBlank() }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (plate != null) {
            Text(
                text = plate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FuelTypeLabel(typeRaw = car.primaryFuelTypeRaw, compact = true)
        car.alternativeFuelTypeRaw?.takeIf { it.isNotBlank() }?.let {
            FuelTypeLabel(typeRaw = it, compact = true)
        }
    }
}

@Composable
fun FuelTypeLabel(typeRaw: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(fuelTypeStringRes(FuelType.fromRaw(typeRaw))),
            modifier = Modifier.padding(
                horizontal = DesignTokens.Spacing.sm,
                vertical = if (compact) 0.dp else DesignTokens.Spacing.xs,
            ),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

fun fuelTypeStringRes(type: FuelType): Int = when (type) {
    FuelType.Gasoline -> R.string.fuel_gasoline
    FuelType.Diesel -> R.string.fuel_diesel
    FuelType.Lpg -> R.string.fuel_lpg
    FuelType.Electric -> R.string.fuel_electric
}

fun eventTypeStringRes(type: EventType): Int = when (type) {
    EventType.Fuel -> R.string.event_type_fuel
    EventType.Repair -> R.string.event_type_service
    EventType.Papers -> R.string.event_type_documents
    EventType.Care -> R.string.event_type_care
}

private data class EventTypeColors(val container: Color, val content: Color)

@Composable
private fun eventTypeColors(type: EventType): EventTypeColors {
    val color = when (type) {
        EventType.Fuel -> DesignTokens.Palette.fuel
        EventType.Repair -> DesignTokens.Palette.repair
        EventType.Papers -> DesignTokens.Palette.papers
        EventType.Care -> DesignTokens.Palette.care
    }
    return EventTypeColors(container = color.copy(alpha = 0.16f), content = color)
}

@Composable
fun EventTypeBadge(type: EventType, modifier: Modifier = Modifier) {
    val colors = eventTypeColors(type)
    Surface(
        modifier = modifier,
        color = colors.container,
        contentColor = colors.content,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(eventTypeStringRes(type)),
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.sm, vertical = DesignTokens.Spacing.xs),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun EventRecordRow(
    event: CarEventEntity,
    modifier: Modifier = Modifier,
    consumption: BigDecimal? = null,
    units: VehicleUnits = VehicleUnits.Km,
    hasPhotos: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val type = EventType.fromRaw(event.typeRaw)
    val isFuel = type == EventType.Fuel
    val fullTank = event.fuelFullTank || event.secondaryFuelFullTank
    val dateText = remember(event.dateMillis) { formatEventDate(event.dateMillis) }
    val timeText = remember(event.dateMillis) { formatEventTime(event.dateMillis) }
    val title = event.name?.takeIf { it.isNotBlank() }
        ?: if (isFuel) {
            stringResource(R.string.event_title_fuel)
        } else {
            event.categoryName?.takeIf { it.isNotBlank() }
                ?: event.comment?.takeIf { it.isNotBlank() }
                ?: dateText
        }
    AppCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        ) {
            EventTypeIcon(type = type, fullTank = isFuel && fullTank)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasPhotos) {
                        Icon(
                            imageVector = Icons.Outlined.Photo,
                            contentDescription = stringResource(R.string.event_photos),
                            modifier = Modifier.size(DesignTokens.Spacing.md),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isFuel) {
                    FuelSubtitle(
                        event = event,
                        timeText = timeText,
                        dateText = dateText,
                    )
                    event.mileage?.let { mileage ->
                        Text(
                            text = stringResource(R.string.mileage_with_unit, formatGroupedInt(mileage), units.raw),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    event.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                        Text(
                            text = comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
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
                    event.mileage?.let { mileage ->
                        Text(
                            text = stringResource(R.string.mileage_with_unit, formatGroupedInt(mileage), units.raw),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    event.partsCost?.let { cost ->
                        EventCostDetail(label = stringResource(R.string.parts_cost), storedCost = cost)
                    }
                    event.labourCost?.let { cost ->
                        EventCostDetail(label = stringResource(R.string.labour_cost), storedCost = cost)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatOrDash(CurrencyFormatter.parseStored(event.totalCost)),
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
private fun EventCostDetail(label: String, storedCost: String) {
    CurrencyFormatter.parseStored(storedCost)?.let { cost ->
        Text(
            text = stringResource(R.string.cost_detail, label, CurrencyFormatter.format(cost)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EventTypeIcon(type: EventType, fullTank: Boolean, modifier: Modifier = Modifier) {
    val colors = eventTypeColors(type)
    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = AppIcons.eventType(type, fullTank = fullTank),
                contentDescription = stringResource(eventTypeStringRes(type)),
                modifier = Modifier.size(24.dp),
                tint = colors.content,
            )
        }
    }
}

@Composable
private fun FuelSubtitle(
    event: CarEventEntity,
    timeText: String,
    dateText: String,
) {
    val types = listOfNotNull(
        event.fuelTypeRaw?.takeIf { it.isNotBlank() && !event.fuelAmount.isNullOrBlank() },
        event.secondaryFuelTypeRaw?.takeIf { it.isNotBlank() && !event.secondaryFuelAmount.isNullOrBlank() },
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        types.forEach { typeRaw -> FuelTypeLabel(typeRaw = typeRaw) }
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
        CurrencyFormatter.parseStored(event.fuelAmount)?.let { formatDecimal(it) },
        CurrencyFormatter.parseStored(event.secondaryFuelAmount)?.let { formatDecimal(it) },
    )
    if (amounts.isEmpty()) return null
    return stringResource(R.string.fuel_liters, amounts.joinToString(" + "))
}

@Composable
private fun eventUnitPriceText(event: CarEventEntity): String? {
    val primaryAmount = CurrencyFormatter.parseStored(event.fuelAmount)
    val secondaryAmount = CurrencyFormatter.parseStored(event.secondaryFuelAmount)
    val unit = when {
        primaryAmount != null && secondaryAmount != null -> null
        primaryAmount != null -> RecordCostService.unitPrice(
            CurrencyFormatter.parseStored(event.fuelCost) ?: CurrencyFormatter.parseStored(event.totalCost),
            primaryAmount,
        )
        secondaryAmount != null -> RecordCostService.unitPrice(
            CurrencyFormatter.parseStored(event.secondaryFuelCost) ?: CurrencyFormatter.parseStored(event.totalCost),
            secondaryAmount,
        )
        else -> null
    } ?: return null
    return stringResource(R.string.price_per_liter, CurrencyFormatter.format(unit))
}

private val eventDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val eventTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatEventDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(eventDateFormatter)

private fun formatEventTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime().format(eventTimeFormatter)

private fun formatGroupedInt(value: Int): String = NumberFormat.getIntegerInstance().format(value)

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
