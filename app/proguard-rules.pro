# DACTuner ProGuard Rules

# Keep USB-related classes from obfuscation
-keep class com.dactuner.usb.** { *; }
-keep class com.dactuner.core.DeviceProfileRegistry { *; }
-keep class com.dactuner.core.DacProfile { *; }
