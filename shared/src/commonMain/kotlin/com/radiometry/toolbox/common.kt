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
fun calculateSpectralRadianceForUnit(
    xInUserUnit: Double,
    wavelengthUnit: WavelengthUnit,
    tempKelvin: Double,
): Double {
    if (xInUserUnit <= 0.0 || tempKelvin <= 0.0) return 0.0
    return when (wavelengthUnit) {
        WavelengthUnit.NANOMETERS -> {
            val lambdaMeters = xInUserUnit * 1e-9
            spectralRadiance(lambdaMeters, tempKelvin) * 1e-9
        }

        WavelengthUnit.MICROMETERS -> {
            val lambdaMeters = xInUserUnit * 1e-6
            spectralRadiance(lambdaMeters, tempKelvin) * 1e-6
        }

        WavelengthUnit.INVERSE_CENTIMETERS -> {
            val lambdaMeters = 1.0 / (100.0 * xInUserUnit)
            spectralRadiance(lambdaMeters, tempKelvin) * (100.0 * lambdaMeters * lambdaMeters)
        }
    }
}

fun spectralRadiance(lambdaMeters: Double, temperatureKelvin: Double): Double {
    if (temperatureKelvin <= 0.0 || lambdaMeters <= 0.0) return 0.0
    val c1 = 1.1910429723971884e-16
    val c2 = 0.014387773538277204
    val x = c2 / (lambdaMeters * temperatureKelvin)
    if (x > 700.0) return 0.0
    val expTerm = kotlin.math.exp(x) - 1.0
    if (expTerm <= 0.0) return 0.0
    return c1 / (lambdaMeters.pow(5.0) * expTerm)
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

    val n = if (steps % 2 != 0) steps + 1 else steps
    val hStep = (lambdaMax - lambdaMin) / n
    var sum = spectralRadiance(lambdaMin, temperatureKelvin) + spectralRadiance(
        lambdaMax,
        temperatureKelvin
    )

    for (i in 1 until n) {
        val lambda = lambdaMin + i * hStep
        val factor = if (i % 2 == 0) 2.0 else 4.0
        sum += factor * spectralRadiance(lambda, temperatureKelvin)
    }

    return (hStep / 3.0) * sum
}

fun calculateIrradiance(
    opticalPower: Double,
    divergenceSr: Double,
    distance: Double,
    useAtmosphericCompensation: Boolean,
    transmissionAt1000m: Double,
): Double {
    if (divergenceSr <= 0.0 || distance < 0.0) return 0.0
    val area = divergenceSr * distance * distance
    if (area <= 0.0) return 0.0
    val unattenuatedIrradiance = opticalPower / area
    if (!useAtmosphericCompensation) return unattenuatedIrradiance

    val tRef = when {
        transmissionAt1000m <= 0.0 -> 0.0
        transmissionAt1000m > 1.0 && transmissionAt1000m <= 100.0 -> transmissionAt1000m / 100.0
        transmissionAt1000m > 100.0 -> 1.0
        else -> transmissionAt1000m
    }

    if (tRef <= 0.0) return 0.0
    if (tRef >= 1.0) return unattenuatedIrradiance

    val transmission = tRef.pow(distance / 1000.0)
    return unattenuatedIrradiance * transmission
}

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

fun formatRadiance(value: Double): String {
    if (value.isNaN() || value < 0.0) return "Invalid Input"
    val formatted = formatNumbers(value)
    return "$formatted W/sr/m²"
}

fun parseTwoColumnCsv(text: String): FileData {

    val x = mutableListOf<Double>()
    val y = mutableListOf<Double>()

    text.lineSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() }
        .filterNot { it.startsWith("#") }
        .forEach { line ->

            val delimiter =
                if (';' in line) ';' else ','

            val cols = line.split(delimiter)

            require(cols.size == 2) {
                "Line does not contain two columns: '$line'"
            }

            val a = cols[0].trim().toDoubleOrNull()
            val b = cols[1].trim().toDoubleOrNull()

            if (a != null && b != null) {
                x += a
                y += b
            }
        }

    return FileData(
        x.toDoubleArray(),
        y.toDoubleArray()
    )
}