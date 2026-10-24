package com.radiometry.toolbox

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

fun formatNumbers(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return "Inf"
    if (abs(value) < 1e-30) return "0"
    val absVal = abs(value)
    return if (absVal !in 1e-1..<1e5) {
        val exp = kotlin.math.floor(kotlin.math.log10(absVal)).toInt()
        val mantissa = value / 10.0.pow(exp.toDouble())
        val roundedMantissa = round(mantissa * 100.0) / 100.0
        "${roundedMantissa}e$exp"
    } else {
        val rounded = round(value * 100.0) / 100.0
        if (abs(rounded - value) < 1e-6 && abs(rounded % 1.0) < 1e-6) {
            "${rounded.toLong()}"
        } else {
            val str = rounded.toString()
            if (str.contains('.')) {
                val parts = str.split('.')
                val decimals = parts[1].take(2).padEnd(2, '0')
                "${parts[0]}.$decimals"
            } else {
                str
            }
        }
    }
}

fun formatTemperature(value: Double, temperatureUnit: TemperatureUnit): String {
    return formatNumbers(value) + " " + temperatureUnit.symbol
}

fun formatIrradiance(value: Double): String {
    val formatted = formatNumbers(value)
    return "$formatted W/m²"
}

fun formatRadiantIntensity(value: Double): String {
    if (value.isNaN()) return "Invalid Input"
    if (value.isInfinite()) return "Inf W/sr"
    val formatted = formatNumbers(value)
    return "$formatted W/sr"
}

fun formatPeakWavelength(temperatureKelvin: Double?, unit: WavelengthUnit): String {
    if (temperatureKelvin == null || temperatureKelvin <= 0.0 || temperatureKelvin.isNaN()) return "Invalid Input"
    val peakVal = calculatePeakWavelength(temperatureKelvin, unit) ?: return "Invalid Input"
    return "${formatNumbers(peakVal)} ${unit.symbol}"
}

// Must allow negative values for radiance since the radiance contrast can be negative.
fun formatRadiance(value: Double): String {
    if (value.isNaN()) return "Invalid Input"
    val formatted = formatNumbers(value)
    return "$formatted W/sr/m²"
}

fun formatIfov(ifovRadians: Double, unit: IfovUnit): String {
    val converted = convertIfovFromRadians(ifovRadians, unit) ?: return "Invalid Input"
    return "${formatNumbers(converted)} ${unit.symbol}"
}

fun formatGsd(gsdMeters: Double): String {
    if (gsdMeters.isNaN() || gsdMeters < 0.0) return "Invalid Input"
    if (gsdMeters == 0.0) return "0 m"
    val formattedMeters = formatNumbers(gsdMeters)
    return when {
        gsdMeters >= 1.0 -> "$formattedMeters m"
        gsdMeters >= 0.01 -> {
            val cm = gsdMeters * 100.0
            "${formatNumbers(cm)} cm ($formattedMeters m)"
        }
        else -> {
            val mm = gsdMeters * 1000.0
            "${formatNumbers(mm)} mm ($formattedMeters m)"
        }
    }
}

fun formatFootprintArea(areaSqMeters: Double): String {
    if (areaSqMeters.isNaN() || areaSqMeters < 0.0) return "Invalid Input"
    val formattedSqM = formatNumbers(areaSqMeters)
    return when {
        areaSqMeters >= 1.0 -> "$formattedSqM m²"
        areaSqMeters >= 0.0001 -> {
            val sqCm = areaSqMeters * 10000.0
            "${formatNumbers(sqCm)} cm² ($formattedSqM m²)"
        }
        else -> {
            val sqMm = areaSqMeters * 1000000.0
            "${formatNumbers(sqMm)} mm² ($formattedSqM m²)"
        }
    }
}

fun formatTotalFov(radians: Double): String {
    if (radians.isNaN() || radians < 0.0) return "Invalid Input"
    val deg = radians * (180.0 / kotlin.math.PI)
    if (deg < 1.0) {
        val arcmin = deg * 60.0
        if (arcmin < 1.0) {
            val arcsec = arcmin * 60.0
            return "${formatNumbers(arcsec)} arcsec (${formatNumbers(deg)}°)"
        }
        return "${formatNumbers(arcmin)} arcmin (${formatNumbers(deg)}°)"
    }
    return "${formatNumbers(deg)}°"
}

fun formatRange(meters: Double): String {
    if (meters.isNaN() || meters < 0.0) return "Invalid Input"
    if (meters == 0.0) return "0 m"
    val formatted = formatNumbers(meters)
    return when {
        meters >= 1000.0 -> {
            val km = meters / 1000.0
            "${formatNumbers(km)} km ($formatted m)"
        }
        meters >= 1.0 -> "$formatted m"
        meters >= 0.01 -> {
            val cm = meters * 100.0
            "${formatNumbers(cm)} cm ($formatted m)"
        }
        else -> {
            val mm = meters * 1000.0
            "${formatNumbers(mm)} mm ($formatted m)"
        }
    }
}
