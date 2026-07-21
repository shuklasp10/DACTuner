package com.dactuner.ui

import com.dactuner.core.AdapterVariant

/**
 * Immutable UI state for the main screen.
 *
 * The entire UI is a function of this state — no mutable state in composables.
 * Updated via [MainViewModel] using copy() for immutable state transitions.
 */
data class UiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val configurationStatus: ConfigurationStatus = ConfigurationStatus.IDLE,
    val deviceInfo: DeviceInfo? = null,
    val warnings: List<Warning> = emptyList(),
    val lastConfiguredTimestamp: Long? = null,
    val settings: AppSettings = AppSettings()
) {
    companion object {
        /** Creates a default UiState with all fields at their initial values. */
        fun default() = UiState()
    }
}

/**
 * Connection status of the supported DAC device.
 */
enum class ConnectionStatus {
    /** No supported DAC is connected. */
    DISCONNECTED,

    /** A supported DAC is connected but not yet configured. */
    CONNECTED,

    /** The DAC has been successfully configured. */
    CONFIGURED,

    /** Configuration was attempted but failed. */
    FAILED
}

/**
 * Status of the DAC configuration operation.
 */
enum class ConfigurationStatus {
    /** No configuration in progress. */
    IDLE,

    /** Configuration is currently running. */
    CONFIGURING,

    /** Configuration completed successfully. */
    SUCCESS,

    /** Configuration partially succeeded (e.g., HAL race detected). */
    PARTIAL_SUCCESS,

    /** Configuration failed. */
    FAILED
}

/**
 * Information about the connected DAC device for display in the UI.
 *
 * All fields are pre-formatted strings ready for display.
 */
data class DeviceInfo(
    val name: String,
    val manufacturer: String,
    val vidPid: String,
    val uacVersion: String,
    val featureUnitId: String = "\u2014",
    val volumeCurrent: String = "\u2014",
    val volumeMax: String = "\u2014",
    val volumeDb: String = "\u2014",
    val variant: AdapterVariant = AdapterVariant.UNKNOWN
)

/**
 * Warning types that can be displayed as banners in the UI.
 */
sealed class Warning {
    abstract val message: String

    /** EU adapter variant detected — informational about hardware volume cap. */
    data class EuAdapter(override val message: String) : Warning()

    /** Samsung Media Volume Limit is enabled — actionable warning. */
    data class SamsungVolumeLimit(override val message: String) : Warning()

    /** Configuration was only partially successful. */
    data class ConfigPartialSuccess(override val message: String) : Warning()
    
    /** General error for displaying error messages in UI. */
    data class GeneralError(override val message: String) : Warning()
}

/**
 * User-configurable app settings.
 */
data class AppSettings(
    /** Auto-configure DAC on USB connection. */
    val autoConfigureEnabled: Boolean = true,

    /** Show transient notifications on configuration events. */
    val showNotifications: Boolean = true,

    /** Enable verbose debug logging and descriptor dumps. */
    val debugModeEnabled: Boolean = false
)
