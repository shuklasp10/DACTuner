package com.dactuner.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Manages transient notifications for DAC configuration events.
 *
 * Creates a dedicated notification channel ("DAC Configuration") and provides
 * methods to show/dismiss configuration success and failure notifications.
 * Notifications auto-dismiss after 3 seconds.
 *
 * Phase 1: Stub implementation — channel creation only.
 * Full notification display implemented in Phase 5.
 */
class NotificationHelper(private val context: Context) {

    /**
     * Creates the notification channel for DAC configuration events.
     * Must be called during application initialization (before any notifications are shown).
     * Safe to call multiple times — Android ignores duplicate channel creation.
     */
    fun initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a transient success notification.
     * Auto-dismisses after 3 seconds.
     *
     * @param deviceName The name of the configured DAC device
     */
    fun showConfigSuccess(deviceName: String) {
        // TODO: Phase 5 — implement notification display
    }

    /**
     * Shows a failure notification with error details.
     *
     * @param errorMessage Human-readable error description
     */
    fun showConfigFailure(errorMessage: String) {
        // TODO: Phase 5 — implement notification display
    }

    /**
     * Dismisses any active configuration notification.
     */
    fun dismiss() {
        // TODO: Phase 5 — implement notification dismissal
    }

    companion object {
        /** Notification channel ID for DAC configuration events. */
        const val CHANNEL_ID = "dac_configuration"

        /** User-visible notification channel name. */
        const val CHANNEL_NAME = "DAC Configuration"

        /** User-visible notification channel description. */
        const val CHANNEL_DESCRIPTION = "Notifications for DAC configuration events"

        /** Notification auto-dismiss timeout in milliseconds. */
        const val NOTIFICATION_TIMEOUT_MS = 3000L
    }
}
