package com.mech.carexpensetracker.ui.welcome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mech.carexpensetracker.R
import com.mech.carexpensetracker.ui.components.AppScreen
import com.mech.carexpensetracker.ui.components.PrimaryButton
import com.mech.carexpensetracker.ui.components.SecondaryButton

@Composable
fun WelcomeScreen(
    onAddCar: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScreen(modifier = modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = stringResource(R.string.add_car),
            onClick = onAddCar,
            icon = Icons.Default.DirectionsCar,
        )
        SecondaryButton(
            text = stringResource(R.string.import_json),
            onClick = onImport,
            icon = Icons.Default.UploadFile,
        )
    }
}
