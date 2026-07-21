package com.dactuner.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import java.io.Closeable

/**
 * Closeable wrapper around a USB device connection.
 *
 * Ensures the [UsbDeviceConnection] is properly closed when the handle goes out of scope.
 * Callers should always use the `use { }` pattern to guarantee resource cleanup:
 *
 * ```kotlin
 * usbDeviceManager.openConnection(device)?.use { handle ->
 *     // Use handle.connection for control transfers
 * }
 * ```
 *
 * @property device The USB device this connection is opened for
 * @property connection The active USB device connection for sending control transfers
 * @property descriptors The parsed UAC descriptors for this device
 */
class UsbConnectionHandle(
    val device: UsbDevice,
    val connection: UsbDeviceConnection,
    val descriptors: UacDescriptors
) : Closeable {

    /**
     * Closes the USB device connection.
     *
     * Safe to call multiple times — subsequent calls are no-ops.
     * Catches and ignores exceptions from close() since the device
     * may already be disconnected.
     */
    override fun close() {
        try {
            connection.close()
        } catch (_: Exception) {
            // Ignore close errors — device may already be disconnected
        }
    }
}
