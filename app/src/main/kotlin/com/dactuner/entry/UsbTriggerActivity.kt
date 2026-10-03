package com.dactuner.entry

import android.app.Activity
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.dactuner.DacTunerApplication
import com.dactuner.core.ConfigurationResult
import com.dactuner.util.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Headless, transparent Activity that handles [UsbManager.ACTION_USB_DEVICE_ATTACHED] events.
 *
 * This Activity is launched by the Android system when a matching USB DAC is plugged in.
 * It uses a completely transparent theme, draws no visual UI, configures the DAC hardware
 * volume in the background, posts a confirmation notification, and immediately finishes.
 *
 * This delivers a completely seamless experience where the user's active application
 * (e.g., Spotify, YouTube, games) remains visible and uninterrupted.
 */
class UsbTriggerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableTransitions()

        val app = application as DacTunerApplication
        val logger = app.diagnosticsLogger
        logger.log("USB_TRIGGER", "UsbTriggerActivity launched via USB event")

        val device = getUsbDeviceFromIntent(intent)
        if (device == null) {
            logger.log("USB_TRIGGER", "No UsbDevice found in intent, finishing", LogLevel.WARNING)
            finishAndRemoveTask()
            disableTransitions()
            return
        }

        val profile = app.dacIdentifier.identify(device.vendorId, device.productId)
        if (profile == null) {
            logger.log(
                "USB_TRIGGER",
                "Connected device is not a supported DAC (VID=0x${String.format("%04X", device.vendorId)} PID=0x${String.format("%04X", device.productId)}), finishing",
                LogLevel.WARNING
            )
            finishAndRemoveTask()
            disableTransitions()
            return
        }

        // Check if this attach is an expected re-enumeration following a reset
        val isExpectedReset = lastConfiguredDevice == (device.vendorId to device.productId) &&
            System.currentTimeMillis() - lastConfiguredTime < RESET_GRACE_PERIOD_MS

        if (isExpectedReset) {
            logger.log("USB_TRIGGER", "Ignoring re-attach event for ${profile.name} during post-configure reset grace period")
            finishAndRemoveTask()
            disableTransitions()
            return
        }

        val prefs = app.preferencesManager

        // If background mode is disabled by user, forward to MainActivity for full UI
        if (!prefs.backgroundModeEnabled) {
            logger.log("USB_TRIGGER", "Background mode disabled in settings — launching MainActivity")
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                action = intent.action
                putExtras(intent)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(mainIntent)
            finish()
            disableTransitions()
            return
        }

        // If auto-configure is disabled, do nothing
        if (!prefs.autoConfigureEnabled) {
            logger.log("USB_TRIGGER", "Auto-configure disabled in settings, finishing")
            finishAndRemoveTask()
            disableTransitions()
            return
        }

        // Execute background configuration with safety timeout
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(EXECUTION_TIMEOUT_MS) {
                    val isMaximized = app.configurationOrchestrator.checkIsVolumeMaximized(device)
                    if (isMaximized) {
                        logger.log("USB_TRIGGER", "DAC is already at maximum volume. Showing notification.")
                        lastConfiguredDevice = device.vendorId to device.productId
                        lastConfiguredTime = System.currentTimeMillis()
                        app.notificationHelper.showConfigSuccess(profile.name)
                    } else {
                        logger.log("USB_TRIGGER", "Configuring DAC hardware volume in background...")
                        val result = app.configurationOrchestrator.configureIfSupported(device)
                        when (result) {
                            is ConfigurationResult.Success -> {
                                logger.log("USB_TRIGGER", "DAC successfully configured in background!")
                                lastConfiguredDevice = device.vendorId to device.productId
                                lastConfiguredTime = System.currentTimeMillis()
                                app.notificationHelper.showConfigSuccess(profile.name)
                            }
                            is ConfigurationResult.PartialSuccess -> {
                                logger.log("USB_TRIGGER", "DAC partially configured: ${result.reason}", LogLevel.WARNING)
                                lastConfiguredDevice = device.vendorId to device.productId
                                lastConfiguredTime = System.currentTimeMillis()
                                app.notificationHelper.showConfigSuccess(profile.name)
                            }
                            is ConfigurationResult.Failure -> {
                                logger.log("USB_TRIGGER", "DAC configuration failed: ${result.error.userMessage}", LogLevel.ERROR)
                                app.notificationHelper.showConfigFailure(result.error.userMessage)
                            }
                        }
                    }
                } ?: run {
                    logger.log("USB_TRIGGER", "Background configuration timed out after ${EXECUTION_TIMEOUT_MS}ms", LogLevel.ERROR)
                }
            } catch (e: Exception) {
                logger.log("USB_TRIGGER", "Exception during background configuration: ${e.message}", LogLevel.ERROR)
            } finally {
                withContext(Dispatchers.Main) {
                    finishAndRemoveTask()
                    disableTransitions()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handled via singleTask; re-trigger onCreate flow if delivered
        setIntent(intent)
    }

    /**
     * Suppresses window animations to eliminate visual flicker.
     */
    @Suppress("DEPRECATION")
    private fun disableTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    /**
     * Extracts the [UsbDevice] from the intent with backwards-compatible API handling.
     */
    @Suppress("DEPRECATION")
    private fun getUsbDeviceFromIntent(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    companion object {
        /** Maximum time to wait for configuration before self-terminating. */
        private const val EXECUTION_TIMEOUT_MS = 6000L

        /** Grace period for ignoring secondary USB re-attaches after a self-triggered reset. */
        private const val RESET_GRACE_PERIOD_MS = 6000L

        /** Timestamp of the last successful background configuration in epoch millis. */
        @Volatile
        private var lastConfiguredTime: Long = 0L

        /** VID and PID of the last configured device. */
        @Volatile
        private var lastConfiguredDevice: Pair<Int, Int>? = null
    }
}
