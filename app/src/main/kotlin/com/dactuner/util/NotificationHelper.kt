package com.dactuner.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.dactuner.DacTunerApplication
import com.dactuner.R

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

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Shows a transient success notification.
     * Auto-dismisses after 3 seconds.
     *
     * @param deviceName The name of the configured DAC device
     */
    fun showConfigSuccess(deviceName: String) {
        val app = context.applicationContext as? DacTunerApplication
        val prefs = app?.preferencesManager
        if (prefs != null && !prefs.showNotifications) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val contentIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            android.content.Intent(context, com.dactuner.entry.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("DACTuner")
            .setContentText("\u2713 $deviceName configured to maximum volume")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Shows a failure notification with error details.
     *
     * @param errorMessage Human-readable error description
     */
    fun showConfigFailure(errorMessage: String) {
        val app = context.applicationContext as? DacTunerApplication
        val prefs = app?.preferencesManager
        if (prefs != null && !prefs.showNotifications) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val contentIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            android.content.Intent(context, com.dactuner.entry.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("DACTuner")
            .setContentText("Configuration issue: $errorMessage")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(5000L)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Dismisses any active configuration notification.
     */
    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
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

        /** Notification ID for configuration status notification. */
        const val NOTIFICATION_ID = 1001
    }
}
