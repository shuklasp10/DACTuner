package com.dactuner.entry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.dactuner.DacTunerApplication

/**
 * BroadcastReceiver for USB device events.
 *
 * Handles [UsbManager.ACTION_USB_DEVICE_DETACHED] to detect when a supported
 * DAC is disconnected. Registered at runtime in [MainActivity] during onResume/onPause.
 *
 * USB_DEVICE_ATTACHED is handled by [MainActivity]'s manifest intent filter, not
 * by this receiver, because Android delivers attach events as Activity intents
 * (not broadcasts) when a device filter is specified.
 *
 * @property onDeviceDetached Callback invoked when any USB device is detached.
 *   The callback receives the VID and PID of the detached device (if available).
 */
class UsbEventReceiver(
    private val onDeviceDetached: (vendorId: Int?, productId: Int?) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as DacTunerApplication
        val logger = app.diagnosticsLogger

        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = getUsbDeviceFromIntent(intent)
                logger.log(
                    "USB_EVENT",
                    "USB device detached: " +
                            if (device != null) {
                                "VID=0x${String.format("%04X", device.vendorId)} " +
                                        "PID=0x${String.format("%04X", device.productId)}"
                            } else {
                                "(device info unavailable)"
                            }
                )
                onDeviceDetached(device?.vendorId, device?.productId)
            }
        }
    }

    /**
     * Extracts the UsbDevice from the intent, handling API level differences.
     */
    @Suppress("DEPRECATION")
    private fun getUsbDeviceFromIntent(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}
