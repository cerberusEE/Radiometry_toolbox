package com.radiometry.toolbox

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
