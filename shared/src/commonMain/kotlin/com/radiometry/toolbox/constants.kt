package com.radiometry.toolbox

import androidx.compose.ui.graphics.Color

enum class ToolType(
    val title: String,
    val description: String,
    val color: Color
) {
    BLACKBODY_RADIANCE("Blackbody Radiance", "Calculate Planck spectral and band radiance", Color(0xFF0A2246)),
    BLACKBODY_CONTRAST("Blackbody Contrast", "Compare radiance contrast between target temperatures", Color(0xFF500A1D)),
    TEMPERATURE_UNITS("Temperature Units", "Convert temperature units and view blackbody color", Color(0xFF002C27)),
    SOURCE_IRRADIANCE("Source Irradiance", "Calculate irradiance at distance from a beam source", Color(0xFF3B0B4C)),
    IFOV_GSD("IFOV & GSD", "Calculate optics IFOV and Ground Sampling Distance", Color(0xFF421C00)),
    SOLAR_POSITION("Solar Position", "Calculate sun elevation, azimuth, and compass orientation", Color(0xFF481400)),
    SUN_EVENTS("Sun Events", "Sunrise, sunset, twilight, and golden/blue hours", Color(0xFF3E1A00)),
    LUX_METER("Lux Meter", "Measure real-time illuminance using the ambient light sensor", Color(0xFF082C0C));
}

data class Tool(
    val type: ToolType,
    val name: String = type.title,
    val description: String = type.description,
    val color: Color = type.color
) {
    constructor(
        name: String,
        description: String,
        color: Color
    ) : this(
        type = ToolType.entries.firstOrNull { it.title == name } ?: ToolType.BLACKBODY_RADIANCE,
        name = name,
        description = description,
        color = color
    )
}

enum class WavelengthUnit(val symbol: String, val label: String) {
    NANOMETERS("nm", "Nanometers"),
    MICROMETERS("µm", "Micrometers"),
    INVERSE_CENTIMETERS("cm⁻¹", "Inverse Centimeters"),
}

enum class TemperatureUnit(val symbol: String, val label: String) {
    CELSIUS("°C", "Celsius"),
    KELVIN("K", "Kelvin"),
    FAHRENHEIT("°F", "Fahrenheit"),
}

enum class IfovUnit(val symbol: String, val label: String) {
    MILLIRADIANS("mrad", "Milliradians"),
    MICRORADIANS("µrad", "Microradians"),
    RADIANS("rad", "Radians"),
    DEGREES("°", "Degrees"),
}

const val WIEN_DISPLACEMENT_CONSTANT_M = 2.897771955e-3
const val MAX_RECENT_TOOLS_COUNT = 3

data class FileData(
    val x: DoubleArray,
    val y: DoubleArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FileData

        if (!x.contentEquals(other.x)) return false
        if (!y.contentEquals(other.y)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.contentHashCode()
        result = 31 * result + y.contentHashCode()
        return result
    }
}

data class RadianceComparisonResult(
    val radiance1: Double,
    val radiance2: Double,
    val contrast: Double,
    val emitted1: Double = radiance1,
    val reflected1: Double = 0.0,
    val emitted2: Double = radiance2,
    val reflected2: Double = 0.0,
)
