package com.radiometry.toolbox

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

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

fun convertToWavelengthInMeters(value: Double, unit: WavelengthUnit): Double? {
    if (value <= 0.0 || value.isNaN() || value.isInfinite()) return null
    return when (unit) {
        WavelengthUnit.NANOMETERS -> value * 1e-9
        WavelengthUnit.MICROMETERS -> value * 1e-6
        WavelengthUnit.INVERSE_CENTIMETERS -> 1.0 / (100.0 * value)
    }
}

fun convertTemperatureToKelvin(value: Double, unit: TemperatureUnit): Double? {
    if (value.isNaN() || value.isInfinite()) return null
    val kelvin = when (unit) {
        TemperatureUnit.CELSIUS -> value + 273.15
        TemperatureUnit.KELVIN -> value
        TemperatureUnit.FAHRENHEIT -> ((value - 32.0) * 5.0 / 9.0) + 273.15
    }
    return if (kelvin > 0.0) kelvin else null
}

fun calculateRadiance(
    startWavelengthMeters: Double,
    stopWavelengthMeters: Double,
    temperatureKelvin: Double,
    steps: Int = 1000,
): Double {
    if (temperatureKelvin <= 0.0) return 0.0
    val lambdaMin = minOf(startWavelengthMeters, stopWavelengthMeters)
    val lambdaMax = maxOf(startWavelengthMeters, stopWavelengthMeters)
    if (lambdaMin <= 0.0 || lambdaMax <= 0.0 || lambdaMin == lambdaMax) return 0.0

    val c1 = 1.1910429723971884e-16
    val c2 = 0.014387773538277204

    fun spectralRadiance(lambda: Double): Double {
        val x = c2 / (lambda * temperatureKelvin)
        if (x > 700.0) return 0.0
        val expTerm = kotlin.math.exp(x) - 1.0
        if (expTerm <= 0.0) return 0.0
        return c1 / (lambda.pow(5.0) * expTerm)
    }

    val n = if (steps % 2 != 0) steps + 1 else steps
    val hStep = (lambdaMax - lambdaMin) / n
    var sum = spectralRadiance(lambdaMin) + spectralRadiance(lambdaMax)

    for (i in 1 until n) {
        val lambda = lambdaMin + i * hStep
        val factor = if (i % 2 == 0) 2.0 else 4.0
        sum += factor * spectralRadiance(lambda)
    }

    return (hStep / 3.0) * sum
}

fun calculateIrradiance(opticalPower: Double, divergenceSr: Double, distance: Double, useAtmosphericCompensation: Boolean, extinctionCoefficient: Double): Double {
    if (opticalPower <= 0.0 || divergenceSr <= 0.0 || distance < 0.0) return 0.0
    val solidAngle = divergenceSr
    val area = solidAngle * distance * distance
    val irradiance = opticalPower / area
    if (!useAtmosphericCompensation) return irradiance
    val atmosphericLoss = kotlin.math.exp(-extinctionCoefficient * distance)
    return irradiance * atmosphericLoss
}

fun formatIrradiance(value: Double): String {
    if (value.isNaN() || value.isInfinite() || value < 0.0) return "Invalid Input"
    if (value == 0.0 || value < 1e-30) return "0 W/m\u00b2"
    val absVal = abs(value)
    return if (absVal !in 1e-3..<1e5) {
        val exp = kotlin.math.floor(kotlin.math.log10(absVal)).toInt()
        val mantissa = absVal / 10.0.pow(exp.toDouble())
        val roundedMantissa = round(mantissa * 100.0) / 100.0
        "${roundedMantissa}e$exp W/m\u00b2"
    } else {
        val rounded = round(value * 100.0) / 100.0
        if (abs(rounded - value) < 1e-6 && abs(rounded % 1.0) < 1e-6) {
            "${rounded.toLong()} W/m\u00b2"
        } else {
            val str = rounded.toString()
            if (str.contains('.')) {
                val parts = str.split('.')
                val decimals = parts[1].take(2).padEnd(2, '0')
                "${parts[0]}.$decimals W/m\u00b2"
            } else {
                "$str W/m\u00b2"
            }
        }
    }
}

fun formatRadiance(value: Double): String {
    if (value.isNaN() || value.isInfinite() || value < 0.0) return "Invalid Input"
    if (value == 0.0 || value < 1e-30) return "0 W/sr/m²"
    val absVal = abs(value)
    return if (absVal !in 1e-3..<1e5) {
        val exp = kotlin.math.floor(kotlin.math.log10(absVal)).toInt()
        val mantissa = absVal / 10.0.pow(exp.toDouble())
        val roundedMantissa = round(mantissa * 100.0) / 100.0
        "${roundedMantissa}e$exp W/sr/m²"
    } else {
        val rounded = round(value * 100.0) / 100.0
        if (abs(rounded - value) < 1e-6 && abs(rounded % 1.0) < 1e-6) {
            "${rounded.toLong()} W/sr/m²"
        } else {
            val str = rounded.toString()
            if (str.contains('.')) {
                val parts = str.split('.')
                val decimals = parts[1].take(2).padEnd(2, '0')
                "${parts[0]}.$decimals W/sr/m²"
            } else {
                "$str W/sr/m²"
            }
        }
    }
}
