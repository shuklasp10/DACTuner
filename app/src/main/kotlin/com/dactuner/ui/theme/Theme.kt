package com.dactuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * DACTuner dark color scheme.
 *
 * Uses a sophisticated dark palette with indigo primary and teal accent.
 * Consistent with the utility-app aesthetic: clean, premium, non-intrusive.
 */
private val DacTunerColorScheme = darkColorScheme(
    primary = DacPrimary,
    onPrimary = DacOnPrimary,
    secondary = DacSecondary,
    background = DacBackground,
    surface = DacSurface,
    surfaceVariant = DacSurfaceVariant,
    onSurface = DacOnSurface,
    onSurfaceVariant = DacOnSurfaceVariant
)

/**
 * DACTuner application theme.
 *
 * Wraps content in a Material 3 dark theme with custom colors and typography.
 * All composables in the app should be rendered inside this theme.
 */
@Composable
fun DacTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DacTunerColorScheme,
        typography = DacTunerTypography,
        content = content
    )
}
