---
name: dactuner-build
description: >
  Build the DACTuner Android app phase by phase. Covers project setup, USB DAC detection, 
  UAC descriptor parsing, hardware volume configuration via control transfers, HAL race 
  condition mitigation, EU/US adapter detection, Samsung volume limit detection, 
  Jetpack Compose UI, and release packaging. Use this skill when implementing any phase 
  of the DACTuner development plan.
---

# DACTuner Build Skill

## Prerequisites

Before starting any phase, ensure you have read:
- [SRS.md](file:///c:/Users/shrip/repo/DACTuner/docs/SRS.md) — Requirements
- [ARCHITECTURE.md](file:///c:/Users/shrip/repo/DACTuner/docs/ARCHITECTURE.md) — Component design
- [DEVELOPMENT_PLAN.md](file:///c:/Users/shrip/repo/DACTuner/docs/DEVELOPMENT_PLAN.md) — Phase tasks and acceptance criteria

## Phase Execution Guide

### General Workflow Per Phase

1. **Read the phase** in `DEVELOPMENT_PLAN.md` — understand every task and acceptance criterion
2. **Check dependencies** — ensure prior phases are complete and tests pass
3. **Implement tasks** in priority order (P0 first, then P1, then P2)
4. **Write tests** alongside implementation, not after
5. **Validate** against acceptance criteria before marking the phase complete
6. **Update** any tracking docs (task.md) as you progress

---

### Phase 1 — Project Setup & USB Detection

**What you're building:** The Android project skeleton and USB device detection pipeline.

**Step-by-step:**

1. **Create the Android project:**
   - Use Android Gradle Plugin with Kotlin DSL
   - `minSdk = 31`, `targetSdk = 35`
   - Add dependencies: Compose BOM, Material 3, Coroutines, Lifecycle ViewModel

2. **Set up the project structure** matching ARCHITECTURE.md §11:
   ```
   app/src/main/kotlin/com/dactuner/
   ├── entry/       (UsbEventReceiver, MainActivity)
   ├── core/        (Orchestrator, Identifier, Registry)
   ├── usb/         (DeviceManager, Parser, ControlTransfer, ClaimStrategy)
   ├── reliability/  (HalRaceMitigator, PhaseCache)
   ├── detection/   (VariantDetector, SamsungDetector)
   ├── ui/          (ViewModel, State, Screen, components/, theme/)
   └── util/        (Logger, NotificationHelper, PreferencesManager)
   ```

3. **Create the manifest** with:
   - `<uses-feature android:name="android.hardware.usb.host" android:required="true" />`
   - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
   - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />`
   - `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
   - USB device filter XML for VID 1452 / PID 4362
   - Intent filter for `USB_DEVICE_ATTACHED` on the Activity

4. **Implement `DeviceProfileRegistry`:**
   ```kotlin
   // Single hardcoded profile for v1.0
   DacProfile(
       vendorId = 0x05AC,
       productId = 0x110A,
       name = "Apple USB-C to 3.5mm Adapter",
       manufacturer = "Apple, Inc.",
       uacVersion = UacVersion.UAC_2_0,
       knownFeatureUnitId = null,  // discovered at runtime
       knownMaxVolume = null,      // discovered at runtime
       variants = listOf(AdapterVariant.US_A2049, AdapterVariant.EU_A2155)
   )
   ```

5. **Implement `DacIdentifier`** — simple VID/PID lookup against the registry.

6. **Implement `UsbEventReceiver`** — BroadcastReceiver that:
   - Extracts `UsbDevice` from intent extras
   - Passes to `DacIdentifier` for matching
   - Logs result via `DiagnosticsLogger`

7. **Implement basic `UsbDeviceManager`** — `openDevice()`, `closeDevice()`, `hasPermission()`, `requestPermission()`.

8. **Create placeholder `MainActivity`** — shows "DAC detected" / "No DAC" text.

**Validation:** Install APK on device, plug in Apple DAC, verify logcat shows detection.

---

### Phase 2 — Core DAC Configuration

**What you're building:** The USB Audio Class descriptor parser and control transfer executor.

**Key implementation details:**

1. **`UacDescriptorParser`** — This is the most complex component. Parse raw bytes from `connection.getRawDescriptors()`:

   ```
   USB Descriptor Walk:
   
   For each descriptor in raw bytes:
     Read bLength (byte 0) and bDescriptorType (byte 1)
     
     If bDescriptorType == 0x04 (INTERFACE):
       Read bInterfaceClass (byte 5)
       If bInterfaceClass == 0x01 (Audio):
         Read bInterfaceSubClass (byte 6)
         If 0x01 → AudioControl interface (record interface number)
         If 0x02 → AudioStreaming interface (record, DO NOT CLAIM)
     
     If bDescriptorType == 0x24 (CS_INTERFACE) and we're inside AudioControl:
       Read bDescriptorSubtype (byte 2)
       If 0x01 → HEADER: extract UAC version (bytes 3-4, BCD)
       If 0x02 → INPUT_TERMINAL
       If 0x03 → OUTPUT_TERMINAL
       If 0x06 → FEATURE_UNIT:
         Extract bUnitID (byte 3)
         Extract bSourceID (byte 4)
         Extract channel count and control bitmasks
         Check if VOLUME control bit is set
     
     Advance by bLength bytes
   ```

   > **Critical:** UAC 1.0 and 2.0 have different Feature Unit descriptor layouts. The control bitmask is per-channel bytes in UAC 1.0 and per-channel 32-bit words in UAC 2.0. Check the UAC version from the Header descriptor before parsing Feature Units.

2. **`UacControlTransferExecutor`** — Construct and send control transfer payloads:

   ```kotlin
   fun setVolume(
       connection: UsbDeviceConnection,
       featureUnitId: Int,
       interfaceNumber: Int,
       channel: Int,
       volume: Int
   ): Boolean {
       val data = ByteArray(2)
       data[0] = (volume and 0xFF).toByte()         // Low byte
       data[1] = ((volume shr 8) and 0xFF).toByte() // High byte
       
       val result = connection.controlTransfer(
           0x21,                                           // bmRequestType
           0x01,                                           // SET_CUR
           (0x02 shl 8) or channel,                        // wValue
           (featureUnitId shl 8) or interfaceNumber,       // wIndex
           data,                                           // data
           data.size,                                      // wLength
           USB_TIMEOUT_MS                                  // timeout
       )
       return result >= 0
   }
   ```

   **Important:** Volume values are 16-bit **signed** integers in 1/256 dB units. The Apple DAC's max is 0x0000 (0 dB). Minimum is a negative value. Always use `getVolumeRange()` to discover min/max, don't assume.

3. **`InterfaceClaimStrategy`** — Implement three phases:
   - **Phase 1:** Call `controlTransfer()` directly. If it returns >= 0, success.
   - **Phase 2:** `claimInterface(audioControlInterface, false)` → transfer → `releaseInterface()`
   - **Phase 3:** `claimInterface(audioControlInterface, true)` → transfer → `releaseInterface()`

4. **Set volume on ALL channels:**
   ```kotlin
   // Channel 0 = Master
   setVolume(connection, featureUnitId, ifaceNum, channel = 0, maxVolume)
   // Channel 1 = Left
   setVolume(connection, featureUnitId, ifaceNum, channel = 1, maxVolume)
   // Channel 2 = Right
   setVolume(connection, featureUnitId, ifaceNum, channel = 2, maxVolume)
   ```

**Validation:** Plug in Apple DAC, tap Configure, listen — audio should be noticeably louder. Verify with Spotify/YouTube (system-wide, not just in-app).

---

### Phase 3 — Reliability & Auto-Configuration

**What you're building:** The auto-configuration pipeline and HAL race condition handling.

**Key implementation details:**

1. **`HalRaceMitigator`** — Uses coroutine delays for non-blocking waits:
   ```kotlin
   suspend fun configureAndVerify(/* ... */): MitigationResult {
       val delays = listOf(500L, 1000L, 2000L)
       
       for ((attempt, delayMs) in delays.withIndex()) {
           setVolumeAllChannels(connection, featureUnitId, ifaceNum, targetVolume)
           delay(delayMs)
           val currentVolume = getVolume(connection, featureUnitId, ifaceNum, channel = 0)
           
           if (currentVolume != null && currentVolume >= targetVolume) {
               return MitigationResult(verified = true, attemptsUsed = attempt + 1)
           }
           logger.log("HAL_RACE", "Volume reset detected. Attempt ${attempt + 1}, retrying...")
       }
       
       return MitigationResult(verified = false, attemptsUsed = delays.size)
   }
   ```

2. **`UsbEventReceiver` with `goAsync()`:**
   ```kotlin
   override fun onReceive(context: Context, intent: Intent) {
       val pendingResult = goAsync()
       
       CoroutineScope(Dispatchers.IO).launch {
           try {
               val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
               // ... configure ...
           } catch (e: Exception) {
               logger.log("RECEIVER", "Error: ${e.message}")
           } finally {
               pendingResult.finish()
           }
       }
   }
   ```

3. **Auto-configuration guard:** Check `PreferencesManager.autoConfigureEnabled` before auto-configuring. If disabled, only respond to manual "Configure Now" taps.

**Validation:** 20+ plug/unplug cycles on Samsung. Force-stop app, replug — should auto-launch and configure.

---

### Phase 4 — Detection & Warnings

**Key implementation details:**

1. **Samsung detection:** `Build.MANUFACTURER.equals("samsung", ignoreCase = true)`

2. **Media Volume Limit:** Try reading Samsung's system setting:
   ```kotlin
   try {
       val enabled = Settings.System.getInt(
           context.contentResolver, 
           "media_volume_limit_enabled", 
           0
       )
       return enabled == 1
   } catch (e: Exception) {
       return false  // Setting doesn't exist on this device
   }
   ```

3. **EU vs US detection:** Compare `GET_MAX` volume response. If the max value is significantly lower than expected (indicating hardware cap), likely EU model. This is heuristic — return `UNKNOWN` if uncertain.

---

### Phase 5 — UI & Integration

**Key implementation details:**

1. **Theme:** Use Material 3 dynamic colors where available, fall back to a dark theme with:
   - Primary: A sophisticated blue/indigo
   - Surface: Dark grays
   - Status colors: Green (success), Yellow/Amber (warning), Red (error), Gray (disconnected)

2. **State-driven rendering:** The entire UI is a function of `UiState`:
   ```kotlin
   @Composable
   fun MainScreen(viewModel: MainViewModel) {
       val uiState by viewModel.uiState.collectAsStateWithLifecycle()
       // Render everything from uiState — no side effects in composables
   }
   ```

3. **Animations:** Use `animateColorAsState` for the status icon color transitions. Keep animations subtle — this is a utility, not a showpiece.

4. **Activity intent handling:** When launched via `USB_DEVICE_ATTACHED`:
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       
       val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
       if (device != null) {
           viewModel.onDacConnected(device)
       }
   }
   ```

---

### Phase 6 — Testing, Polish & Release

1. **ProGuard/R8 rules:** Keep USB-related classes from being obfuscated:
   ```proguard
   -keep class com.dactuner.usb.** { *; }
   -keep class com.dactuner.core.DeviceProfileRegistry { *; }
   ```

2. **Signing:** Create a release keystore, configure signing in `build.gradle.kts`.

3. **README.md** must include:
   - What the app does (one paragraph)
   - Supported devices (Apple DAC on Samsung)
   - Installation instructions (download APK, enable unknown sources)
   - Usage (plug in, grant permission, done)
   - FAQ (EU vs US, why permission needed, Samsung volume limit)
   - Contributing (how to add DAC profiles)

---

## Common Pitfalls

### USB Descriptor Parsing
- **Pitfall:** Assuming descriptor offsets are fixed. They're not — always read `bLength` and walk dynamically.
- **Pitfall:** Confusing descriptor types. `0x04` is a standard Interface descriptor. `0x24` is a Class-Specific (CS) Interface descriptor. UAC info is in `0x24` descriptors nested within `0x04` Audio interfaces.
- **Pitfall:** Not handling zero-length descriptors or malformed data. Always bounds-check.

### Control Transfers
- **Pitfall:** Forgetting that `controlTransfer()` returns the number of bytes transferred on success, or a negative value on failure. Check `result >= 0`, not `result == true`.
- **Pitfall:** Running `controlTransfer()` on the main thread. It's blocking I/O — always use `Dispatchers.IO`.
- **Pitfall:** Not setting volume on all channels. Some DACs ignore master channel and only respond to individual L/R channels. Set all three.

### Android Lifecycle
- **Pitfall:** Holding a `UsbDeviceConnection` across Activity recreation. Open, use, close — all in one operation.
- **Pitfall:** Not calling `pendingResult.finish()` in `goAsync()` BroadcastReceiver. This leaks the receiver and eventually triggers ANR.
- **Pitfall:** Updating UI state from a BroadcastReceiver without going through the ViewModel. Use a shared state mechanism (application-scoped Flow or EventBus).

### Samsung-Specific
- **Pitfall:** Assuming Samsung's `Settings.System` keys exist on all Samsung devices. Always wrap in try/catch with a safe default.
- **Pitfall:** Not accounting for Samsung's aggressive battery optimization. If the app is battery-optimized, USB broadcasts may be delayed or dropped. Recommend users exempt the app from battery optimization in the README/FAQ.
