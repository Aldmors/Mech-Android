package com.mech.carexpensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mech.carexpensetracker.data.db.entity.CarEntity
import com.mech.carexpensetracker.domain.model.EventType
import com.mech.carexpensetracker.ui.theme.DesignTokens

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
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DesignTokens.Palette.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content,
    )
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.md)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = DesignTokens.Palette.textSecondary)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Palette.accent),
    ) {
        Text(text)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun CarHeader(car: CarEntity?, modifier: Modifier = Modifier) {
    if (car == null) return
    Column(modifier = modifier) {
        Text(text = car.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        car.plateNumber?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = DesignTokens.Palette.textSecondary)
        }
    }
}

@Composable
fun EventTypeBadge(type: EventType, modifier: Modifier = Modifier) {
    val color = when (type) {
        EventType.Fuel -> DesignTokens.Palette.fuel
        EventType.Repair -> DesignTokens.Palette.repair
        EventType.Papers -> DesignTokens.Palette.papers
    }
    Text(
        text = type.raw,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun EmptyStateCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = message,
            modifier = Modifier.padding(DesignTokens.Spacing.lg),
            color = DesignTokens.Palette.textSecondary,
        )
    }
}
