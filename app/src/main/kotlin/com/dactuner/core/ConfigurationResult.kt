package com.dactuner.core

/**
 * Result of a DAC configuration attempt.
 *
 * Represents the outcome of the end-to-end configuration flow:
 * open device → parse descriptors → set volume → verify → close.
 *
 * Phase 1: Stub with basic success/failure types.
 * Full implementation with variant detection and warnings in Phase 3-4.
 */
sealed class ConfigurationResult {

    /**
     * Configuration completed successfully.
     * Volume was set to the target value and verified.
     *
     * @property volumeSet The volume value that was set (raw 16-bit value)
     * @property volumeMax The maximum volume value reported by the device
     * @property phase The claim phase that was successful
     */
    data class Success(
        val volumeSet: Int,
        val volumeMax: Int,
        val phase: com.dactuner.usb.ClaimPhase? = null
    ) : ConfigurationResult()

    /**
     * Configuration partially succeeded.
     * Volume was set but could not be verified, or was reset by the HAL.
     *
     * @property volumeSet The volume value that was last set
     * @property volumeMax The maximum volume value reported by the device
     * @property reason Human-readable explanation of why it was only partial
     * @property phase The claim phase that was successful
     */
    data class PartialSuccess(
        val volumeSet: Int,
        val volumeMax: Int,
        val reason: String,
        val phase: com.dactuner.usb.ClaimPhase? = null
    ) : ConfigurationResult()

    /**
     * Configuration failed entirely.
     *
     * @property error The specific error that caused the failure
     * @property phase The claim phase that failed
     */
    data class Failure(
        val error: ConfigurationError,
        val phase: com.dactuner.usb.ClaimPhase? = null
    ) : ConfigurationResult()
}

/**
 * Specific error types for configuration failures.
 *
 * Classified into recoverable (user action can fix), degraded (partially working),
 * and fatal (cannot configure) categories.
 */
sealed class ConfigurationError {

    // --- Recoverable: user action can fix ---

    /** User denied USB permission. */
    data object PermissionDenied : ConfigurationError()

    /** The USB device was not found or was disconnected. */
    data object DeviceNotFound : ConfigurationError()

    /** The USB device is busy (another app has claimed it). */
    data object DeviceBusy : ConfigurationError()

    // --- Fatal: cannot configure ---

    /** Failed to parse USB audio descriptors. */
    data object DescriptorParseFailure : ConfigurationError()

    /** No Feature Unit with volume control was found in the descriptors. */
    data object NoFeatureUnitFound : ConfigurationError()

    /** All interface claiming phases failed. */
    data object AllPhasesFailed : ConfigurationError()

    /**
     * A USB control transfer returned an error.
     *
     * @property errorCode The negative error code from controlTransfer()
     */
    data class ControlTransferFailed(val errorCode: Int) : ConfigurationError()

    /**
     * An unexpected exception occurred during configuration.
     *
     * @property throwable The exception that was caught
     */
    data class UnexpectedException(val throwable: Throwable) : ConfigurationError()
}
