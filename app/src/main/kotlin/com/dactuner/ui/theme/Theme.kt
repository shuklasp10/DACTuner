package com.dactuner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Cupertino dark color scheme for DACTuner.
 *
 * Uses OLED pure black background (#000000), inset card surfaces (#1C1C1E),
 * and vibrant iOS system accent colors.
 */
private val CupertinoColorScheme = darkColorScheme(
    primary = CupertinoBlue,
    onPrimary = Color.White,
    secondary = CupertinoTeal,
    onSecondary = Color.Black,
    tertiary = CupertinoIndigo,
    background = CupertinoBackgroundDark,
    surface = CupertinoCardDark,
    surfaceVariant = CupertinoTertiaryDark,
    outline = CupertinoSeparatorDark,
    onSurface = CupertinoLabelDark,
    onSurfaceVariant = CupertinoLabelSecondaryDark,
    error = CupertinoRed
)

/**
 * DACTuner application theme.
 *
 * Wraps content in a Cupertino-styled dark theme adhering to Apple's Human Interface Guidelines.
 */
@Composable
fun DacTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CupertinoColorScheme,
        typography = DacTunerTypography,
        content = content
    )
}
