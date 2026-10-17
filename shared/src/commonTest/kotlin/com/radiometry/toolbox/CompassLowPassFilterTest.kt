package com.radiometry.toolbox

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompassLowPassFilterTest {

    @Test
    fun testInitialReadingPassesThroughUnfiltered() {
        val filter = CompassLowPassFilter(timeConstantSeconds = 0.5)
        val initialRawHeading = 123.4f
        val result = filter.filter(initialRawHeading)
        assertEquals(123.4f, result, 1e-4f, "First reading should initialize filter without lag")
    }

    @Test
    fun testJitterFiltering() {
        val filter = CompassLowPassFilter(timeConstantSeconds = 0.5)

        // Initialize at 180°
        filter.filter(180.0, dtSeconds = 0.06)

        // Raw noise fluctuates: 185.0, 175.0, 184.0, 176.0
        val f1 = filter.filter(185.0, dtSeconds = 0.06)
        val f2 = filter.filter(175.0, dtSeconds = 0.06)
        val f3 = filter.filter(184.0, dtSeconds = 0.06)
        val f4 = filter.filter(176.0, dtSeconds = 0.06)

        // Verify the filtered output smoothed out raw fluctuations
        assertTrue(f1 in 180.0..181.0, "f1 should smooth step towards 185: $f1")
        assertTrue(f2 in 179.0..180.5, "f2 should smooth step towards 175: $f2")
        assertTrue(f3 in 179.0..181.0, "f3 should stay close to 180: $f3")
        assertTrue(f4 in 179.0..181.0, "f4 should stay close to 180: $f4")
    }

    @Test
    fun testAngleWrapAroundNear360And0() {
        val filter = CompassLowPassFilter(timeConstantSeconds = 0.5)

        // Initialize near 359°
        val init = filter.filter(359.0)
        assertEquals(359.0, init, 1e-4)

        // Raw reading jumps across 360° to 1°
        val f1 = filter.filter(1.0, dtSeconds = 0.06)

        // Should move forward across 360°/0° boundary (e.g., ~359.2°), NOT average to 180°
        assertTrue(f1 !in 1.5..358.5, "Filtered angle must stay near 0°/360° boundary, got: $f1")

        // Next reading still at 1°
        val f2 = filter.filter(1.0, dtSeconds = 0.06)
        assertTrue(f2 !in 2.0..359.0, "Filtered angle continues smooth transition to 1°, got: $f2")
    }

    @Test
    fun testAngleWrapAroundReverse0To360() {
        val filter = CompassLowPassFilter(timeConstantSeconds = 0.5)

        // Initialize near 1°
        filter.filter(1.0)

        // Raw reading jumps backward to 359°
        val f1 = filter.filter(359.0, dtSeconds = 0.06)

        // Should move backward across 0°/360° boundary (e.g., ~0.8° or ~0.0° or ~359.8°), NOT average to 180°
        assertTrue(f1 !in 1.5..358.0, "Filtered angle must handle reverse wrap-around, got: $f1")
    }

    @Test
    fun testTunableAlphaOverride() {
        // High alpha (0.9) responds quickly
        val fastFilter = CompassLowPassFilter(alpha = 0.9)
        fastFilter.filter(100.0)
        val fastResult = fastFilter.filter(200.0)

        // Low alpha (0.1) responds slowly
        val slowFilter = CompassLowPassFilter(alpha = 0.1)
        slowFilter.filter(100.0)
        val slowResult = slowFilter.filter(200.0)

        assertEquals(190.0, fastResult, 1e-3, "Fast filter with alpha=0.9 moves 90% of delta")
        assertEquals(110.0, slowResult, 1e-3, "Slow filter with alpha=0.1 moves 10% of delta")
    }

    @Test
    fun testCutoffFrequencyConversion() {
        val filter = CompassLowPassFilter.fromCutoffFrequency(cutoffFrequencyHz = 2.0)
        val expectedTau = 1.0 / (2.0 * PI * 2.0)

        assertEquals(expectedTau, filter.timeConstantSeconds, 1e-5)
        assertEquals(2.0, filter.cutoffFrequencyHz, 1e-5)

        // Setting time constant updates cutoff frequency
        filter.timeConstantSeconds = 0.5
        val expectedFc = 1.0 / (2.0 * PI * 0.5)
        assertEquals(expectedFc, filter.cutoffFrequencyHz, 1e-5)
    }

    @Test
    fun testResetClearsState() {
        val filter = CompassLowPassFilter(timeConstantSeconds = 0.5)
        filter.filter(180.0)
        filter.filter(200.0, dtSeconds = 0.06)

        filter.reset()

        // After reset, first reading should immediately become 90° without lag
        val fNew = filter.filter(90.0)
        assertEquals(90.0, fNew, 1e-4)
    }

    @Test
    fun testNormalizeHeading360And180() {
        assertEquals(10.0, CompassLowPassFilter.normalizeHeading360(370.0), 1e-5)
        assertEquals(350.0, CompassLowPassFilter.normalizeHeading360(-10.0), 1e-5)

        assertEquals(20.0, CompassLowPassFilter.normalizeAngle180(10.0 - 350.0), 1e-5)
        assertEquals(-20.0, CompassLowPassFilter.normalizeAngle180(350.0 - 10.0), 1e-5)
    }
}
