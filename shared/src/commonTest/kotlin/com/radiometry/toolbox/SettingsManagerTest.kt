package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsManagerTest {

    @Test
    fun testStringPersistence() {
        val settingsManager = InMemorySettingsManager()
        assertEquals("default", settingsManager.getString("key1", "default"))

        settingsManager.putString("key1", "custom_value")
        assertEquals("custom_value", settingsManager.getString("key1", "default"))
    }

    @Test
    fun testIntPersistence() {
        val settingsManager = InMemorySettingsManager()
        assertEquals(0, settingsManager.getInt("tool_index", 0))

        settingsManager.putInt("tool_index", 2)
        assertEquals(2, settingsManager.getInt("tool_index", 0))
    }

    @Test
    fun testBooleanPersistence() {
        val settingsManager = InMemorySettingsManager()
        assertFalse(settingsManager.getBoolean("atm_comp", false))

        settingsManager.putBoolean("atm_comp", true)
        assertTrue(settingsManager.getBoolean("atm_comp", false))
    }

    @Test
    fun testSharedTemperatureStatePersistence() {
        val settingsManager = InMemorySettingsManager()
        settingsManager.putString("key_shared_temperature", "300")
        settingsManager.putString("key_shared_temperature_unit", TemperatureUnit.CELSIUS.name)

        assertEquals("300", settingsManager.getString("key_shared_temperature", "5800"))
        assertEquals(TemperatureUnit.CELSIUS.name, settingsManager.getString("key_shared_temperature_unit", TemperatureUnit.KELVIN.name))
    }
}
