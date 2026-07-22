---
name: android-usb-patterns
description: >
  Android USB Host API patterns and Samsung-specific behaviors for DACTuner development.
  Covers UsbManager usage, BroadcastReceiver patterns for USB events, permission handling,
  goAsync() with coroutines, foreground service contingency, and Samsung One UI quirks.
  Use this skill when implementing USB event handling, permission flows, or debugging
  Samsung-specific issues.
---

# Android USB Host API Patterns

## 1. USB Permission Flow

### First-Run Flow (via USB_DEVICE_ATTACHED intent)

When the app is launched by Android's USB intent system (user selected "Open DACTuner" in the system dialog), permission is granted automatically. The user can also check "Always open DACTuner for this device" to skip the dialog on future connections.

```kotlin
// In MainActivity.onCreate()
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
        // Permission is auto-granted when launched via USB intent
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if (device != null) {
            viewModel.onDacConnected(device)
        }
    }
}
```

### Manual Permission Request (user opens app directly)

```kotlin
class UsbDeviceManager(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }
    
    fun requestPermission(device: UsbDevice, callback: (Boolean) -> Unit) {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        
        // Register receiver for permission result
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                ctx.unregisterReceiver(this)
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                callback(granted)
            }
        }, filter, Context.RECEIVER_NOT_EXPORTED)
        
        usbManager.requestPermission(device, permissionIntent)
    }
    
    companion object {
        private const val ACTION_USB_PERMISSION = "com.dactuner.USB_PERMISSION"
    }
}
```

### Finding Connected Devices (when app is already running)

```kotlin
fun findConnectedDac(): UsbDevice? {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    return usbManager.deviceList.values.firstOrNull { device ->
        dacIdentifier.identify(device) != null
    }
}
```

## 2. BroadcastReceiver Patterns

### Manifest Registration

```xml
<!-- AndroidManifest.xml -->
<receiver
    android:name=".entry.UsbEventReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
        <action android:name="android.hardware.usb.action.USB_DEVICE_DETACHED" />
    </intent-filter>
    <meta-data
        android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
        android:resource="@xml/usb_device_filter" />
</receiver>

<!-- Also register on the Activity for auto-launch with permission grant -->
<activity
    android:name=".entry.MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
    </intent-filter>
    <meta-data
        android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
        android:resource="@xml/usb_device_filter" />
</activity>
```

> **Important:** If both the Activity and the Receiver are registered for `USB_DEVICE_ATTACHED`, Android will prefer the Activity (shows "Open DACTuner?" dialog). The Receiver serves as a backup for when the Activity is not in the foreground and the user has already granted "Always open" permission.

### goAsync() + Coroutines Pattern

```kotlin
class UsbEventReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    ?: return
                
                // goAsync() extends the receiver's lifecycle beyond onReceive()
                val pendingResult = goAsync()
                
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        handleDeviceAttached(context, device)
                    } catch (e: Exception) {
                        // NEVER let an exception crash the receiver
                        Log.e("UsbEventReceiver", "Configuration failed", e)
                    } finally {
                        // MUST call finish() to release the receiver
                        pendingResult.finish()
                    }
                }
            }
            
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                handleDeviceDetached(context, device)
                // Detach handling is synchronous — no async needed
            }
        }
    }
    
    private suspend fun handleDeviceAttached(context: Context, device: UsbDevice) {
        // Get or create orchestrator (via Application class or DI)
        val app = context.applicationContext as DacTunerApplication
        val orchestrator = app.configurationOrchestrator
        
        val result = orchestrator.configureIfSupported(device)
        
        // Update shared state so UI can reflect changes
        app.lastConfigurationResult.emit(result)
    }
}
```

> **Critical:** Always call `pendingResult.finish()` in a `finally` block. Forgetting this causes ANR (Application Not Responding) after ~10 seconds.

## 3. USB Device Connection Pattern

### Safe Connection Wrapper

```kotlin
class UsbConnectionHandle(
    val device: UsbDevice,
    val connection: UsbDeviceConnection,
    val descriptors: UacDescriptors
) : Closeable {
    
    private var claimedInterface: UsbInterface? = null
    
    fun claimInterface(iface: UsbInterface, force: Boolean): Boolean {
        val result = connection.claimInterface(iface, force)
        if (result) claimedInterface = iface
        return result
    }
    
    fun releaseClaimedInterface() {
        claimedInterface?.let {
            connection.releaseInterface(it)
            claimedInterface = null
        }
    }
    
    override fun close() {
        releaseClaimedInterface()
        connection.close()
    }
}

// Usage — guaranteed cleanup:
usbDeviceManager.openConnection(device)?.use { handle ->
    // Do work with handle.connection
    // Connection automatically closed even if exception occurs
}
```

### Getting Raw Descriptors

```kotlin
fun getRawDescriptors(connection: UsbDeviceConnection): ByteArray? {
    // Android API — returns the raw USB configuration descriptor
    return connection.rawDescriptors
}
```

> **Note:** `getRawDescriptors()` was added in API 13 but has been available in practice since Android 3.1. On our min SDK 31, it is always available.

## 4. Foreground Service Contingency

Only use this if the BroadcastReceiver's execution window is insufficient on specific devices.

### Service Declaration

```xml
<!-- AndroidManifest.xml -->
<service
    android:name=".entry.DacConfigurationService"
    android:exported="false"
    android:foregroundServiceType="connectedDevice" />
```

### Minimal Service Implementation

```kotlin
class DacConfigurationService : Service() {
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device = intent?.getParcelableExtra<UsbDevice>(EXTRA_DEVICE)
            ?: run { stopSelf(); return START_NOT_STICKY }
        
        // Show minimal notification (required for foreground service)
        val notification = createMinimalNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val app = applicationContext as DacTunerApplication
                val result = app.configurationOrchestrator.configureIfSupported(device)
                app.lastConfigurationResult.emit(result)
            } finally {
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        const val EXTRA_DEVICE = "usb_device"
        const val NOTIFICATION_ID = 1001
    }
}
```

### Starting the Service (from BroadcastReceiver)

```kotlin
// Only if direct execution fails or is restricted
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(
        Intent(context, DacConfigurationService::class.java).apply {
            putExtra(DacConfigurationService.EXTRA_DEVICE, device)
        }
    )
}
```

## 5. Samsung One UI Specific Behaviors

### Known Quirks

| Behavior | Impact | Handling |
|----------|--------|---------|
| **Aggressive battery optimization** | May delay or drop USB broadcasts for backgrounded apps | Recommend user exempt app from battery optimization |
| **Media Volume Limit** | Caps software volume below max | Detect and warn user |
| **Lock-screen USB blocking** | USB peripherals may not be accessible when locked | Configuration happens at plug-in (device typically unlocked) |
| **Audio HAL race condition** | HAL may reset hardware volume after our SET_CUR | Verify + retry pattern |
| **USB permission reset** | Some One UI versions reset USB permissions on app update | Handle permission denial gracefully, re-prompt |

### Detecting Samsung

```kotlin
fun isSamsungDevice(): Boolean {
    return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}

fun getOneUiVersion(): String? {
    return try {
        val semPlatformInt = Build.VERSION::class.java
            .getDeclaredField("SEM_PLATFORM_INT")
            .getInt(null)
        val major = semPlatformInt / 10000
        val minor = (semPlatformInt % 10000) / 100
        "$major.$minor"
    } catch (e: Exception) {
        null  // Not a Samsung device or field not available
    }
}
```

### Reading Samsung Media Volume Limit

```kotlin
fun isMediaVolumeLimitEnabled(context: Context): Boolean {
    if (!isSamsungDevice()) return false
    
    return try {
        Settings.System.getInt(
            context.contentResolver,
            "media_volume_limit_enabled",
            0
        ) == 1
    } catch (e: SecurityException) {
        false
    } catch (e: Exception) {
        false
    }
}

fun getMediaVolumeLimitValue(context: Context): Int {
    return try {
        Settings.System.getInt(
            context.contentResolver,
            "media_volume_limit_value",
            15  // Samsung default max is usually 15
        )
    } catch (e: Exception) {
        -1
    }
}
```

### Deep-Linking to Samsung Sound Settings

```kotlin
fun getSamsungSoundSettingsIntent(): Intent {
    return Intent().apply {
        // Try Samsung-specific sound settings
        component = ComponentName(
            "com.android.settings",
            "com.android.settings.Settings\$SoundSettingsActivity"
        )
        // Fallback: generic sound settings
        action = android.provider.Settings.ACTION_SOUND_SETTINGS
    }
}
```

## 6. Android Version Compatibility Notes

| API Level | Android | Relevant Changes |
|-----------|---------|------------------|
| 31 | 12 | Stricter PendingIntent mutability flags required |
| 32 | 12L | No significant USB changes |
| 33 | 13 | `POST_NOTIFICATIONS` permission required at runtime |
| 34 | 14 | `foregroundServiceType` mandatory for foreground services |
| 35 | 15 | Foreground services from `BOOT_COMPLETED` restricted |

### Runtime Permission for Notifications (Android 13+)

```kotlin
// In MainActivity, before showing notifications
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
            != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }
}
```

### PendingIntent Flags (Android 12+)

```kotlin
// Always include FLAG_MUTABLE or FLAG_IMMUTABLE on API 31+
val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
} else {
    PendingIntent.FLAG_UPDATE_CURRENT
}
```
