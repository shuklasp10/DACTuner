package com.dactuner.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DeviceProfileRegistry].
 *
 * Verifies the registry contains the correct profiles and lookup behavior.
 */
class DeviceProfileRegistryTest {

    private lateinit var registry: DeviceProfileRegistry

    @BeforeEach
    fun setUp() {
        registry = DeviceProfileRegistry()
    }

    @Test
    @DisplayName("Registry contains exactly one profile for v1.0")
    fun `getAllProfiles returns one profile`() {
        val profiles = registry.getAllProfiles()

        assertEquals(1, profiles.size)
    }

    @Test
    @DisplayName("Registry contains the Apple USB-C DAC profile")
    fun `getProfile returns Apple DAC profile`() {
        val profile = registry.getProfile(
            vendorId = DeviceProfileRegistry.APPLE_VID,
            productId = DeviceProfileRegistry.APPLE_PID
        )

        assertNotNull(profile)
        assertEquals("Apple USB-C to 3.5mm Adapter", profile!!.name)
        assertEquals("Apple, Inc.", profile.manufacturer)
    }

    @Test
    @DisplayName("Apple VID constant is correct (0x05AC)")
    fun `APPLE_VID is correct hex value`() {
        assertEquals(0x05AC, DeviceProfileRegistry.APPLE_VID)
        assertEquals(1452, DeviceProfileRegistry.APPLE_VID)
    }

    @Test
    @DisplayName("Apple PID constant is correct (0x110A)")
    fun `APPLE_PID is correct hex value`() {
        assertEquals(0x110A, DeviceProfileRegistry.APPLE_PID)
        assertEquals(4362, DeviceProfileRegistry.APPLE_PID)
    }

    @Test
    @DisplayName("Unknown VID/PID returns null")
    fun `getProfile returns null for unknown VID PID`() {
        val profile = registry.getProfile(vendorId = 0x1234, productId = 0x5678)

        assertNull(profile)
    }

    @Test
    @DisplayName("Apple profile uses UAC 2.0")
    fun `Apple profile has UAC 2_0`() {
        val profile = registry.getProfile(DeviceProfileRegistry.APPLE_VID, DeviceProfileRegistry.APPLE_PID)

        assertEquals(UacVersion.UAC_2_0, profile!!.uacVersion)
    }

    @Test
    @DisplayName("Apple profile has null knownFeatureUnitId (discovered at runtime)")
    fun `Apple profile discovers feature unit at runtime`() {
        val profile = registry.getProfile(DeviceProfileRegistry.APPLE_VID, DeviceProfileRegistry.APPLE_PID)

        assertNull(profile!!.knownFeatureUnitId)
    }

    @Test
    @DisplayName("Apple profile has null knownMaxVolume (discovered at runtime)")
    fun `Apple profile discovers max volume at runtime`() {
        val profile = registry.getProfile(DeviceProfileRegistry.APPLE_VID, DeviceProfileRegistry.APPLE_PID)

        assertNull(profile!!.knownMaxVolume)
    }

    @Test
    @DisplayName("getAllProfiles returns a defensive copy")
    fun `getAllProfiles returns copy not internal list`() {
        val profiles1 = registry.getAllProfiles()
        val profiles2 = registry.getAllProfiles()

        // Should be equal in content
        assertEquals(profiles1, profiles2)
        // But not the same list instance (defensive copy)
        assertTrue(profiles1 !== profiles2)
    }
}
