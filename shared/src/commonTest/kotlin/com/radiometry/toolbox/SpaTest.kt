package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    // Tests using Stockholm, Sweden as ground truth
    // Latitude: 59.33°N, Longitude: 18.07°E
    // Reference data from https://www.gaisma.com/en/location/stockholm.html

    @Test
    fun testStockholmSolarNoonEquinox() {
        // Stockholm at solar noon on autumn equinox (2024-09-23 ~12:00 local time, UTC+2)
        val location = SpaLocation(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 9,
            day = 23,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 2.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 59.33°N on equinox, solar elevation at noon should be ~90 - 59.33 = ~30.67°
        // With atmospheric refraction, it should be slightly higher
        assertTrue(result.elevationAngleDeg in 28.0..33.0)

        // Azimuth should be roughly 180° (south) at solar noon in Northern Hemisphere
        assertTrue(result.azimuthAngleDeg in 170.0..190.0)

        // Zenith = 90 - elevation
        assertTrue(result.zenithAngleDeg in 57.0..62.0)
    }

    @Test
    fun testStockholmSummerSolsticeNoon() {
        // Stockholm at solar noon on summer solstice (2024-06-21 ~12:00 local time, UTC+2)
        val location = SpaLocation(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 6,
            day = 21,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 2.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 59.33°N on summer solstice (declination ~23.44°)
        // Max elevation = 90 - 59.33 + 23.44 = ~54.11°
        assertTrue(result.elevationAngleDeg in 52.0..56.0)

        // Azimuth should be roughly 180° (south)
        assertTrue(result.azimuthAngleDeg in 170.0..190.0)
    }

    @Test
    fun testStockholmWinterSolsticeNoon() {
        // Stockholm at solar noon on winter solstice (2024-12-21 ~12:00 local time, UTC+1)
        val location = SpaLocation(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 12,
            day = 21,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 1.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 59.33°N on winter solstice (declination ~-23.44°)
        // Max elevation = 90 - 59.33 - 23.44 = ~6.23°
        assertTrue(result.elevationAngleDeg in 4.0..8.0)

        // Azimuth should be roughly 180° (south)
        assertTrue(result.azimuthAngleDeg in 170.0..190.0)
    }

    @Test
    fun testStockholmSunrise() {
        // Stockholm at sunrise on 2024-10-19
        // From Gaisma: Sunrise ~07:35 CEST (UTC+2)
        val location = SpaLocation(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 10,
            day = 19,
            hour = 7,
            minute = 35,
            second = 0.0,
            timeZoneOffsetHours = 2.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At sunrise, elevation should be approximately 0° (with refraction ~0.5°)
        // Allowing for calculation timing differences
        assertTrue(result.elevationAngleDeg in -1.0..1.0)

        // Azimuth at sunrise in Northern Hemisphere should be roughly east (90°)
        assertTrue(result.azimuthAngleDeg in 80.0..100.0)
    }

    @Test
    fun testStockholmSunset() {
        // Stockholm at sunset on 2024-10-19
        // From Gaisma: Sunset ~17:29 CEST (UTC+2)
        val location = SpaLocation(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 10,
            day = 19,
            hour = 17,
            minute = 29,
            second = 0.0,
            timeZoneOffsetHours = 2.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At sunset, elevation should be approximately 0°
        assertTrue(result.elevationAngleDeg in -1.0..1.0)

        // Azimuth at sunset in Northern Hemisphere should be roughly west (270°)
        assertTrue(result.azimuthAngleDeg in 260.0..280.0)
    }

    @Test
    fun testEquatorEquinoxNoon() {
        // Quito, Ecuador (0°N, 78.5°W) at solar noon on equinox
        val location = SpaLocation(
            latitudeDeg = 0.0,
            longitudeDeg = -78.5
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 3,
            day = 20,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = -5.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At equator on equinox, sun should be nearly overhead
        assertTrue(result.elevationAngleDeg > 88.0)
        assertTrue(result.zenithAngleDeg < 2.0)
    }

    @Test
    fun testEquatorSolsticeNoon() {
        // Quito, Ecuador (0°N, 78.5°W) at solar noon on summer solstice
        val location = SpaLocation(
            latitudeDeg = 0.0,
            longitudeDeg = -78.5
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 6,
            day = 21,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = -5.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At equator on summer solstice, declination is ~23.44°
        // The sun should be very high in the sky
        assertTrue(result.elevationAngleDeg > 85.0)
    }

    @Test
    fun testNorthPoleSummerSolstice() {
        // North Pole region (89°N, 0°E) on summer solstice
        val location = SpaLocation(
            latitudeDeg = 89.0,
            longitudeDeg = 0.0
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 6,
            day = 21,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 0.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At North Pole on summer solstice, sun is at horizon or slightly above
        // Elevation = 90 - 89 + 23.44 = ~24.44°
        // But at the pole itself (90°N), it's different
        // For 89°N: elevation ~ 23.44° + 1° = ~24.44°
        assertTrue(result.elevationAngleDeg in 20.0..28.0)
    }

    @Test
    fun testSouthPoleSummerSolstice() {
        // South Pole region (89°S, 0°E) on December solstice (summer in SH)
        val location = SpaLocation(
            latitudeDeg = -89.0,
            longitudeDeg = 0.0
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 12,
            day = 21,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 0.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At South Pole on summer solstice, sun is at horizon or slightly above
        assertTrue(result.elevationAngleDeg in 20.0..28.0)
    }

    @Test
    fun testSydneySummerNoon() {
        // Sydney, Australia (-33.87°S, 151.21°E) at solar noon on summer solstice
        val location = SpaLocation(
            latitudeDeg = -33.87,
            longitudeDeg = 151.21
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 12,
            day = 21,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 10.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 33.87°S on summer solstice
        // elevation = 90 - |-33.87 - (-23.44)| = 90 - 10.43 = 79.57°
        assertTrue(result.elevationAngleDeg in 77.0..82.0)

        // Azimuth should be roughly 0° (north) at solar noon in Southern Hemisphere
        assertTrue(result.azimuthAngleDeg in 350.0..10.0 || result.azimuthAngleDeg in 0.0..10.0)
    }

    @Test
    fun testSydneyWinterNoon() {
        // Sydney, Australia (-33.87°S, 151.21°E) at solar noon on winter solstice
        val location = SpaLocation(
            latitudeDeg = -33.87,
            longitudeDeg = 151.21
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 6,
            day = 21,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 10.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 33.87°S on June solstice (declination ~23.44°)
        // elevation = 90 - |-33.87 - 23.44| = 90 - 57.31 = 32.69°
        assertTrue(result.elevationAngleDeg in 30.0..36.0)

        // Azimuth should be roughly 0° (north) at solar noon in Southern Hemisphere
        assertTrue(result.azimuthAngleDeg in 350.0..10.0 || result.azimuthAngleDeg in 0.0..10.0)
    }

    @Test
    fun testTokyoEquinoxNoon() {
        // Tokyo, Japan (35.68°N, 139.77°E) at solar noon on equinox
        val location = SpaLocation(
            latitudeDeg = 35.68,
            longitudeDeg = 139.77
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 3,
            day = 20,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 9.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 35.68°N on equinox: elevation = 90 - 35.68 = 54.32°
        assertTrue(result.elevationAngleDeg in 52.0..57.0)

        // Azimuth should be roughly 180° (south)
        assertTrue(result.azimuthAngleDeg in 170.0..190.0)
    }

    @Test
    fun testCapeTownEquinoxNoon() {
        // Cape Town, South Africa (-33.92°S, 18.42°E) at solar noon on equinox
        val location = SpaLocation(
            latitudeDeg = -33.92,
            longitudeDeg = 18.42
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 9,
            day = 23,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 2.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 33.92°S on equinox: elevation = 90 - 33.92 = 56.08°
        assertTrue(result.elevationAngleDeg in 54.0..59.0)

        // Azimuth should be roughly 0° (north) in Southern Hemisphere
        assertTrue(result.azimuthAngleDeg in 350.0..10.0 || result.azimuthAngleDeg in 0.0..10.0)
    }

    @Test
    fun testRandomDateNewYork() {
        // New York, USA (40.71°N, -74.01°W) on 2024-07-04 (Independence Day)
        val location = SpaLocation(
            latitudeDeg = 40.71,
            longitudeDeg = -74.01
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 7,
            day = 4,
            hour = 13,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = -4.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 40.71°N in early July, declination is ~22.8°
        // elevation = 90 - 40.71 + 22.8 = ~72.09°
        assertTrue(result.elevationAngleDeg in 70.0..75.0)

        // Sun distance should be reasonable (~1 AU)
        assertTrue(result.sunDistanceAu in 0.98..1.02)
    }

    @Test
    fun testRandomDateLondon() {
        // London, UK (51.51°N, -0.13°W) on 2024-01-01
        val location = SpaLocation(
            latitudeDeg = 51.51,
            longitudeDeg = -0.13
        )
        val dateTime = SpaDateTime(
            year = 2024,
            month = 1,
            day = 1,
            hour = 12,
            minute = 0,
            second = 0.0,
            timeZoneOffsetHours = 0.0
        )

        val result = SpaCalculator.calculateSolarPosition(location, dateTime, 69.0)

        // At 51.51°N in early January, declination is ~-23.0°
        // elevation = 90 - 51.51 - 23.0 = ~15.49°
        assertTrue(result.elevationAngleDeg in 13.0..18.0)

        // Azimuth should be roughly 180° (south)
        assertTrue(result.azimuthAngleDeg in 170.0..190.0)
    }
}
