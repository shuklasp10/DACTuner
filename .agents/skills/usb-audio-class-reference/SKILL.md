---
name: usb-audio-class-reference
description: >
  Technical reference for USB Audio Class (UAC) 1.0 and 2.0 protocols as they apply to 
  configuring DAC hardware volume. Covers descriptor structures, control transfer byte 
  layouts, Feature Unit parsing, and Apple DAC-specific details. Use this skill when 
  implementing or debugging USB descriptor parsing or control transfer code.
---

# USB Audio Class Protocol Reference

## Overview

USB Audio Class (UAC) defines how USB audio devices communicate with hosts. DACTuner uses UAC control transfers to set the hardware volume on DAC adapters. This reference covers only the parts of UAC relevant to DACTuner.

## 1. USB Descriptor Hierarchy

A USB audio device exposes its capabilities through a hierarchy of descriptors:

```
Device Descriptor
└── Configuration Descriptor
    ├── Interface 0: AudioControl (bInterfaceClass=0x01, bInterfaceSubClass=0x01)
    │   ├── CS_INTERFACE: Header (subtype 0x01) — UAC version
    │   ├── CS_INTERFACE: Input Terminal (subtype 0x02)
    │   ├── CS_INTERFACE: Feature Unit (subtype 0x06) ← VOLUME CONTROL
    │   └── CS_INTERFACE: Output Terminal (subtype 0x03)
    ├── Interface 1: AudioStreaming (bInterfaceClass=0x01, bInterfaceSubClass=0x02)
    │   └── [Playback — DO NOT CLAIM]
    └── Interface 2: AudioStreaming (bInterfaceClass=0x01, bInterfaceSubClass=0x02)
        └── [Microphone — DO NOT CLAIM]
```

## 2. Descriptor Parsing Algorithm

### Step-by-step walk of raw descriptor bytes

```
offset = 0
currentAudioControlInterface = -1

while offset < rawDescriptors.length:
    bLength = rawDescriptors[offset]
    bDescriptorType = rawDescriptors[offset + 1]
    
    if bLength == 0:
        break  // Malformed — prevent infinite loop
    
    switch bDescriptorType:
    
        case 0x04:  // INTERFACE descriptor
            bInterfaceNumber = rawDescriptors[offset + 2]
            bInterfaceClass = rawDescriptors[offset + 5]
            bInterfaceSubClass = rawDescriptors[offset + 6]
            
            if bInterfaceClass == 0x01:  // Audio
                if bInterfaceSubClass == 0x01:  // AudioControl
                    currentAudioControlInterface = bInterfaceNumber
                else if bInterfaceSubClass == 0x02:  // AudioStreaming
                    record as streaming interface (DO NOT CLAIM)
                    currentAudioControlInterface = -1
            else:
                currentAudioControlInterface = -1
        
        case 0x24:  // CS_INTERFACE (Class-Specific)
            if currentAudioControlInterface >= 0:
                parseAudioControlDescriptor(rawDescriptors, offset)
    
    offset += bLength
```

### Parsing AudioControl CS_INTERFACE descriptors

```
function parseAudioControlDescriptor(data, offset):
    bDescriptorSubtype = data[offset + 2]
    
    switch bDescriptorSubtype:
    
        case 0x01:  // HEADER
            bcdADC = data[offset + 3] | (data[offset + 4] << 8)
            if bcdADC >= 0x0200:
                uacVersion = UAC_2_0
            else:
                uacVersion = UAC_1_0
        
        case 0x02:  // INPUT_TERMINAL
            bTerminalID = data[offset + 3]
            wTerminalType = data[offset + 4] | (data[offset + 5] << 8)
            // 0x0101 = USB Streaming, 0x0201 = Microphone
        
        case 0x03:  // OUTPUT_TERMINAL
            bTerminalID = data[offset + 3]
            wTerminalType = data[offset + 4] | (data[offset + 5] << 8)
            bSourceID = data[offset + 7]
            // 0x0301 = Speaker, 0x0302 = Headphones
        
        case 0x06:  // FEATURE_UNIT
            parseFeatureUnit(data, offset)
```

### Feature Unit Parsing (UAC 1.0 vs 2.0)

#### UAC 1.0 Feature Unit

```
Offset  Field               Size    Description
0       bLength             1       Total descriptor length
1       bDescriptorType     1       0x24 (CS_INTERFACE)
2       bDescriptorSubtype  1       0x06 (FEATURE_UNIT)
3       bUnitID             1       ← Feature Unit ID (needed for control transfers)
4       bSourceID           1       ID of the unit/terminal connected to input
5       bControlSize        1       Size of each bmaControls entry (usually 1 or 2)
6       bmaControls(0)      N       Master channel controls bitmask
6+N     bmaControls(1)      N       Channel 1 controls bitmask
...     ...                 ...     ...

Control bits (per channel):
  Bit 0: Mute
  Bit 1: Volume  ← This is what we need
  Bit 2: Bass
  Bit 3: Mid
  Bit 4: Treble
```

#### UAC 2.0 Feature Unit

```
Offset  Field               Size    Description
0       bLength             1       Total descriptor length
1       bDescriptorType     1       0x24 (CS_INTERFACE)
2       bDescriptorSubtype  1       0x06 (FEATURE_UNIT)
3       bUnitID             1       ← Feature Unit ID
4       bSourceID           1       Source unit/terminal ID
5       bmaControls(0)      4       Master channel controls (32-bit bitmask)
9       bmaControls(1)      4       Channel 1 controls (32-bit bitmask)
13      bmaControls(2)      4       Channel 2 controls (32-bit bitmask)

Control bits (per channel, 2 bits per control):
  Bits 0-1: Mute Control         (01=read-only, 11=read-write)
  Bits 2-3: Volume Control       (01=read-only, 11=read-write) ← This is what we need
  Bits 4-5: Bass Control
  ...
```

**Key difference:** UAC 1.0 uses variable-size byte arrays per channel. UAC 2.0 uses fixed 4-byte (32-bit) words per channel with 2-bit control fields.

## 3. Control Transfer Byte Layout

### Request Types

| Constant | Value | Direction | Type |
|----------|-------|-----------|------|
| `SET_CUR` request type | `0x21` | Host → Device | Class, Interface |
| `GET_CUR` request type | `0xA1` | Device → Host | Class, Interface |

### Request Codes

| Request | Code |
|---------|------|
| `SET_CUR` | `0x01` |
| `GET_CUR` | `0x81` |
| `GET_MIN` | `0x82` |
| `GET_MAX` | `0x83` |
| `GET_RES` | `0x84` |

### Control Selectors

| Control | Selector Value |
|---------|---------------|
| Mute | `0x01` |
| Volume | `0x02` |
| Bass | `0x03` |
| Mid | `0x04` |
| Treble | `0x05` |

### Channel Numbers

| Channel | Number |
|---------|--------|
| Master | `0` |
| Left (Channel 1) | `1` |
| Right (Channel 2) | `2` |

### Constructing wValue and wIndex

```
wValue = (controlSelector << 8) | channelNumber
  Example: Volume on Master = (0x02 << 8) | 0 = 0x0200
  Example: Volume on Left   = (0x02 << 8) | 1 = 0x0201
  Example: Volume on Right  = (0x02 << 8) | 2 = 0x0202

wIndex = (featureUnitId << 8) | audioControlInterfaceNumber
  Example: Feature Unit 10 on Interface 0 = (0x0A << 8) | 0 = 0x0A00
```

### Volume Data Format

- **Size:** 2 bytes (16-bit signed integer, little-endian)
- **Unit:** 1/256 dB
- **Range:** Discovered via `GET_MIN` / `GET_MAX` / `GET_RES`
- **Maximum (0 dB):** Typically `0x0000`
- **Minimum:** Negative value (e.g., `0x8100` = -32,512 in 1/256 dB)

```
To encode volume value as bytes:
  data[0] = volume & 0xFF          // Low byte
  data[1] = (volume >> 8) & 0xFF   // High byte

To decode volume from bytes:
  volume = data[0] | (data[1] << 8)
  if volume > 32767:               // Sign extension for 16-bit signed
      volume -= 65536
```

## 4. Complete controlTransfer() Call Examples

### SET_CUR — Set Master Volume to Maximum (0 dB)

```kotlin
val data = byteArrayOf(0x00, 0x00)  // 0x0000 = 0 dB

val result = connection.controlTransfer(
    0x21,                                       // bmRequestType: Host→Device, Class, Interface
    0x01,                                       // bRequest: SET_CUR
    (0x02 shl 8) or 0,                          // wValue: Volume Control (0x02), Master Channel (0)
    (featureUnitId shl 8) or interfaceNumber,   // wIndex: Feature Unit ID + Interface
    data,                                       // data: volume value
    data.size,                                  // wLength: 2
    1000                                        // timeout: 1000ms
)
// result >= 0 means success (number of bytes transferred)
// result < 0 means failure
```

### GET_CUR — Read Current Master Volume

```kotlin
val data = ByteArray(2)

val result = connection.controlTransfer(
    0xA1,                                       // bmRequestType: Device→Host, Class, Interface
    0x81,                                       // bRequest: GET_CUR
    (0x02 shl 8) or 0,                          // wValue: Volume Control, Master Channel
    (featureUnitId shl 8) or interfaceNumber,   // wIndex
    data,                                       // buffer to receive data
    data.size,                                  // wLength: 2
    1000                                        // timeout
)

if (result >= 2) {
    val volume = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
    val signedVolume = if (volume > 32767) volume - 65536 else volume
    // signedVolume is in 1/256 dB units
}
```

### GET_MAX — Discover Maximum Volume

```kotlin
val data = ByteArray(2)

val result = connection.controlTransfer(
    0xA1,       // Device→Host
    0x83,       // GET_MAX
    (0x02 shl 8) or 0,
    (featureUnitId shl 8) or interfaceNumber,
    data, data.size, 1000
)
// Parse same as GET_CUR
```

## 5. Apple USB-C DAC Specifics

| Property | Value |
|----------|-------|
| VID | `0x05AC` (1452) |
| PID | `0x110A` (4362) |
| UAC Version | 2.0 |
| Channels | 2 (stereo) |
| Volume Steps | ~120 |
| Max Volume | 0 dB (`0x0000`) |
| Max Audio Format | 24-bit / 48 kHz PCM |
| Power | Bus-powered (loses state on unplug) |

### EU vs US Variants

| Model | Region | Max Output | Hardware Limit |
|-------|--------|-----------|----------------|
| A2049 | US/Asia | 1.0 Vrms | No |
| A2155 | EU | 0.5 Vrms | Yes — cannot be bypassed by software |

The EU model's hardware cap is enforced at the analog output stage. Setting the digital volume to maximum (0 dB) on both variants produces different actual output voltages. The software cannot distinguish this from the USB interface — both report the same digital volume range. Distinguish by USB serial number or BCD device version if possible.

## 6. Troubleshooting Reference

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `controlTransfer()` returns -1 | No permission, or interface claimed by kernel | Check `hasPermission()`, try `claimInterface()` |
| `getRawDescriptors()` returns null | Device disconnected or connection invalid | Re-open connection |
| Feature Unit not found in descriptors | Parsing error or unexpected descriptor layout | Log raw hex dump, inspect manually |
| Volume resets after SET_CUR | Android HAL re-initializing | Use HalRaceMitigator retry pattern |
| Audio stops after `claimInterface()` | ALSA driver detached | Use Phase 1 (no claim) or release immediately |
| `GET_CUR` returns unexpected value | Wrong Feature Unit ID or channel | Verify descriptor parsing, check wIndex |
