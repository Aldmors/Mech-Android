package com.mech.carexpensetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.MainViewModel
import com.mech.carexpensetracker.ui.components.PrimaryButton
import com.mech.carexpensetracker.ui.components.SecondaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader
import com.mech.carexpensetracker.ui.theme.DesignTokens
import kotlinx.coroutines.launch

@Composable
fun CarFormScreen(
    carId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val existing = cars.find { it.externalId == carId }
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var plate by remember(existing) { mutableStateOf(existing?.plateNumber ?: "") }
    var units by remember(existing) { mutableStateOf(existing?.vehicleUnits ?: "km") }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md)) {
        item {
            SectionHeader(
                title = if (carId == null) stringResource(R.string.add_car) else stringResource(R.string.edit),
            )
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.car_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it },
                label = { Text(stringResource(R.string.plate_number)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = units,
                onValueChange = { units = it },
                label = { Text(stringResource(R.string.units)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.save),
                onClick = {
                    viewModel.saveCar(
                        externalId = carId,
                        name = name,
                        plateNumber = plate.ifBlank { null },
                        vehicleUnits = units,
                        primaryFuelType = existing?.primaryFuelTypeRaw ?: "gasoline",
                        alternativeFuelType = existing?.alternativeFuelTypeRaw,
                    )
                    onDone()
                },
            )
        }
        if (carId != null) {
            item {
                SecondaryButton(
                    text = stringResource(R.string.delete),
                    onClick = {
                        viewModel.deleteCar(carId)
                        onDone()
                    },
                )
            }
        }
        item { SectionHeader(title = stringResource(R.string.cars)) }
        items(cars) { car ->
            Text(
                text = car.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { viewModel.selectCar(car.externalId) } }
                    .padding(DesignTokens.Spacing.sm),
            )
        }
    }
}

@Composable
fun AddFuelScreen(
    carExternalId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
    eventsViewModel: com.mech.carexpensetracker.ui.events.EventsViewModel = hiltViewModel(),
) {
    var mileage by remember { mutableStateOf("") }
    var fuelAmount by remember { mutableStateOf("") }
    var fuelCost by remember { mutableStateOf("") }
    var secondaryAmount by remember { mutableStateOf("") }
    var secondaryCost by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val car by mainViewModel.selectedCar.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md)) {
        item { SectionHeader(title = stringResource(R.string.add_fuel)) }
        item {
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text(stringResource(R.string.mileage)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = fuelAmount,
                onValueChange = { fuelAmount = it },
                label = { Text(stringResource(R.string.fuel_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = fuelCost,
                onValueChange = { fuelCost = it },
                label = { Text(stringResource(R.string.fuel_cost)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = secondaryAmount,
                onValueChange = { secondaryAmount = it },
                label = { Text("Secondary fuel amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = secondaryCost,
                onValueChange = { secondaryCost = it },
                label = { Text("Secondary fuel cost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(R.string.comment)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.save),
                onClick = {
                    scope.launch {
                        eventsViewModel.addFuelEvent(
                            carExternalId = carExternalId,
                            primaryFuelType = car?.primaryFuelTypeRaw ?: "gasoline",
                            altFuelType = car?.alternativeFuelTypeRaw,
                            mileage = mileage.toIntOrNull(),
                            fuelAmount = fuelAmount.toBigDecimalOrNull(),
                            fuelCost = fuelCost.toBigDecimalOrNull(),
                            secondaryFuelAmount = secondaryAmount.toBigDecimalOrNull(),
                            secondaryFuelCost = secondaryCost.toBigDecimalOrNull(),
                            fullTank = true,
                            secondaryFullTank = false,
                            comment = comment.ifBlank { null },
                        )
                        onDone()
                    }
                },
            )
        }
    }
}

@Composable
fun AddExpenseScreen(
    carExternalId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    eventsViewModel: com.mech.carexpensetracker.ui.events.EventsViewModel = hiltViewModel(),
) {
    var mileage by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("") }
    var labour by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.md)) {
        item { SectionHeader(title = stringResource(R.string.add_expense)) }
        item {
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it },
                label = { Text(stringResource(R.string.mileage)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.category)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = parts,
                onValueChange = { parts = it },
                label = { Text("Parts cost") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = labour,
                onValueChange = { labour = it },
                label = { Text("Labour cost") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text(stringResource(R.string.comment)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.save),
                onClick = {
                    scope.launch {
                        eventsViewModel.addExpenseEvent(
                            carExternalId = carExternalId,
                            type = com.mech.carexpensetracker.domain.model.EventType.Repair,
                            mileage = mileage.toIntOrNull(),
                            categoryName = category.ifBlank { null },
                            partsCost = parts.toBigDecimalOrNull(),
                            labourCost = labour.toBigDecimalOrNull(),
                            comment = comment.ifBlank { null },
                        )
                        onDone()
                    }
                },
            )
        }
    }
}
