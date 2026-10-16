package com.radiometry.toolbox

import kotlin.math.PI

fun convertToWavelengthInMeters(value: Double, unit: WavelengthUnit): Double? {
    if (value <= 0.0 || value.isNaN() || value.isInfinite()) return null
    return when (unit) {
        WavelengthUnit.NANOMETERS -> value * 1e-9
        WavelengthUnit.MICROMETERS -> value * 1e-6
        WavelengthUnit.INVERSE_CENTIMETERS -> 1.0 / (100.0 * value)
    }
}

fun convertWavelength(value: Double, fromUnit: WavelengthUnit, toUnit: WavelengthUnit): Double? {
    val meters = convertToWavelengthInMeters(value, fromUnit) ?: return null
    return when (toUnit) {
        WavelengthUnit.NANOMETERS -> meters * 1e9
        WavelengthUnit.MICROMETERS -> meters * 1e6
        WavelengthUnit.INVERSE_CENTIMETERS -> 1.0 / (100.0 * meters)
    }
}

fun convertTemperatureToKelvin(value: Double, unit: TemperatureUnit): Double {
    if (value.isNaN() || value.isInfinite()) return value
    val kelvin = when (unit) {
        TemperatureUnit.CELSIUS -> value + 273.15
        TemperatureUnit.KELVIN -> value
        TemperatureUnit.FAHRENHEIT -> ((value - 32.0) * 5.0 / 9.0) + 273.15
    }
    return if (kelvin >= 0.0) kelvin else Double.NaN
}

fun convertTemperature(value: Double, fromUnit: TemperatureUnit, toUnit: TemperatureUnit): Double {
    val tempInKelvin = convertTemperatureToKelvin(value, fromUnit)
    return when (toUnit) {
        TemperatureUnit.CELSIUS -> tempInKelvin - 273.15
        TemperatureUnit.KELVIN -> tempInKelvin
        TemperatureUnit.FAHRENHEIT -> ((tempInKelvin - 273.15) * 9.0 / 5.0) + 32.0
    }
}

fun convertIfovToRadians(value: Double, unit: IfovUnit): Double? {
    if (value <= 0.0 || value.isNaN() || value.isInfinite()) return null
    return when (unit) {
        IfovUnit.MILLIRADIANS -> value * 1e-3
        IfovUnit.MICRORADIANS -> value * 1e-6
        IfovUnit.RADIANS -> value
        IfovUnit.DEGREES -> value * (PI / 180.0)
    }
}

fun convertIfovFromRadians(radians: Double, unit: IfovUnit): Double? {
    if (radians <= 0.0 || radians.isNaN() || radians.isInfinite()) return null
    return when (unit) {
        IfovUnit.MILLIRADIANS -> radians * 1e3
        IfovUnit.MICRORADIANS -> radians * 1e6
        IfovUnit.RADIANS -> radians
        IfovUnit.DEGREES -> radians * (180.0 / PI)
    }
}
