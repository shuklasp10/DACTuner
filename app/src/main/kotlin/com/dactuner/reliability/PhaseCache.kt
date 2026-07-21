package com.dactuner.reliability

import com.dactuner.usb.ClaimPhase
import com.dactuner.util.PreferencesManager

class PhaseCache(
    private val prefs: PreferencesManager
) {
    fun getCachedPhase(vendorId: Int, productId: Int): ClaimPhase? {
        val ordinal = prefs.getCachedPhase(vendorId, productId)
        if (ordinal != null && ordinal in 0 until ClaimPhase.entries.size) {
            return ClaimPhase.entries[ordinal]
        }
        return null
    }

    fun cachePhase(vendorId: Int, productId: Int, phase: ClaimPhase) {
        prefs.setCachedPhase(vendorId, productId, phase.ordinal)
    }

    fun clearCache() {
        prefs.clearPhaseCache()
    }
}
