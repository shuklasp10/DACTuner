package com.dactuner.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dactuner.ui.theme.CupertinoBackgroundDark
import com.dactuner.ui.theme.CupertinoBlue
import com.dactuner.ui.theme.CupertinoIndigo
import com.dactuner.ui.theme.CupertinoLabelDark
import com.dactuner.ui.theme.CupertinoLabelSecondaryDark
import com.dactuner.ui.theme.CupertinoMonospace
import com.dactuner.ui.theme.CupertinoOrange
import com.dactuner.ui.theme.CupertinoRed
import com.dactuner.ui.theme.CupertinoSeparatorDark
import com.dactuner.ui.theme.DacTunerTypography
import com.dactuner.util.LogEntry
import com.dactuner.util.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Expandable iOS-styled diagnostic terminal and event console.
 *
 * @param logs List of log entries to display
 * @param onExportLogs Callback returning the exported log string for sharing
 * @param onClearLogs Callback to clear logs
 * @param modifier Optional modifier
 */
@Composable
fun DebugLogSection(
    logs: List<LogEntry>,
    onExportLogs: () -> String,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "DebugChevronRotation"
    )

    // Scroll to the bottom when new logs arrive while open
    LaunchedEffect(logs.size, expanded) {
        if (expanded && logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    CupertinoCard(
        title = "Diagnostic Event Logs",
        footer = if (expanded) "Real-time records of USB detection, interface claims, and control transfers." else null,
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
                        .background(CupertinoIndigo, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Terminal,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Diagnostics & Console",
                        style = DacTunerTypography.titleMedium,
                        color = CupertinoLabelDark
                    )
                    Text(
                        text = if (expanded) "Tap to collapse" else "${logs.size} log entries recorded",
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

        // Expanded Terminal
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Terminal Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CupertinoBackgroundDark)
                        .border(0.5.dp, CupertinoSeparatorDark, RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = "No diagnostic events logged yet.",
                            style = CupertinoMonospace,
                            color = CupertinoLabelSecondaryDark,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(logs) { entry ->
                                LogEntryLine(entry = entry)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons (Copy / Share / Clear)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy to clipboard
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(onExportLogs()))
                                copied = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (copied) CupertinoIcons.Checkmark else CupertinoIcons.Copy,
                            contentDescription = null,
                            tint = CupertinoBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (copied) "Copied" else "Copy",
                            style = DacTunerTypography.labelSmall,
                            color = CupertinoBlue
                        )
                    }

                    // Share Via System Sheet
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, onExportLogs())
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export DACTuner Logs")
                                context.startActivity(shareIntent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Share,
                            contentDescription = null,
                            tint = CupertinoBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Share",
                            style = DacTunerTypography.labelSmall,
                            color = CupertinoBlue
                        )
                    }

                    // Clear logs
                    Text(
                        text = "Clear",
                        style = DacTunerTypography.labelSmall,
                        color = CupertinoRed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onClearLogs()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogEntryLine(entry: LogEntry) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val timeStr = remember(entry.timestamp) { dateFormat.format(Date(entry.timestamp)) }

    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> CupertinoIndigo
        LogLevel.INFO -> CupertinoBlue
        LogLevel.WARNING -> CupertinoOrange
        LogLevel.ERROR -> CupertinoRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = "$timeStr ",
            style = CupertinoMonospace,
            color = CupertinoLabelSecondaryDark
        )
        Text(
            text = "[${entry.tag}] ",
            style = CupertinoMonospace,
            color = levelColor
        )
        Text(
            text = entry.message,
            style = CupertinoMonospace,
            color = CupertinoLabelDark
        )
    }
}
