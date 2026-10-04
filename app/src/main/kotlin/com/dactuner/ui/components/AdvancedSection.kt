package com.dactuner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.dactuner.ui.DeviceInfo
import com.dactuner.ui.displayName
import com.dactuner.ui.theme.CupertinoBlue
import com.dactuner.ui.theme.CupertinoLabelDark
import com.dactuner.ui.theme.CupertinoLabelSecondaryDark
import com.dactuner.ui.theme.CupertinoPurple
import com.dactuner.ui.theme.CupertinoSeparatorDark
import com.dactuner.ui.theme.DacTunerTypography

/**
 * Expandable iOS-style section for hardware USB descriptors and volume specifications.
 *
 * @param deviceInfo Information about the connected DAC, or null if disconnected
 * @param modifier Optional modifier
 */
@Composable
fun AdvancedSection(
    deviceInfo: DeviceInfo?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "AdvancedChevronRotation"
    )

    CupertinoCard(
        title = "Hardware & Specifications",
        footer = if (expanded) "Low-level USB Audio Class descriptors parsed at runtime directly from the device." else null,
        modifier = modifier
    ) {
        // Expand/Collapse Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = !expanded
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(CupertinoPurple, androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CupertinoIcons.InfoCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Advanced USB Details",
                        style = DacTunerTypography.titleMedium,
                        color = CupertinoLabelDark
                    )
                    Text(
                        text = if (expanded) "Tap to collapse" else "VID/PID, UAC, and feature units",
                        style = DacTunerTypography.bodySmall,
                        color = CupertinoLabelSecondaryDark
                    )
                }
            }

            Icon(
                imageVector = CupertinoIcons.ChevronRight,
                contentDescription = null,
                tint = CupertinoLabelSecondaryDark,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation)
            )
        }

        // Expanded Specs Table
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    thickness = 0.5.dp,
                    color = CupertinoSeparatorDark
                )

                SpecRow(label = "Hardware Name", value = deviceInfo?.name ?: "Apple USB-C Adapter")
                SpecRow(label = "Manufacturer", value = deviceInfo?.manufacturer ?: "Apple, Inc.")
                SpecRow(label = "VID / PID", value = deviceInfo?.vidPid ?: "05AC:110A")
                SpecRow(label = "UAC Standard", value = deviceInfo?.uacVersion ?: "UAC 2.0")
                SpecRow(label = "Feature Unit ID", value = deviceInfo?.featureUnitId ?: "0x0A (10)")
                SpecRow(label = "Volume Gain", value = deviceInfo?.volumeDb ?: "0.0 dB")
                SpecRow(label = "Max Hardware Output", value = deviceInfo?.volumeMax ?: "0x0000")
                SpecRow(label = "Adapter Variant", value = deviceInfo?.variant?.displayName ?: "US Model (A2049)", isLast = false)

                // Copy Specs action row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val specsText = buildString {
                                appendLine("--- DACTuner Hardware Specs ---")
                                appendLine("Device: ${deviceInfo?.name ?: "Apple USB-C Adapter"}")
                                appendLine("Manufacturer: ${deviceInfo?.manufacturer ?: "Apple, Inc."}")
                                appendLine("VID/PID: ${deviceInfo?.vidPid ?: "05AC:110A"}")
                                appendLine("UAC Version: ${deviceInfo?.uacVersion ?: "UAC 2.0"}")
                                appendLine("Feature Unit: ${deviceInfo?.featureUnitId ?: "0x0A"}")
                                appendLine("Volume Gain: ${deviceInfo?.volumeDb ?: "0.0 dB"}")
                                appendLine("Variant: ${deviceInfo?.variant?.displayName ?: "US Model (A2049)"}")
                            }
                            clipboardManager.setText(AnnotatedString(specsText))
                            copied = true
                        }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (copied) CupertinoIcons.Checkmark else CupertinoIcons.Copy,
                        contentDescription = null,
                        tint = CupertinoBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (copied) "Specifications Copied ✓" else "Copy Specifications to Clipboard",
                        style = DacTunerTypography.labelLarge,
                        color = CupertinoBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = DacTunerTypography.bodyMedium,
                color = CupertinoLabelSecondaryDark
            )
            Text(
                text = value,
                style = DacTunerTypography.bodyMedium,
                color = CupertinoLabelDark
            )
        }
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = CupertinoSeparatorDark
            )
        }
    }
}
