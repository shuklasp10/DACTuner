package com.dactuner.ui.theme

import androidx.compose.ui.graphics.Color

// --- Primary palette ---
/** Deep indigo primary color. */
val DacPrimary = Color(0xFF7C4DFF)

/** Darker indigo for active/pressed states. */
val DacPrimaryVariant = Color(0xFF651FFF)

/** Teal secondary accent color. */
val DacSecondary = Color(0xFF03DAC6)

// --- Surface and background ---
/** Dark background for the app. */
val DacBackground = Color(0xFF121212)

/** Card and elevated surface color. */
val DacSurface = Color(0xFF1E1E1E)

/** Variant surface for secondary cards. */
val DacSurfaceVariant = Color(0xFF2C2C2C)

// --- On-colors (text/icons on top of surfaces) ---
/** Text/icon color on primary surfaces. */
val DacOnPrimary = Color.White

/** Primary text/icon color on dark surfaces. */
val DacOnSurface = Color(0xFFE0E0E0)

/** Secondary text/icon color on dark surfaces. */
val DacOnSurfaceVariant = Color(0xFFB0B0B0)

// --- Status indicator colors ---
/** Green — DAC configured successfully. */
val StatusConfigured = Color(0xFF66BB6A)

/** Amber — DAC connected but not yet configured. */
val StatusConnected = Color(0xFFFFA726)

/** Red — configuration failed. */
val StatusFailed = Color(0xFFEF5350)

/** Gray — no DAC connected. */
val StatusDisconnected = Color(0xFF757575)
