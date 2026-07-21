package com.dactuner.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.dactuner.util.DiagnosticsLogger

class UacDescriptorParser(
    private val logger: DiagnosticsLogger
) {
    fun parse(device: UsbDevice, connection: UsbDeviceConnection): UacDescriptors {
        val rawDescriptors = connection.rawDescriptors ?: return createEmptyDescriptors()
        return parseBytes(rawDescriptors)
    }

    // Exposed for testing
    internal fun parseBytes(rawDescriptors: ByteArray): UacDescriptors {
        var uacVersion = UacVersion.UAC_1_0
        var audioControlInterfaceNumber = -1
        val featureUnits = mutableListOf<FeatureUnitDescriptor>()
        val inputTerminals = mutableListOf<InputTerminalDescriptor>()
        val outputTerminals = mutableListOf<OutputTerminalDescriptor>()
        val streamingInterfaces = mutableListOf<Int>()

        var offset = 0
        var currentAudioControlInterface = -1

        while (offset < rawDescriptors.size - 1) {
            val bLength = rawDescriptors[offset].toInt() and 0xFF
            if (bLength == 0 || offset + bLength > rawDescriptors.size) {
                break // Prevent infinite loop or out of bounds
            }
            
            val bDescriptorType = rawDescriptors[offset + 1].toInt() and 0xFF

            when (bDescriptorType) {
                0x04 -> { // INTERFACE descriptor
                    if (bLength >= 9) {
                        val bInterfaceNumber = rawDescriptors[offset + 2].toInt() and 0xFF
                        val bInterfaceClass = rawDescriptors[offset + 5].toInt() and 0xFF
                        val bInterfaceSubClass = rawDescriptors[offset + 6].toInt() and 0xFF

                        if (bInterfaceClass == 0x01) { // Audio
                            if (bInterfaceSubClass == 0x01) { // AudioControl
                                currentAudioControlInterface = bInterfaceNumber
                                audioControlInterfaceNumber = bInterfaceNumber
                            } else if (bInterfaceSubClass == 0x02) { // AudioStreaming
                                streamingInterfaces.add(bInterfaceNumber)
                                currentAudioControlInterface = -1
                            }
                        } else {
                            currentAudioControlInterface = -1
                        }
                    }
                }
                0x24 -> { // CS_INTERFACE (Class-Specific)
                    if (currentAudioControlInterface >= 0 && bLength >= 3) {
                        val bDescriptorSubtype = rawDescriptors[offset + 2].toInt() and 0xFF
                        when (bDescriptorSubtype) {
                            0x01 -> { // HEADER
                                if (bLength >= 6) {
                                    val bcdADC = (rawDescriptors[offset + 3].toInt() and 0xFF) or 
                                                 ((rawDescriptors[offset + 4].toInt() and 0xFF) shl 8)
                                    uacVersion = if (bcdADC >= 0x0200) UacVersion.UAC_2_0 else UacVersion.UAC_1_0
                                }
                            }
                            0x02 -> { // INPUT_TERMINAL
                                if (bLength >= 6) {
                                    val bTerminalID = rawDescriptors[offset + 3].toInt() and 0xFF
                                    val wTerminalType = (rawDescriptors[offset + 4].toInt() and 0xFF) or 
                                                        ((rawDescriptors[offset + 5].toInt() and 0xFF) shl 8)
                                    inputTerminals.add(InputTerminalDescriptor(bTerminalID, wTerminalType))
                                }
                            }
                            0x03 -> { // OUTPUT_TERMINAL
                                if (bLength >= 8) {
                                    val bTerminalID = rawDescriptors[offset + 3].toInt() and 0xFF
                                    val wTerminalType = (rawDescriptors[offset + 4].toInt() and 0xFF) or 
                                                        ((rawDescriptors[offset + 5].toInt() and 0xFF) shl 8)
                                    val bSourceID = rawDescriptors[offset + 7].toInt() and 0xFF
                                    outputTerminals.add(OutputTerminalDescriptor(bTerminalID, wTerminalType, bSourceID))
                                }
                            }
                            0x06 -> { // FEATURE_UNIT
                                featureUnits.add(parseFeatureUnit(rawDescriptors, offset, bLength, uacVersion))
                            }
                        }
                    }
                }
            }
            offset += bLength
        }

        return UacDescriptors(
            uacVersion = uacVersion,
            audioControlInterfaceNumber = audioControlInterfaceNumber,
            featureUnits = featureUnits,
            inputTerminals = inputTerminals,
            outputTerminals = outputTerminals,
            streamingInterfaces = streamingInterfaces
        )
    }

    private fun parseFeatureUnit(data: ByteArray, offset: Int, length: Int, version: UacVersion): FeatureUnitDescriptor {
        // Bounds checking
        if (offset + 4 >= data.size) {
            return FeatureUnitDescriptor(0, 0, 0, emptySet())
        }

        val unitId = data[offset + 3].toInt() and 0xFF
        val sourceId = data[offset + 4].toInt() and 0xFF
        val controls = mutableSetOf<FeatureControl>()
        var channelCount = 0

        if (version == UacVersion.UAC_1_0) {
            if (length >= 7 && offset + 6 < data.size) {
                val bControlSize = data[offset + 5].toInt() and 0xFF
                channelCount = (length - 7) / bControlSize
                
                // Parse master channel controls
                if (bControlSize >= 1 && offset + 6 < data.size) {
                    val masterControlMask = data[offset + 6].toInt() and 0xFF
                    if ((masterControlMask and 0x01) != 0) controls.add(FeatureControl.MUTE)
                    if ((masterControlMask and 0x02) != 0) controls.add(FeatureControl.VOLUME)
                    if ((masterControlMask and 0x04) != 0) controls.add(FeatureControl.BASS)
                    if ((masterControlMask and 0x08) != 0) controls.add(FeatureControl.MID)
                    if ((masterControlMask and 0x10) != 0) controls.add(FeatureControl.TREBLE)
                }
            }
        } else {
            // UAC 2.0 Feature Unit
            if (length >= 13 && offset + 8 < data.size) {
                channelCount = (length - 6) / 4 - 1 // -1 for master channel
                
                // Parse master channel controls (32-bit word at offset 5)
                val masterControlMask = (data[offset + 5].toInt() and 0xFF) or 
                                        ((data[offset + 6].toInt() and 0xFF) shl 8) or
                                        ((data[offset + 7].toInt() and 0xFF) shl 16) or
                                        ((data[offset + 8].toInt() and 0xFF) shl 24)
                                        
                // In UAC 2.0, 2 bits per control: 01=read-only, 11=read-write
                if ((masterControlMask and 0x03) != 0) controls.add(FeatureControl.MUTE)
                if ((masterControlMask and 0x0C) != 0) controls.add(FeatureControl.VOLUME)
                if ((masterControlMask and 0x30) != 0) controls.add(FeatureControl.BASS)
            }
        }

        return FeatureUnitDescriptor(
            unitId = unitId,
            sourceId = sourceId,
            channelCount = channelCount,
            controls = controls
        )
    }

    private fun createEmptyDescriptors() = UacDescriptors(
        uacVersion = UacVersion.UAC_1_0,
        audioControlInterfaceNumber = -1,
        featureUnits = emptyList(),
        inputTerminals = emptyList(),
        outputTerminals = emptyList(),
        streamingInterfaces = emptyList()
    )
}
