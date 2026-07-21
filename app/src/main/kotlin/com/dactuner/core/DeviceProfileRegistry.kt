package com.dactuner.core

/**
 * Registry of known DAC device profiles.
 *
 * For v1.0, contains a single hardcoded entry for the Apple USB-C to 3.5mm adapter.
 * The architecture supports extending this to a JSON file or database for multi-DAC
 * support in future versions.
 */
class DeviceProfileRegistry {

    private val profiles: List<DacProfile> = listOf(
        DacProfile(
            vendorId = APPLE_VID,
            productId = APPLE_PID,
            name = "Apple USB-C to 3.5mm Adapter",
            manufacturer = "Apple, Inc.",
            uacVersion = UacVersion.UAC_2_0,
            knownFeatureUnitId = null,   // Discovered at runtime via descriptor parsing
            knownMaxVolume = null,       // Discovered at runtime via GET_MAX
            variants = listOf(AdapterVariant.US_A2049, AdapterVariant.EU_A2155)
        )
    )

    /**
     * Looks up a DAC profile by USB vendor ID and product ID.
     *
     * @param vendorId USB Vendor ID
     * @param productId USB Product ID
     * @return Matching [DacProfile] or null if the device is not supported
     */
    fun getProfile(vendorId: Int, productId: Int): DacProfile? {
        return profiles.find { it.vendorId == vendorId && it.productId == productId }
    }

    /**
     * Returns all registered DAC profiles.
     */
    fun getAllProfiles(): List<DacProfile> = profiles.toList()

    companion object {
        /** Apple, Inc. USB Vendor ID */
        const val APPLE_VID = 0x05AC  // 1452 decimal

        /** Apple USB-C to 3.5mm Adapter Product ID */
        const val APPLE_PID = 0x110A  // 4362 decimal
    }
}

/**
 * Profile data for a known USB DAC device.
 *
 * @property vendorId USB Vendor ID (e.g., 0x05AC for Apple)
 * @property productId USB Product ID (e.g., 0x110A for Apple DAC)
 * @property name Human-readable device name
 * @property manufacturer Device manufacturer name
 * @property uacVersion USB Audio Class version supported by the device
 * @property knownFeatureUnitId Hint for the Feature Unit ID; null means discover at runtime
 * @property knownMaxVolume Hint for the maximum volume value; null means discover at runtime
 * @property variants Known hardware variants of this device
 */
data class DacProfile(
    val vendorId: Int,
    val productId: Int,
    val name: String,
    val manufacturer: String,
    val uacVersion: UacVersion,
    val knownFeatureUnitId: Int?,
    val knownMaxVolume: Int?,
    val variants: List<AdapterVariant>
)

/**
 * USB Audio Class version.
 */
enum class UacVersion {
    /** USB Audio Class 1.0 */
    UAC_1_0,

    /** USB Audio Class 2.0 */
    UAC_2_0;

    /** Returns a human-readable display name (e.g., "UAC 2.0"). */
    fun displayName(): String = name.replace("_", " ").replace("UAC ", "UAC ")
}

/**
 * Known adapter hardware variants.
 *
 * The Apple USB-C to 3.5mm adapter comes in two models:
 * - US model (A2049): 1.0 Vrms max output
 * - EU model (A2155): 0.5 Vrms max output (hardware-capped for EU safety regulations)
 */
enum class AdapterVariant {
    /** US model A2049 — max output 1.0 Vrms */
    US_A2049,

    /** EU model A2155 — max output 0.5 Vrms (hardware-capped) */
    EU_A2155,

    /** Variant could not be reliably determined */
    UNKNOWN
}
