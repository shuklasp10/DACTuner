package com.dactuner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dactuner.ui.theme.CupertinoGreen
import com.dactuner.ui.theme.CupertinoSwitchTrackDark

/**
 * High-fidelity iOS Cupertino toggle switch composable.
 *
 * Implements the Apple Human Interface Guidelines toggle proportions (51x31dp)
 * with bouncy spring physics, subtle drop shadow, and tactile haptic feedback.
 *
 * @param checked Whether the switch is currently on
 * @param onCheckedChange Callback invoked when the switch is toggled
 * @param modifier Optional modifier for the switch container
 * @param enabled Whether the switch is interactive
 */
@Composable
fun CupertinoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    // Spring animation for thumb translation (51dp width - 27dp thumb - 4dp padding = 20dp max offset)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 600f
        ),
        label = "CupertinoSwitchThumbOffset"
    )

    // Smooth color animation for the pill track
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            CupertinoGreen
        } else {
            CupertinoSwitchTrackDark
        },
        animationSpec = tween(durationMillis = 200),
        label = "CupertinoSwitchTrackColor"
    )

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .background(
                color = if (enabled) trackColor else trackColor.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .then(
                if (enabled && onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null // Cupertino switches do not show Android ripple
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCheckedChange(!checked)
                    }
                } else {
                    Modifier
                }
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Drop-shadowed white circular thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = CircleShape,
                    clip = false
                )
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
        )
    }
}
