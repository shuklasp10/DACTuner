package com.dactuner.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
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
import com.dactuner.ui.theme.CupertinoBlue
import com.dactuner.ui.theme.CupertinoLabelDark
import com.dactuner.ui.theme.CupertinoLabelSecondaryDark
import com.dactuner.ui.theme.CupertinoSeparatorDark
import com.dactuner.ui.theme.DacTunerTypography

/**
 * Cupertino-styled table list row for grouped inset sections.
 *
 * Replicates the standard iOS Settings table view row with:
 * - Rounded squircle icon tile
 * - Primary title & secondary subtitle
 * - Hairline bottom divider with 58dp start indent
 * - Trailing accessory slot (Switch, Chevron, detail text)
 *
 * @param title Primary label text
 * @param subtitle Optional secondary descriptor text
 * @param icon Optional leading icon glyph
 * @param iconColor Background tint of the squircle icon container
 * @param trailing Composable slot for the trailing accessory
 * @param showDivider Whether to render the hairline separator at the bottom of this row
 * @param onClick Optional row click listener
 */
@Composable
fun CupertinoListTile(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = CupertinoBlue,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Leading Icon Squircle
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            // Title & Subtitle column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    style = DacTunerTypography.titleMedium,
                    color = CupertinoLabelDark
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = DacTunerTypography.bodySmall,
                        color = CupertinoLabelSecondaryDark
                    )
                }
            }

            // Trailing accessory
            if (trailing != null) {
                trailing()
            }
        }

        // Hairline divider with start indent matching content alignment
        if (showDivider) {
            val startIndent = if (icon != null) 58.dp else 16.dp
            HorizontalDivider(
                modifier = Modifier.padding(start = startIndent),
                thickness = 0.5.dp,
                color = CupertinoSeparatorDark
            )
        }
    }
}
