package com.radiometry.toolbox

import kotlin.math.exp
import kotlin.math.pow

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
    val expTerm = exp(x) - 1.0
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

fun calculateRadianceContrast(
    startWavelengthMeters: Double,
    stopWavelengthMeters: Double,
    temp1Kelvin: Double,
    emissivity1: Double,
    temp2Kelvin: Double,
    emissivity2: Double,
    tempBgKelvin: Double = 300.0,
): Double? {
    return calculateRadianceComparison(
        startWavelengthMeters = startWavelengthMeters,
        stopWavelengthMeters = stopWavelengthMeters,
        temp1Kelvin = temp1Kelvin,
        emissivity1 = emissivity1,
        temp2Kelvin = temp2Kelvin,
        emissivity2 = emissivity2,
        tempBgKelvin = tempBgKelvin,
    )?.contrast
}

fun calculateRadianceComparison(
    startWavelengthMeters: Double,
    stopWavelengthMeters: Double,
    temp1Kelvin: Double,
    emissivity1: Double,
    temp2Kelvin: Double,
    emissivity2: Double,
    tempBgKelvin: Double = 300.0,
): RadianceComparisonResult? {
    if (temp1Kelvin <= 0.0 || temp2Kelvin <= 0.0 || tempBgKelvin <= 0.0) return null
    if (emissivity1 < 0.0 || emissivity2 < 0.0) return null
    if (startWavelengthMeters <= 0.0 || stopWavelengthMeters <= 0.0) return null
    if (startWavelengthMeters == stopWavelengthMeters) return null

    val bbRadiance1 = calculateRadiance(startWavelengthMeters, stopWavelengthMeters, temp1Kelvin)
    val bbRadiance2 = calculateRadiance(startWavelengthMeters, stopWavelengthMeters, temp2Kelvin)
    val bbRadianceBg = calculateRadiance(startWavelengthMeters, stopWavelengthMeters, tempBgKelvin)

    val emitted1 = emissivity1 * bbRadiance1
    val reflected1 = (1.0 - emissivity1).coerceAtLeast(0.0) * bbRadianceBg
    val rad1 = emitted1 + reflected1

    val emitted2 = emissivity2 * bbRadiance2
    val reflected2 = (1.0 - emissivity2).coerceAtLeast(0.0) * bbRadianceBg
    val rad2 = emitted2 + reflected2

    val contrast = rad1 - rad2

    return RadianceComparisonResult(
        radiance1 = rad1,
        radiance2 = rad2,
        contrast = contrast,
        emitted1 = emitted1,
        reflected1 = reflected1,
        emitted2 = emitted2,
        reflected2 = reflected2,
    )
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

fun calculatePeakWavelength(temperatureKelvin: Double, unit: WavelengthUnit): Double? {
    if (temperatureKelvin <= 0.0 || temperatureKelvin.isNaN() || temperatureKelvin.isInfinite()) return null
    val peakMeters = WIEN_DISPLACEMENT_CONSTANT_M / temperatureKelvin
    return convertWavelength(peakMeters * 1e6, WavelengthUnit.MICROMETERS, unit)
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

fun calculateIfovInRadians(focalLengthMm: Double, pixelPitchUm: Double): Double? {
    if (focalLengthMm <= 0.0 || pixelPitchUm <= 0.0 || focalLengthMm.isNaN() || pixelPitchUm.isNaN() || focalLengthMm.isInfinite() || pixelPitchUm.isInfinite()) return null
    val focalLengthMeters = focalLengthMm * 1e-3
    val pixelPitchMeters = pixelPitchUm * 1e-6
    return pixelPitchMeters / focalLengthMeters
}

fun calculatePixelPitchUm(ifovRadians: Double, focalLengthMm: Double): Double? {
    if (ifovRadians <= 0.0 || focalLengthMm <= 0.0 || ifovRadians.isNaN() || focalLengthMm.isNaN() || ifovRadians.isInfinite() || focalLengthMm.isInfinite()) return null
    val focalLengthMeters = focalLengthMm * 1e-3
    val pixelPitchMeters = ifovRadians * focalLengthMeters
    return pixelPitchMeters * 1e6
}

fun calculateGsdInMeters(ifovRadians: Double, distanceMeters: Double): Double? {
    if (ifovRadians <= 0.0 || distanceMeters < 0.0 || ifovRadians.isNaN() || distanceMeters.isNaN() || ifovRadians.isInfinite() || distanceMeters.isInfinite()) return null
    return ifovRadians * distanceMeters
}

fun calculatePixelFootprintArea(gsdMeters: Double): Double? {
    if (gsdMeters < 0.0 || gsdMeters.isNaN() || gsdMeters.isInfinite()) return null
    return gsdMeters * gsdMeters
}
