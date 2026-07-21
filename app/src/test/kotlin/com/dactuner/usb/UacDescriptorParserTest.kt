package com.dactuner.usb

import com.dactuner.util.DiagnosticsLogger
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UacDescriptorParserTest {

    private lateinit var logger: DiagnosticsLogger
    private lateinit var parser: UacDescriptorParser

    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        parser = UacDescriptorParser(logger)
    }

    @Test
    fun `parseBytes extracts UAC 1_0 descriptors correctly`() {
        // Simulated raw descriptors for an Apple USB-C DAC
        // Contains Interface 1 (AudioControl), Header (UAC 1.0), and Feature Unit with Volume
        val rawData = byteArrayOf(
            // INTERFACE descriptor (Length 9, Type 4, Interface 1, Alt 0, NumEndpoints 0, Class 1, Subclass 1)
            0x09, 0x04, 0x01, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00,
            // CS_INTERFACE HEADER (Length 10, Type 0x24, Subtype 1, bcdADC 0x0100)
            0x0A, 0x24.toByte(), 0x01, 0x00, 0x01, 0x27, 0x00, 0x01, 0x01, 0x02,
            // CS_INTERFACE FEATURE_UNIT (Length 9, Type 0x24, Subtype 6, UnitId 2, SourceId 1, ControlSize 1, bmaControls[0]=0x03)
            0x09, 0x24.toByte(), 0x06, 0x02, 0x01, 0x01, 0x03, 0x00, 0x00
        )

        val result = parser.parseBytes(rawData)

        assertEquals(UacVersion.UAC_1_0, result.uacVersion)
        assertEquals(1, result.audioControlInterfaceNumber)
        assertEquals(1, result.featureUnits.size)

        val featureUnit = result.featureUnits[0]
        assertEquals(2, featureUnit.unitId)
        assertTrue(featureUnit.controls.contains(FeatureControl.MUTE))
        assertTrue(featureUnit.controls.contains(FeatureControl.VOLUME))
    }

    @Test
    fun `parseBytes handles empty descriptors safely`() {
        val result = parser.parseBytes(ByteArray(0))
        
        assertEquals(UacVersion.UAC_1_0, result.uacVersion)
        assertEquals(-1, result.audioControlInterfaceNumber)
        assertTrue(result.featureUnits.isEmpty())
    }
}
