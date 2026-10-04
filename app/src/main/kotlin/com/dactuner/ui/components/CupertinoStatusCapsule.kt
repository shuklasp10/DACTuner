package com.dactuner.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dactuner.ui.ConfigurationStatus
import com.dactuner.ui.ConnectionStatus
import com.dactuner.ui.UiState
import com.dactuner.ui.theme.CupertinoBlue
import com.dactuner.ui.theme.CupertinoGray
import com.dactuner.ui.theme.CupertinoGreen
import com.dactuner.ui.theme.CupertinoLabelDark
import com.dactuner.ui.theme.CupertinoOrange
import com.dactuner.ui.theme.CupertinoRed
import com.dactuner.ui.theme.CupertinoSeparatorDark
import com.dactuner.ui.theme.CupertinoTertiaryDark
import com.dactuner.ui.theme.DacTunerTypography

/**
 * Dynamic live hardware status capsule widget inspired by iOS Dynamic Island status badges.
 *
 * Provides real-time visual feedback for the Apple USB-C DAC adapter connection,
 * pulsing green aura for 0dB maximum gain state, and activity spinning during tuning.
 *
 * @param uiState Current immutable UI state
 * @param modifier Optional modifier
 */
@Composable
fun CupertinoStatusCapsule(
    uiState: UiState,
    modifier: Modifier = Modifier
) {
    val isConfiguring = uiState.configurationStatus == ConfigurationStatus.CONFIGURING
    val isConfigured = uiState.connectionStatus == ConnectionStatus.CONFIGURED
    val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED

    // Infinite breathing/pulsing animation for the active aura ring
    val infiniteTransition = rememberInfiniteTransition(label = "CapsulePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    val dotColor = when {
        isConfiguring -> CupertinoBlue
        isConfigured -> CupertinoGreen
        isConnected -> CupertinoOrange
        uiState.connectionStatus == ConnectionStatus.FAILED -> CupertinoRed
        else -> CupertinoGray
    }

    val statusBadgeText = when {
        isConfiguring -> "Tuning..."
        isConfigured -> "0.0 dB (Max)"
        isConnected -> "Ready"
        uiState.connectionStatus == ConnectionStatus.FAILED -> "Failed"
        else -> "Disconnected"
    }

    val statusBadgeColor = dotColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(CupertinoTertiaryDark)
            .border(0.5.dp, CupertinoSeparatorDark, CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Glowing state dot with animated aura
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if ((isConfigured || isConnected) && !isConfiguring) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(pulseScale)
                                .background(dotColor.copy(alpha = pulseAlpha), CircleShape)
                        )
                    }

                    if (isConfiguring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = CupertinoBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Hardware Name / Description
                Text(
                    text = uiState.deviceInfo?.name ?: "Apple USB-C to 3.5mm DAC",
                    style = DacTunerTypography.titleMedium,
                    color = CupertinoLabelDark
                )
            }

            // Right: Capsule Badge with status
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusBadgeColor.copy(alpha = 0.16f))
                    .border(0.5.dp, statusBadgeColor.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusBadgeText,
                    style = DacTunerTypography.labelSmall,
                    color = statusBadgeColor
                )
            }
        }
    }
}
