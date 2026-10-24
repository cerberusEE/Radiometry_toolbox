package com.radiometry.toolbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.round
import kotlin.math.sqrt

sealed class SolarEventTime {
    data class Occurred(
        val hour: Int,
        val minute: Int,
        val second: Int
    ) : SolarEventTime() {
        fun toShortString(): String =
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

        fun toFullString(): String =
            "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')}"
    }

    object AlwaysAbove : SolarEventTime()
    object AlwaysBelow : SolarEventTime()
}

data class SolarTimespan(
    val start: SolarEventTime,
    val end: SolarEventTime
) {
    fun toDisplayString(solarNoonStr: String? = null): String {
        val startStr = when (start) {
            is SolarEventTime.Occurred -> start.toShortString()
            is SolarEventTime.AlwaysAbove -> "Sun Above"
            is SolarEventTime.AlwaysBelow -> "Sun Below"
        }
        val endStr = when (end) {
            is SolarEventTime.Occurred -> end.toShortString()
            is SolarEventTime.AlwaysAbove -> solarNoonStr ?: "Sun Above"
            is SolarEventTime.AlwaysBelow -> solarNoonStr ?: "Sun Below"
        }

        if (start is SolarEventTime.AlwaysAbove && end is SolarEventTime.AlwaysAbove) {
            return "Sun remains above range all day"
        }
        if (start is SolarEventTime.AlwaysBelow && end is SolarEventTime.AlwaysBelow) {
            return "Sun remains below range all day"
        }
        return "$startStr – $endStr"
    }
}

data class SolarEventsResult(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val year: Int,
    val month: Int,
    val day: Int,
    val timeZoneOffsetHours: Double,

    val solarNoonTime: SolarEventTime.Occurred,
    val solarNoonElevationDeg: Double,
    val solarNoonAzimuthDeg: Double,

    val sunrise: SolarEventTime,
    val sunset: SolarEventTime,
    val daylightDurationSeconds: Long?,

    val civilSunrise: SolarEventTime,
    val civilSunset: SolarEventTime,

    val astronomicalSunrise: SolarEventTime,
    val astronomicalSunset: SolarEventTime,

    val morningGoldenHour: SolarTimespan,
    val eveningGoldenHour: SolarTimespan,

    val morningBlueHour: SolarTimespan,
    val eveningBlueHour: SolarTimespan
)

object SolarEventCalculator {

    private const val CIVIL_TWILIGHT_ELEVATION = -6.0
    private const val ASTRONOMICAL_TWILIGHT_ELEVATION = -18.0
    private const val GOLDEN_HOUR_LOW_ELEVATION = -4.0
    private const val GOLDEN_HOUR_HIGH_ELEVATION = 6.0
    private const val BLUE_HOUR_LOW_ELEVATION = -8.0
    private const val BLUE_HOUR_HIGH_ELEVATION = -4.0

    private fun createOccurredTime(hour: Int, minute: Int, secondsDouble: Double): SolarEventTime.Occurred {
        var s = round(secondsDouble).toInt()
        var m = minute
        var h = hour
        if (s >= 60) {
            s -= 60
            m += 1
        }
        if (m >= 60) {
            m -= 60
            h = (h + 1) % 24
        }
        return SolarEventTime.Occurred(h, m, s)
    }

    /**
     * Calculates all daily solar events (sunrise, sunset, twilight, solar noon, golden/blue hours)
     * using the high-accuracy NREL SPA algorithm.
     */
    fun calculateSolarEvents(
        latitudeDeg: Double,
        longitudeDeg: Double,
        year: Int,
        month: Int,
        day: Int,
        timeZoneOffsetHours: Double,
        deltaTSeconds: Double = 69.0,
        pressureMbar: Double = 1013.25,
        temperatureC: Double = 15.0,
        elevationMeters: Double = 0.0
    ): SolarEventsResult {
        val location = SpaLocation(latitudeDeg, longitudeDeg, elevationMeters)

        fun elevationAt(hourOfDay: Double): Double {
            val clampedHour = hourOfDay.coerceIn(0.0, 24.0)
            val h = clampedHour.toInt().coerceIn(0, 23)
            val remM = (clampedHour - h) * 60.0
            val m = remM.toInt().coerceIn(0, 59)
            val s = ((remM - m) * 60.0).coerceIn(0.0, 59.999)

            val dateTime = SpaDateTime(
                year = year,
                month = month,
                day = day,
                hour = h,
                minute = m,
                second = s,
                timeZoneOffsetHours = timeZoneOffsetHours
            )
            return SpaCalculator.calculateSolarPosition(
                location = location,
                dateTime = dateTime,
                deltaTSeconds = deltaTSeconds,
                pressureMbar = pressureMbar,
                temperatureC = temperatureC
            ).elevationAngleDeg
        }

        val sampleCount = 144
        val stepHours = 24.0 / sampleCount
        val times = DoubleArray(sampleCount + 1)
        val elevations = DoubleArray(sampleCount + 1)

        var maxElev = -900.0
        var maxElevIndex = 0
        var minElev = 900.0

        for (i in 0..sampleCount) {
            val t = i * stepHours
            times[i] = t
            val elev = elevationAt(t)
            elevations[i] = elev
            if (elev > maxElev) {
                maxElev = elev
                maxElevIndex = i
            }
            if (elev < minElev) {
                minElev = elev
            }
        }

        val tNoonLow = if (maxElevIndex > 0) times[maxElevIndex - 1] else 0.0
        val tNoonHigh = if (maxElevIndex < sampleCount) times[maxElevIndex + 1] else 24.0

        var aNoon = tNoonLow
        var bNoon = tNoonHigh
        val goldenRatio = (sqrt(5.0) - 1.0) / 2.0
        var cNoon = bNoon - goldenRatio * (bNoon - aNoon)
        var dNoon = aNoon + goldenRatio * (bNoon - aNoon)

        repeat(25) {
            if (elevationAt(cNoon) > elevationAt(dNoon)) {
                bNoon = dNoon
            } else {
                aNoon = cNoon
            }
            cNoon = bNoon - goldenRatio * (bNoon - aNoon)
            dNoon = aNoon + goldenRatio * (bNoon - aNoon)
        }
        var tNoon = (aNoon + bNoon) / 2.0

        // Parabolic 3-point peak refinement for solar noon
        val stepH = 0.0001
        val e1 = elevationAt(tNoon - stepH)
        val e2 = elevationAt(tNoon)
        val e3 = elevationAt(tNoon + stepH)
        val denom = 2.0 * e2 - e1 - e3
        if (denom > 1e-12) {
            val tPeak = tNoon + (stepH / 2.0) * (e1 - e3) / denom
            if (tPeak in (tNoon - stepH)..(tNoon + stepH)) {
                tNoon = tPeak
            }
        }

        val noonResult = run {
            val h = tNoon.toInt().coerceIn(0, 23)
            val remM = (tNoon - h) * 60.0
            val m = remM.toInt().coerceIn(0, 59)
            val s = (remM - m) * 60.0
            val dt = SpaDateTime(year, month, day, h, m, s.coerceIn(0.0, 59.999), timeZoneOffsetHours)
            val spa = SpaCalculator.calculateSolarPosition(location, dt, deltaTSeconds)
            Triple(
                createOccurredTime(h, m, s),
                spa.elevationAngleDeg,
                spa.azimuthAngleDeg
            )
        }

        // Pass 2: Brent's Method Root Refinement for target elevation crossing
        fun refineCrossing(
            targetElev: Double,
            low: Double,
            high: Double
        ): Double {
            var a = low
            var b = high
            var fa = elevationAt(a) - targetElev
            var fb = elevationAt(b) - targetElev

            if (kotlin.math.abs(fa) < 1e-9) return a
            if (kotlin.math.abs(fb) < 1e-9) return b

            var c = a
            var fc = fa
            var d = b - a
            var e = d

            repeat(30) {
                if (kotlin.math.abs(fb) < 1e-9 || kotlin.math.abs(b - a) < 1e-8) {
                    return b
                }

                if ((fa > 0.0 && fb > 0.0) || (fa < 0.0 && fb < 0.0)) {
                    a = c
                    fa = fc
                    d = b - a
                    e = d
                }

                if (kotlin.math.abs(fa) < kotlin.math.abs(fb)) {
                    c = b; b = a; a = c
                    fc = fb; fb = fa; fa = fc
                }

                val tol1 = 2.0 * 1e-12 * kotlin.math.abs(b) + 0.5 * 1e-8
                val xm = 0.5 * (a - b)

                if (kotlin.math.abs(xm) <= tol1 || kotlin.math.abs(fb) <= 1e-9) {
                    return b
                }

                if (kotlin.math.abs(e) >= tol1 && kotlin.math.abs(fc) > kotlin.math.abs(fb)) {
                    val s = fb / fc
                    val p: Double
                    val q: Double
                    if (a == c) {
                        p = 2.0 * xm * s
                        q = 1.0 - s
                    } else {
                        val q0 = fc / fa
                        val r = fb / fa
                        p = s * (2.0 * xm * q0 * (q0 - r) - (b - c) * (r - 1.0))
                        q = (q0 - 1.0) * (r - 1.0) * (s - 1.0)
                    }
                    var p1 = p
                    var q1 = q
                    if (p1 > 0.0) q1 = -q1
                    p1 = kotlin.math.abs(p1)

                    val min1 = 3.0 * xm * q1 - kotlin.math.abs(tol1 * q1)
                    val min2 = kotlin.math.abs(e * q1)

                    if (2.0 * p1 < if (min1 < min2) min1 else min2) {
                        e = d
                        d = p1 / q1
                    } else {
                        d = xm
                        e = d
                    }
                } else {
                    d = xm
                    e = d
                }

                c = b
                fc = fb

                b += if (kotlin.math.abs(d) > tol1) {
                    d
                } else {
                    if (xm > 0.0) tol1 else -tol1
                }
                fb = elevationAt(b) - targetElev
            }
            return b
        }

        fun findMorningCrossing(targetElev: Double): SolarEventTime {
            if (maxElev < targetElev) return SolarEventTime.AlwaysBelow
            if (minElev > targetElev) return SolarEventTime.AlwaysAbove

            for (i in 0 until sampleCount) {
                if (elevations[i] <= targetElev && elevations[i + 1] > targetElev) {
                    val tCross = refineCrossing(targetElev, times[i], times[i + 1])
                    val h = tCross.toInt().coerceIn(0, 23)
                    val remM = (tCross - h) * 60.0
                    val m = remM.toInt().coerceIn(0, 59)
                    val s = (remM - m) * 60.0
                    return createOccurredTime(h, m, s)
                }
            }

            if (elevations[0] > targetElev) {
                return SolarEventTime.AlwaysAbove
            }
            return SolarEventTime.AlwaysBelow
        }

        fun findEveningCrossing(targetElev: Double): SolarEventTime {
            if (maxElev < targetElev) return SolarEventTime.AlwaysBelow
            if (minElev > targetElev) return SolarEventTime.AlwaysAbove

            for (i in 0 until sampleCount) {
                if (elevations[i] >= targetElev && elevations[i + 1] < targetElev) {
                    val tCross = refineCrossing(targetElev, times[i], times[i + 1])
                    val h = tCross.toInt().coerceIn(0, 23)
                    val remM = (tCross - h) * 60.0
                    val m = remM.toInt().coerceIn(0, 59)
                    val s = (remM - m) * 60.0
                    return createOccurredTime(h, m, s)
                }
            }

            if (elevations[sampleCount] > targetElev) {
                return SolarEventTime.AlwaysAbove
            }
            return SolarEventTime.AlwaysBelow
        }

        val horizonDipDeg = if (elevationMeters > 0.0) {
            (1.76 / 60.0) * sqrt(elevationMeters)
        } else {
            0.0
        }

        val safePressure = pressureMbar.coerceAtLeast(0.0)
        val safeTempK = (temperatureC + 273.15).coerceAtLeast(1.0)
        val horizonRefractionDeg = (34.0 / 60.0) * (safePressure / 1013.25) * (288.15 / safeTempK)
        val solarSemidiameterDeg = 16.0 / 60.0

        val officialSunriseSunsetElevation = -solarSemidiameterDeg - horizonRefractionDeg - horizonDipDeg
        val civilTwilightElevation = CIVIL_TWILIGHT_ELEVATION - horizonDipDeg
        val astronomicalTwilightElevation = ASTRONOMICAL_TWILIGHT_ELEVATION - horizonDipDeg

        val goldenHourLowElevation = GOLDEN_HOUR_LOW_ELEVATION - horizonDipDeg
        val goldenHourHighElevation = GOLDEN_HOUR_HIGH_ELEVATION - horizonDipDeg

        val blueHourLowElevation = BLUE_HOUR_LOW_ELEVATION - horizonDipDeg
        val blueHourHighElevation = BLUE_HOUR_HIGH_ELEVATION - horizonDipDeg

        val sunrise = findMorningCrossing(officialSunriseSunsetElevation)
        val sunset = findEveningCrossing(officialSunriseSunsetElevation)

        val civilSunrise = findMorningCrossing(civilTwilightElevation)
        val civilSunset = findEveningCrossing(civilTwilightElevation)

        val astroSunrise = findMorningCrossing(astronomicalTwilightElevation)
        val astroSunset = findEveningCrossing(astronomicalTwilightElevation)

        val goldenLowMorning = findMorningCrossing(goldenHourLowElevation)
        val goldenHighMorning = findMorningCrossing(goldenHourHighElevation)
        val morningGoldenHour = SolarTimespan(goldenLowMorning, goldenHighMorning)

        val goldenHighEvening = findEveningCrossing(goldenHourHighElevation)
        val goldenLowEvening = findEveningCrossing(goldenHourLowElevation)
        val eveningGoldenHour = SolarTimespan(goldenHighEvening, goldenLowEvening)

        val blueLowMorning = findMorningCrossing(blueHourLowElevation)
        val blueHighMorning = findMorningCrossing(blueHourHighElevation)
        val morningBlueHour = SolarTimespan(blueLowMorning, blueHighMorning)

        val blueHighEvening = findEveningCrossing(blueHourHighElevation)
        val blueLowEvening = findEveningCrossing(blueHourLowElevation)
        val eveningBlueHour = SolarTimespan(blueHighEvening, blueLowEvening)

        val daylightDurationSeconds: Long? = when (sunrise) {
            is SolarEventTime.Occurred if sunset is SolarEventTime.Occurred -> {
                val secRise = sunrise.hour * 3600L + sunrise.minute * 60L + sunrise.second
                val secSet = sunset.hour * 3600L + sunset.minute * 60L + sunset.second
                if (secSet >= secRise) secSet - secRise else (86400L - secRise) + secSet
            }

            is SolarEventTime.AlwaysAbove -> 86400L
            is SolarEventTime.AlwaysBelow -> 0L
            else -> null
        }

        return SolarEventsResult(
            latitudeDeg = latitudeDeg,
            longitudeDeg = longitudeDeg,
            year = year,
            month = month,
            day = day,
            timeZoneOffsetHours = timeZoneOffsetHours,

            solarNoonTime = noonResult.first,
            solarNoonElevationDeg = noonResult.second,
            solarNoonAzimuthDeg = noonResult.third,

            sunrise = sunrise,
            sunset = sunset,
            daylightDurationSeconds = daylightDurationSeconds,

            civilSunrise = civilSunrise,
            civilSunset = civilSunset,

            astronomicalSunrise = astroSunrise,
            astronomicalSunset = astroSunset,

            morningGoldenHour = morningGoldenHour,
            eveningGoldenHour = eveningGoldenHour,

            morningBlueHour = morningBlueHour,
            eveningBlueHour = eveningBlueHour
        )
    }
}

fun formatEventTime(
    event: SolarEventTime,
    alwaysAboveLabel: String = "Sun above threshold",
    alwaysBelowLabel: String = "Sun below threshold"
): String {
    return when (event) {
        is SolarEventTime.Occurred -> event.toFullString()
        is SolarEventTime.AlwaysAbove -> alwaysAboveLabel
        is SolarEventTime.AlwaysBelow -> alwaysBelowLabel
    }
}

fun formatDaylightDuration(seconds: Long?): String {
    if (seconds == null) return "N/A"
    if (seconds >= 86400L) return "24h 00m 00s (Polar Day)"
    if (seconds <= 0L) return "00h 00m 00s (Polar Night)"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "${h}h ${m.toString().padStart(2, '0')}m ${s.toString().padStart(2, '0')}s"
}

@Composable
fun SolarEventsVisualization(settingsManager: SettingsManager) {
    val locationProvider = rememberLocationProvider()
    val mapLauncher = rememberMapLauncher()
    val clipboardManager = rememberClipboardManager()
    val pressureSensor = rememberAmbientPressureSensor()

    var liveSensorPressure by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(Unit) {
        if (pressureSensor.isSensorAvailable()) {
            pressureSensor.startListening { rawPressure ->
                liveSensorPressure = rawPressure
            }
        }
        onDispose {
            pressureSensor.stopListening()
        }
    }

    val systemNow = remember { getSystemDateTime() }

    var latitudeText by remember {
        mutableStateOf(settingsManager.getString("key_solar_latitude", "39.742476"))
    }
    var longitudeText by remember {
        mutableStateOf(settingsManager.getString("key_solar_longitude", "-105.1786"))
    }
    var altitudeText by remember {
        mutableStateOf(settingsManager.getString("key_solar_altitude", "0.0"))
    }

    var yearText by remember {
        mutableStateOf(settingsManager.getString("key_solar_year", systemNow.year.toString()))
    }
    var monthText by remember {
        mutableStateOf(settingsManager.getString("key_solar_month", systemNow.month.toString()))
    }
    var dayText by remember {
        mutableStateOf(settingsManager.getString("key_solar_day", systemNow.day.toString()))
    }
    var tzOffsetText by remember {
        val initialTz = if (systemNow.timeZoneOffsetHours >= 0) "+${systemNow.timeZoneOffsetHours}" else systemNow.timeZoneOffsetHours.toString()
        mutableStateOf(settingsManager.getString("key_solar_tz_offset", initialTz))
    }

    var pressureText by remember {
        val saved = settingsManager.getString("key_solar_pressure", "")
        mutableStateOf(saved.ifEmpty {
            val alt = altitudeText.toDoubleOrNull() ?: 0.0
            val p = liveSensorPressure?.toDouble() ?: estimatePressureFromAltitude(alt)
            formatNumbers(p)
        })
    }
    var temperatureText by remember {
        val saved = settingsManager.getString("key_solar_temperature", "")
        mutableStateOf(saved.ifEmpty {
            val alt = altitudeText.toDoubleOrNull() ?: 0.0
            val lat = latitudeText.toDoubleOrNull() ?: 0.0
            val mo = monthText.toIntOrNull() ?: 6
            val t = estimateTemperatureFromAltitudeAndLatitude(alt, lat, mo)
            formatNumbers(t)
        })
    }

    val autoRecalculateAtmosphere: (Double) -> Unit = { newAlt ->
        val p = liveSensorPressure?.toDouble() ?: estimatePressureFromAltitude(newAlt)
        val lat = latitudeText.toDoubleOrNull() ?: 0.0
        val mo = monthText.toIntOrNull() ?: 6
        val t = estimateTemperatureFromAltitudeAndLatitude(newAlt, lat, mo)

        val pStr = formatNumbers(p)
        val tStr = formatNumbers(t)

        pressureText = pStr
        temperatureText = tStr

        settingsManager.putString("key_solar_pressure", pStr)
        settingsManager.putString("key_solar_temperature", tStr)
    }

    var locationStatusMessage by remember { mutableStateOf<String?>(null) }

    val lat = latitudeText.toDoubleOrNull()
    val lon = longitudeText.toDoubleOrNull()
    val yr = yearText.toIntOrNull()
    val mo = monthText.toIntOrNull()
    val dy = dayText.toIntOrNull()
    val tz = tzOffsetText.replace("+", "").toDoubleOrNull()
    val alt = altitudeText.toDoubleOrNull() ?: 0.0
    val press = pressureText.toDoubleOrNull() ?: estimatePressureFromAltitude(alt)
    val temp = temperatureText.toDoubleOrNull() ?: 15.0

    val result: SolarEventsResult? = if (lat != null && lon != null && yr != null && mo != null && dy != null && tz != null) {
        runCatching {
            SolarEventCalculator.calculateSolarEvents(
                latitudeDeg = lat,
                longitudeDeg = lon,
                year = yr,
                month = mo,
                day = dy,
                timeZoneOffsetHours = tz,
                pressureMbar = press,
                temperatureC = temp,
                elevationMeters = alt
            )
        }.getOrNull()
    } else {
        null
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (result != null) {
            // Main Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "SUNRISE & SUNSET",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Sunrise",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                            )
                            Text(
                                text = formatEventTime(result.sunrise, "Polar Day (No Sunset)", "Polar Night (No Rise)"),
                                color = Color(0xFFFFB74D),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Sunset",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                            )
                            Text(
                                text = formatEventTime(result.sunset, "Polar Day (No Sunset)", "Polar Night (No Rise)"),
                                color = Color(0xFFFF8A65),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Daylight Duration",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                            )
                            Text(
                                text = formatDaylightDuration(result.daylightDurationSeconds),
                                color = Color(0xFF81C784),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Solar Noon",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                            )
                            Text(
                                text = "${result.solarNoonTime.toFullString()} (${formatNumbers(result.solarNoonElevationDeg)}°)",
                                color = Color(0xFFFFD54F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            // Twilight Transitions Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "TWILIGHT TRANSITIONS",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    // Civil Twilight (-6.0°)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Civil Twilight (-6°)",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${formatEventTime(result.civilSunrise, "> -6°", "< -6°")} / ${formatEventTime(result.civilSunset, "> -6°", "< -6°")}",
                            color = Color(0xFF90CAF9),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Astronomical Twilight (-18.0°)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Astronomical (-18°)",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${formatEventTime(result.astronomicalSunrise, "> -18°", "< -18°")} / ${formatEventTime(result.astronomicalSunset, "> -18°", "< -18°")}",
                            color = Color(0xFFB39DDB),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Golden & Blue Hour Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "GOLDEN & BLUE HOURS",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    // Golden Hour
                    Text(
                        text = "Golden Hour (-4° to +6°)",
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Morning: ${result.morningGoldenHour.toDisplayString()}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        Text("Evening: ${result.eveningGoldenHour.toDisplayString()}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Blue Hour
                    Text(
                        text = "Blue Hour (-8° to -4°)",
                        color = Color(0xFF64B5F6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Morning: ${result.morningBlueHour.toDisplayString()}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        Text("Evening: ${result.eveningBlueHour.toDisplayString()}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Text(
                    text = "Invalid Position or Date Input",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Observer Position Inputs
        Text(
            text = "Observer Position (WGS84)",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = latitudeText,
                onValueChange = {
                    latitudeText = it
                    settingsManager.putString("key_solar_latitude", it)
                },
                label = { Text("Latitude (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )

            OutlinedTextField(
                value = longitudeText,
                onValueChange = {
                    longitudeText = it
                    settingsManager.putString("key_solar_longitude", it)
                },
                label = { Text("Longitude (°)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )

            OutlinedButton(
                onClick = {
                    locationStatusMessage = "Fetching GPS location..."
                    locationProvider.getCurrentLocation { coordinates ->
                        if (coordinates != null) {
                            val latStr = formatNumbers(coordinates.latitude)
                            val lonStr = formatNumbers(coordinates.longitude)
                            val altStr = formatNumbers(coordinates.altitudeMeters)

                            latitudeText = latStr
                            longitudeText = lonStr
                            altitudeText = altStr

                            settingsManager.putString("key_solar_latitude", latStr)
                            settingsManager.putString("key_solar_longitude", lonStr)
                            settingsManager.putString("key_solar_altitude", altStr)

                            autoRecalculateAtmosphere(coordinates.altitudeMeters)
                            locationStatusMessage = "Location & Elevation updated!"
                        } else {
                            locationStatusMessage = "Could not fetch GPS. Check permissions."
                        }
                    }
                },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .height(56.dp)
                    .width(44.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                LocationPinIcon(modifier = Modifier.size(20.dp))
            }

            OutlinedButton(
                onClick = {
                    val latVal = latitudeText.toDoubleOrNull() ?: 0.0
                    val lonVal = longitudeText.toDoubleOrNull() ?: 0.0
                    mapLauncher.openMap(latVal, lonVal)
                    locationStatusMessage = "Opened Maps app. Drop a pin & copy coordinates!"
                },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .height(56.dp)
                    .width(44.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                MapIcon(modifier = Modifier.size(20.dp))
            }

            OutlinedButton(
                onClick = {
                    val clipText = clipboardManager.getClipboardText()
                    if (!clipText.isNullOrBlank()) {
                        val parsed = parseCoordinates(clipText)
                        if (parsed != null) {
                            val latStr = formatNumbers(parsed.latitude)
                            val lonStr = formatNumbers(parsed.longitude)
                            latitudeText = latStr
                            longitudeText = lonStr
                            settingsManager.putString("key_solar_latitude", latStr)
                            settingsManager.putString("key_solar_longitude", lonStr)
                            locationStatusMessage = "Pasted coordinates from clipboard!"
                        } else {
                            locationStatusMessage = "Clipboard text contains no valid coordinates."
                        }
                    } else {
                        locationStatusMessage = "Clipboard is empty."
                    }
                },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .height(56.dp)
                    .width(44.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                ClipboardIcon(modifier = Modifier.size(20.dp))
            }
        }

        AdvancedAtmosphereAccordion(
            altitudeText = altitudeText,
            onAltitudeChange = {
                altitudeText = it
                settingsManager.putString("key_solar_altitude", it)
                val newAlt = it.toDoubleOrNull() ?: 0.0
                autoRecalculateAtmosphere(newAlt)
            },
            pressureText = pressureText,
            onPressureChange = {
                pressureText = it
                settingsManager.putString("key_solar_pressure", it)
            },
            temperatureText = temperatureText,
            onTemperatureChange = {
                temperatureText = it
                settingsManager.putString("key_solar_temperature", it)
            },
            onResetToAuto = {
                val altVal = altitudeText.toDoubleOrNull() ?: 0.0
                autoRecalculateAtmosphere(altVal)
            },
            sensorPressureMbar = liveSensorPressure,
            textFieldColors = textFieldColors
        )

        if (locationStatusMessage != null) {
            Text(
                text = locationStatusMessage!!,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
            )
        }

        // Date Inputs
        Text(
            text = "Date Input",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = yearText,
                onValueChange = {
                    yearText = it
                    settingsManager.putString("key_solar_year", it)
                },
                label = { Text("Year") },
                modifier = Modifier.weight(1.2f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            OutlinedTextField(
                value = monthText,
                onValueChange = {
                    monthText = it
                    settingsManager.putString("key_solar_month", it)
                },
                label = { Text("Month") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            OutlinedTextField(
                value = dayText,
                onValueChange = {
                    dayText = it
                    settingsManager.putString("key_solar_day", it)
                },
                label = { Text("Day") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
            OutlinedTextField(
                value = tzOffsetText,
                onValueChange = {
                    tzOffsetText = it
                    settingsManager.putString("key_solar_tz_offset", it)
                },
                label = { Text("UTC Offset") },
                modifier = Modifier.weight(1.2f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
            )
        }

        OutlinedButton(
            onClick = {
                val now = getSystemDateTime()
                yearText = now.year.toString()
                monthText = now.month.toString()
                dayText = now.day.toString()
                tzOffsetText = if (now.timeZoneOffsetHours >= 0) "+${now.timeZoneOffsetHours}" else now.timeZoneOffsetHours.toString()

                settingsManager.putString("key_solar_year", yearText)
                settingsManager.putString("key_solar_month", monthText)
                settingsManager.putString("key_solar_day", dayText)
                settingsManager.putString("key_solar_tz_offset", tzOffsetText)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        ) {
            Text("Set to Current Device Date")
        }
    }
}
