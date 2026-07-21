package com.dactuner.entry

import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.dactuner.DacTunerApplication
import com.dactuner.ui.MainScreen
import com.dactuner.ui.MainViewModel
import com.dactuner.ui.ConnectionStatus

/**
 * Main (and only) Activity for DACTuner.
 *
 * Entry points:
 * 1. Normal launch via MAIN/LAUNCHER intent — shows UI with current DAC status.
 * 2. Auto-launch via USB_DEVICE_ATTACHED intent — handles DAC connection,
 *    shows UI (or can finish() for background-only config in later phases).
 *
 * Uses `singleTask` launch mode so that subsequent USB_DEVICE_ATTACHED intents
 * are delivered via [onNewIntent] rather than creating new Activity instances.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * Runtime-registered receiver for USB_DEVICE_DETACHED events.
     * Registered in onResume, unregistered in onPause.
     */
    private val usbDetachReceiver = UsbEventReceiver { _, _ ->
        // On any USB detach, re-check connected devices to update state
        checkConnectedDevices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DacTunerApplication
        app.diagnosticsLogger.log("ACTIVITY", "MainActivity created")

        // Handle USB_DEVICE_ATTACHED intent if launched via USB plug-in
        handleUsbIntent(intent)

        setContent {
            MainScreen(viewModel = viewModel)
        }
    }

    /**
     * Called when a new intent is delivered to an already-running Activity.
     * This happens for USB_DEVICE_ATTACHED when launchMode is singleTask.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        // Register for USB detach events
        registerReceiver(
            usbDetachReceiver,
            IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED),
            RECEIVER_NOT_EXPORTED
        )

        // Check if a supported DAC is already connected
        checkConnectedDevices()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(usbDetachReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was not registered — safe to ignore
        }
    }

    /**
     * Handles a USB_DEVICE_ATTACHED intent by extracting the device
     * and notifying the ViewModel.
     */
    private fun handleUsbIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device = getUsbDeviceFromIntent(intent)
            if (device != null) {
                val app = application as DacTunerApplication
                app.diagnosticsLogger.log(
                    "ACTIVITY",
                    "USB_DEVICE_ATTACHED intent: VID=0x${String.format("%04X", device.vendorId)} " +
                            "PID=0x${String.format("%04X", device.productId)}"
                )
                viewModel.onDacConnected(
                    vendorId = device.vendorId,
                    productId = device.productId,
                    deviceName = device.deviceName
                )
            }
        }
    }

    /**
     * Scans currently connected USB devices and updates the ViewModel.
     * Called in onResume and after USB detach events.
     */
    private fun checkConnectedDevices() {
        val app = application as DacTunerApplication
        val connectedDevices = app.usbDeviceManager.getConnectedDevices()
        var foundSupportedDevice = false

        for (device in connectedDevices.values) {
            val profile = app.dacIdentifier.identify(device.vendorId, device.productId)
            if (profile != null) {
                viewModel.onDacConnected(
                    vendorId = device.vendorId,
                    productId = device.productId,
                    deviceName = device.deviceName
                )
                foundSupportedDevice = true
                break
            }
        }

        if (!foundSupportedDevice &&
            viewModel.uiState.value.connectionStatus != ConnectionStatus.DISCONNECTED
        ) {
            viewModel.onDacDisconnected()
        }
    }

    /**
     * Extracts UsbDevice from intent, handling API level differences
     * for getParcelableExtra deprecation.
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
