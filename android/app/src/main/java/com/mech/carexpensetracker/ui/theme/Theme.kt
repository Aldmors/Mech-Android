package com.mech.carexpensetracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D57),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7E7CB),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4E6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D7),
    onSecondaryContainer = Color(0xFF0C1F15),
    tertiary = Color(0xFF3B5BA5),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E2FF),
    onTertiaryContainer = Color(0xFF001A41),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1F2E),
    surface = Color(0xFFF5F7FA),
    onSurface = Color(0xFF1A1F2E),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404942),
    outline = Color(0xFF707972),
    outlineVariant = Color(0xFFC0C9C0),
    inverseSurface = Color(0xFF2C322E),
    inverseOnSurface = Color(0xFFEDF2EB),
    inversePrimary = Color(0xFF9BD4B3),
    surfaceTint = Color(0xFF2E7D57),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF5F7FA),
    surfaceDim = Color(0xFFD5DADF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF3F7),
    surfaceContainer = Color(0xFFE9EEF3),
    surfaceContainerHigh = Color(0xFFE3E9F0),
    surfaceContainerHighest = Color(0xFFDDE4EC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD4B3),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF145C3D),
    onPrimaryContainer = Color(0xFFB7E7CB),
    secondary = Color(0xFFB5CCBB),
    onSecondary = Color(0xFF213529),
    secondaryContainer = Color(0xFF374B3F),
    onSecondaryContainer = Color(0xFFD1E8D7),
    tertiary = Color(0xFFAFC6FF),
    onTertiary = Color(0xFF002F68),
    tertiaryContainer = Color(0xFF20428C),
    onTertiaryContainer = Color(0xFFD6E2FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF404942),
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8A938B),
    outlineVariant = Color(0xFF404942),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2C322E),
    inversePrimary = Color(0xFF2E7D57),
    surfaceTint = Color(0xFF9BD4B3),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF373A40),
    surfaceDim = Color(0xFF111318),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C21),
    surfaceContainer = Color(0xFF1D2025),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

@Composable
fun CarExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
