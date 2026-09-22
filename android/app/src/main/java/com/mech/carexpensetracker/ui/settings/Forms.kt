package com.mech.carexpensetracker.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.data.db.entity.CarEventEntity
import com.mech.carexpensetracker.domain.model.CarIcon
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.domain.model.FuelType
import com.mech.carexpensetracker.domain.service.CurrencyFormatter
import com.mech.carexpensetracker.domain.service.MileageValidationService
import com.mech.carexpensetracker.domain.service.RecordCostService
import com.mech.carexpensetracker.ui.MainViewModel
import com.mech.carexpensetracker.ui.components.AppIcons
import com.mech.carexpensetracker.ui.components.AppLazyColumn
import com.mech.carexpensetracker.ui.components.CarAvatar
import com.mech.carexpensetracker.ui.components.CarMetaRow
import com.mech.carexpensetracker.ui.components.InputUnitSuffix
import com.mech.carexpensetracker.ui.components.PrimaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.components.fuelTypeStringRes
import com.mech.carexpensetracker.ui.events.EventPhotoNotesSection
import com.mech.carexpensetracker.ui.events.rememberEventPhotoSession
import com.mech.carexpensetracker.ui.theme.DesignTokens
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarFormScreen(
    carId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onEditCar: ((String) -> Unit)? = null,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val existing = cars.find { it.externalId == carId }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var plate by remember(existing) { mutableStateOf(existing?.plateNumber ?: "") }
    var buyDateMillis by remember(existing) { mutableStateOf(existing?.buyDateMillis) }
    var iconName by remember(existing) { mutableStateOf(CarIcon.resolve(existing?.iconName)) }
    var fuelType by remember(existing) { mutableStateOf(FuelType.fromRaw(existing?.primaryFuelTypeRaw).raw) }
    var alternativeFuelType by remember(existing) { mutableStateOf(existing?.alternativeFuelTypeRaw) }
    var showIconPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val nameBlank = name.isBlank()
    val showNameError = name.isNotEmpty() && nameBlank

    AppLazyColumn(modifier = modifier) {
        item {
            ListItem(
                leadingContent = { CarAvatar(iconName = iconName) },
                headlineContent = { Text(stringResource(R.string.car_icon)) },
                modifier = Modifier.clickable { showIconPicker = true },
            )
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.car_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = showNameError,
                supportingText = errorText(showNameError, R.string.required_field),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )
        }
        item {
            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it },
                label = { Text(stringResource(R.string.plate_number)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        }
        item {
            OptionalDateField(
                dateMillis = buyDateMillis,
                onDateMillisChange = { buyDateMillis = it },
                labelRes = R.string.buy_date,
            )
        }
        item { SectionHeader(title = stringResource(R.string.fuel_type)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                FuelType.entries.forEach { type ->
                    FilterChip(
                        selected = fuelType == type.raw,
                        onClick = {
                            fuelType = type.raw
                            if (alternativeFuelType == type.raw) alternativeFuelType = null
                        },
                        label = { Text(stringResource(fuelTypeStringRes(type))) },
                    )
                }
            }
        }
        item { SectionHeader(title = stringResource(R.string.alternative_fuel_type)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                FilterChip(
                    selected = alternativeFuelType == null,
                    onClick = { alternativeFuelType = null },
                    label = { Text(stringResource(R.string.no_alternative_fuel)) },
                )
                FuelType.entries.filterNot { it.raw == fuelType }.forEach { type ->
                    FilterChip(
                        selected = alternativeFuelType == type.raw,
                        onClick = { alternativeFuelType = type.raw },
                        label = { Text(stringResource(fuelTypeStringRes(type))) },
                    )
                }
            }
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.save),
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            viewModel.saveCar(
                                externalId = carId,
                                name = name.trim(),
                                plateNumber = plate.trim().ifBlank { null },
                                vehicleUnits = existing?.vehicleUnits ?: "km",
                                primaryFuelType = fuelType,
                                alternativeFuelType = alternativeFuelType,
                                iconName = iconName,
                                buyDateMillis = buyDateMillis,
                            )
                            onDone()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !nameBlank,
                loading = isSaving,
            )
        }
        if (carId != null) {
            item {
                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
        if (carId == null) {
            item { SectionHeader(title = stringResource(R.string.cars)) }
            items(cars) { car ->
                ListItem(
                    leadingContent = { CarAvatar(iconName = car.iconName) },
                    headlineContent = { Text(car.name) },
                    supportingContent = { CarMetaRow(car = car) },
                    modifier = Modifier.clickable {
                        scope.launch { viewModel.selectCar(car.externalId) }
                        onEditCar?.invoke(car.externalId)
                    },
                )
            }
        }
    }

    if (showIconPicker) {
        CarIconPickerDialog(
            selected = iconName,
            onSelect = { chosen ->
                iconName = chosen
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
        )
    }

    if (confirmDelete && carId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_car_title)) },
            text = { Text(stringResource(R.string.delete_car_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch {
                            viewModel.deleteCar(carId)
                            onDone()
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddFuelScreen(
    carExternalId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    eventId: String? = null,
    mainViewModel: MainViewModel = hiltViewModel(),
    eventsViewModel: com.mech.carexpensetracker.ui.events.EventsViewModel = hiltViewModel(),
) {
    var existing by remember(eventId) { mutableStateOf<CarEventEntity?>(null) }
    LaunchedEffect(eventId) {
        if (eventId != null) {
            existing = eventsViewModel.getEvent(eventId)
            if (existing == null) onDone()
        }
    }
    if (eventId != null && existing == null) return

    val defaultFuelName = stringResource(R.string.event_title_fuel)
    val existingIsAlt = existing.isAlternativeFill()
    var name by rememberSaveable(eventId) {
        mutableStateOf(
            existing?.name?.takeIf { it.isNotBlank() }
                ?: existing?.categoryName?.takeIf { it.isNotBlank() }
                ?: defaultFuelName,
        )
    }
    var mileage by rememberSaveable(eventId) { mutableStateOf(existing?.mileage?.toString().orEmpty()) }
    var useAltFuel by rememberSaveable(eventId) { mutableStateOf(existingIsAlt) }
    var fuelAmount by rememberSaveable(eventId) {
        mutableStateOf(
            if (existingIsAlt) existing?.secondaryFuelAmount.orEmpty() else existing?.fuelAmount.orEmpty(),
        )
    }
    var fuelCost by rememberSaveable(eventId) {
        mutableStateOf(
            if (existingIsAlt) existing?.secondaryFuelCost.orEmpty() else existing?.fuelCost.orEmpty(),
        )
    }
    var comment by rememberSaveable(eventId) { mutableStateOf(existing?.comment.orEmpty()) }
    var fullTank by rememberSaveable(eventId) {
        mutableStateOf(
            if (existingIsAlt) existing?.secondaryFuelFullTank ?: true else existing?.fuelFullTank ?: true,
        )
    }
    var dateMillis by rememberSaveable(eventId) { mutableLongStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val cars by mainViewModel.cars.collectAsStateWithLifecycle()
    val eventsState by eventsViewModel.uiState.collectAsStateWithLifecycle()
    val photoSession = rememberEventPhotoSession(eventsViewModel)
    val savedPhotosFlow = remember(eventId) { eventsViewModel.observePhotos(eventId) }
    val savedPhotos by savedPhotosFlow.collectAsStateWithLifecycle(emptyList())
    val car = cars.find { it.externalId == carExternalId }
    val hasAltFuel = !car?.alternativeFuelTypeRaw.isNullOrBlank()
    val mileageUnit = eventsState.vehicleUnits.raw
    val currency = CurrencyFormatter.currencySymbol()
    val literUnit = stringResource(R.string.unit_liter)
    val pricePerLiterUnit = stringResource(R.string.price_per_liter, currency)
    val mileageState = mileageInputState(mileage, eventsState.events, eventId, dateMillis)
    val fuelAmountValue = CurrencyFormatter.parse(fuelAmount)
    val fuelCostValue = CurrencyFormatter.parse(fuelCost)
    val fuelUnitPriceValue = RecordCostService.unitPrice(fuelCostValue, fuelAmountValue)
    val fuelAmountError = fuelAmount.isNotBlank() && fuelAmountValue?.signum() != 1
    val fuelCostError = fuelCost.isNotBlank() && fuelCostValue?.signum() != 1
    val isValid = !mileageState.isError &&
        fuelAmountValue?.signum() == 1 &&
        fuelCostValue?.signum() == 1

    AppLazyColumn(modifier = modifier) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.event_name)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            EventDateField(dateMillis = dateMillis, onDateMillisChange = { dateMillis = it })
        }
        item {
            MileageField(
                value = mileage,
                onValueChange = { mileage = it },
                state = mileageState,
                unit = mileageUnit,
            )
        }
        if (hasAltFuel) {
            item { SectionHeader(title = stringResource(R.string.refuel_fuel_choice)) }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                    FilterChip(
                        selected = !useAltFuel,
                        onClick = { useAltFuel = false },
                        label = {
                            Text(stringResource(fuelTypeStringRes(FuelType.fromRaw(car?.primaryFuelTypeRaw))))
                        },
                    )
                    FilterChip(
                        selected = useAltFuel,
                        onClick = { useAltFuel = true },
                        label = {
                            Text(stringResource(fuelTypeStringRes(FuelType.fromRaw(car?.alternativeFuelTypeRaw))))
                        },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = fuelAmount,
                onValueChange = { fuelAmount = it },
                label = { Text(stringResource(R.string.fuel_amount)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                isError = fuelAmountError,
                supportingText = errorText(fuelAmountError, R.string.positive_number_required),
                suffix = { InputUnitSuffix(literUnit) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = fuelUnitPriceValue?.toInputText().orEmpty(),
                onValueChange = {},
                enabled = false,
                label = { Text(stringResource(R.string.fuel_unit_price)) },
                suffix = { InputUnitSuffix(pricePerLiterUnit) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = fuelCost,
                onValueChange = { fuelCost = it },
                label = { Text(stringResource(R.string.fuel_cost)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                isError = fuelCostError,
                supportingText = errorText(fuelCostError, R.string.positive_number_required),
                suffix = { InputUnitSuffix(currency) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fullTank = !fullTank },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                Checkbox(checked = fullTank, onCheckedChange = { fullTank = it })
                Text(stringResource(R.string.full_tank))
            }
        }
        item {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(R.string.comment)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            EventPhotoNotesSection(
                savedPhotos = savedPhotos,
                session = photoSession,
                photoModel = { eventsViewModel.photoFile(it) },
            )
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.save),
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val savedId = eventsViewModel.addFuelEvent(
                                carExternalId = carExternalId,
                                primaryFuelType = car?.primaryFuelTypeRaw ?: "gasoline",
                                altFuelType = car?.alternativeFuelTypeRaw,
                                isAlternativeFuel = hasAltFuel && useAltFuel,
                                mileage = mileageState.parsed,
                                fuelAmount = fuelAmountValue,
                                fuelCost = fuelCostValue,
                                fullTank = fullTank,
                                comment = comment.ifBlank { null },
                                name = name.ifBlank { null },
                                dateMillis = dateMillis,
                                eventId = eventId,
                            )
                            if (savedId != null) {
                                photoSession.commit(savedId, savedPhotos)
                                onDone()
                            }
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = isValid,
                loading = isSaving,
            )
        }
        if (eventId != null) {
            item {
                EventDeleteButton(onConfirm = {
                    eventsViewModel.deleteEvent(eventId)
                    onDone()
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    carExternalId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    eventId: String? = null,
    eventsViewModel: com.mech.carexpensetracker.ui.events.EventsViewModel = hiltViewModel(),
) {
    var existing by remember(eventId) { mutableStateOf<CarEventEntity?>(null) }
    LaunchedEffect(eventId) {
        if (eventId != null) {
            existing = eventsViewModel.getEvent(eventId)
            if (existing == null) onDone()
        }
    }
    if (eventId != null && existing == null) return

    var mileage by rememberSaveable(eventId) { mutableStateOf(existing?.mileage?.toString().orEmpty()) }
    var name by rememberSaveable(eventId) {
        mutableStateOf(
            existing?.name?.takeIf { it.isNotBlank() } ?: existing?.categoryName.orEmpty(),
        )
    }
    var parts by rememberSaveable(eventId) { mutableStateOf(existing?.partsCost.orEmpty()) }
    var labour by rememberSaveable(eventId) { mutableStateOf(existing?.labourCost.orEmpty()) }
    var total by rememberSaveable(eventId) { mutableStateOf(existing?.totalCost.orEmpty()) }
    var comment by rememberSaveable(eventId) { mutableStateOf(existing?.comment.orEmpty()) }
    var eventType by rememberSaveable(eventId) {
        mutableStateOf(existing?.let { EventType.fromRaw(it.typeRaw) } ?: EventType.Repair)
    }
    var dateMillis by rememberSaveable(eventId) { mutableLongStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val eventsState by eventsViewModel.uiState.collectAsStateWithLifecycle()
    val photoSession = rememberEventPhotoSession(eventsViewModel)
    val savedPhotosFlow = remember(eventId) { eventsViewModel.observePhotos(eventId) }
    val savedPhotos by savedPhotosFlow.collectAsStateWithLifecycle(emptyList())
    val mileageState = mileageInputState(mileage, eventsState.events, eventId, dateMillis)
    val mileageUnit = eventsState.vehicleUnits.raw
    val currency = CurrencyFormatter.currencySymbol()
    val partsValue = CurrencyFormatter.parse(parts)
    val labourValue = CurrencyFormatter.parse(labour)
    val totalValue = CurrencyFormatter.parse(total)
    val partsError = parts.isNotBlank() && (partsValue == null || partsValue.signum() < 0)
    val labourError = labour.isNotBlank() && (labourValue == null || labourValue.signum() < 0)
    val totalError = total.isNotBlank() && (totalValue == null || totalValue.signum() < 0)
    val breakdownLocked = parts.isNotBlank() || labour.isNotBlank()
    val missingCost = RecordCostService.repairTotal(partsValue, labourValue, totalValue) == null
    val isValid = !mileageState.isError && !partsError && !labourError && !totalError && !missingCost

    AppLazyColumn(modifier = modifier) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.event_name)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            EventDateField(dateMillis = dateMillis, onDateMillisChange = { dateMillis = it })
        }
        item {
            MileageField(
                value = mileage,
                onValueChange = { mileage = it },
                state = mileageState,
                unit = mileageUnit,
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                FilterChip(
                    selected = eventType == EventType.Repair,
                    onClick = { eventType = EventType.Repair },
                    label = { Text(stringResource(R.string.event_type_service)) },
                )
                FilterChip(
                    selected = eventType == EventType.Papers,
                    onClick = { eventType = EventType.Papers },
                    label = { Text(stringResource(R.string.event_type_documents)) },
                )
                FilterChip(
                    selected = eventType == EventType.Care,
                    onClick = { eventType = EventType.Care },
                    label = { Text(stringResource(R.string.event_type_care)) },
                )
            }
        }
        item {
            OutlinedTextField(
                value = parts,
                onValueChange = { value ->
                    parts = value
                    RecordCostService.repairTotal(CurrencyFormatter.parse(value), CurrencyFormatter.parse(labour))
                        ?.let { total = it.toInputText() }
                },
                label = { Text(stringResource(R.string.parts_cost)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                isError = partsError,
                supportingText = errorText(partsError, R.string.invalid_non_negative_number),
                suffix = { InputUnitSuffix(currency) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = labour,
                onValueChange = { value ->
                    labour = value
                    RecordCostService.repairTotal(CurrencyFormatter.parse(parts), CurrencyFormatter.parse(value))
                        ?.let { total = it.toInputText() }
                },
                label = { Text(stringResource(R.string.labour_cost)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                isError = labourError,
                supportingText = errorText(labourError, R.string.invalid_non_negative_number),
                suffix = { InputUnitSuffix(currency) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = total,
                onValueChange = { if (!breakdownLocked) total = it },
                readOnly = breakdownLocked,
                label = { Text(stringResource(R.string.total_cost)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
                isError = totalError,
                supportingText = errorText(totalError, R.string.invalid_non_negative_number),
                suffix = { InputUnitSuffix(currency) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(R.string.comment)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            EventPhotoNotesSection(
                savedPhotos = savedPhotos,
                session = photoSession,
                photoModel = { eventsViewModel.photoFile(it) },
            )
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.save),
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val savedId = eventsViewModel.addExpenseEvent(
                                carExternalId = carExternalId,
                                type = eventType,
                                mileage = mileageState.parsed,
                                name = name.ifBlank { null },
                                partsCost = partsValue,
                                labourCost = labourValue,
                                totalCost = totalValue,
                                comment = comment.ifBlank { null },
                                dateMillis = dateMillis,
                                eventId = eventId,
                            )
                            if (savedId != null) {
                                photoSession.commit(savedId, savedPhotos)
                                onDone()
                            }
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = isValid,
                loading = isSaving,
            )
        }
        if (eventId != null) {
            item {
                EventDeleteButton(onConfirm = {
                    eventsViewModel.deleteEvent(eventId)
                    onDone()
                })
            }
        }
    }
}

@Composable
private fun EventDeleteButton(onConfirm: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Button(
        onClick = { confirm = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(stringResource(R.string.delete))
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.delete_event_title)) },
            text = { Text(stringResource(R.string.delete_event_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        onConfirm()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDateField(
    dateMillis: Long,
    onDateMillisChange: (Long) -> Unit,
) {
    FormDateField(
        dateMillis = dateMillis,
        onDateMillisChange = { millis -> millis?.let(onDateMillisChange) },
        labelRes = R.string.date,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionalDateField(
    dateMillis: Long?,
    onDateMillisChange: (Long?) -> Unit,
    @StringRes labelRes: Int,
) {
    FormDateField(
        dateMillis = dateMillis,
        onDateMillisChange = onDateMillisChange,
        labelRes = labelRes,
        optional = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormDateField(
    dateMillis: Long?,
    onDateMillisChange: (Long?) -> Unit,
    @StringRes labelRes: Int,
    optional: Boolean = false,
) {
    var showPicker by remember { mutableStateOf(false) }
    val label = remember(dateMillis) { dateMillis?.let(::formatFormDate).orEmpty() }
    val dateDescription = stringResource(labelRes)
    val canClear = optional && dateMillis != null
    Box {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(dateDescription) },
            trailingIcon = {
                if (canClear) {
                    IconButton(onClick = { onDateMillisChange(null) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_date),
                        )
                    }
                } else {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(if (canClear) Modifier.padding(end = DesignTokens.Spacing.xl + DesignTokens.Spacing.md) else Modifier)
                .clickable(onClick = { showPicker = true })
                .semantics { contentDescription = dateDescription },
        )
    }
    if (showPicker) {
        val current = dateMillis ?: System.currentTimeMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = utcMidnightMillis(current),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { picked ->
                            onDateMillisChange(applyPickedUtcDate(current, picked))
                        }
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

internal fun utcMidnightMillis(localMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(localMillis).atZone(zone).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

internal fun applyPickedUtcDate(
    currentMillis: Long,
    pickedUtcMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    val time = Instant.ofEpochMilli(currentMillis).atZone(zone).toLocalTime()
    val date = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
}

private fun CarEventEntity?.isAlternativeFill(): Boolean =
    this != null && fuelAmount.isNullOrBlank() && !secondaryFuelAmount.isNullOrBlank()

private data class MileageInputState(
    val parsed: Int?,
    val isError: Boolean,
    val formatError: Boolean,
    val minAllowed: Int?,
)

private fun mileageInputState(
    raw: String,
    previousEvents: List<CarEventEntity>,
    excludeExternalId: String?,
    dateMillis: Long,
): MileageInputState {
    val parsed = parseMileage(raw)
    val validation = MileageValidationService.validate(
        parsed,
        previousEvents,
        excludeExternalId,
        dateMillis,
    )
    val formatError = raw.isNotBlank() && parsed?.let { it >= 0 } != true
    val tooLow = parsed != null && !validation.isValid && validation.minAllowed != null
    return MileageInputState(
        parsed = parsed,
        isError = formatError || tooLow,
        formatError = formatError,
        minAllowed = validation.minAllowed,
    )
}

@Composable
private fun MileageField(
    value: String,
    onValueChange: (String) -> Unit,
    state: MileageInputState,
    unit: String,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.mileage)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
        ),
        isError = state.isError,
        supportingText = when {
            state.formatError -> errorText(true, R.string.invalid_non_negative_number)
            state.isError && state.minAllowed != null -> {
                { Text(stringResource(R.string.mileage_below_recorded, state.minAllowed)) }
            }
            else -> null
        },
        suffix = { InputUnitSuffix(unit) },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun parseMileage(raw: String): Int? =
    raw.replace(Regex("[\\s\u00A0]"), "").toIntOrNull()

private fun BigDecimal.toInputText(): String = stripTrailingZeros().toPlainString()

private fun formatFormDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarIconPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.car_icon)) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            ) {
                CarIcon.names.forEach { name ->
                    val isSelected = name == selected
                    Surface(
                        onClick = { onSelect(name) },
                        shape = CircleShape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Icon(
                            imageVector = AppIcons.car(name),
                            contentDescription = name,
                            modifier = Modifier.padding(DesignTokens.Spacing.sm),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun errorText(
    visible: Boolean,
    @StringRes messageRes: Int,
): (@Composable () -> Unit)? = if (visible) {
    { Text(stringResource(messageRes)) }
} else {
    null
}
