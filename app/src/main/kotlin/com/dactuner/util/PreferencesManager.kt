package com.dactuner.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences wrapper for app settings and cached data.
 *
 * Uses SharedPreferences for v1 simplicity. Stores app settings (auto-configure,
 * notifications, debug mode), timestamps, and phase cache data.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        if (prefs.getInt(KEY_PHASE_CACHE_VERSION, 1) < PHASE_CACHE_VERSION) {
            clearPhaseCache()
            prefs.edit().putInt(KEY_PHASE_CACHE_VERSION, PHASE_CACHE_VERSION).apply()
        }
    }

    /** Whether to automatically configure the DAC on USB connection. Default: true. */
    var autoConfigureEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONFIGURE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONFIGURE, value).apply()

    /** Whether to show transient notifications on configuration events. Default: true. */
    var showNotifications: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_NOTIFICATIONS, value).apply()

    /** Whether debug mode (verbose logging, descriptor dumps) is enabled. Default: false. */
    var debugModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    /** Timestamp of the last successful DAC configuration, in epoch millis. */
    var lastConfiguredTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CONFIGURED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CONFIGURED, value).apply()

    /**
     * Retrieves the cached claim phase for a device identified by VID/PID.
     * Returns null if no phase has been cached.
     */
    fun getCachedPhase(vendorId: Int, productId: Int): Int? {
        val key = "${KEY_PHASE_CACHE_PREFIX}${vendorId}_${productId}"
        val value = prefs.getInt(key, -1)
        return if (value >= 0) value else null
    }

    /**
     * Caches the successful claim phase ordinal for a device identified by VID/PID.
     */
    fun setCachedPhase(vendorId: Int, productId: Int, phaseOrdinal: Int) {
        val key = "${KEY_PHASE_CACHE_PREFIX}${vendorId}_${productId}"
        prefs.edit().putInt(key, phaseOrdinal).apply()
    }

    /**
     * Clears the phase cache for all devices.
     */
    fun clearPhaseCache() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(KEY_PHASE_CACHE_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "dactuner_prefs"
        private const val KEY_AUTO_CONFIGURE = "auto_configure_enabled"
        private const val KEY_SHOW_NOTIFICATIONS = "show_notifications"
        private const val KEY_DEBUG_MODE = "debug_mode_enabled"
        private const val KEY_LAST_CONFIGURED = "last_configured_timestamp"
        private const val KEY_PHASE_CACHE_PREFIX = "phase_cache_"
        private const val PHASE_CACHE_VERSION = 2
        private const val KEY_PHASE_CACHE_VERSION = "phase_cache_version"
    }
}
