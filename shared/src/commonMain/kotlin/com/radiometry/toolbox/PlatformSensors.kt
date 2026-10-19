package com.radiometry.toolbox

import androidx.compose.runtime.Composable

data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double
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

interface MapLauncher {
    fun openMap(latitude: Double, longitude: Double)
}

interface ClipboardManager {
    fun getClipboardText(): String?
}

fun parseCoordinates(input: String): LocationCoordinates? {
    if (input.isBlank()) return null
    // Matches decimal degrees e.g. "59.3293, 18.0686" or "59.3293, -105.1786" or "39.742476 -105.1786"
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
expect fun rememberMapLauncher(): MapLauncher

@Composable
expect fun rememberClipboardManager(): ClipboardManager

expect fun getSystemDateTime(): SystemDateTime
