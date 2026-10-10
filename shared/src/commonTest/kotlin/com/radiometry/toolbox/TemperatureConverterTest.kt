package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemperatureConverterTest {

    @Test
    fun testCelsiusToKelvin() {
        val result = convertTemperature(0.0, TemperatureUnit.CELSIUS, TemperatureUnit.KELVIN)
        assertEquals(273.15, result, 0.001)
    }

    @Test
    fun testCelsiusToFahrenheit() {
        val result = convertTemperature(100.0, TemperatureUnit.CELSIUS, TemperatureUnit.FAHRENHEIT)
        assertEquals(212.0, result, 0.001)
    }

    @Test
    fun testKelvinToCelsius() {
        val result = convertTemperature(300.0, TemperatureUnit.KELVIN, TemperatureUnit.CELSIUS)
        assertEquals(26.85, result, 0.001)
    }

    @Test
    fun testFahrenheitToCelsius() {
        val result = convertTemperature(-40.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.CELSIUS)
        assertEquals(-40.0, result, 0.001)
    }

    @Test
    fun testFahrenheitToKelvin() {
        val result = convertTemperature(32.0, TemperatureUnit.FAHRENHEIT, TemperatureUnit.KELVIN)
        assertEquals(273.15, result, 0.001)
    }

    @Test
    fun testSameUnitConversion() {
        val celsius = convertTemperature(25.0, TemperatureUnit.CELSIUS, TemperatureUnit.CELSIUS)
        assertEquals(25.0, celsius, 0.001)

        val kelvin = convertTemperature(300.0, TemperatureUnit.KELVIN, TemperatureUnit.KELVIN)
        assertEquals(300.0, kelvin, 0.001)

        val fahrenheit = convertTemperature(98.6, TemperatureUnit.FAHRENHEIT, TemperatureUnit.FAHRENHEIT)
        assertEquals(98.6, fahrenheit, 0.001)
    }

    @Test
    fun testFormatTemperature() {
        assertEquals("300", formatTemperature(300.0))
        assertEquals("26.85", formatTemperature(26.850012))
        assertEquals("-40", formatTemperature(-40.0))
        assertEquals("98.6", formatTemperature(98.6))
    }

    @Test
    fun testBlackbodyColorDescription() {
        assertEquals("Infrared (Invisible / Black)", blackbodyColorDescription(300.0))
        assertEquals("Faint Dull Red Glow", blackbodyColorDescription(773.15)) // 500 °C
        assertEquals("Bright Red-Orange Glow", blackbodyColorDescription(1200.0))
        assertEquals("Daylight White", blackbodyColorDescription(5800.0))
        assertEquals("Cool White", blackbodyColorDescription(8000.0))
    }

    @Test
    fun testDullRedGlowAt500Celsius() {
        val kelvin500C = convertTemperature(500.0, TemperatureUnit.CELSIUS, TemperatureUnit.KELVIN) // 773.15 K
        val color = kelvinToBlackbodyColor(kelvin500C)

        // At 500°C (~773 K, Draper point range), human eye perceives a faint dull red glow:
        // Red channel should be non-zero (dull red), Green channel very low, Blue channel zero.
        assertTrue(color.red > 0.1f && color.red < 0.5f, "Red should be a dull red (between 0.1 and 0.5)")
        assertTrue(color.green < 0.1f, "Green should be near zero")
        assertEquals(0.0f, color.blue, "Blue should be 0")
    }
}
