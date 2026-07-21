package com.dactuner.usb

/**
 * Represents the volume range of a USB Audio Class device.
 * Values are 16-bit signed integers in 1/256 dB units.
 */
data class VolumeRange(
    val min: Int,
    val max: Int,
    val resolution: Int
)
