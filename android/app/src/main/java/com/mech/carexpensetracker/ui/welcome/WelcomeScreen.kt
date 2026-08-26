package com.mech.carexpensetracker.ui.welcome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.components.AppScreen
import com.mech.carexpensetracker.ui.components.EmptyStateCard
import com.mech.carexpensetracker.ui.components.PrimaryButton
import com.mech.carexpensetracker.ui.components.SecondaryButton
import com.mech.carexpensetracker.ui.components.SectionHeader

@Composable
fun WelcomeScreen(
    onAddCar: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScreen(modifier = modifier.fillMaxSize()) {
        SectionHeader(title = stringResource(R.string.welcome_title))
        EmptyStateCard(message = stringResource(R.string.welcome_subtitle))
        PrimaryButton(text = stringResource(R.string.add_car), onClick = onAddCar)
        SecondaryButton(text = stringResource(R.string.import_json), onClick = onImport)
    }
}
