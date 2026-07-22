# DACTuner — Agent Rules

## Project Identity

DACTuner is an Android utility app that restores full hardware volume on USB DAC adapters (starting with the Apple USB-C to 3.5mm adapter) on Android devices. It uses standard USB Audio Class control transfers via Android's USB Host API. No root required.

## Critical Documents — Read Before Any Work

Before starting any development task, read these documents in order:

1. **[SRS.md](file:///c:/Users/shrip/repo/DACTuner/docs/SRS.md)** — Requirements, functional specs, known limitations
2. **[ARCHITECTURE.md](file:///c:/Users/shrip/repo/DACTuner/docs/ARCHITECTURE.md)** — System design, component structure, data flows
3. **[DEVELOPMENT_PLAN.md](file:///c:/Users/shrip/repo/DACTuner/docs/DEVELOPMENT_PLAN.md)** — Phased plan, task breakdown, acceptance criteria

These are the source of truth. Do not deviate from the architecture or requirements without explicit user approval.

---

## Language & Framework Rules

- **Language:** Kotlin (no Java, no mixed-language modules)
- **UI:** Jetpack Compose with Material 3. No XML layouts, no View-based UI.
- **Async:** Kotlin Coroutines + Flow. No RxJava, no AsyncTask, no raw threads.
- **Build:** Gradle with Kotlin DSL (`.gradle.kts`). No Groovy build files.
- **Min SDK:** 31 (Android 12). Do not use APIs below this level.
- **Target SDK:** 35 (Android 15).

## Coding Conventions

### General
- Follow official Kotlin coding conventions: https://kotlinlang.org/docs/coding-conventions.html
- Use `data class` for all value types (models, results, descriptors)
- Use `sealed class` / `sealed interface` for type hierarchies (errors, results, states)
- Use `enum class` for fixed sets (phases, variants, status)
- Prefer immutable (`val`) over mutable (`var`) everywhere
- Never suppress warnings without a comment explaining why

### Naming
- Package: `com.dactuner.<layer>` (e.g., `com.dactuner.usb`, `com.dactuner.core`)
- Classes: PascalCase, descriptive (e.g., `UacDescriptorParser`, not `Parser`)
- Functions: camelCase, verb-first (e.g., `parseDescriptors()`, `setVolume()`)
- Constants: UPPER_SNAKE_CASE in companion objects
- USB/UAC constants: use hex literals with comments (e.g., `0x21 // Host-to-Device, Class, Interface`)

### Documentation
- All public classes and functions MUST have KDoc comments
- USB protocol constants MUST have inline comments explaining the byte values
- Complex logic MUST have step-by-step comments
- Do NOT delete existing comments unless they are factually wrong

### Error Handling
- Never use bare `try/catch` with empty catch blocks
- All USB operations must be wrapped in try/catch — a disconnected adapter must never crash the app
- Use `Closeable.use {}` pattern for all USB connections
- Log all errors via `DiagnosticsLogger` before propagating

---

## Architecture Rules

### Layer Dependencies (Strict)
```
Entry Points → Core → USB, Reliability, Detection, Utility
UI (ViewModel) → Core, Utility
```

**Violations that are NOT allowed:**
- USB layer importing from Core, UI, or Entry
- Detection layer importing from USB layer (receives data via parameters)
- Utility layer importing from any other layer
- Any layer importing from Entry Points
- Any circular dependencies

### Component Boundaries
- Only `UsbDeviceManager` and `UacControlTransferExecutor` may call Android USB APIs (`UsbManager`, `UsbDeviceConnection`, `controlTransfer`)
- Only `ConfigurationOrchestrator` may coordinate multiple components together
- Only `MainViewModel` may emit UI state changes
- Only `UsbEventReceiver` may receive USB broadcasts

### USB-Specific Rules
- **NEVER** claim the AudioStreaming interface — this will break system audio
- **ALWAYS** close `UsbDeviceConnection` in a finally block or via `.use {}`
- **ALWAYS** release claimed interfaces before closing the connection
- Control transfers MUST execute on `Dispatchers.IO`, never on the main thread
- Log every `controlTransfer()` call with parameters and result code

### State Management
- UI state is a single `UiState` data class exposed as `StateFlow`
- State updates happen only through `MainViewModel`
- No mutable state in composables — all state flows from ViewModel
- Use `copy()` for state updates, never mutate in place

---

## Testing Rules

- Unit tests go in `src/test/` (not `src/androidTest/`)
- Use JUnit 5 (not JUnit 4)
- Use MockK for mocking (not Mockito)
- Test file naming: `<ClassName>Test.kt` in the same package structure
- Every parser and payload builder MUST have tests with real captured byte data
- Do NOT write tests that require a physical USB device — mock the USB layer

---

## Git Conventions

- Branch naming: `phase-N/description` (e.g., `phase-2/descriptor-parser`)
- Commit messages: imperative mood, reference phase (e.g., `[Phase 2] Implement UAC descriptor parser`)
- One logical change per commit — don't bundle unrelated changes
- Never commit generated files, build outputs, or IDE-specific configs

---

## USB Protocol Quick Reference

When implementing USB control transfers, use these exact byte values:

```
SET_CUR Volume:
  bmRequestType = 0x21  (Host→Device | Class | Interface)
  bRequest      = 0x01  (SET_CUR)
  wValue        = (0x02 << 8) | channel   // 0x02 = Volume Control Selector
  wIndex        = (featureUnitId << 8) | interfaceNumber
  wLength       = 2
  data          = 16-bit signed LE volume value

GET_CUR Volume:
  bmRequestType = 0xA1  (Device→Host | Class | Interface)
  bRequest      = 0x81  (GET_CUR)
  [wValue, wIndex, wLength same as SET_CUR]

GET_MIN = 0x82, GET_MAX = 0x83, GET_RES = 0x84
```

Apple DAC identifiers:
- VID: `0x05AC` (decimal: 1452)
- PID: `0x110A` (decimal: 4362)

---

## What NOT to Do

- Do NOT add internet permission or any network-related code
- Do NOT add analytics, crash reporting, or telemetry
- Do NOT register for `BOOT_COMPLETED` broadcast
- Do NOT create persistent background services
- Do NOT use root-level operations or shell commands
- Do NOT hardcode Feature Unit IDs — always parse descriptors at runtime
- Do NOT race Android's audio HAL — use the verify + retry pattern instead
- Do NOT add dependencies without explicit user approval
