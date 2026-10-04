package com.dactuner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dactuner.ui.theme.CupertinoCardDark
import com.dactuner.ui.theme.CupertinoLabelSecondaryDark
import com.dactuner.ui.theme.CupertinoSeparatorDark
import com.dactuner.ui.theme.DacTunerTypography

/**
 * Inset grouped section container adhering to iOS 17/18 Settings styling.
 *
 * Includes optional uppercase section title header, rounded squircle card container,
 * hairline perimeter border, and optional explanatory section footer caption.
 *
 * @param title Optional uppercase section header text
 * @param footer Optional footnote caption explaining section behavior
 * @param modifier Optional modifier
 * @param content Items inside the inset grouped card
 */
@Composable
fun CupertinoCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title.uppercase(),
                style = DacTunerTypography.labelMedium,
                color = CupertinoLabelSecondaryDark,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CupertinoCardDark)
                .border(
                    width = 0.5.dp,
                    color = CupertinoSeparatorDark,
                    shape = RoundedCornerShape(16.dp)
                ),
            content = content
        )

        if (!footer.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = footer,
                style = DacTunerTypography.bodySmall,
                color = CupertinoLabelSecondaryDark,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
