package com.radiometry.toolbox

import androidx.compose.runtime.Composable
import kotlin.math.pow

data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0
)

data class SystemDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val timeZoneOffsetHours: Double
)

interface LocationProvider {
    fun getCurrentLocation(onLocationResult: (LocationCoordinates?) -> Unit)
}

interface CompassSensor {
    fun startListening(onHeadingChanged: (Float) -> Unit)
    fun stopListening()
}

interface AmbientLightSensor {
    fun isSensorAvailable(): Boolean
    fun startListening(onLuxChanged: (Float) -> Unit)
    fun stopListening()
}

interface AmbientPressureSensor {
    fun isSensorAvailable(): Boolean
    fun startListening(onPressureMbarChanged: (Float) -> Unit)
    fun stopListening()
}

interface MapLauncher {
    fun openMap(latitude: Double, longitude: Double)
}

interface ClipboardManager {
    fun getClipboardText(): String?
}

fun estimatePressureFromAltitude(altitudeMeters: Double): Double {
    if (altitudeMeters <= 0.0) return 1013.25
    val p = 1013.25 * (1.0 - 2.25577e-5 * altitudeMeters).pow(5.25588)
    return p.coerceIn(300.0, 1100.0)
}

fun estimateTemperatureFromAltitudeAndLatitude(
    altitudeMeters: Double,
    latitudeDeg: Double,
    month: Int = 6
): Double {
    val latRad = latitudeDeg * (kotlin.math.PI / 180.0)
    val dayOfYearApprox = ((month - 1) * 30 + 15).coerceIn(1, 365)
    val seasonalFactor = kotlin.math.cos(2.0 * kotlin.math.PI * (dayOfYearApprox - 172) / 365.0)
    val seaLevelTempC = 15.0 + 15.0 * kotlin.math.cos(latRad) * seasonalFactor
    return seaLevelTempC - (0.0065 * altitudeMeters)
}

fun parseCoordinates(input: String): LocationCoordinates? {
    if (input.isBlank()) return null
    val decimalRegex = Regex("""(-?\d{1,2}(?:\.\d+)?)\s*[,;\s]\s*(-?\d{1,3}(?:\.\d+)?)""")
    val match = decimalRegex.find(input)
    if (match != null) {
        val lat = match.groupValues[1].toDoubleOrNull()
        val lon = match.groupValues[2].toDoubleOrNull()
        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
            return LocationCoordinates(lat, lon)
        }
    }
    return null
}

@Composable
expect fun rememberLocationProvider(): LocationProvider

@Composable
expect fun rememberCompassSensor(): CompassSensor

@Composable
expect fun rememberAmbientLightSensor(): AmbientLightSensor

@Composable
expect fun rememberAmbientPressureSensor(): AmbientPressureSensor

@Composable
expect fun rememberMapLauncher(): MapLauncher

@Composable
expect fun rememberClipboardManager(): ClipboardManager

expect fun getSystemDateTime(): SystemDateTime
