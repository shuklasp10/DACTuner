package com.dactuner

import android.app.Application
import com.dactuner.core.DacIdentifier
import com.dactuner.core.DeviceProfileRegistry
import com.dactuner.usb.UsbDeviceManager
import com.dactuner.util.DiagnosticsLogger
import com.dactuner.util.NotificationHelper
import com.dactuner.util.PreferencesManager

/**
 * Application class for DACTuner.
 *
 * Initializes and provides access to application-scoped singleton components.
 * Uses manual dependency injection for v1 simplicity — all components are
 * created here and accessed via [instance].
 *
 * No DI framework (Hilt/Dagger) is used to minimize the dependency footprint.
 */
class DacTunerApplication : Application() {

    /** In-memory diagnostics logger. */
    lateinit var diagnosticsLogger: DiagnosticsLogger
        private set

    /** SharedPreferences wrapper for app settings. */
    lateinit var preferencesManager: PreferencesManager
        private set

    /** Notification manager for configuration events. */
    lateinit var notificationHelper: NotificationHelper
        private set

    /** Registry of known DAC device profiles. */
    lateinit var deviceProfileRegistry: DeviceProfileRegistry
        private set

    /** Identifies supported DAC devices by VID/PID. */
    lateinit var dacIdentifier: DacIdentifier
        private set

    /** Manages USB device connections and permissions. */
    lateinit var usbDeviceManager: UsbDeviceManager
        private set

    /** Caches the successful USB claim phase. */
    lateinit var phaseCache: com.dactuner.reliability.PhaseCache
        private set

    /** Strategy for claiming USB interfaces. */
    lateinit var interfaceClaimStrategy: com.dactuner.usb.InterfaceClaimStrategy
        private set

    /** Executes UAC control transfers. */
    lateinit var controlTransferExecutor: com.dactuner.usb.UacControlTransferExecutor
        private set

    /** Mitigates HAL race conditions when setting volume. */
    lateinit var halRaceMitigator: com.dactuner.reliability.HalRaceMitigator
        private set

    /** Orchestrates the DAC configuration process. */
    lateinit var configurationOrchestrator: com.dactuner.core.ConfigurationOrchestrator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize utility layer (no upward dependencies)
        diagnosticsLogger = DiagnosticsLogger()
        preferencesManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        notificationHelper.initialize()

        // Initialize core layer
        deviceProfileRegistry = DeviceProfileRegistry()
        dacIdentifier = DacIdentifier(deviceProfileRegistry)

        // Initialize reliability layer
        phaseCache = com.dactuner.reliability.PhaseCache(preferencesManager)

        // Initialize USB layer
        usbDeviceManager = UsbDeviceManager(this, diagnosticsLogger)
        interfaceClaimStrategy = com.dactuner.usb.InterfaceClaimStrategy(phaseCache, diagnosticsLogger)
        controlTransferExecutor = com.dactuner.usb.UacControlTransferExecutor(diagnosticsLogger)

        // Initialize orchestrator
        halRaceMitigator = com.dactuner.reliability.HalRaceMitigator(controlTransferExecutor, interfaceClaimStrategy, diagnosticsLogger)
        
        configurationOrchestrator = com.dactuner.core.ConfigurationOrchestrator(
            dacIdentifier,
            usbDeviceManager,
            interfaceClaimStrategy,
            controlTransferExecutor,
            halRaceMitigator,
            diagnosticsLogger
        )

        diagnosticsLogger.log("APP", "DACTuner application initialized")
    }

    companion object {
        /**
         * Singleton application instance.
         * Available after [onCreate] has been called.
         */
        lateinit var instance: DacTunerApplication
            private set
    }
}
