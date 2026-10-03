package com.dactuner.util

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PreferencesManager].
 */
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private val inMemoryPrefs = mutableMapOf<String, Any>()

    @BeforeEach
    fun setUp() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        inMemoryPrefs.clear()
        inMemoryPrefs["phase_cache_version"] = 2 // match current version

        every { context.getSharedPreferences("dactuner_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor

        every { sharedPreferences.getInt(any(), any()) } answers {
            (inMemoryPrefs[firstArg()] as? Int) ?: secondArg()
        }
        every { sharedPreferences.getBoolean(any(), any()) } answers {
            (inMemoryPrefs[firstArg()] as? Boolean) ?: secondArg()
        }
        every { sharedPreferences.getLong(any(), any()) } answers {
            (inMemoryPrefs[firstArg()] as? Long) ?: secondArg()
        }
        every { sharedPreferences.all } answers { inMemoryPrefs }

        every { editor.putBoolean(any(), any()) } answers {
            inMemoryPrefs[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { editor.putInt(any(), any()) } answers {
            inMemoryPrefs[firstArg()] = secondArg<Int>()
            editor
        }
        every { editor.putLong(any(), any()) } answers {
            inMemoryPrefs[firstArg()] = secondArg<Long>()
            editor
        }
        every { editor.remove(any()) } answers {
            inMemoryPrefs.remove(firstArg())
            editor
        }
        every { editor.apply() } answers { }
    }

    @Test
    @DisplayName("backgroundModeEnabled defaults to true")
    fun `backgroundModeEnabled defaults to true`() {
        val manager = PreferencesManager(context)
        assertTrue(manager.backgroundModeEnabled)
    }

    @Test
    @DisplayName("backgroundModeEnabled updates and persists value")
    fun `backgroundModeEnabled updates and persists`() {
        val manager = PreferencesManager(context)
        manager.backgroundModeEnabled = false

        assertFalse(manager.backgroundModeEnabled)
        verify { editor.putBoolean("background_mode_enabled", false) }
    }

    @Test
    @DisplayName("showNotifications defaults to true")
    fun `showNotifications defaults to true`() {
        val manager = PreferencesManager(context)
        assertTrue(manager.showNotifications)
    }

    @Test
    @DisplayName("showNotifications updates and persists value")
    fun `showNotifications updates and persists`() {
        val manager = PreferencesManager(context)
        manager.showNotifications = false

        assertFalse(manager.showNotifications)
        verify { editor.putBoolean("show_notifications", false) }
    }

    @Test
    @DisplayName("autoConfigureEnabled defaults to true")
    fun `autoConfigureEnabled defaults to true`() {
        val manager = PreferencesManager(context)
        assertTrue(manager.autoConfigureEnabled)
    }

    @Test
    @DisplayName("autoConfigureEnabled updates and persists value")
    fun `autoConfigureEnabled updates and persists`() {
        val manager = PreferencesManager(context)
        manager.autoConfigureEnabled = false

        assertFalse(manager.autoConfigureEnabled)
        verify { editor.putBoolean("auto_configure_enabled", false) }
    }

    @Test
    @DisplayName("phase cache stores and retrieves phase ordinal")
    fun `phase cache stores and retrieves phase`() {
        val manager = PreferencesManager(context)
        manager.setCachedPhase(0x05AC, 0x110A, 1)

        assertEquals(1, manager.getCachedPhase(0x05AC, 0x110A))
    }
}
