package com.mech.carexpensetracker.ui.reminders

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.db.entity.CarReminderEntity
import com.mech.carexpensetracker.domain.service.CategoryCatalog
import com.mech.carexpensetracker.reminders.CalendarSyncResult
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.PrimaryButton
import com.mech.carexpensetracker.ui.components.SecondaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.components.hexToHue
import com.mech.carexpensetracker.ui.components.hsvHex
import com.mech.carexpensetracker.ui.components.parseCategoryColor
import com.mech.carexpensetracker.ui.settings.FormDateField
import com.mech.carexpensetracker.ui.theme.DesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RemindersScreen(
    onMessage: (String) -> Unit,
    onReminderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDone by remember { mutableStateOf<ReminderItemUi?>(null) }
    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) viewModel.syncCalendar()
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { result ->
            onMessage(
                when (result) {
                    is CalendarSyncResult.Ok -> if (result.added > 0) {
                        context.getString(R.string.reminder_sync_success, result.added)
                    } else {
                        context.getString(R.string.reminder_sync_none)
                    }
                    CalendarSyncResult.NoCalendar -> context.getString(R.string.reminder_sync_no_calendar)
                    CalendarSyncResult.Failed -> context.getString(R.string.reminder_sync_failed)
                },
            )
        }
    }

    AppLazyColumn(modifier = modifier, clearFab = true) {
        item {
            SecondaryButton(
                text = stringResource(R.string.reminder_sync_calendar),
                onClick = {
                    val read = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CALENDAR,
                    ) == PackageManager.PERMISSION_GRANTED
                    val write = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_CALENDAR,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (read && write) {
                        viewModel.syncCalendar()
                    } else {
                        calendarLauncher.launch(
                            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                        )
                    }
                },
                enabled = state.reminders.any { !it.isCompleted },
            )
        }
        if (state.reminders.isEmpty()) {
            item { EmptyStateCard(message = stringResource(R.string.no_reminders)) }
        } else {
            items(state.reminders, key = { it.externalId }) { reminder ->
                ReminderRow(
                    reminder = reminder,
                    unit = state.vehicleUnits.raw,
                    onClick = { onReminderClick(reminder.externalId) },
                    onDone = { pendingDone = reminder },
                    onDelete = { viewModel.deleteReminder(reminder.externalId) },
                )
            }
        }
    }

    pendingDone?.let { reminder ->
        AlertDialog(
            onDismissRequest = {
                viewModel.complete(reminder.externalId, repeat = false)
                pendingDone = null
            },
            title = { Text(stringResource(R.string.reminder_repeat_title)) },
            text = { Text(stringResource(R.string.reminder_repeat_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.complete(reminder.externalId, repeat = true)
                        pendingDone = null
                    },
                ) {
                    Text(stringResource(R.string.reminder_repeat_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.complete(reminder.externalId, repeat = false)
                        pendingDone = null
                    },
                ) {
                    Text(stringResource(R.string.reminder_repeat_no))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddReminderScreen(
    onDone: () -> Unit,
    reminderId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var existing by remember(reminderId) { mutableStateOf<CarReminderEntity?>(null) }
    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            existing = viewModel.getReminder(reminderId)
        }
    }
    if (reminderId != null && existing == null) return

    var title by rememberSaveable(reminderId) { mutableStateOf(existing?.title.orEmpty()) }
    var colorHex by rememberSaveable(reminderId) {
        mutableStateOf(existing?.colorHex ?: CategoryCatalog.DEFAULT_COLOR)
    }
    var byDate by rememberSaveable(reminderId) { mutableStateOf(existing?.dueDateMillis != null || existing == null) }
    var byKm by rememberSaveable(reminderId) {
        mutableStateOf(existing?.dueMileage != null || existing?.intervalKm != null)
    }
    var dueDateMillis by rememberSaveable(reminderId) {
        mutableLongStateOf(existing?.dueDateMillis ?: System.currentTimeMillis())
    }
    var intervalKm by rememberSaveable(reminderId) {
        mutableStateOf(existing?.intervalKm?.toString().orEmpty())
    }
    val focusManager = LocalFocusManager.current
    val hue = remember(colorHex) { hexToHue(colorHex) }
    val colorLabel = stringResource(R.string.reminder_color)
    val titleBlank = title.isBlank()
    val intervalValue = intervalKm.toIntOrNull()
    val intervalInvalid = intervalValue == null || intervalValue < 1
    val intervalError = byKm && intervalKm.isNotBlank() && intervalInvalid
    val canSave = !titleBlank &&
        state.carExternalId != null &&
        (byDate || byKm) &&
        (!byKm || !intervalInvalid)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DesignTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.reminder_title)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )
        SectionHeader(title = stringResource(R.string.reminder_color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        ) {
            CategoryCatalog.COLORS.forEach { hex ->
                ReminderColorSwatch(
                    hex = hex,
                    selected = hex.equals(colorHex, ignoreCase = true),
                    onClick = { colorHex = hex },
                )
            }
        }
        Slider(
            value = hue,
            onValueChange = { colorHex = hsvHex(it) },
            valueRange = 0f..360f,
            modifier = Modifier.semantics { contentDescription = colorLabel },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
            FilterChip(
                selected = byDate,
                onClick = { byDate = !byDate },
                label = { Text(stringResource(R.string.reminder_condition_date)) },
            )
            FilterChip(
                selected = byKm,
                onClick = { byKm = !byKm },
                label = { Text(stringResource(R.string.reminder_condition_km)) },
            )
        }
        if (byDate) {
            FormDateField(
                dateMillis = dueDateMillis,
                onDateMillisChange = { millis -> millis?.let { dueDateMillis = it } },
                labelRes = R.string.reminder_due_date_label,
            )
        }
        if (byKm) {
            OutlinedTextField(
                value = intervalKm,
                onValueChange = { intervalKm = it },
                label = { Text(stringResource(R.string.reminder_interval_km)) },
                isError = intervalError,
                supportingText = {
                    when {
                        intervalError -> Text(stringResource(R.string.positive_number_required))
                        state.currentMileage != null -> Text(
                            stringResource(
                                R.string.reminder_interval_km_hint,
                                state.currentMileage ?: 0,
                                state.vehicleUnits.raw,
                            ),
                        )
                        else -> Text(stringResource(R.string.reminder_interval_km_no_mileage))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PrimaryButton(
            text = stringResource(if (reminderId == null) R.string.add_reminder else R.string.save),
            onClick = {
                viewModel.saveReminder(
                    title = title,
                    colorHex = colorHex,
                    dueDateMillis = dueDateMillis.takeIf { byDate },
                    intervalKm = intervalValue.takeIf { byKm },
                    reminderId = reminderId,
                )
                onDone()
            },
            enabled = canSave,
        )
    }
}

@Composable
private fun ReminderRow(
    reminder: ReminderItemUi,
    unit: String,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = remember(reminder.colorHex) { parseCategoryColor(reminder.colorHex) }
    val supportingColor = if (reminder.isDue && !reminder.isCompleted) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    ListItem(
        headlineContent = { Text(reminder.title) },
        supportingContent = {
            Column {
                if (reminder.isDue && !reminder.isCompleted) {
                    Text(
                        text = stringResource(R.string.reminder_overdue),
                        color = supportingColor,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                reminder.daysLeft?.let { days ->
                    val text = when {
                        reminder.isCompleted -> null
                        days < 0 -> null
                        days == 0L -> stringResource(R.string.reminder_due_today)
                        else -> pluralStringResource(R.plurals.reminder_days_left, days.toInt(), days.toInt())
                    }
                    if (text != null) {
                        Text(text = text, color = supportingColor, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                reminder.dueDateMillis?.let { millis ->
                    Text(
                        text = stringResource(R.string.reminder_due_date, formatReminderDate(millis)),
                        color = supportingColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
                reminder.dueMileage?.let { mileage ->
                    Text(
                        text = stringResource(R.string.reminder_due_mileage, mileage, unit),
                        color = supportingColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
        trailingContent = {
            Row {
                if (!reminder.isCompleted) {
                    TextButton(onClick = onDone) {
                        Text(stringResource(R.string.reminder_done))
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.reminder_delete),
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ReminderColorSwatch(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = remember(hex) { parseCategoryColor(hex) }
    Surface(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = hex },
        shape = CircleShape,
        color = color,
        content = {},
    )
}

private val reminderDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private fun formatReminderDate(millis: Long): String =
    reminderDateFormatter.format(Instant.ofEpochMilli(millis))
