package com.radiometry.toolbox

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalarLowPassFilterTest {

    @Test
    fun testInitialReadingPassesThroughUnfiltered() {
        val filter = ScalarLowPassFilter(timeConstantSeconds = 0.5)
        val initialLux = 450.0f
        val result = filter.filter(initialLux)
        assertEquals(450.0f, result, 1e-4f, "First reading should initialize filter without lag")
    }

    @Test
    fun testJitterFiltering() {
        val filter = ScalarLowPassFilter(timeConstantSeconds = 0.5)

        // Initialize at 500 lx
        filter.filter(500.0, dtSeconds = 0.06)

        // Fluctuate raw readings: 550, 450, 540, 460
        val f1 = filter.filter(550.0, dtSeconds = 0.06)
        val f2 = filter.filter(450.0, dtSeconds = 0.06)
        val f3 = filter.filter(540.0, dtSeconds = 0.06)
        val f4 = filter.filter(460.0, dtSeconds = 0.06)

        // Verify outputs smooth out raw fluctuations
        assertTrue(f1 in 500.0..510.0, "f1 should smooth step towards 550: $f1")
        assertTrue(f2 in 490.0..505.0, "f2 should smooth step towards 450: $f2")
        assertTrue(f3 in 490.0..510.0, "f3 should stay near 500: $f3")
        assertTrue(f4 in 490.0..510.0, "f4 should stay near 500: $f4")
    }

    @Test
    fun testTunableAlphaOverride() {
        val fastFilter = ScalarLowPassFilter(alpha = 0.9)
        fastFilter.filter(100.0)
        val fastResult = fastFilter.filter(200.0)

        val slowFilter = ScalarLowPassFilter(alpha = 0.1)
        slowFilter.filter(100.0)
        val slowResult = slowFilter.filter(200.0)

        assertEquals(190.0, fastResult, 1e-3, "Fast filter with alpha=0.9 moves 90% of delta")
        assertEquals(110.0, slowResult, 1e-3, "Slow filter with alpha=0.1 moves 10% of delta")
    }

    @Test
    fun testCutoffFrequencyConversion() {
        val filter = ScalarLowPassFilter.fromCutoffFrequency(cutoffFrequencyHz = 2.0)
        val expectedTau = 1.0 / (2.0 * PI * 2.0)

        assertEquals(expectedTau, filter.timeConstantSeconds, 1e-5)
        assertEquals(2.0, filter.cutoffFrequencyHz, 1e-5)

        filter.timeConstantSeconds = 0.5
        val expectedFc = 1.0 / (2.0 * PI * 0.5)
        assertEquals(expectedFc, filter.cutoffFrequencyHz, 1e-5)
    }

    @Test
    fun testResetClearsState() {
        val filter = ScalarLowPassFilter(timeConstantSeconds = 0.5)
        filter.filter(500.0)
        filter.filter(600.0, dtSeconds = 0.06)

        filter.reset()

        val fNew = filter.filter(100.0)
        assertEquals(100.0, fNew, 1e-4, "After reset, first reading immediately initializes without lag")
    }

    @Test
    fun testRepeatedSamplingSettlesToTargetValue() {
        val filter = ScalarLowPassFilter(timeConstantSeconds = 0.1)
        // Initialize at 100.0 lx
        filter.filter(100.0, dtSeconds = 0.03)

        // Raw value jumps to 500.0 lx and stays constant (simulating event-driven sensor pausing events)
        var filtered = 0.0
        repeat(30) {
            filtered = filter.filter(500.0, dtSeconds = 0.03)
        }

        assertEquals(500.0, filtered, 1.0, "Repeated filtering with constant raw target value settles to target")
    }
}
