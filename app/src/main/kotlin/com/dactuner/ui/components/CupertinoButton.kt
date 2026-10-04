package com.dactuner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.dactuner.ui.theme.CupertinoBlue
import com.dactuner.ui.theme.CupertinoGray
import com.dactuner.ui.theme.DacTunerTypography

/**
 * Bouncy iOS-style primary action button.
 *
 * Implements Apple's signature press bounce (scales down to 0.96x under touch with spring return),
 * smooth rounded squircle corners (14dp), disabled state dimming, and tactile haptic feedback.
 *
 * @param text Button label text
 * @param onClick Callback invoked when tapped
 * @param modifier Optional modifier
 * @param enabled Whether the button is clickable
 * @param isLoading Whether to render an indeterminate spinner instead of label
 * @param containerColor Background color of the button (defaults to iOS System Blue)
 * @param contentColor Text/spinner color
 */
@Composable
fun CupertinoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = CupertinoBlue,
    contentColor: Color = Color.White
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Bouncy spring scale effect on touch
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 600f
        ),
        label = "CupertinoButtonScale"
    )

    val actualBackgroundColor = when {
        !enabled -> CupertinoGray.copy(alpha = 0.35f)
        isLoading -> containerColor.copy(alpha = 0.8f)
        else -> containerColor
    }

    Box(
        modifier = modifier
            .scale(scale)
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(actualBackgroundColor)
            .then(
                if (enabled && !isLoading) {
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
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                style = DacTunerTypography.labelLarge,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)
            )
        }
    }
}
