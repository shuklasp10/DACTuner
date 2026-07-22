package com.dactuner.core

import android.hardware.usb.UsbDevice
import com.dactuner.reliability.HalRaceMitigator
import com.dactuner.usb.ClaimPhase
import com.dactuner.usb.FeatureControl
import com.dactuner.usb.InterfaceClaimStrategy
import com.dactuner.usb.UacControlTransferExecutor
import com.dactuner.usb.UsbDeviceManager
import com.dactuner.usb.VolumeRange
import com.dactuner.util.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfigurationOrchestrator(
    private val dacIdentifier: DacIdentifier,
    private val usbDeviceManager: UsbDeviceManager,
    private val claimStrategy: InterfaceClaimStrategy,
    private val controlTransferExecutor: UacControlTransferExecutor,
    private val halRaceMitigator: HalRaceMitigator,
    private val logger: DiagnosticsLogger
) {
    suspend fun configureIfSupported(device: UsbDevice): ConfigurationResult = withContext(Dispatchers.IO) {
        val profile = dacIdentifier.identify(device.vendorId, device.productId)
        if (profile == null) {
            return@withContext ConfigurationResult.Failure(ConfigurationError.DeviceNotFound)
        }

        if (!usbDeviceManager.hasPermission(device)) {
            return@withContext ConfigurationResult.Failure(ConfigurationError.PermissionDenied)
        }

        val handle = usbDeviceManager.openConnection(device)
            ?: return@withContext ConfigurationResult.Failure(ConfigurationError.DeviceBusy)

        handle.use { usbHandle ->
            val descriptors = usbHandle.descriptors
            if (descriptors.audioControlInterfaceNumber < 0) {
                return@withContext ConfigurationResult.Failure(ConfigurationError.DescriptorParseFailure)
            }
            
            // Find the feature unit that supports volume control
            val volumeFeatureUnit = descriptors.featureUnits.find { fu ->
                fu.controls.contains(FeatureControl.VOLUME)
            }
            
            if (volumeFeatureUnit == null) {
                return@withContext ConfigurationResult.Failure(ConfigurationError.NoFeatureUnitFound)
            }
            
            val audioControlInterface = usbHandle.device.getInterface(descriptors.audioControlInterfaceNumber)
            
            var maxVolumeFound = 0
            
            if (profile.knownMaxVolume != null) {
                maxVolumeFound = profile.knownMaxVolume
            } else {
                // Try channel 1 first, fallback to 0
                val queryChannel = 1
                var range: VolumeRange? = null
                claimStrategy.executeWithBestStrategy(
                    vendorId = device.vendorId,
                    productId = device.productId,
                    connection = usbHandle.connection,
                    audioControlInterface = audioControlInterface
                ) { connection ->
                    range = controlTransferExecutor.getVolumeRange(
                        connection = connection,
                        featureUnitId = volumeFeatureUnit.unitId,
                        interfaceNumber = descriptors.audioControlInterfaceNumber,
                        channel = queryChannel
                    )
                    true
                }
                maxVolumeFound = range?.max ?: 0 // Default to 0 dB if range fails
            }
            
            // Use the HalRaceMitigator to set volume and verify it sticks
            // It will handle claiming/releasing the interface internally per-attempt
            val result = halRaceMitigator.configureAndVerify(
                vendorId = device.vendorId,
                productId = device.productId,
                connection = usbHandle.connection,
                audioControlInterface = audioControlInterface,
                featureUnitId = volumeFeatureUnit.unitId,
                interfaceNumber = descriptors.audioControlInterfaceNumber,
                channelCount = volumeFeatureUnit.channelCount,
                targetVolume = maxVolumeFound
            )
            
            if (result.verified) {
                val phaseUsed = claimStrategy.phaseCache.getCachedPhase(device.vendorId, device.productId)

                if (phaseUsed == com.dactuner.usb.ClaimPhase.PHASE_3_FORCE_CLAIM) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        logger.log("CONFIG", "Force-claim was used — resetting device to restore kernel audio driver binding")
                        try {
                            val resetMethod = usbHandle.connection.javaClass.getMethod("resetDevice")
                            val resetSuccess = resetMethod.invoke(usbHandle.connection) as? Boolean ?: false
                            logger.log("CONFIG", "resetDevice() via reflection returned: $resetSuccess")
                        } catch (e: Exception) {
                            logger.log("CONFIG", "resetDevice() reflection failed: ${e.message}", com.dactuner.util.LogLevel.WARNING)
                        }
                    } else {
                        logger.log(
                            "CONFIG",
                            "Force-claim used but device on API < 31 — cannot auto-reset; user must replug",
                            com.dactuner.util.LogLevel.WARNING
                        )
                    }
                }

                return@withContext ConfigurationResult.Success(
                    volumeSet = maxVolumeFound,
                    volumeMax = maxVolumeFound,
                    phase = phaseUsed
                )
            } else {
                return@withContext ConfigurationResult.Failure(
                    ConfigurationError.AllPhasesFailed,
                    phase = claimStrategy.phaseCache.getCachedPhase(device.vendorId, device.productId)
                )
            }
        }
    }
}
