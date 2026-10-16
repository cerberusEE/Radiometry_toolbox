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

@Composable
expect fun rememberLocationProvider(): LocationProvider

@Composable
expect fun rememberCompassSensor(): CompassSensor

@Composable
expect fun rememberAmbientLightSensor(): AmbientLightSensor

expect fun getSystemDateTime(): SystemDateTime
