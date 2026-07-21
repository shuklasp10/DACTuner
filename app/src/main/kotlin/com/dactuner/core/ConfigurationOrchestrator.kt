package com.dactuner.core

import android.hardware.usb.UsbDevice
import com.dactuner.usb.ClaimPhase
import com.dactuner.usb.FeatureControl
import com.dactuner.usb.InterfaceClaimStrategy
import com.dactuner.usb.UacControlTransferExecutor
import com.dactuner.usb.UsbDeviceManager
import com.dactuner.util.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfigurationOrchestrator(
    private val dacIdentifier: DacIdentifier,
    private val usbDeviceManager: UsbDeviceManager,
    private val claimStrategy: InterfaceClaimStrategy,
    private val controlTransferExecutor: UacControlTransferExecutor,
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
            
            val strategyResult = claimStrategy.executeWithBestStrategy(
                vendorId = device.vendorId,
                productId = device.productId,
                connection = usbHandle.connection,
                audioControlInterface = audioControlInterface
            ) { connection ->
                
                // Get Volume Range to discover Max volume
                val range = controlTransferExecutor.getVolumeRange(
                    connection = connection,
                    featureUnitId = volumeFeatureUnit.unitId,
                    interfaceNumber = descriptors.audioControlInterfaceNumber,
                    channel = 0 // Master channel
                )
                
                maxVolumeFound = range?.max ?: 0 // Default to 0 dB if range fails
                
                // Set volume on Master (0), Left (1), and Right (2) channels
                val successMaster = controlTransferExecutor.setVolume(
                    connection, volumeFeatureUnit.unitId, descriptors.audioControlInterfaceNumber, 0, maxVolumeFound
                )
                val successLeft = controlTransferExecutor.setVolume(
                    connection, volumeFeatureUnit.unitId, descriptors.audioControlInterfaceNumber, 1, maxVolumeFound
                )
                val successRight = controlTransferExecutor.setVolume(
                    connection, volumeFeatureUnit.unitId, descriptors.audioControlInterfaceNumber, 2, maxVolumeFound
                )
                
                // Return true if at least one channel was configured successfully
                successMaster || successLeft || successRight
            }
            
            if (strategyResult.success) {
                return@withContext ConfigurationResult.Success(
                    volumeSet = maxVolumeFound,
                    volumeMax = maxVolumeFound,
                    phase = strategyResult.phaseUsed
                )
            } else {
                return@withContext ConfigurationResult.Failure(
                    ConfigurationError.AllPhasesFailed,
                    phase = strategyResult.phaseUsed
                )
            }
        }
    }
}
