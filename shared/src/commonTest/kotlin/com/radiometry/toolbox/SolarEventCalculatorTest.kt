package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SolarEventCalculatorTest {

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

        // Solar Noon expected around 12:01 local standard time (UTC-7) with elevation ~ 73.7°
        val noon = result.solarNoonTime
        assertEquals(12, noon.hour)
        assertTrue(noon.minute in 0..5)
        assertTrue(result.solarNoonElevationDeg > 70.0)

        // Sunrise expected around 04:32 UTC-7
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(4, rise.hour)
        assertTrue(rise.minute in 25..40)

        // Sunset expected around 19:31 UTC-7
        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(19, set.hour)
        assertTrue(set.minute in 25..40)

        // Daylight duration ~ 15 hours (~53900 - 54000 s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 50000L..58000L)

        // Civil twilight expected
        assertTrue(result.civilSunrise is SolarEventTime.Occurred)
        assertTrue(result.civilSunset is SolarEventTime.Occurred)

        // Astronomical twilight expected
        assertTrue(result.astronomicalSunrise is SolarEventTime.Occurred)
        assertTrue(result.astronomicalSunset is SolarEventTime.Occurred)

        // Golden and Blue hours
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

        // Sun remains above horizon (-0.833°) all 24 hours
        assertEquals(SolarEventTime.AlwaysAbove, result.sunrise)
        assertEquals(SolarEventTime.AlwaysAbove, result.sunset)
        assertEquals(86400L, result.daylightDurationSeconds)

        // Solar Noon still occurs (max elevation ~ 43.8°)
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

        // Sun remains below horizon (-0.833°) all day (max elevation < -2.0°)
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

        // Solar Noon near 12:14 with elevation ~ 89.9°
        val noon = result.solarNoonTime
        assertEquals(12, noon.hour)
        assertTrue(result.solarNoonElevationDeg > 88.0)

        // Sunrise ~ 06:14
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(6, rise.hour)

        // Sunset ~ 18:14
        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(18, set.hour)

        // Daylight duration ~ 12h (43200s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..44400L)
    }
}
