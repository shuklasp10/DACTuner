package com.dactuner.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * In-memory diagnostics logger with ring buffer storage.
 *
 * Stores log entries for display in the debug UI and export for bug reports.
 * Thread-safe for concurrent access from USB and UI threads.
 * Does not depend on android.util.Log to remain unit-testable.
 */
class DiagnosticsLogger {

    private val _entries = ArrayDeque<LogEntry>()
    private val lock = Any()

    /**
     * Log a general message.
     */
    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        addEntry(LogEntry(System.currentTimeMillis(), tag, message, level))
    }

    /**
     * Log a USB operation with timing information.
     */
    fun logUsbOperation(operation: String, result: String, durationMs: Long) {
        log("USB", "$operation → $result (${durationMs}ms)", LogLevel.INFO)
    }

    /**
     * Log a USB control transfer with full parameter details.
     *
     * @param direction Transfer direction (e.g., "OUT" for host-to-device, "IN" for device-to-host)
     * @param request Request name (e.g., "SET_CUR", "GET_CUR")
     * @param value wValue field of the control transfer
     * @param index wIndex field of the control transfer
     * @param data Raw data bytes sent or received
     * @param result Return value from controlTransfer() — bytes transferred or negative error code
     */
    fun logControlTransfer(
        direction: String,
        request: String,
        value: Int,
        index: Int,
        data: ByteArray,
        result: Int
    ) {
        val dataHex = data.joinToString(" ") { String.format("%02X", it) }
        val message = "$direction $request wValue=0x${String.format("%04X", value)} " +
                "wIndex=0x${String.format("%04X", index)} data=[$dataHex] result=$result"
        log(
            "CTRL_TRANSFER",
            message,
            if (result >= 0) LogLevel.INFO else LogLevel.ERROR
        )
    }

    /**
     * Log a raw USB descriptor dump as hex.
     * Intended for debug mode only — produces verbose output.
     */
    fun logDescriptorDump(descriptors: ByteArray) {
        val hexDump = descriptors.toList()
            .chunked(16)
            .mapIndexed { i, bytes ->
                val offset = String.format("%04X", i * 16)
                val hex = bytes.joinToString(" ") { String.format("%02X", it) }
                "$offset: $hex"
            }
            .joinToString("\n")
        log("DESCRIPTOR_DUMP", "\n$hexDump", LogLevel.DEBUG)
    }

    /**
     * Returns all log entries in chronological order.
     */
    fun getLogEntries(): List<LogEntry> = synchronized(lock) { _entries.toList() }

    /**
     * Exports all log entries as a plain text string suitable for bug reports.
     * Format: [timestamp] [LEVEL] TAG: message
     */
    fun exportToString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return synchronized(lock) {
            _entries.joinToString("\n") { entry ->
                val timestamp = dateFormat.format(Date(entry.timestamp))
                "[$timestamp] [${entry.level}] ${entry.tag}: ${entry.message}"
            }
        }
    }

    /**
     * Clears all log entries.
     */
    fun clear() {
        synchronized(lock) { _entries.clear() }
    }

    private fun addEntry(entry: LogEntry) {
        synchronized(lock) {
            _entries.addLast(entry)
            while (_entries.size > MAX_ENTRIES) {
                _entries.removeFirst()
            }
        }
    }

    companion object {
        /** Maximum number of log entries stored in the ring buffer. */
        const val MAX_ENTRIES = 500
    }
}

/** Severity level for log entries. */
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

/** A single log entry with timestamp, tag, message, and severity level. */
data class LogEntry(
    val timestamp: Long,
    val tag: String,
    val message: String,
    val level: LogLevel
)
