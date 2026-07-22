package com.dactuner.reliability

import android.hardware.usb.UsbDeviceConnection
import com.dactuner.usb.UacControlTransferExecutor
import com.dactuner.util.DiagnosticsLogger
import kotlinx.coroutines.delay
import android.hardware.usb.UsbInterface
import com.dactuner.usb.InterfaceClaimStrategy

data class MitigationResult(
    val verified: Boolean,
    val attemptsUsed: Int
)

class HalRaceMitigator(
    private val controlTransferExecutor: UacControlTransferExecutor,
    private val claimStrategy: InterfaceClaimStrategy,
    private val logger: DiagnosticsLogger
) {
    /**
     * Executes the volume set command and then verifies that the volume sticks.
     * Android's AudioService often races us to set the volume on attach, which can
     * overwrite our command. We retry with increasing delays until it verifies or times out.
     */
    suspend fun configureAndVerify(
        vendorId: Int,
        productId: Int,
        connection: UsbDeviceConnection,
        audioControlInterface: UsbInterface,
        featureUnitId: Int,
        interfaceNumber: Int,
        channelCount: Int,
        targetVolume: Int
    ): MitigationResult {
        // Delays between attempts. First attempt is immediate (0ms delay before firing).
        // Then we wait a bit, check if it stuck. If not, wait longer and fire again.
        val delays = listOf(50L, 500L, 1500L, 2500L)
        
        logger.log("HAL_RACE", "Starting configureAndVerify loop. Target volume: $targetVolume, channelCount: $channelCount")
        
        for ((attempt, delayMs) in delays.withIndex()) {
            logger.log("HAL_RACE", "--- Attempt ${attempt + 1}/${delays.size} ---")
            var setAny = false
            
            // Unconditionally set all channels, claiming interface around the entire set block
            logger.log("HAL_RACE", "Requesting claim strategy to execute SET_CUR block...")
            claimStrategy.executeWithBestStrategy(vendorId, productId, connection, audioControlInterface) { conn ->
                logger.log("HAL_RACE", "Claim acquired for SET_CUR block. Iterating channels 0 to $channelCount...")
                for (ch in 0..channelCount) {
                    logger.log("HAL_RACE", "Setting volume on channel $ch to $targetVolume...")
                    val success = controlTransferExecutor.setVolume(
                        conn, featureUnitId, interfaceNumber, ch, targetVolume
                    )
                    logger.log("HAL_RACE", "Set volume on channel $ch result: $success")
                    if (success) setAny = true
                }
                logger.log("HAL_RACE", "Finished iterating channels. Releasing claim.")
                setAny
            }
            
            if (!setAny) {
                logger.log("HAL_RACE", "All setVolume calls failed on attempt ${attempt + 1}")
            } else {
                logger.log("HAL_RACE", "Successfully sent at least one SET_CUR command.")
            }
            
            logger.log("HAL_RACE", "Yielding (delay) for ${delayMs}ms to let HAL settle WITHOUT holding the claim...")
            delay(delayMs)
            logger.log("HAL_RACE", "Woke up from delay. Requesting claim strategy to verify volume...")
            
            // Verify by claiming interface just for the get requests
            var currentVolume: Int? = null
            claimStrategy.executeWithBestStrategy(vendorId, productId, connection, audioControlInterface) { conn ->
                logger.log("HAL_RACE", "Claim acquired for GET_CUR block. Attempting to read Channel 1...")
                currentVolume = controlTransferExecutor.getVolume(
                    conn, featureUnitId, interfaceNumber, 1
                )
                if (currentVolume == null) {
                    logger.log("HAL_RACE", "Failed to read Channel 1, falling back to Channel 0 (Master)...")
                    currentVolume = controlTransferExecutor.getVolume(
                        conn, featureUnitId, interfaceNumber, 0
                    )
                }
                logger.log("HAL_RACE", "Finished GET_CUR block. Read volume: $currentVolume. Releasing claim.")
                currentVolume != null
            }
            
            if (currentVolume != null && currentVolume!! >= targetVolume) {
                logger.log("HAL_RACE", "Volume successfully verified at $currentVolume on attempt ${attempt + 1}")
                return MitigationResult(verified = true, attemptsUsed = attempt + 1)
            }
            
            logger.log(
                "HAL_RACE", 
                "Volume reset detected! (Target: $targetVolume, Found: ${currentVolume ?: "null"}). " +
                "Attempt ${attempt + 1} failed, retrying..."
            )
        }
        
        logger.log("HAL_RACE", "CRITICAL: Failed to mitigate HAL race condition after ${delays.size} attempts.")
        return MitigationResult(verified = false, attemptsUsed = delays.size)
    }
}
