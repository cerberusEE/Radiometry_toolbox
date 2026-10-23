package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals

class AtmosphereEstimationTest {

    @Test
    fun testSeaLevelAtmosphereEstimation() {
        val pressure = estimatePressureFromAltitude(0.0)
        assertEquals(1013.25, pressure, 1e-2)
    }

    @Test
    fun testDenverAtmosphereEstimation() {
        val pressure = estimatePressureFromAltitude(1650.0)
        assertEquals(832.0, pressure, 10.0)
    }

    @Test
    fun testLapseRateTemperatureEstimation() {
        val seaLevelTemp = estimateTemperatureFromAltitudeAndLatitude(0.0, 40.0, month = 6)
        val mountainTemp = estimateTemperatureFromAltitudeAndLatitude(2000.0, 40.0, month = 6)

        assertEquals(13.0, seaLevelTemp - mountainTemp, 1e-2)
    }
}
