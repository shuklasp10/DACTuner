package com.dactuner.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ConfigurationResultTest {

    @Test
    @DisplayName("All ConfigurationError subclasses provide informative userMessages")
    fun `configuration errors have descriptive user messages`() {
        val errors = listOf(
            ConfigurationError.PermissionDenied,
            ConfigurationError.DeviceNotFound,
            ConfigurationError.DeviceBusy,
            ConfigurationError.DescriptorParseFailure,
            ConfigurationError.NoFeatureUnitFound,
            ConfigurationError.AllPhasesFailed,
            ConfigurationError.ControlTransferFailed(-1),
            ConfigurationError.UnexpectedException(IllegalStateException("Test failure"))
        )

        for (error in errors) {
            val msg = error.userMessage
            assertFalse(msg.isBlank(), "userMessage should not be blank for ${error::class.simpleName}")
        }
    }

    @Test
    @DisplayName("DescriptorParseFailure userMessage instructs checking earphones")
    fun `descriptor parse failure provides earphone guidance`() {
        val error = ConfigurationError.DescriptorParseFailure
        assertTrue(error.userMessage.contains("earphones", ignoreCase = true))
    }
}
