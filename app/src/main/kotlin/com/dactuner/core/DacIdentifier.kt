package com.dactuner.core

/**
 * Identifies whether a USB device is a supported DAC.
 *
 * Matches USB Vendor ID and Product ID against the [DeviceProfileRegistry].
 * Returns the matching [DacProfile] if the device is supported, or null if not.
 *
 * Designed to accept raw VID/PID integers so it can be unit-tested without
 * Android framework dependencies (no UsbDevice import needed).
 */
class DacIdentifier(
    private val registry: DeviceProfileRegistry
) {

    /**
     * Identifies a DAC by its USB Vendor ID and Product ID.
     *
     * @param vendorId USB Vendor ID (e.g., 0x05AC for Apple)
     * @param productId USB Product ID (e.g., 0x110A for Apple DAC)
     * @return Matching [DacProfile] if the device is a supported DAC, or null
     */
    fun identify(vendorId: Int, productId: Int): DacProfile? {
        return registry.getProfile(vendorId, productId)
    }
}
