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

    private const val OFFICIAL_SUNRISE_SUNSET_ELEVATION = -0.833333
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
        deltaTSeconds: Double = 69.0
    ): SolarEventsResult {
        val location = SpaLocation(latitudeDeg, longitudeDeg)

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
            return SpaCalculator.calculateSolarPosition(location, dateTime, deltaTSeconds).elevationAngleDeg
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
        val tNoon = (aNoon + bNoon) / 2.0

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

        val noonElevation = noonResult.second

        fun findMorningCrossing(targetElev: Double): SolarEventTime {
            if (noonElevation < targetElev) return SolarEventTime.AlwaysBelow
            if (minElev > targetElev) return SolarEventTime.AlwaysAbove

            var bracketStart = -1.0
            var bracketEnd = -1.0
            for (i in 0 until maxElevIndex) {
                if (elevations[i] <= targetElev && elevations[i + 1] >= targetElev) {
                    bracketStart = times[i]
                    bracketEnd = times[i + 1]
                    break
                }
            }

            if (bracketStart < 0.0) {
                if (elevations[0] > targetElev) {
                    return SolarEventTime.AlwaysAbove
                }
                return SolarEventTime.AlwaysBelow
            }

            var low = bracketStart
            var high = bracketEnd
            repeat(20) {
                val mid = (low + high) / 2.0
                if (elevationAt(mid) < targetElev) {
                    low = mid
                } else {
                    high = mid
                }
            }
            val tCross = (low + high) / 2.0
            val h = tCross.toInt().coerceIn(0, 23)
            val remM = (tCross - h) * 60.0
            val m = remM.toInt().coerceIn(0, 59)
            val s = (remM - m) * 60.0
            return createOccurredTime(h, m, s)
        }

        fun findEveningCrossing(targetElev: Double): SolarEventTime {
            if (noonElevation < targetElev) return SolarEventTime.AlwaysBelow
            if (minElev > targetElev) return SolarEventTime.AlwaysAbove

            var bracketStart = -1.0
            var bracketEnd = -1.0
            for (i in maxElevIndex until sampleCount) {
                if (elevations[i] >= targetElev && elevations[i + 1] <= targetElev) {
                    bracketStart = times[i]
                    bracketEnd = times[i + 1]
                    break
                }
            }

            if (bracketStart < 0.0) {
                if (elevations[sampleCount] > targetElev) {
                    return SolarEventTime.AlwaysAbove
                }
                return SolarEventTime.AlwaysBelow
            }

            var low = bracketStart
            var high = bracketEnd
            repeat(20) {
                val mid = (low + high) / 2.0
                if (elevationAt(mid) > targetElev) {
                    low = mid
                } else {
                    high = mid
                }
            }
            val tCross = (low + high) / 2.0
            val h = tCross.toInt().coerceIn(0, 23)
            val remM = (tCross - h) * 60.0
            val m = remM.toInt().coerceIn(0, 59)
            val s = (remM - m) * 60.0
            return createOccurredTime(h, m, s)
        }

        val sunrise = findMorningCrossing(OFFICIAL_SUNRISE_SUNSET_ELEVATION)
        val sunset = findEveningCrossing(OFFICIAL_SUNRISE_SUNSET_ELEVATION)

        val civilSunrise = findMorningCrossing(CIVIL_TWILIGHT_ELEVATION)
        val civilSunset = findEveningCrossing(CIVIL_TWILIGHT_ELEVATION)

        val astroSunrise = findMorningCrossing(ASTRONOMICAL_TWILIGHT_ELEVATION)
        val astroSunset = findEveningCrossing(ASTRONOMICAL_TWILIGHT_ELEVATION)

        val goldenLowMorning = findMorningCrossing(GOLDEN_HOUR_LOW_ELEVATION)
        val goldenHighMorning = findMorningCrossing(GOLDEN_HOUR_HIGH_ELEVATION)
        val morningGoldenHour = SolarTimespan(goldenLowMorning, goldenHighMorning)

        val goldenHighEvening = findEveningCrossing(GOLDEN_HOUR_HIGH_ELEVATION)
        val goldenLowEvening = findEveningCrossing(GOLDEN_HOUR_LOW_ELEVATION)
        val eveningGoldenHour = SolarTimespan(goldenHighEvening, goldenLowEvening)

        val blueLowMorning = findMorningCrossing(BLUE_HOUR_LOW_ELEVATION)
        val blueHighMorning = findMorningCrossing(BLUE_HOUR_HIGH_ELEVATION)
        val morningBlueHour = SolarTimespan(blueLowMorning, blueHighMorning)

        val blueHighEvening = findEveningCrossing(BLUE_HOUR_HIGH_ELEVATION)
        val blueLowEvening = findEveningCrossing(BLUE_HOUR_LOW_ELEVATION)
        val eveningBlueHour = SolarTimespan(blueLowEvening, blueHighEvening)

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
    val systemNow = remember { getSystemDateTime() }

    var latitudeText by remember {
        mutableStateOf(settingsManager.getString("key_solar_latitude", "39.742476"))
    }
    var longitudeText by remember {
        mutableStateOf(settingsManager.getString("key_solar_longitude", "-105.1786"))
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

    var locationStatusMessage by remember { mutableStateOf<String?>(null) }

    val lat = latitudeText.toDoubleOrNull()
    val lon = longitudeText.toDoubleOrNull()
    val yr = yearText.toIntOrNull()
    val mo = monthText.toIntOrNull()
    val dy = dayText.toIntOrNull()
    val tz = tzOffsetText.replace("+", "").toDoubleOrNull()

    val result: SolarEventsResult? = if (lat != null && lon != null && yr != null && mo != null && dy != null && tz != null) {
        runCatching {
            SolarEventCalculator.calculateSolarEvents(
                latitudeDeg = lat,
                longitudeDeg = lon,
                year = yr,
                month = mo,
                day = dy,
                timeZoneOffsetHours = tz
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
                            latitudeText = latStr
                            longitudeText = lonStr
                            settingsManager.putString("key_solar_latitude", latStr)
                            settingsManager.putString("key_solar_longitude", lonStr)
                            locationStatusMessage = "Location updated!"
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
