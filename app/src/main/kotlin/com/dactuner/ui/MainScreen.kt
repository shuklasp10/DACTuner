package com.dactuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dactuner.ui.theme.DacTunerTheme
import com.dactuner.ui.theme.StatusConfigured
import com.dactuner.ui.theme.StatusConnected
import com.dactuner.ui.theme.StatusDisconnected
import com.dactuner.ui.theme.StatusFailed

/**
 * Main screen composable for DACTuner.
 *
 * Phase 1: Displays connection status with animated status indicator.
 * Phase 5: Will include full UI with configure button, settings, warnings,
 * advanced section, and debug log viewer.
 *
 * The entire UI is a function of [UiState] — no side effects in composables.
 */
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DacTunerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // App title
                Text(
                    text = "DACTuner",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Status card
                DeviceStatusCard(uiState = uiState)

                Spacer(modifier = Modifier.height(32.dp))

                // Configure button
                if (uiState.connectionStatus == ConnectionStatus.CONNECTED || uiState.connectionStatus == ConnectionStatus.CONFIGURED) {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.configureConnectedDac() },
                        enabled = uiState.configurationStatus != ConfigurationStatus.CONFIGURING,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.configurationStatus == ConfigurationStatus.CONFIGURING) {
                            Text("Configuring...")
                        } else {
                            Text("Configure DAC Volume")
                        }
                    }
                }
                
                if (uiState.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.warnings.joinToString("\n"),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Card showing the DAC connection status with an animated indicator.
 *
 * Displays:
 * - Color-coded status circle (gray/amber/green/red)
 * - Device name or "No supported DAC connected"
 * - Status description text
 */
@Composable
private fun DeviceStatusCard(uiState: UiState) {
    // Animate status indicator color transitions
    val statusColor by animateColorAsState(
        targetValue = when (uiState.connectionStatus) {
            ConnectionStatus.DISCONNECTED -> StatusDisconnected
            ConnectionStatus.CONNECTED -> StatusConnected
            ConnectionStatus.CONFIGURED -> StatusConfigured
            ConnectionStatus.FAILED -> StatusFailed
        },
        animationSpec = tween(durationMillis = 500),
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated status indicator
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = statusColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = statusColor.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color = statusColor, shape = CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Device name or fallback text
            Text(
                text = uiState.deviceInfo?.name ?: "No supported DAC connected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status description
            Text(
                text = when (uiState.connectionStatus) {
                    ConnectionStatus.DISCONNECTED ->
                        "Plug in your Apple USB-C adapter to get started"
                    ConnectionStatus.CONNECTED ->
                        "Connected \u2014 ready to configure"
                    ConnectionStatus.CONFIGURED ->
                        "\u2713 Configured at maximum volume"
                    ConnectionStatus.FAILED ->
                        "Configuration failed \u2014 see details below"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Show VID/PID when connected
            if (uiState.deviceInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "VID/PID: ${uiState.deviceInfo.vidPid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
