package com.mech.carexpensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DesignTokens.Palette.accent,
    onPrimary = Color.White,
    secondary = DesignTokens.Palette.accentLight,
    background = DesignTokens.Palette.background,
    surface = DesignTokens.Palette.surface,
    onBackground = DesignTokens.Palette.textPrimary,
    onSurface = DesignTokens.Palette.textPrimary,
    error = DesignTokens.Palette.error,
)

private val DarkColors = darkColorScheme(
    primary = DesignTokens.Palette.accentLight,
)

@Composable
fun CarExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
