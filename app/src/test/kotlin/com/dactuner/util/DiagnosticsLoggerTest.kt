package com.dactuner.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DiagnosticsLogger].
 *
 * Tests the in-memory ring buffer, log entry creation, export, and clear functionality.
 * Does not test logcat output (android.util.Log) since that is not available in unit tests.
 */
class DiagnosticsLoggerTest {

    private lateinit var logger: DiagnosticsLogger

    @BeforeEach
    fun setUp() {
        logger = DiagnosticsLogger()
    }

    @Test
    @DisplayName("Log entries are stored and retrievable")
    fun `log stores entries`() {
        logger.log("TEST", "Hello")
        logger.log("TEST", "World")

        val entries = logger.getLogEntries()
        assertEquals(2, entries.size)
        assertEquals("Hello", entries[0].message)
        assertEquals("World", entries[1].message)
    }

    @Test
    @DisplayName("Log entries have correct tag and level")
    fun `log stores tag and level correctly`() {
        logger.log("USB", "Connected", LogLevel.INFO)
        logger.log("ERROR", "Failed", LogLevel.ERROR)

        val entries = logger.getLogEntries()
        assertEquals("USB", entries[0].tag)
        assertEquals(LogLevel.INFO, entries[0].level)
        assertEquals("ERROR", entries[1].tag)
        assertEquals(LogLevel.ERROR, entries[1].level)
    }

    @Test
    @DisplayName("Default log level is INFO")
    fun `log defaults to INFO level`() {
        logger.log("TEST", "message")

        assertEquals(LogLevel.INFO, logger.getLogEntries().first().level)
    }

    @Test
    @DisplayName("Log entries have timestamps")
    fun `log entries have non-zero timestamps`() {
        logger.log("TEST", "message")

        assertTrue(logger.getLogEntries().first().timestamp > 0)
    }

    @Test
    @DisplayName("Ring buffer enforces MAX_ENTRIES limit")
    fun `ring buffer size is limited to MAX_ENTRIES`() {
        // Add more than MAX_ENTRIES
        repeat(DiagnosticsLogger.MAX_ENTRIES + 100) { i ->
            logger.log("TEST", "Entry $i")
        }

        val entries = logger.getLogEntries()
        assertEquals(DiagnosticsLogger.MAX_ENTRIES, entries.size)
    }

    @Test
    @DisplayName("Ring buffer drops oldest entries when full")
    fun `ring buffer drops oldest entries`() {
        repeat(DiagnosticsLogger.MAX_ENTRIES + 5) { i ->
            logger.log("TEST", "Entry $i")
        }

        val entries = logger.getLogEntries()
        // The first entry should be "Entry 5" (oldest 5 were dropped)
        assertEquals("Entry 5", entries.first().message)
        // The last entry should be the most recent
        assertEquals("Entry ${DiagnosticsLogger.MAX_ENTRIES + 4}", entries.last().message)
    }

    @Test
    @DisplayName("logUsbOperation creates formatted entry")
    fun `logUsbOperation formats correctly`() {
        logger.logUsbOperation("openDevice", "success", 42)

        val entry = logger.getLogEntries().first()
        assertEquals("USB", entry.tag)
        assertTrue(entry.message.contains("openDevice"))
        assertTrue(entry.message.contains("success"))
        assertTrue(entry.message.contains("42ms"))
    }

    @Test
    @DisplayName("logControlTransfer creates formatted entry")
    fun `logControlTransfer formats correctly`() {
        logger.logControlTransfer(
            direction = "OUT",
            request = "SET_CUR",
            value = 0x0200,
            index = 0x0A00,
            data = byteArrayOf(0x00, 0x78),
            result = 2
        )

        val entry = logger.getLogEntries().first()
        assertEquals("CTRL_TRANSFER", entry.tag)
        assertTrue(entry.message.contains("SET_CUR"))
        assertTrue(entry.message.contains("0200"))
        assertTrue(entry.message.contains("00 78"))
        assertEquals(LogLevel.INFO, entry.level)
    }

    @Test
    @DisplayName("logControlTransfer logs as ERROR when result is negative")
    fun `logControlTransfer logs error on negative result`() {
        logger.logControlTransfer(
            direction = "OUT",
            request = "SET_CUR",
            value = 0x0200,
            index = 0x0A00,
            data = byteArrayOf(0x00, 0x00),
            result = -1
        )

        assertEquals(LogLevel.ERROR, logger.getLogEntries().first().level)
    }

    @Test
    @DisplayName("exportToString produces non-empty formatted output")
    fun `exportToString returns formatted log`() {
        logger.log("TEST", "Hello World")

        val exported = logger.exportToString()
        assertTrue(exported.isNotEmpty())
        assertTrue(exported.contains("TEST"))
        assertTrue(exported.contains("Hello World"))
        assertTrue(exported.contains("INFO"))
    }

    @Test
    @DisplayName("exportToString returns empty string when no entries")
    fun `exportToString returns empty when no entries`() {
        assertEquals("", logger.exportToString())
    }

    @Test
    @DisplayName("clear removes all entries")
    fun `clear removes all entries`() {
        logger.log("TEST", "Entry 1")
        logger.log("TEST", "Entry 2")

        logger.clear()

        assertTrue(logger.getLogEntries().isEmpty())
    }

    @Test
    @DisplayName("getLogEntries returns a snapshot copy")
    fun `getLogEntries returns snapshot`() {
        logger.log("TEST", "Entry 1")
        val snapshot = logger.getLogEntries()

        // Adding more entries doesn't affect the snapshot
        logger.log("TEST", "Entry 2")

        assertEquals(1, snapshot.size)
        assertEquals(2, logger.getLogEntries().size)
    }
}
