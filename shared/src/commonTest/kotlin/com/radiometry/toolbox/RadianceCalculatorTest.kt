package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RadianceCalculatorTest {

    @Test
    fun testConvertToWavelengthInMeters() {
        // Nanometers (380 nm -> 3.8e-7 m)
        val nmResult = convertToWavelengthInMeters(380.0, WavelengthUnit.NANOMETERS)
        assertNotNull(nmResult)
        assertEquals(3.8e-7, nmResult, 1e-12)

        // Micrometers (10.0 µm -> 1.0e-5 m)
        val umResult = convertToWavelengthInMeters(10.0, WavelengthUnit.MICROMETERS)
        assertNotNull(umResult)
        assertEquals(1.0e-5, umResult, 1e-12)

        // Inverse Centimeters (10000 cm⁻¹ -> 1.0 µm = 1.0e-6 m)
        val cmResult = convertToWavelengthInMeters(10000.0, WavelengthUnit.INVERSE_CENTIMETERS)
        assertNotNull(cmResult)
        assertEquals(1.0e-6, cmResult, 1e-12)

        // Invalid inputs
        assertNull(convertToWavelengthInMeters(0.0, WavelengthUnit.NANOMETERS))
        assertNull(convertToWavelengthInMeters(-100.0, WavelengthUnit.MICROMETERS))
        assertNull(convertToWavelengthInMeters(Double.NaN, WavelengthUnit.INVERSE_CENTIMETERS))
    }

    @Test
    fun testConvertTemperatureToKelvin() {
        // Celsius (0 °C -> 273.15 K)
        val cResult = convertTemperatureToKelvin(0.0, TemperatureUnit.CELSIUS)
        assertNotNull(cResult)
        assertEquals(273.15, cResult, 0.001)

        // Kelvin (300 K -> 300 K)
        val kResult = convertTemperatureToKelvin(300.0, TemperatureUnit.KELVIN)
        assertNotNull(kResult)
        assertEquals(300.0, kResult, 0.001)

        // Fahrenheit (32 °F -> 273.15 K)
        val fResult = convertTemperatureToKelvin(32.0, TemperatureUnit.FAHRENHEIT)
        assertNotNull(fResult)
        assertEquals(273.15, fResult, 0.001)

        // Below absolute zero
        assertTrue(convertTemperatureToKelvin(-300.0, TemperatureUnit.CELSIUS).isNaN())
        assertTrue(convertTemperatureToKelvin(-10.0, TemperatureUnit.KELVIN).isNaN())
    }

    @Test
    fun testConvertWavelength() {
        // Nanometers to Micrometers (380 nm -> 0.38 µm)
        val nmToUm = convertWavelength(380.0, WavelengthUnit.NANOMETERS, WavelengthUnit.MICROMETERS)
        assertNotNull(nmToUm)
        assertEquals(0.38, nmToUm, 1e-6)

        // Micrometers to Nanometers (0.78 µm -> 780 nm)
        val umToNm = convertWavelength(0.78, WavelengthUnit.MICROMETERS, WavelengthUnit.NANOMETERS)
        assertNotNull(umToNm)
        assertEquals(780.0, umToNm, 1e-6)

        // Nanometers to Inverse Centimeters (500 nm -> 20000 cm⁻¹)
        val nmToCm = convertWavelength(500.0, WavelengthUnit.NANOMETERS, WavelengthUnit.INVERSE_CENTIMETERS)
        assertNotNull(nmToCm)
        assertEquals(20000.0, nmToCm, 1e-4)

        // Inverse Centimeters to Nanometers (20000 cm⁻¹ -> 500 nm)
        val cmToNm = convertWavelength(20000.0, WavelengthUnit.INVERSE_CENTIMETERS, WavelengthUnit.NANOMETERS)
        assertNotNull(cmToNm)
        assertEquals(500.0, cmToNm, 1e-4)

        // Invalid inputs
        assertNull(convertWavelength(0.0, WavelengthUnit.NANOMETERS, WavelengthUnit.MICROMETERS))
        assertNull(convertWavelength(-100.0, WavelengthUnit.MICROMETERS, WavelengthUnit.NANOMETERS))
        assertNull(convertWavelength(Double.NaN, WavelengthUnit.INVERSE_CENTIMETERS, WavelengthUnit.NANOMETERS))
    }

    @Test
    fun testCalculateRadiance() {
        // Solar visible range (380 - 780 nm at 5800 K)
        val startMeters = 380e-9
        val stopMeters = 780e-9
        val tempK = 5800.0
        val solarRadiance = calculateRadiance(startMeters, stopMeters, tempK)
        assertTrue(solarRadiance > 5.0e6, "Solar radiance should be > 5e6 W/sr/m², was $solarRadiance")

        // Room temp IR (8 - 14 µm at 300 K)
        val irStart = 8e-6
        val irStop = 14e-6
        val roomTempK = 300.0
        val irRadiance = calculateRadiance(irStart, irStop, roomTempK)
        assertTrue(irRadiance in 50.0..60.0, "Room temp IR radiance should be ~54.9 W/sr/m², was $irRadiance")

        // Equal start and stop wavelengths -> 0.0
        assertEquals(0.0, calculateRadiance(1e-6, 1e-6, 300.0), 1e-12)

        // Invalid temperature -> 0.0
        assertEquals(0.0, calculateRadiance(380e-9, 780e-9, -100.0), 1e-12)
    }

    @Test
    fun testFormatRadiance() {
        assertEquals("Invalid Input", formatRadiance(Double.NaN))
        assertEquals("Invalid Input", formatRadiance(-10.0))
        assertEquals("0 W/sr/m²", formatRadiance(0.0))

        assertEquals("29.80 W/sr/m²", formatRadiance(29.8021))
        assertEquals("2.58e7 W/sr/m²", formatRadiance(25800000.0))
        assertEquals("1.23e-4 W/sr/m²", formatRadiance(0.000123))
    }

    @Test
    fun testCalculatePeakWavelength() {
        // Sun @ 5800 K -> ~499.6 nm -> ~0.5 µm
        val sunNm = calculatePeakWavelength(5800.0, WavelengthUnit.NANOMETERS)
        assertNotNull(sunNm)
        assertEquals(499.615, sunNm, 0.1)

        val sunUm = calculatePeakWavelength(5800.0, WavelengthUnit.MICROMETERS)
        assertNotNull(sunUm)
        assertEquals(0.4996, sunUm, 0.01)

        val sunCm = calculatePeakWavelength(5800.0, WavelengthUnit.INVERSE_CENTIMETERS)
        assertNotNull(sunCm)
        assertEquals(20015.38, sunCm, 1.0)

        // Room Temp @ 300 K -> ~9.66 µm -> ~9659 nm
        val roomUm = calculatePeakWavelength(300.0, WavelengthUnit.MICROMETERS)
        assertNotNull(roomUm)
        assertEquals(9.66, roomUm, 0.01)

        // Invalid temperatures
        assertNull(calculatePeakWavelength(0.0, WavelengthUnit.NANOMETERS))
        assertNull(calculatePeakWavelength(-100.0, WavelengthUnit.MICROMETERS))
        assertNull(calculatePeakWavelength(Double.NaN, WavelengthUnit.INVERSE_CENTIMETERS))
    }

    @Test
    fun testFormatPeakWavelength() {
        assertEquals("499.62 nm", formatPeakWavelength(5800.0, WavelengthUnit.NANOMETERS))
        assertEquals("0.50 µm", formatPeakWavelength(5800.0, WavelengthUnit.MICROMETERS))
        assertEquals("20015.38 cm⁻¹", formatPeakWavelength(5800.0, WavelengthUnit.INVERSE_CENTIMETERS))

        assertEquals("9.66 µm", formatPeakWavelength(300.0, WavelengthUnit.MICROMETERS))

        assertEquals("Invalid Input", formatPeakWavelength(null, WavelengthUnit.NANOMETERS))
        assertEquals("Invalid Input", formatPeakWavelength(-50.0, WavelengthUnit.MICROMETERS))
        assertEquals("Invalid Input", formatPeakWavelength(Double.NaN, WavelengthUnit.INVERSE_CENTIMETERS))
    }
}
