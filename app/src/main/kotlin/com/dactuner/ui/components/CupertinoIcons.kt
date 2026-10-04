package com.dactuner.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Collection of standalone, Cupertino-styled vector glyphs for DACTuner.
 *
 * Built directly using Compose ImageVector to avoid dependency on extended icon libraries
 * while maintaining pixel-perfect iOS Human Interface aesthetics.
 */
object CupertinoIcons {

    /** iOS-style Chevron Right (>) */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 6f)
                lineTo(15f, 12f)
                lineTo(9f, 18f)
            }
        }.build()
    }

    /** Volume / Speaker wave glyph */
    val SpeakerWave: ImageVector by lazy {
        ImageVector.Builder(
            name = "SpeakerWave",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(4f, 9f)
                lineTo(8f, 9f)
                lineTo(13f, 5f)
                lineTo(13f, 19f)
                lineTo(8f, 15f)
                lineTo(4f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16.5f, 8.5f)
                curveTo(17.8f, 9.8f, 18.5f, 11.2f, 18.5f, 12f)
                curveTo(18.5f, 12.8f, 17.8f, 14.2f, 16.5f, 15.5f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(19f, 6f)
                curveTo(21f, 8f, 22f, 10.2f, 22f, 12f)
                curveTo(22f, 13.8f, 21f, 16f, 19f, 18f)
            }
        }.build()
    }

    /** USB / DAC cable icon */
    val UsbCable: ImageVector by lazy {
        ImageVector.Builder(
            name = "UsbCable",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 3f)
                lineTo(17f, 3f)
                lineTo(17f, 10f)
                lineTo(7f, 10f)
                close()
                moveTo(10f, 10f)
                lineTo(10f, 14f)
                moveTo(14f, 10f)
                lineTo(14f, 14f)
                moveTo(9f, 14f)
                lineTo(15f, 14f)
                lineTo(15f, 21f)
                lineTo(9f, 21f)
                close()
            }
        }.build()
    }

    /** Lightning / Auto-configure icon */
    val Bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12.5f, 2f)
                lineTo(5f, 13f)
                lineTo(11.5f, 13f)
                lineTo(10.5f, 22f)
                lineTo(19f, 10f)
                lineTo(12.5f, 10f)
                close()
            }
        }.build()
    }

    /** Moon / Background silent mode icon */
    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12.3f, 2f)
                curveTo(6.6f, 2.3f, 2f, 7f, 2f, 12.8f)
                curveTo(2f, 18.9f, 7f, 23.9f, 13.1f, 23.9f)
                curveTo(17.8f, 23.9f, 21.8f, 20.9f, 23.3f, 16.5f)
                curveTo(15.7f, 18.1f, 8.8f, 11.2f, 10.4f, 3.6f)
                curveTo(11f, 2.9f, 11.7f, 2.4f, 12.3f, 2f)
                close()
            }
        }.build()
    }

    /** Bell / Notification icon */
    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                curveTo(8.7f, 3f, 6f, 5.7f, 6f, 9f)
                lineTo(6f, 15f)
                lineTo(4f, 17f)
                lineTo(20f, 17f)
                lineTo(18f, 15f)
                lineTo(18f, 9f)
                curveTo(18f, 5.7f, 15.3f, 3f, 12f, 3f)
                close()
                moveTo(10f, 19f)
                curveTo(10f, 20.1f, 10.9f, 21f, 12f, 21f)
                curveTo(13.1f, 21f, 14f, 20.1f, 14f, 19f)
            }
        }.build()
    }

    /** Info Circle */
    val InfoCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "InfoCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                curveTo(2f, 17.5f, 6.5f, 22f, 12f, 22f)
                curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
                curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f)
                close()
                moveTo(12f, 11f)
                lineTo(12f, 17f)
            }
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12f, 7f)
                curveTo(12.6f, 7f, 13f, 7.4f, 13f, 8f)
                curveTo(13f, 8.6f, 12.6f, 9f, 12f, 9f)
                curveTo(11.4f, 9f, 11f, 8.6f, 11f, 8f)
                curveTo(11f, 7.4f, 11.4f, 7f, 12f, 7f)
                close()
            }
        }.build()
    }

    /** Terminal / Code log glyph */
    val Terminal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Terminal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 17f)
                lineTo(9f, 12f)
                lineTo(4f, 7f)
                moveTo(12f, 17f)
                lineTo(19f, 17f)
            }
        }.build()
    }

    /** Copy to clipboard glyph */
    val Copy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 9f)
                lineTo(19f, 9f)
                lineTo(19f, 20f)
                lineTo(9f, 20f)
                close()
                moveTo(5f, 15f)
                lineTo(5f, 5f)
                lineTo(15f, 5f)
            }
        }.build()
    }

    /** Share glyph */
    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 14f)
                lineTo(12f, 3f)
                moveTo(8f, 7f)
                lineTo(12f, 3f)
                lineTo(16f, 7f)
                moveTo(5f, 10f)
                lineTo(5f, 20f)
                lineTo(19f, 20f)
                lineTo(19f, 10f)
            }
        }.build()
    }

    /** Checkmark glyph */
    val Checkmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "Checkmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                lineTo(10f, 17f)
                lineTo(19f, 7f)
            }
        }.build()
    }

    /** Warning triangle glyph */
    val WarningTriangle: ImageVector by lazy {
        ImageVector.Builder(
            name = "WarningTriangle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                lineTo(22f, 20f)
                lineTo(2f, 20f)
                close()
                moveTo(12f, 9f)
                lineTo(12f, 13f)
            }
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12f, 16.5f)
                curveTo(12.4f, 16.5f, 12.8f, 16.8f, 12.8f, 17.2f)
                curveTo(12.8f, 17.6f, 12.4f, 18f, 12f, 18f)
                curveTo(11.6f, 18f, 11.2f, 17.6f, 11.2f, 17.2f)
                curveTo(11.2f, 16.8f, 11.6f, 16.5f, 12f, 16.5f)
                close()
            }
        }.build()
    }
}
