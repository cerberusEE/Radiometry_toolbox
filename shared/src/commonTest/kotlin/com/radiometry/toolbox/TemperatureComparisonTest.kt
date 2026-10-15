package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemperatureComparisonTest {

    @Test
    fun testBlackbodyRadianceContrast() {
        // LWIR range: 8 µm to 12 µm (8e-6 m to 12e-6 m)
        val startMeters = 8.0e-6
        val stopMeters = 12.0e-6
        val temp1K = 310.0
        val temp2K = 300.0
        val tempBgK = 290.0

        val result = calculateRadianceComparison(
            startWavelengthMeters = startMeters,
            stopWavelengthMeters = stopMeters,
            temp1Kelvin = temp1K,
            emissivity1 = 1.0,
            temp2Kelvin = temp2K,
            emissivity2 = 1.0,
            tempBgKelvin = tempBgK,
        )

        assertNotNull(result)
        val rad1 = calculateRadiance(startMeters, stopMeters, temp1K)
        val rad2 = calculateRadiance(startMeters, stopMeters, temp2K)
        val expectedContrast = kotlin.math.abs(rad1 - rad2)

        assertEquals(rad1, result.radiance1, 1e-6)
        assertEquals(rad2, result.radiance2, 1e-6)
        assertEquals(expectedContrast, result.contrast, 1e-6)
        assertEquals(0.0, result.reflected1, 1e-12)
        assertEquals(0.0, result.reflected2, 1e-12)
        assertTrue(result.contrast > 0.0, "Contrast between 300K and 310K should be positive")
    }

    @Test
    fun testBackgroundReflectionWithEmissivityBelowOne() {
        val startMeters = 8.0e-6
        val stopMeters = 12.0e-6
        val temp1K = 310.0
        val temp2K = 300.0
        val tempBgK = 290.0
        val e1 = 0.8
        val e2 = 0.6

        val result = calculateRadianceComparison(
            startWavelengthMeters = startMeters,
            stopWavelengthMeters = stopMeters,
            temp1Kelvin = temp1K,
            emissivity1 = e1,
            temp2Kelvin = temp2K,
            emissivity2 = e2,
            tempBgKelvin = tempBgK,
        )

        assertNotNull(result)
        val bbRad1 = calculateRadiance(startMeters, stopMeters, temp1K)
        val bbRad2 = calculateRadiance(startMeters, stopMeters, temp2K)
        val bbRadBg = calculateRadiance(startMeters, stopMeters, tempBgK)

        val expectedEmitted1 = 0.8 * bbRad1
        val expectedReflected1 = 0.2 * bbRadBg
        val expectedRad1 = expectedEmitted1 + expectedReflected1

        val expectedEmitted2 = 0.6 * bbRad2
        val expectedReflected2 = 0.4 * bbRadBg
        val expectedRad2 = expectedEmitted2 + expectedReflected2

        val expectedContrast = kotlin.math.abs(expectedRad1 - expectedRad2)

        assertEquals(expectedEmitted1, result.emitted1, 1e-6)
        assertEquals(expectedReflected1, result.reflected1, 1e-6)
        assertEquals(expectedRad1, result.radiance1, 1e-6)

        assertEquals(expectedEmitted2, result.emitted2, 1e-6)
        assertEquals(expectedReflected2, result.reflected2, 1e-6)
        assertEquals(expectedRad2, result.radiance2, 1e-6)

        assertEquals(expectedContrast, result.contrast, 1e-6)
    }

    @Test
    fun testThermalEquilibriumWithBackground() {
        // When T1 = T2 = Tbg, total radiance equals blackbody radiance for any emissivity
        val startMeters = 8.0e-6
        val stopMeters = 12.0e-6
        val tempK = 300.0

        val result = calculateRadianceComparison(
            startWavelengthMeters = startMeters,
            stopWavelengthMeters = stopMeters,
            temp1Kelvin = tempK,
            emissivity1 = 0.4,
            temp2Kelvin = tempK,
            emissivity2 = 0.9,
            tempBgKelvin = tempK,
        )

        assertNotNull(result)
        val bbRad = calculateRadiance(startMeters, stopMeters, tempK)
        assertEquals(bbRad, result.radiance1, 1e-6)
        assertEquals(bbRad, result.radiance2, 1e-6)
        assertEquals(0.0, result.contrast, 1e-10)
    }

    @Test
    fun testZeroContrastForIdenticalGreybodiesAndBackground() {
        val startMeters = 3e-6
        val stopMeters = 5e-6
        val tempK = 500.0
        val tempBgK = 300.0
        val emissivity = 0.85

        val contrast = calculateRadianceContrast(
            startWavelengthMeters = startMeters,
            stopWavelengthMeters = stopMeters,
            temp1Kelvin = tempK,
            emissivity1 = emissivity,
            temp2Kelvin = tempK,
            emissivity2 = emissivity,
            tempBgKelvin = tempBgK,
        )

        assertNotNull(contrast)
        assertEquals(0.0, contrast, 1e-12)
    }

    @Test
    fun testInvalidInputsReturnNull() {
        // Temperature <= 0 K
        assertNull(
            calculateRadianceComparison(
                startWavelengthMeters = 8e-6,
                stopWavelengthMeters = 12e-6,
                temp1Kelvin = -10.0,
                emissivity1 = 1.0,
                temp2Kelvin = 300.0,
                emissivity2 = 1.0,
                tempBgKelvin = 300.0,
            )
        )

        // Invalid Background Temperature <= 0 K
        assertNull(
            calculateRadianceComparison(
                startWavelengthMeters = 8e-6,
                stopWavelengthMeters = 12e-6,
                temp1Kelvin = 300.0,
                emissivity1 = 1.0,
                temp2Kelvin = 300.0,
                emissivity2 = 1.0,
                tempBgKelvin = -50.0,
            )
        )

        // Negative emissivity
        assertNull(
            calculateRadianceComparison(
                startWavelengthMeters = 8e-6,
                stopWavelengthMeters = 12e-6,
                temp1Kelvin = 300.0,
                emissivity1 = -0.5,
                temp2Kelvin = 300.0,
                emissivity2 = 1.0,
                tempBgKelvin = 300.0,
            )
        )

        // Zero wavelength or equal start and stop
        assertNull(
            calculateRadianceComparison(
                startWavelengthMeters = 0.0,
                stopWavelengthMeters = 12e-6,
                temp1Kelvin = 300.0,
                emissivity1 = 1.0,
                temp2Kelvin = 300.0,
                emissivity2 = 1.0,
                tempBgKelvin = 300.0,
            )
        )

        assertNull(
            calculateRadianceComparison(
                startWavelengthMeters = 10e-6,
                stopWavelengthMeters = 10e-6,
                temp1Kelvin = 300.0,
                emissivity1 = 1.0,
                temp2Kelvin = 300.0,
                emissivity2 = 1.0,
                tempBgKelvin = 300.0,
            )
        )
    }
}
