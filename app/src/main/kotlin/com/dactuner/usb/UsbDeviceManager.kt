package com.dactuner.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.dactuner.util.DiagnosticsLogger
import com.dactuner.util.LogLevel

/**
 * Manages USB device connections for DAC configuration.
 *
 * Provides permission checking, permission requesting, and device connection
 * opening/closing. Only this class and [com.dactuner.usb.UacControlTransferExecutor]
 * (Phase 2) may directly call Android USB APIs.
 *
 * @property context Application context for accessing UsbManager
 * @property logger Diagnostics logger for recording USB operations
 */
class UsbDeviceManager(
    private val context: Context,
    private val logger: DiagnosticsLogger
) {

    private val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager
        
    private val descriptorParser = UacDescriptorParser(logger)

    /**
     * Checks whether the app has permission to access the given USB device.
     *
     * @param device The USB device to check permission for
     * @return true if permission has been granted
     */
    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    /**
     * Requests permission to access the given USB device.
     *
     * Shows the system USB permission dialog to the user. The result is
     * delivered asynchronously via the provided callback.
     *
     * @param device The USB device to request permission for
     * @param callback Called with true if permission was granted, false otherwise
     */
    fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit) {
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == ACTION_USB_PERMISSION) {
                    val granted = intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED, false
                    )
                    logger.log(
                        "USB_PERMISSION",
                        "Permission ${if (granted) "granted" else "denied"} for device"
                    )
                    callback(granted)
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: IllegalArgumentException) {
                        // Receiver may already be unregistered
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(ACTION_USB_PERMISSION),
            Context.RECEIVER_NOT_EXPORTED
        )

        logger.log("USB_PERMISSION", "Requesting USB permission for device")
        usbManager.requestPermission(device, permissionIntent)
    }

    /**
     * Opens a connection to the USB device and parses its UAC descriptors.
     *
     * Returns a [UsbConnectionHandle] that must be closed after use (preferably
     * via the `use { }` pattern). Returns null if the connection cannot be opened.
     *
     * @param device The USB device to connect to
     * @return A [UsbConnectionHandle] or null if the connection failed
     */
    fun openConnection(device: UsbDevice): UsbConnectionHandle? {
        val startTime = System.currentTimeMillis()
        return try {
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                val descriptors = descriptorParser.parse(device, connection)
                val handle = UsbConnectionHandle(device, connection, descriptors)
                
                val duration = System.currentTimeMillis() - startTime
                logger.logUsbOperation("openDevice + parse", "success", duration)
                handle
            } else {
                val duration = System.currentTimeMillis() - startTime
                logger.logUsbOperation(
                    "openDevice",
                    "failed (null connection — check permissions)",
                    duration
                )
                null
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.logUsbOperation("openDevice", "exception: ${e.message}", duration)
            logger.log("USB", "Failed to open device: ${e.message}", LogLevel.ERROR)
            null
        }
    }

    /**
     * Returns a map of currently connected USB devices.
     * Key is the device name (path), value is the UsbDevice.
     */
    fun getConnectedDevices(): Map<String, UsbDevice> {
        return usbManager.deviceList
    }

    companion object {
        /** Action for USB permission PendingIntent. */
        private const val ACTION_USB_PERMISSION = "com.dactuner.USB_PERMISSION"
    }
}
