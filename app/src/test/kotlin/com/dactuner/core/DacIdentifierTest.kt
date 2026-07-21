package com.dactuner.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DacIdentifier].
 *
 * Tests VID/PID matching against the [DeviceProfileRegistry].
 * Uses raw integer VID/PID values to avoid Android framework dependencies.
 */
class DacIdentifierTest {

    private lateinit var registry: DeviceProfileRegistry
    private lateinit var identifier: DacIdentifier

    @BeforeEach
    fun setUp() {
        registry = DeviceProfileRegistry()
        identifier = DacIdentifier(registry)
    }

    @Test
    @DisplayName("Apple DAC VID/PID returns matching profile")
    fun `identify returns profile for Apple DAC`() {
        val profile = identifier.identify(
            vendorId = 0x05AC,
            productId = 0x110A
        )

        assertNotNull(profile)
        assertEquals("Apple USB-C to 3.5mm Adapter", profile!!.name)
        assertEquals("Apple, Inc.", profile.manufacturer)
        assertEquals(0x05AC, profile.vendorId)
        assertEquals(0x110A, profile.productId)
    }

    @Test
    @DisplayName("Apple DAC with decimal VID/PID values returns matching profile")
    fun `identify returns profile for Apple DAC using decimal values`() {
        // VID 0x05AC = 1452, PID 0x110A = 4362
        val profile = identifier.identify(
            vendorId = 1452,
            productId = 4362
        )

        assertNotNull(profile)
        assertEquals("Apple USB-C to 3.5mm Adapter", profile!!.name)
    }

    @Test
    @DisplayName("Unknown VID/PID returns null")
    fun `identify returns null for unknown device`() {
        val profile = identifier.identify(
            vendorId = 0x1234,
            productId = 0x5678
        )

        assertNull(profile)
    }

    @Test
    @DisplayName("Correct VID with wrong PID returns null")
    fun `identify returns null for Apple VID with wrong PID`() {
        val profile = identifier.identify(
            vendorId = 0x05AC,
            productId = 0x0000
        )

        assertNull(profile)
    }

    @Test
    @DisplayName("Wrong VID with correct PID returns null")
    fun `identify returns null for wrong VID with Apple PID`() {
        val profile = identifier.identify(
            vendorId = 0x0000,
            productId = 0x110A
        )

        assertNull(profile)
    }

    @Test
    @DisplayName("Identified profile has correct UAC version")
    fun `profile has UAC 2_0 version`() {
        val profile = identifier.identify(0x05AC, 0x110A)

        assertNotNull(profile)
        assertEquals(UacVersion.UAC_2_0, profile!!.uacVersion)
    }

    @Test
    @DisplayName("Identified profile lists both adapter variants")
    fun `profile contains both US and EU variants`() {
        val profile = identifier.identify(0x05AC, 0x110A)

        assertNotNull(profile)
        assertEquals(2, profile!!.variants.size)
        assert(AdapterVariant.US_A2049 in profile.variants)
        assert(AdapterVariant.EU_A2155 in profile.variants)
    }
}
