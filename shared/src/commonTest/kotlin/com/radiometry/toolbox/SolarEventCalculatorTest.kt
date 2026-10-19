package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SolarEventCalculatorTest {

    // Stockholm, Sweden - Latitude: 59.33, Longitude: 18.07, Timezone: UTC+1 (or UTC+2 with DST)
    // Ground truth data from https://www.gaisma.com/en/location/stockholm.html
    // For October 19, 2026: Sunrise ~07:35, Sunset ~17:29, Daylight ~9:54

    @Test
    fun testStockholmAutumnEquinox() {
        // Stockholm, Sweden (Lat: 59.33, Lon: 18.07, TZ: +2.0 for CEST/DST) on 2024-09-23
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07,
            year = 2024,
            month = 9,
            day = 23,
            timeZoneOffsetHours = 2.0
        )

        // Sunrise expected around 06:45-07:00 CEST
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertTrue(rise.hour in 6..7)

        // Sunset expected around 18:50-19:10 CEST
        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 18..19)

        // Daylight duration ~ 12h (43200s) for equinox at this latitude
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..45000L)

        // Solar noon elevation should be reasonable for this latitude
        // At 59.33°N, max elevation ~ 90 - 59.33 + declination ~ 30.67 + 0 = ~30.7° at equinox
        assertTrue(result.solarNoonElevationDeg in 25.0..35.0)

        // Civil twilight should exist
        assertTrue(result.civilSunrise is SolarEventTime.Occurred)
        assertTrue(result.civilSunset is SolarEventTime.Occurred)

        // Astronomical twilight should exist
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

        // From Gaisma: Summer solstice sunrise ~03:31, sunset ~22:08
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertTrue(rise.hour in 3..4)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 21..22)

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

        // From Gaisma: Winter solstice sunrise ~08:41, sunset ~14:47
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertTrue(rise.hour in 8..9)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 14..15)

        // Daylight duration ~ 6h 6m (22000s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 21000L..23000L)

        // Solar noon elevation: 90 - 59.33 - 23.44 = ~6.23°
        assertTrue(result.solarNoonElevationDeg in 4.0..8.0)
    }

    @Test
    fun testStockholmCurrentDate() {
        // Stockholm, Sweden (Lat: 59.33, Lon: 18.07, TZ: +2.0) on 2024-10-19
        // From Gaisma: Sunrise ~07:35, Sunset ~17:29, Daylight ~9:54
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 59.33,
            longitudeDeg = 18.07,
            year = 2024,
            month = 10,
            day = 19,
            timeZoneOffsetHours = 2.0
        )

        // Sunrise expected around 07:30-07:40
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(7, rise.hour)
        assertTrue(rise.minute in 30..45)

        // Sunset expected around 17:25-17:35
        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(17, set.hour)
        assertTrue(set.minute in 20..40)

        // Daylight duration ~ 9h 54m (35640s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 35000L..36500L)

        // Solar noon elevation for mid-October at 59.33°N
        // Declination ~ -9.5°, so elevation ~ 90 - 59.33 - 9.5 = ~21.2°
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

        // At equator on equinox, sunrise ~06:00, sunset ~18:00
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertEquals(6, rise.hour)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertEquals(18, set.hour)

        // Daylight duration ~ 12h
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 43000L..43400L)

        // Solar noon elevation should be very close to 90° at equator on equinox
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

        // Summer in Sydney (Southern Hemisphere)
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertTrue(rise.hour in 5..6)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 19..20)

        // Daylight duration ~ 14h 30m (52200s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 51000L..54000L)

        // Solar noon elevation: 90 - (-33.87) + 23.44 = ~147.31° -> but capped at 90 - lat + decl
        // Actually: 90 - |-33.87| + 23.44 = 90 - 33.87 + 23.44 = ~79.57°
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

        // Winter in Sydney
        val rise = result.sunrise
        assertTrue(rise is SolarEventTime.Occurred)
        assertTrue(rise.hour in 6..7)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 16..17)

        // Daylight duration ~ 9h 50m (35400s)
        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 34000L..37000L)

        // Solar noon elevation: 90 - 33.87 - 23.44 = ~32.69°
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

        // Should be polar day (midnight sun) at this latitude in June
        assertEquals(SolarEventTime.AlwaysAbove, result.sunrise)
        assertEquals(SolarEventTime.AlwaysAbove, result.sunset)
        assertEquals(86400L, result.daylightDurationSeconds)

        // Solar noon elevation should still be reasonable
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

        // Should be polar day (midnight sun) at this latitude in December
        assertEquals(SolarEventTime.AlwaysAbove, result.sunrise)
        assertEquals(SolarEventTime.AlwaysAbove, result.sunset)
        assertEquals(86400L, result.daylightDurationSeconds)
    }

    @Test
    fun testArcticCircleWinter() {
        // Rovaniemi, Finland (Lat: 66.5, Lon: 25.7, TZ: +2.0) on 2024-12-21
        val result = SolarEventCalculator.calculateSolarEvents(
            latitudeDeg = 66.5,
            longitudeDeg = 25.7,
            year = 2024,
            month = 12,
            day = 21,
            timeZoneOffsetHours = 2.0
        )

        // Should be polar night at Arctic Circle on winter solstice
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
        assertTrue(rise.hour in 5..6)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 17..18)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..44000L)

        // Solar noon elevation: 90 - 35.68 + 0 = ~54.32° (at equinox)
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
        assertTrue(rise.hour in 5..6)

        val set = result.sunset
        assertTrue(set is SolarEventTime.Occurred)
        assertTrue(set.hour in 17..18)

        val duration = result.daylightDurationSeconds
        assertNotNull(duration)
        assertTrue(duration in 42000L..44000L)

        // Solar noon elevation: 90 - 33.92 + 0 = ~56.08° (at equinox)
        assertTrue(result.solarNoonElevationDeg in 54.0..59.0)
    }

    @Test
    fun testGoldenSummerSolsticeEvents() {

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
