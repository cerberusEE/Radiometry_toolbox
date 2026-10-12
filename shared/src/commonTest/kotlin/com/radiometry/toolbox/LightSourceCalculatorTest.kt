package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals

class LightSourceCalculatorTest {

    @Test
    fun testUnattenuatedIrradiance() {
        val opticalPower = 10.0
        val divergenceSr = 0.2
        val distance = 50.0

        val result = calculateIrradiance(
            opticalPower = opticalPower,
            divergenceSr = divergenceSr,
            distance = distance,
            useAtmosphericCompensation = false,
            transmissionAt1000m = 0.9,
        )

        // Area = 0.2 * 50^2 = 500 m^2
        // Irradiance = 10 / 500 = 0.02 W/m^2
        assertEquals(0.02, result, 1e-6)
    }

    @Test
    fun testAtmosphericTransmissionAtReferenceDistance() {
        val opticalPower = 1.0
        val divergenceSr = 0.1
        val distance = 1000.0 // 1000m reference distance
        val t1000 = 0.9

        val result = calculateIrradiance(
            opticalPower = opticalPower,
            divergenceSr = divergenceSr,
            distance = distance,
            useAtmosphericCompensation = true,
            transmissionAt1000m = t1000,
        )

        // Unattenuated = 1.0 / (0.1 * 1000^2) = 1e-5 W/m^2
        // Transmission at 1000m = 0.9^1 = 0.9
        // Expected = 0.9e-5
        val expected = 1e-5 * 0.9
        assertEquals(expected, result, 1e-10)
    }

    @Test
    fun testBeersLawExtrapolationAtDoubleDistance() {
        val opticalPower = 1.0
        val divergenceSr = 0.1
        val distance = 2000.0
        val t1000 = 0.8

        val result = calculateIrradiance(
            opticalPower = opticalPower,
            divergenceSr = divergenceSr,
            distance = distance,
            useAtmosphericCompensation = true,
            transmissionAt1000m = t1000,
        )

        // Unattenuated = 1.0 / (0.1 * 2000^2) = 2.5e-6 W/m^2
        // Transmission at 2000m = 0.8^(2000/1000) = 0.8^2 = 0.64
        val expected = 2.5e-6 * 0.64
        assertEquals(expected, result, 1e-10)
    }

    @Test
    fun testBeersLawExtrapolationAtHalfDistance() {
        val opticalPower = 1.0
        val divergenceSr = 0.1
        val distance = 500.0
        val t1000 = 0.81

        val result = calculateIrradiance(
            opticalPower = opticalPower,
            divergenceSr = divergenceSr,
            distance = distance,
            useAtmosphericCompensation = true,
            transmissionAt1000m = t1000,
        )

        // Unattenuated = 1.0 / (0.1 * 500^2) = 4e-5 W/m^2
        // Transmission at 500m = 0.81^(500/1000) = sqrt(0.81) = 0.9
        val expected = 4e-5 * 0.9
        assertEquals(expected, result, 1e-10)
    }

    @Test
    fun testPercentageTransmissionInput() {
        val opticalPower = 1.0
        val divergenceSr = 0.1
        val distance = 1000.0

        // User enters "90" (meaning 90%)
        val result = calculateIrradiance(
            opticalPower = opticalPower,
            divergenceSr = divergenceSr,
            distance = distance,
            useAtmosphericCompensation = true,
            transmissionAt1000m = 90.0,
        )

        val expected = 1e-5 * 0.9
        assertEquals(expected, result, 1e-10)
    }

    @Test
    fun testInvalidInputs() {
        // Zero or negative divergence / distance
        assertEquals(0.0, calculateIrradiance(1.0, 0.0, 100.0, false, 0.9))
        assertEquals(0.0, calculateIrradiance(1.0, 0.1, -10.0, false, 0.9))
    }
}
