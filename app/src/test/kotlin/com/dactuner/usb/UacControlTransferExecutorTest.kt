package com.dactuner.usb

import android.hardware.usb.UsbDeviceConnection
import com.dactuner.util.DiagnosticsLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UacControlTransferExecutorTest {

    private lateinit var logger: DiagnosticsLogger
    private lateinit var connection: UsbDeviceConnection
    private lateinit var executor: UacControlTransferExecutor

    @BeforeEach
    fun setup() {
        logger = mockk(relaxed = true)
        connection = mockk(relaxed = true)
        executor = UacControlTransferExecutor(logger)
    }

    @Test
    fun `setVolume constructs correct payload and parameters`() {
        val featureUnitId = 2
        val interfaceNumber = 1
        val channel = 0
        val volume = 0x0123 // 291 in decimal

        every { 
            connection.controlTransfer(any(), any(), any(), any(), any(), any(), any())
        } returns 2

        val success = executor.setVolume(connection, featureUnitId, interfaceNumber, channel, volume)

        assertTrue(success)

        verify {
            connection.controlTransfer(
                0x21, // REQ_TYPE_SET
                0x01, // REQ_SET_CUR
                (0x02 shl 8) or channel, // wValue = (CS_VOLUME << 8) | channel
                (featureUnitId shl 8) or interfaceNumber, // wIndex = (featureUnitId << 8) | interfaceNumber
                match { data -> data[0] == 0x23.toByte() && data[1] == 0x01.toByte() }, // Little endian 0x0123
                2,
                1000
            )
        }
    }
}
