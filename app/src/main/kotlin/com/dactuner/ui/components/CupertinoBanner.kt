package com.dactuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dactuner.ui.theme.CupertinoLabelDark
import com.dactuner.ui.theme.CupertinoOrange
import com.dactuner.ui.theme.DacTunerTypography

/**
 * Cupertino-styled warning and informational callout card.
 *
 * Renders an inset translucent card with tint matching the severity,
 * leading glyph, descriptive explanation, and optional action button.
 *
 * @param message Description text
 * @param modifier Optional modifier
 * @param icon Leading icon glyph
 * @param tint Accent color for borders, icon, and background wash
 * @param actionLabel Optional text for the inline action button (e.g. "Open Settings")
 * @param onAction Optional click listener for the inline action button
 */
@Composable
fun CupertinoBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = CupertinoIcons.WarningTriangle,
    tint: Color = CupertinoOrange,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(0.5.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = message,
                    style = DacTunerTypography.bodySmall,
                    color = CupertinoLabelDark,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!actionLabel.isNullOrBlank() && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tint)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAction()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            style = DacTunerTypography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
