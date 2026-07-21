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
    private val phaseCache: PhaseCache,
    private val logger: DiagnosticsLogger
) {
    suspend fun executeWithBestStrategy(
        vendorId: Int,
        productId: Int,
        connection: UsbDeviceConnection,
        audioControlInterface: UsbInterface,
        action: (UsbDeviceConnection) -> Boolean
    ): StrategyResult {
        // Try cached phase first
        val cachedPhase = phaseCache.getCachedPhase(vendorId, productId)
        if (cachedPhase != null) {
            logger.log("CLAIM", "Using cached phase: $cachedPhase")
            val success = executePhase(cachedPhase, connection, audioControlInterface, action)
            if (success) {
                return StrategyResult(true, cachedPhase, null)
            }
            logger.log("CLAIM", "Cached phase failed, falling back to full strategy")
        }

        // Phase 1: No claim
        logger.log("CLAIM", "Trying Phase 1: No claim")
        if (executePhase(ClaimPhase.PHASE_1_NO_CLAIM, connection, audioControlInterface, action)) {
            phaseCache.cachePhase(vendorId, productId, ClaimPhase.PHASE_1_NO_CLAIM)
            return StrategyResult(true, ClaimPhase.PHASE_1_NO_CLAIM, null)
        }

        // Phase 2: Claim without force
        logger.log("CLAIM", "Trying Phase 2: Claim (force=false)")
        if (executePhase(ClaimPhase.PHASE_2_CLAIM_CONTROL, connection, audioControlInterface, action)) {
            phaseCache.cachePhase(vendorId, productId, ClaimPhase.PHASE_2_CLAIM_CONTROL)
            return StrategyResult(true, ClaimPhase.PHASE_2_CLAIM_CONTROL, null)
        }

        // Phase 3: Force claim
        logger.log("CLAIM", "Trying Phase 3: Claim (force=true)")
        if (executePhase(ClaimPhase.PHASE_3_FORCE_CLAIM, connection, audioControlInterface, action)) {
            phaseCache.cachePhase(vendorId, productId, ClaimPhase.PHASE_3_FORCE_CLAIM)
            return StrategyResult(true, ClaimPhase.PHASE_3_FORCE_CLAIM, null)
        }

        return StrategyResult(false, null, "All claim phases failed")
    }

    private fun executePhase(
        phase: ClaimPhase,
        connection: UsbDeviceConnection,
        usbInterface: UsbInterface,
        action: (UsbDeviceConnection) -> Boolean
    ): Boolean {
        return try {
            when (phase) {
                ClaimPhase.PHASE_1_NO_CLAIM -> {
                    action(connection)
                }
                ClaimPhase.PHASE_2_CLAIM_CONTROL -> {
                    val claimed = connection.claimInterface(usbInterface, false)
                    if (claimed) {
                        val result = action(connection)
                        connection.releaseInterface(usbInterface)
                        result
                    } else {
                        false
                    }
                }
                ClaimPhase.PHASE_3_FORCE_CLAIM -> {
                    val claimed = connection.claimInterface(usbInterface, true)
                    if (claimed) {
                        val result = action(connection)
                        connection.releaseInterface(usbInterface)
                        result
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            logger.log("CLAIM", "Exception during phase \$phase: \${e.message}", com.dactuner.util.LogLevel.ERROR)
            false
        }
    }
}
