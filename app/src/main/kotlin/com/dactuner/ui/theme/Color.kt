package com.dactuner.ui.theme

import androidx.compose.ui.graphics.Color

// --- Cupertino Semantic Colors (iOS 17 / iOS 18) ---

// System Backgrounds
/** Pure black for OLED screens (iOS dark grouped background). */
val CupertinoBackgroundDark = Color(0xFF000000)

/** Inset card and grouped list container background in dark mode. */
val CupertinoCardDark = Color(0xFF1C1C1E)

/** Tertiary background for inner elevated badges, capsules, and pressed rows. */
val CupertinoTertiaryDark = Color(0xFF2C2C2E)

/** Subtle hairline separator color for dark mode (0.5dp borders/dividers). */
val CupertinoSeparatorDark = Color(0xFF38383A)

// Labels
/** High-contrast primary label (titles, item headings). */
val CupertinoLabelDark = Color(0xFFFFFFFF)

/** Muted secondary label (subtitles, section headers, trailing values). */
val CupertinoLabelSecondaryDark = Color(0xFF8E8E93)

/** Tertiary label for captions, placeholders, and disabled elements. */
val CupertinoLabelTertiaryDark = Color(0xFF48484A)

// Semantic Accents (Cupertino Dynamic Palette)
/** iOS System Blue — primary actions and interactive accents. */
val CupertinoBlue = Color(0xFF0A84FF)

/** iOS System Green — success status, 0dB configured, and active switches. */
val CupertinoGreen = Color(0xFF30D158)

/** iOS System Orange — connected/pending states, attention notices. */
val CupertinoOrange = Color(0xFFFF9F0A)

/** iOS System Red — error states and failure indications. */
val CupertinoRed = Color(0xFFFF453A)

/** iOS System Purple — diagnostics and hardware telemetry. */
val CupertinoPurple = Color(0xFFBF5AF2)

/** iOS System Indigo — secondary system tools. */
val CupertinoIndigo = Color(0xFF5E5CE6)

/** iOS System Teal — audio streaming and waveform indicators. */
val CupertinoTeal = Color(0xFF64D2FF)

/** iOS System Gray. */
val CupertinoGray = Color(0xFF8E8E93)

/** Cupertino Switch track background when off (dark mode). */
val CupertinoSwitchTrackDark = Color(0xFF39393D)

// --- Backward Compatibility Aliases for DACTuner ---
val DacPrimary = CupertinoBlue
val DacPrimaryVariant = Color(0xFF0062CC)
val DacSecondary = CupertinoTeal
val DacBackground = CupertinoBackgroundDark
val DacSurface = CupertinoCardDark
val DacSurfaceVariant = CupertinoTertiaryDark
val DacOnPrimary = Color.White
val DacOnSurface = CupertinoLabelDark
val DacOnSurfaceVariant = CupertinoLabelSecondaryDark

val StatusConfigured = CupertinoGreen
val StatusConnected = CupertinoOrange
val StatusFailed = CupertinoRed
val StatusDisconnected = CupertinoGray
