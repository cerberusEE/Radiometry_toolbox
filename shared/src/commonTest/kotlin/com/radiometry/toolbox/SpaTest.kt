package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals

class SpaTest {

    @Test
    fun testNrelSpaGoldenExample() {
        // NREL SPA Golden, CO Test Case (2003-10-17 12:30:30 local = UTC-7)
        // Lat: 39.742476, Lon: -105.1786
        val location = SpaLocation(
            latitudeDeg = 39.742476,
            longitudeDeg = -105.1786
        )
        val dateTime = SpaDateTime(
            year = 2003,
            month = 10,
            day = 17,
            hour = 12,
            minute = 30,
            second = 30.0,
            timeZoneOffsetHours = -7.0
        )

        val result = SpaCalculator.calculateSolarPosition(
            location = location,
            dateTime = dateTime,
            deltaTSeconds = 67.0
        )

        // NREL SPA Report (TP-560-34302) Example 1 values for 0m elevation:
        // Zenith angle ~ 50.11 degrees
        // Elevation angle ~ 39.89 degrees
        // Azimuth angle ~ 194.34 degrees
        assertEquals(50.1116, result.zenithAngleDeg, 0.05)
        assertEquals(39.8884, result.elevationAngleDeg, 0.05)
        assertEquals(194.3402, result.azimuthAngleDeg, 0.05)
        assertEquals(0.9965, result.sunDistanceAu, 0.005)
    }

    @Test
    fun testSolarIncidenceAngle() {
        val incidence = SpaCalculator.calculateSolarIncidenceAngle(
            zenithAngleDeg = 30.0,
            azimuthAngleDeg = 180.0,
            surfaceTiltDeg = 30.0,
            surfaceAzimuthDeg = 180.0
        )
        assertEquals(0.0, incidence, 1e-6)

        val incidenceHorizontal = SpaCalculator.calculateSolarIncidenceAngle(
            zenithAngleDeg = 45.0,
            azimuthAngleDeg = 120.0,
            surfaceTiltDeg = 0.0,
            surfaceAzimuthDeg = 180.0
        )
        assertEquals(45.0, incidenceHorizontal, 1e-6)
    }

    @Test
    fun testValidationInputs() {
        val validLoc = SpaLocation(0.0, 0.0)
        assertEquals(0.0, validLoc.latitudeDeg)

        val validDT = SpaDateTime(2024, 6, 21, 12, 0)
        assertEquals(2024, validDT.year)
    }
}
