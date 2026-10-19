package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SolarEventCalculatorTest {

    @Test
    fun testStockholmAutumnEquinox() {
        // Stockholm, Sweden (Lat: 59.33, Lon: 18.07, TZ: +2.0 for CEST) on 2024-09-23
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07,
            year = 2024,
            month = 9,
            day = 23,
            timeZoneOffsetHours = 2.0
        )

        // Sunrise expected around 06:38 CEST
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(6, rise.hour)
        assertTrue(rise.minute in 30..45)

        // Sunset expected around 18:57 CEST
        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(18, set.hour)

        // Daylight duration ~ 12h (43200s) for equinox
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..45000L)

        // Solar noon elevation ~ 30.7° at equinox
        assertTrue(result.solarNoonElevationDeg in 28.0..33.0)

        // Civil twilight expected
        assertTrue(result.civilSunrise is SolarEventTime.Occurred)
        assertTrue(result.civilSunset is SolarEventTime.Occurred)

        // Astronomical twilight expected
        assertTrue(result.astronomicalSunrise is SolarEventTime.Occurred)
        assertTrue(result.astronomicalSunset is SolarEventTime.Occurred)
    }

    @Test
    fun testStockholmSummerSolstice() {
        // Stockholm, Sweden (Lat: 59.33, Lon: 18.07, TZ: +2.0) on 2024-06-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07,
            year = 2024,
            month = 6,
            day = 21,
            timeZoneOffsetHours = 2.0
        )

        // Summer solstice sunrise ~03:31, sunset ~22:08
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(3, rise.hour)
        assertTrue(rise.minute in 20..40)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(22, set.hour)

        // Daylight duration ~ 18h 37m (67020s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 66000L..68000L)

        // Solar noon elevation: 90 - 59.33 + 23.44 = ~54.11°
        assertTrue(result.solarNoonElevationDeg in 52.0..56.0)
    }

    @Test
    fun testStockholmWinterSolstice() {
        // Stockholm, Sweden (Lat: 59.33, Lon: 18.07, TZ: +1.0) on 2024-12-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07,
            year = 2024,
            month = 12,
            day = 21,
            timeZoneOffsetHours = 1.0
        )

        // Winter solstice sunrise ~08:43, sunset ~14:41
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(8, rise.hour)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(14, set.hour)

        // Daylight duration ~ 5h 58m (21480s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 20500L..22500L)

        // Solar noon elevation: 90 - 59.33 - 23.44 = ~6.23°
        assertTrue(result.solarNoonElevationDeg in 4.0..8.0)
    }

    @Test
    fun testStockholmCurrentDate() {
        // Stockholm, Sweden (Lat: 59.33, Lon: 18.07, TZ: +2.0) on 2024-10-19
        // Sunrise ~07:38, Sunset ~17:28
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07,
            year = 2024,
            month = 10,
            day = 19,
            timeZoneOffsetHours = 2.0
        )

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(7, rise.hour)
        assertTrue(rise.minute in 30..45)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(17, set.hour)
        assertTrue(set.minute in 20..40)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 34500L..36500L)

        assertTrue(result.solarNoonElevationDeg in 18.0..24.0)
    }

    @Test
    fun testEquatorSeptemberEquinox() {
        // Quito, Ecuador (Lat: 0.0, Lon: -78.5, TZ: -5.0) on 2024-09-23
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 0.0,
            longitudeDeg = -78.5,
            year = 2024,
            month = 9,
            day = 23,
            timeZoneOffsetHours = -5.0
        )

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(6, rise.hour)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(18, set.hour)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 43000L..44000L)

        assertTrue(result.solarNoonElevationDeg > 88.0)
    }

    @Test
    fun testSydneySummer() {
        // Sydney, Australia (Lat: -33.87, Lon: 151.21, TZ: +10.0) on 2024-12-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = -33.87,
            longitudeDeg = 151.21,
            year = 2024,
            month = 12,
            day = 21,
            timeZoneOffsetHours = 10.0
        )

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(4, rise.hour)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 19..20)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 51000L..55000L)

        assertTrue(result.solarNoonElevationDeg in 77.0..82.0)
    }

    @Test
    fun testSydneyWinter() {
        // Sydney, Australia (Lat: -33.87, Lon: 151.21, TZ: +10.0) on 2024-06-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = -33.87,
            longitudeDeg = 151.21,
            year = 2024,
            month = 6,
            day = 21,
            timeZoneOffsetHours = 10.0
        )

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertTrue(rise.hour in 6..7)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(16, set.hour)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 34000L..37000L)

        assertTrue(result.solarNoonElevationDeg in 30.0..36.0)
    }

    @Test
    fun testNorthPoleRegion() {
        // Longyearbyen, Svalbard (Lat: 78.22, Lon: 15.63, TZ: +1.0) on 2024-06-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 78.22,
            longitudeDeg = 15.63,
            year = 2024,
            month = 6,
            day = 21,
            timeZoneOffsetHours = 1.0
        )

        assertEquals(SolarEventTime.AlwaysAbove, result.sunrise)
        assertEquals(SolarEventTime.AlwaysAbove, result.sunset)
        assertEquals(86400L, result.daylightDurationSeconds)

        assertTrue(result.solarNoonElevationDeg > 30.0)
    }

    @Test
    fun testSouthPoleRegion() {
        // McMurdo Station, Antarctica (Lat: -77.85, Lon: 166.67, TZ: +12.0) on 2024-12-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = -77.85,
            longitudeDeg = 166.67,
            year = 2024,
            month = 12,
            day = 21,
            timeZoneOffsetHours = 12.0
        )

        assertEquals(SolarEventTime.AlwaysAbove, result.sunrise)
        assertEquals(SolarEventTime.AlwaysAbove, result.sunset)
        assertEquals(86400L, result.daylightDurationSeconds)
    }

    @Test
    fun testArcticCircleWinter() {
        // Tromsø, Norway (Lat: 69.65, Lon: 18.95, TZ: +2.0) on 2024-12-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 69.65,
            longitudeDeg = 18.95,
            year = 2024,
            month = 12,
            day = 21,
            timeZoneOffsetHours = 2.0
        )

        // Polar night at 69.65°N on winter solstice
        assertEquals(SolarEventTime.AlwaysBelow, result.sunrise)
        assertEquals(SolarEventTime.AlwaysBelow, result.sunset)
        assertEquals(0L, result.daylightDurationSeconds)
        assertTrue(result.solarNoonElevationDeg < 0.0)
    }

    @Test
    fun testRandomLocationTokyo() {
        // Tokyo, Japan (Lat: 35.68, Lon: 139.77, TZ: +9.0) on 2024-03-20
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 35.68,
            longitudeDeg = 139.77,
            year = 2024,
            month = 3,
            day = 20,
            timeZoneOffsetHours = 9.0
        )

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(5, rise.hour)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(17, set.hour)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..44000L)

        assertTrue(result.solarNoonElevationDeg in 52.0..57.0)
    }

    @Test
    fun testRandomLocationCapeTown() {
        // Cape Town, South Africa (Lat: -33.92, Lon: 18.42, TZ: +2.0) on 2024-09-23
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = -33.92,
            longitudeDeg = 18.42,
            year = 2024,
            month = 9,
            day = 23,
            timeZoneOffsetHours = 2.0
        )

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(6, rise.hour)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(18, set.hour)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..44000L)

        assertTrue(result.solarNoonElevationDeg in 54.0..59.0)
    }

    @Test
    fun testGoldenSummerSolsticeEvents() {
        // Golden, CO (Lat: 39.742476, Lon: -105.1786, TZ: -7.0) on 2024-06-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 39.742476,
            longitudeDeg = -105.1786,
            year = 2024,
            month = 6,
            day = 21,
            timeZoneOffsetHours = -7.0
        )

        val noon = result.solarNoonTime
        assertEquals(12, noon.hour)
        assertTrue(noon.minute in 0..5)
        assertTrue(result.solarNoonElevationDeg > 70.0)

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(4, rise.hour)
        assertTrue(rise.minute in 25..40)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(19, set.hour)
        assertTrue(set.minute in 25..40)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 50000L..58000L)

        assertTrue(result.civilSunrise is SolarEventTime.Occurred)
        assertTrue(result.civilSunset is SolarEventTime.Occurred)

        assertTrue(result.astronomicalSunrise is SolarEventTime.Occurred)
        assertTrue(result.astronomicalSunset is SolarEventTime.Occurred)

        assertTrue(result.morningGoldenHour.start is SolarEventTime.Occurred)
        assertTrue(result.morningGoldenHour.end is SolarEventTime.Occurred)
        assertTrue(result.morningBlueHour.start is SolarEventTime.Occurred)
        assertTrue(result.morningBlueHour.end is SolarEventTime.Occurred)
    }

    @Test
    fun testTromsoPolarDayMidnightSun() {
        // Tromsø, Norway (Lat: 69.6492, Lon: 18.9553, TZ: +2.0) on 2024-06-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 69.6492,
            longitudeDeg = 18.9553,
            year = 2024,
            month = 6,
            day = 21,
            timeZoneOffsetHours = 2.0
        )

        assertEquals(SolarEventTime.AlwaysAbove, result.sunrise)
        assertEquals(SolarEventTime.AlwaysAbove, result.sunset)
        assertEquals(86400L, result.daylightDurationSeconds)

        assertTrue(result.solarNoonElevationDeg > 40.0)
    }

    @Test
    fun testTromsoPolarNight() {
        // Tromsø, Norway (Lat: 69.6492, Lon: 18.9553, TZ: +2.0) on 2024-12-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 69.6492,
            longitudeDeg = 18.9553,
            year = 2024,
            month = 12,
            day = 21,
            timeZoneOffsetHours = 2.0
        )

        assertEquals(SolarEventTime.AlwaysBelow, result.sunrise)
        assertEquals(SolarEventTime.AlwaysBelow, result.sunset)
        assertEquals(0L, result.daylightDurationSeconds)
        assertTrue(result.solarNoonElevationDeg < 0.0)
    }

    @Test
    fun testEquatorEquinoxEvents() {
        // Quito, Ecuador (Lat: 0.0, Lon: -78.5, TZ: -5.0) on 2024-03-20
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 0.0,
            longitudeDeg = -78.5,
            year = 2024,
            month = 3,
            day = 20,
            timeZoneOffsetHours = -5.0
        )

        val noon = result.solarNoonTime
        assertEquals(12, noon.hour)
        assertTrue(result.solarNoonElevationDeg > 88.0)

        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(6, rise.hour)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(18, set.hour)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..44400L)
    }
}
