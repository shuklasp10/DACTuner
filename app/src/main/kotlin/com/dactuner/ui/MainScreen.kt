package com.dactuner.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dactuner.ui.components.AdvancedSection
import com.dactuner.ui.components.CupertinoBanner
import com.dactuner.ui.components.CupertinoButton
import com.dactuner.ui.components.CupertinoCard
import com.dactuner.ui.components.CupertinoIcons
import com.dactuner.ui.components.CupertinoListTile
import com.dactuner.ui.components.CupertinoStatusCapsule
import com.dactuner.ui.components.CupertinoSwitch
import com.dactuner.ui.components.DebugLogSection
import com.dactuner.ui.theme.CupertinoBackgroundDark
import com.dactuner.ui.theme.CupertinoBlue
import com.dactuner.ui.theme.CupertinoGray
import com.dactuner.ui.theme.CupertinoGreen
import com.dactuner.ui.theme.CupertinoIndigo
import com.dactuner.ui.theme.CupertinoLabelDark
import com.dactuner.ui.theme.CupertinoLabelSecondaryDark
import com.dactuner.ui.theme.CupertinoOrange
import com.dactuner.ui.theme.CupertinoPurple
import com.dactuner.ui.theme.CupertinoRed
import com.dactuner.ui.theme.CupertinoTeal
import com.dactuner.ui.theme.DacTunerTheme
import com.dactuner.ui.theme.DacTunerTypography

/**
 * Main screen composable for DACTuner styled after the latest iOS 17/18 Cupertino design language.
 *
 * Implements:
 * - Large navigation title with status accessories
 * - Dynamic Island-inspired real-time hardware status capsule
 * - Inset grouped table cards with indented dividers and squircle icons
 * - Authentic Cupertino toggle switches with spring physics
 * - Bouncy primary action button with haptic feedback
 * - Deep-linked warning callouts (Samsung Media Volume Limit, EU model info)
 * - Expandable low-level USB specifications and real-time event console
 *
 * The entire screen is driven exclusively from [UiState] — zero side-effects in composables.
 */
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED ||
            uiState.connectionStatus == ConnectionStatus.CONFIGURED
    val isConfiguring = uiState.configurationStatus == ConfigurationStatus.CONFIGURING

    DacTunerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CupertinoBackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Top iOS Navigation Bar / Large Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DACTuner",
                            style = DacTunerTypography.headlineLarge,
                            color = CupertinoLabelDark
                        )
                        Text(
                            text = "Hardware Audio Controller",
                            style = DacTunerTypography.bodySmall,
                            color = CupertinoLabelSecondaryDark
                        )
                    }

                    // Top right Apple DAC accessory badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CupertinoBlue.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.UsbCable,
                            contentDescription = "USB DAC",
                            tint = CupertinoBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic Live Hardware Status Capsule
                CupertinoStatusCapsule(uiState = uiState)

                Spacer(modifier = Modifier.height(16.dp))

                // Warning & Notice Banners (Samsung Volume Limit, EU Adapter, Replug)
                uiState.warnings.forEach { warning ->
                    when (warning) {
                        is Warning.SamsungVolumeLimit -> {
                            CupertinoBanner(
                                message = warning.message,
                                tint = CupertinoOrange,
                                icon = CupertinoIcons.WarningTriangle,
                                actionLabel = "Open Sound Settings",
                                onAction = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                                    } catch (_: Exception) {
                                        // Fallback to general settings if vendor restricts sound settings intent
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        is Warning.EuAdapter -> {
                            CupertinoBanner(
                                message = warning.message,
                                tint = CupertinoBlue,
                                icon = CupertinoIcons.InfoCircle
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        is Warning.GeneralError -> {
                            CupertinoBanner(
                                message = warning.message,
                                tint = CupertinoRed,
                                icon = CupertinoIcons.WarningTriangle
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        is Warning.ConfigPartialSuccess -> {
                            CupertinoBanner(
                                message = warning.message,
                                tint = CupertinoOrange,
                                icon = CupertinoIcons.WarningTriangle
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        is Warning.ReplugRequired -> {
                            CupertinoBanner(
                                message = warning.message,
                                tint = CupertinoTeal,
                                icon = CupertinoIcons.InfoCircle
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                // Primary Cupertino Action Button
                CupertinoButton(
                    text = when {
                        !isConnected -> "Plug in Apple USB-C DAC to Configure"
                        isConfiguring -> "Configuring Audio Pipe..."
                        uiState.connectionStatus == ConnectionStatus.CONFIGURED -> "Reconfigure DAC Volume"
                        else -> "Configure DAC Volume"
                    },
                    onClick = { viewModel.configureConnectedDac() },
                    enabled = isConnected && !isConfiguring,
                    isLoading = isConfiguring,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Inset Group 1: Device Status
                CupertinoCard(
                    title = "Device Status",
                    footer = "DACTuner programs the hardware mixer Feature Unit to 0 dB, overcoming the 14 dB attenuation on Android."
                ) {
                    // Row 1: Connection Status
                    CupertinoListTile(
                        title = "Connection State",
                        subtitle = when (uiState.connectionStatus) {
                            ConnectionStatus.DISCONNECTED -> "No supported DAC attached"
                            ConnectionStatus.CONNECTED -> "Attached, ready to tune"
                            ConnectionStatus.CONFIGURED -> "Active at 0 dB max hardware volume"
                            ConnectionStatus.FAILED -> "Configuration failed"
                        },
                        icon = CupertinoIcons.UsbCable,
                        iconColor = when (uiState.connectionStatus) {
                            ConnectionStatus.CONFIGURED -> CupertinoGreen
                            ConnectionStatus.CONNECTED -> CupertinoOrange
                            ConnectionStatus.FAILED -> CupertinoRed
                            else -> CupertinoGray
                        },
                        trailing = {
                            Text(
                                text = when (uiState.connectionStatus) {
                                    ConnectionStatus.DISCONNECTED -> "Disconnected"
                                    ConnectionStatus.CONNECTED -> "Ready"
                                    ConnectionStatus.CONFIGURED -> "Configured ✓"
                                    ConnectionStatus.FAILED -> "Failed ✗"
                                },
                                style = DacTunerTypography.bodyMedium,
                                color = when (uiState.connectionStatus) {
                                    ConnectionStatus.CONFIGURED -> CupertinoGreen
                                    ConnectionStatus.CONNECTED -> CupertinoOrange
                                    ConnectionStatus.FAILED -> CupertinoRed
                                    else -> CupertinoLabelSecondaryDark
                                }
                            )
                        }
                    )

                    // Row 2: Hardware Mixer Gain
                    CupertinoListTile(
                        title = "Hardware Gain",
                        subtitle = "USB Audio Class Feature Unit mixer attenuation",
                        icon = CupertinoIcons.SpeakerWave,
                        iconColor = CupertinoTeal,
                        trailing = {
                            Text(
                                text = if (uiState.connectionStatus == ConnectionStatus.CONFIGURED) {
                                    "0.0 dB (100%)"
                                } else {
                                    uiState.deviceInfo?.volumeDb ?: "—"
                                },
                                style = DacTunerTypography.bodyMedium,
                                color = if (uiState.connectionStatus == ConnectionStatus.CONFIGURED) {
                                    CupertinoGreen
                                } else {
                                    CupertinoLabelSecondaryDark
                                }
                            )
                        }
                    )

                    // Row 3: Adapter Model Variant
                    CupertinoListTile(
                        title = "Hardware Model",
                        subtitle = uiState.deviceInfo?.vidPid ?: "Apple 05AC:110A",
                        icon = CupertinoIcons.InfoCircle,
                        iconColor = CupertinoPurple,
                        showDivider = false,
                        trailing = {
                            Text(
                                text = uiState.deviceInfo?.variant?.displayName ?: "Apple USB-C",
                                style = DacTunerTypography.bodyMedium,
                                color = CupertinoLabelSecondaryDark
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Inset Group 2: Preferences
                CupertinoCard(
                    title = "Preferences",
                    footer = "Configured settings persist across USB reconnections and device reboots."
                ) {
                    // Auto-Configure on Connect toggle
                    CupertinoListTile(
                        title = "Auto-Configure on Connect",
                        subtitle = "Maximizes volume immediately upon DAC attachment",
                        icon = CupertinoIcons.Bolt,
                        iconColor = CupertinoGreen,
                        trailing = {
                            CupertinoSwitch(
                                checked = uiState.settings.autoConfigureEnabled,
                                onCheckedChange = { viewModel.onAutoConfigureToggled(it) }
                            )
                        }
                    )

                    // Silent Background Mode toggle
                    CupertinoListTile(
                        title = "Silent Background Mode",
                        subtitle = "Tunes DAC silently without bringing app to foreground",
                        icon = CupertinoIcons.Moon,
                        iconColor = CupertinoBlue,
                        trailing = {
                            CupertinoSwitch(
                                checked = uiState.settings.backgroundModeEnabled,
                                onCheckedChange = { viewModel.onBackgroundModeToggled(it) }
                            )
                        }
                    )

                    // Transient Notifications toggle
                    CupertinoListTile(
                        title = "Show Notifications",
                        subtitle = "Display transient toast when volume is maximized",
                        icon = CupertinoIcons.Bell,
                        iconColor = CupertinoOrange,
                        trailing = {
                            CupertinoSwitch(
                                checked = uiState.settings.showNotifications,
                                onCheckedChange = { viewModel.onShowNotificationsToggled(it) }
                            )
                        }
                    )

                    // Verbose Debug Logging toggle
                    CupertinoListTile(
                        title = "Debug Logging",
                        subtitle = "Capture USB descriptors and control transfer dumps",
                        icon = CupertinoIcons.Terminal,
                        iconColor = CupertinoIndigo,
                        showDivider = false,
                        trailing = {
                            CupertinoSwitch(
                                checked = uiState.settings.debugModeEnabled,
                                onCheckedChange = { viewModel.onDebugModeToggled(it) }
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Inset Group 3: Expandable Hardware USB Descriptors
                AdvancedSection(deviceInfo = uiState.deviceInfo)

                Spacer(modifier = Modifier.height(24.dp))

                // Inset Group 4: Expandable Diagnostic Console & Logs
                DebugLogSection(
                    logs = viewModel.getLogEntries(),
                    onExportLogs = { viewModel.exportLogs() },
                    onClearLogs = { viewModel.clearLogs() }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // iOS Footer Brand Tag
                Text(
                    text = "DACTuner for Android • Built for Apple USB-C DAC",
                    style = DacTunerTypography.bodySmall,
                    color = CupertinoLabelSecondaryDark.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
