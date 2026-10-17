package com.radiometry.toolbox

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LowPassFilterTest {

    @Test
    fun testBaseFilterPassesFirstValue() {
        val filter = LowPassFilter(timeConstantSeconds = 0.5)
        val initial = 12.34
        val result = filter.filter(initial)
        assertEquals(initial, result, 1e-5)
    }

    @Test
    fun testBaseFilterSmoothsValues() {
        val filter = LowPassFilter(timeConstantSeconds = 0.5)
        filter.filter(100.0, dtSeconds = 0.06)

        val f1 = filter.filter(200.0, dtSeconds = 0.06)
        assertTrue(f1 in 100.0..120.0, "Base filter should smoothly step towards target: $f1")
    }

    @Test
    fun testCutoffFrequencyConversion() {
        val filter = LowPassFilter.fromCutoffFrequency(cutoffFrequencyHz = 2.0)
        val expectedTau = 1.0 / (2.0 * PI * 2.0)

        assertEquals(expectedTau, filter.timeConstantSeconds, 1e-5)
        assertEquals(2.0, filter.cutoffFrequencyHz, 1e-5)
    }

    @Test
    fun testReset() {
        val filter = LowPassFilter(timeConstantSeconds = 0.5)
        filter.filter(50.0)
        filter.reset()

        val fNew = filter.filter(99.0)
        assertEquals(99.0, fNew, 1e-5, "Filter reset clears history")
    }
}
