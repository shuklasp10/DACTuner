package com.dactuner.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import com.dactuner.reliability.PhaseCache
import com.dactuner.util.DiagnosticsLogger

enum class ClaimPhase {
    PHASE_1_NO_CLAIM,
    PHASE_2_CLAIM_CONTROL,
    PHASE_3_FORCE_CLAIM
}

data class StrategyResult(
    val success: Boolean,
    val phaseUsed: ClaimPhase?,
    val error: String?
)

class InterfaceClaimStrategy(
    val phaseCache: PhaseCache,
    private val logger: DiagnosticsLogger
) {
    suspend fun executeWithBestStrategy(
        vendorId: Int,
        productId: Int,
        connection: UsbDeviceConnection,
        audioControlInterface: UsbInterface,
        action: suspend (UsbDeviceConnection) -> Boolean
    ): StrategyResult {
        logger.log("CLAIM", "Executing strategy for $vendorId:$productId. Interface ID: ${audioControlInterface.id}")
        
        // Try cached phase first
        val cachedPhase = phaseCache.getCachedPhase(vendorId, productId)
        if (cachedPhase != null) {
            logger.log("CLAIM", "Found cached phase: $cachedPhase. Attempting it first...")
            val result = executePhase(cachedPhase, connection, audioControlInterface, action)
            if (result) {
                logger.log("CLAIM", "Cached phase $cachedPhase succeeded. Execution complete.")
                return StrategyResult(true, cachedPhase, null)
            }
            logger.log("CLAIM", "Cached phase $cachedPhase failed, falling back to full strategy...")
        } else {
            logger.log("CLAIM", "No cached phase found for $vendorId:$productId. Trying all phases...")
        }

        // Try all phases in order
        for (phase in ClaimPhase.values()) {
            logger.log("CLAIM", "Evaluating Phase: $phase")
            if (phase == cachedPhase) {
                logger.log("CLAIM", "Skipping Phase $phase (already tried via cache)")
                continue // Already tried
            }
            
            val result = executePhase(phase, connection, audioControlInterface, action)
            if (result) {
                logger.log("CLAIM", "Phase $phase succeeded! Caching it for future use.")
                phaseCache.cachePhase(vendorId, productId, phase)
                return StrategyResult(true, phase, null)
            } else {
                logger.log("CLAIM", "Phase $phase failed.")
            }
        }

        logger.log("CLAIM", "CRITICAL: All claim phases exhausted and failed.")
        return StrategyResult(false, null, "All claim phases failed")
    }

    private suspend fun executePhase(
        phase: ClaimPhase,
        connection: UsbDeviceConnection,
        usbInterface: UsbInterface,
        action: suspend (UsbDeviceConnection) -> Boolean
    ): Boolean {
        return try {
            when (phase) {
                ClaimPhase.PHASE_1_NO_CLAIM -> {
                    logger.log("CLAIM", "[Phase 1] Executing action WITHOUT claiming interface ${usbInterface.id}")
                    val result = action(connection)
                    logger.log("CLAIM", "[Phase 1] Action returned: $result")
                    result
                }
                ClaimPhase.PHASE_2_CLAIM_CONTROL -> {
                    logger.log("CLAIM", "[Phase 2] Attempting claimInterface(force=false) on interface ${usbInterface.id}")
                    val claimed = connection.claimInterface(usbInterface, false)
                    logger.log("CLAIM", "[Phase 2] claimInterface(force=false) result: $claimed")
                    if (claimed) {
                        logger.log("CLAIM", "[Phase 2] Executing action holding claim")
                        val result = action(connection)
                        logger.log("CLAIM", "[Phase 2] Action finished, result: $result. Releasing interface ${usbInterface.id}")
                        connection.releaseInterface(usbInterface)
                        logger.log("CLAIM", "[Phase 2] Interface released")
                        result
                    } else {
                        logger.log("CLAIM", "[Phase 2] Failed to claim interface, skipping action")
                        false
                    }
                }
                ClaimPhase.PHASE_3_FORCE_CLAIM -> {
                    logger.log("CLAIM", "[Phase 3] Attempting claimInterface(force=true) on interface ${usbInterface.id}")
                    val claimed = connection.claimInterface(usbInterface, true)
                    logger.log("CLAIM", "[Phase 3] claimInterface(force=true) result: $claimed")
                    if (claimed) {
                        logger.log("CLAIM", "[Phase 3] Executing action holding force claim")
                        val result = action(connection)
                        logger.log("CLAIM", "[Phase 3] Action finished, result: $result. Releasing interface ${usbInterface.id}")
                        connection.releaseInterface(usbInterface)
                        logger.log("CLAIM", "[Phase 3] Interface released")
                        result
                    } else {
                        logger.log("CLAIM", "[Phase 3] Failed to force claim interface, skipping action")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            logger.log("CLAIM", "Exception during phase $phase: ${e.message}", com.dactuner.util.LogLevel.ERROR)
            false
        }
    }
}
