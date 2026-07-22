package com.dactuner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dactuner.DacTunerApplication
import com.dactuner.util.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen.
 *
 * Manages [UiState] as a [StateFlow] for the Compose UI to observe.
 * Coordinates between USB events (from Activity/Receiver) and UI state.
 *
 * Phase 1: Handles connection/disconnection state only.
 * Phase 2+: Will coordinate full configuration flow via ConfigurationOrchestrator.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DacTunerApplication
    private val dacIdentifier = app.dacIdentifier
    private val logger = app.diagnosticsLogger

    private var expectedResetVidPid: Pair<Int, Int>? = null
    private var expectedResetUntil: Long = 0L

    companion object {
        // Observed reconnect took ~2.8s on the S21 FE; leave margin.
        private const val RESET_GRACE_PERIOD_MS = 6000L
    }

    private val _uiState = MutableStateFlow(UiState.default())

    /** Observable UI state for the Compose UI. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Called when a supported DAC device is connected.
     *
     * Identifies the device via [DacIdentifier] and updates the UI state
     * to show the device info and CONNECTED status.
     *
     * @param vendorId USB Vendor ID of the connected device
     * @param productId USB Product ID of the connected device
     * @param deviceName Optional device name string from Android USB subsystem
     */
    fun onDacConnected(vendorId: Int, productId: Int, deviceName: String?) {
        val profile = dacIdentifier.identify(vendorId, productId)
        if (profile != null) {
            val isExpectedReset = expectedResetVidPid == (vendorId to productId) &&
                System.currentTimeMillis() < expectedResetUntil

            logger.log(
                "VIEWMODEL",
                "Supported DAC detected: ${profile.name} (${String.format("%04X:%04X", profile.vendorId, profile.productId)})" +
                    if (isExpectedReset) " — resuming after self-triggered reset" else ""
            )

            _uiState.update { state ->
                state.copy(
                    connectionStatus = if (isExpectedReset) ConnectionStatus.CONFIGURED else ConnectionStatus.CONNECTED,
                    deviceInfo = DeviceInfo(
                        name = profile.name,
                        manufacturer = profile.manufacturer,
                        vidPid = String.format("%04X:%04X", profile.vendorId, profile.productId),
                        uacVersion = profile.uacVersion.displayName()
                    )
                )
            }

            if (isExpectedReset) {
                expectedResetVidPid = null
                expectedResetUntil = 0L
            }
        } else {
            logger.log(
                "VIEWMODEL",
                "Unsupported USB device ignored: " +
                        String.format("%04X:%04X", vendorId, productId),
                LogLevel.WARNING
            )
        }
    }

    /**
     * Called when the supported DAC device is disconnected.
     *
     * Resets the UI state to DISCONNECTED with no device info.
     */
    fun onDacDisconnected(vendorId: Int? = null, productId: Int? = null) {
        val isExpectedReset = vendorId != null && productId != null &&
            expectedResetVidPid == (vendorId to productId) &&
            System.currentTimeMillis() < expectedResetUntil

        if (isExpectedReset) {
            logger.log("VIEWMODEL", "DAC disconnected as part of an expected post-configure reset — preserving state")
            return // don't wipe deviceInfo/warnings/status; wait for the reattach
        }

        logger.log("VIEWMODEL", "DAC disconnected")
        _uiState.update { state ->
            state.copy(
                connectionStatus = ConnectionStatus.DISCONNECTED,
                configurationStatus = ConfigurationStatus.IDLE,
                deviceInfo = null,
                warnings = emptyList()
            )
        }
    }

    /**
     * Triggers manual configuration of the currently connected DAC.
     */
    fun configureConnectedDac() {
        val state = _uiState.value
        if (state.connectionStatus != ConnectionStatus.CONNECTED) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(configurationStatus = ConfigurationStatus.CONFIGURING) }
            
            val connectedDevices = app.usbDeviceManager.getConnectedDevices()
            val device = connectedDevices.values.find { 
                String.format("%04X:%04X", it.vendorId, it.productId) == state.deviceInfo?.vidPid 
            }
            
            if (device != null) {
                val result = app.configurationOrchestrator.configureIfSupported(device)
                
                _uiState.update { currentState ->
                    when (result) {
                        is com.dactuner.core.ConfigurationResult.Success -> {
                            if (result.phase == com.dactuner.usb.ClaimPhase.PHASE_3_FORCE_CLAIM) {
                                expectedResetVidPid = device.vendorId to device.productId
                                expectedResetUntil = System.currentTimeMillis() + RESET_GRACE_PERIOD_MS
                            }
                            
                            val replugWarning = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && 
                                result.phase == com.dactuner.usb.ClaimPhase.PHASE_3_FORCE_CLAIM) {
                                listOf(Warning.ReplugRequired("Unplug and replug the adapter to hear audio (required on this Android version)."))
                            } else {
                                emptyList()
                            }
                            
                            currentState.copy(
                                connectionStatus = ConnectionStatus.CONFIGURED,
                                configurationStatus = ConfigurationStatus.SUCCESS,
                                warnings = replugWarning,
                                lastConfiguredTimestamp = System.currentTimeMillis()
                            )
                        }
                        is com.dactuner.core.ConfigurationResult.PartialSuccess -> {
                            currentState.copy(
                                connectionStatus = ConnectionStatus.CONFIGURED,
                                configurationStatus = ConfigurationStatus.PARTIAL_SUCCESS,
                                warnings = listOf(Warning.ConfigPartialSuccess(result.reason)),
                                lastConfiguredTimestamp = System.currentTimeMillis()
                            )
                        }
                        is com.dactuner.core.ConfigurationResult.Failure -> {
                            currentState.copy(
                                configurationStatus = ConfigurationStatus.FAILED,
                                warnings = listOf(Warning.GeneralError("Configuration failed: ${result.error}"))
                            )
                        }
                    }
                }
            } else {
                 _uiState.update { it.copy(
                    configurationStatus = ConfigurationStatus.FAILED,
                    warnings = listOf(Warning.GeneralError("Device not found"))
                )}
            }
        }
    }
}
