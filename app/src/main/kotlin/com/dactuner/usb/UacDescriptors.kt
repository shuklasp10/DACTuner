package com.dactuner.usb

enum class UacVersion {
    UAC_1_0,
    UAC_2_0
}

enum class FeatureControl {
    MUTE,
    VOLUME,
    BASS,
    MID,
    TREBLE,
    GRAPHIC_EQUALIZER,
    AUTOMATIC_GAIN,
    DELAY,
    BASS_BOOST,
    LOUDNESS,
    INPUT_GAIN,
    INPUT_GAIN_PAD,
    PHASE_INVERTER,
    UNDERFLOW_OVERFLOW,
    LATENCY_CONTROL
}

data class FeatureUnitDescriptor(
    val unitId: Int,
    val sourceId: Int,
    val channelCount: Int,
    val controls: Set<FeatureControl>
)

data class InputTerminalDescriptor(
    val terminalId: Int,
    val terminalType: Int
)

data class OutputTerminalDescriptor(
    val terminalId: Int,
    val terminalType: Int,
    val sourceId: Int
)

data class UacDescriptors(
    val uacVersion: UacVersion,
    val audioControlInterfaceNumber: Int,
    val featureUnits: List<FeatureUnitDescriptor>,
    val inputTerminals: List<InputTerminalDescriptor>,
    val outputTerminals: List<OutputTerminalDescriptor>,
    val streamingInterfaces: List<Int>
)
