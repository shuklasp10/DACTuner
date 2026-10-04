# DACTuner ProGuard Rules

# Keep USB-related classes from obfuscation
-keep class com.dactuner.usb.** { *; }
-keep class com.dactuner.core.** { *; }
-keep class com.dactuner.entry.** { *; }
-keep class com.dactuner.DacTunerApplication { *; }
-keep class com.dactuner.ui.** { *; }
-keep class com.dactuner.util.** { *; }
