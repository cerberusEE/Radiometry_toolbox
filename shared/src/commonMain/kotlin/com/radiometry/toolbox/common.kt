package com.radiometry.toolbox

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.time.TimeSource

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
    return 2.0 * kotlin.math.atan(pixelPitchMeters / (2.0 * focalLengthMeters))
}

fun calculatePixelPitchUm(ifovRadians: Double, focalLengthMm: Double): Double? {
    if (ifovRadians <= 0.0 || focalLengthMm <= 0.0 || ifovRadians.isNaN() || focalLengthMm.isNaN() || ifovRadians.isInfinite() || focalLengthMm.isInfinite()) return null
    val focalLengthMeters = focalLengthMm * 1e-3
    val pixelPitchMeters = 2.0 * focalLengthMeters * kotlin.math.tan(ifovRadians / 2.0)
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

/**
 * Base class for first-order IIR / Exponential Moving Average (EMA) low-pass filters.
 *
 * Manages timing, alpha calculation, cutoff frequency, and filter state for sensor stream data.
 * Subclasses can override preprocessing, delta calculation, or postprocessing to handle domain-specific
 * requirements (such as angular wrap-around for compass headings).
 *
 * @param timeConstantSeconds Target time constant tau in seconds (default: 0.5 seconds).
 * @param alpha Optional direct filter constant (0.0 < alpha <= 1.0). If specified, overrides dynamic time constant calculation.
 */
open class LowPassFilter(
    var timeConstantSeconds: Double = 0.5,
    var alpha: Double? = null
) {
    var filteredValue: Double? = null
        protected set

    private var lastTimeMark: TimeSource.Monotonic.ValueTimeMark? = null

    /**
     * Target cutoff frequency limit in Hz ($f_c = 1 / (2 \pi \tau)$).
     * Getting or setting this converts to/from [timeConstantSeconds].
     */
    var cutoffFrequencyHz: Double
        get() = 1.0 / (2.0 * PI * timeConstantSeconds.coerceAtLeast(1e-4))
        set(value) {
            require(value > 0.0) { "Cutoff frequency must be positive" }
            timeConstantSeconds = 1.0 / (2.0 * PI * value)
        }

    /**
     * Resets the filter state. Call this when enabling the sensor or starting a new session.
     */
    open fun reset() {
        filteredValue = null
        lastTimeMark = null
    }

    /**
     * Filters a new raw sensor reading (Float precision).
     *
     * @param rawValue Raw sensor reading.
     * @param dtSeconds Optional explicit elapsed time since previous sample in seconds.
     * @return Filtered value.
     */
    fun filter(rawValue: Float, dtSeconds: Double? = null): Float {
        return filter(rawValue.toDouble(), dtSeconds).toFloat()
    }

    /**
     * Filters a new raw sensor reading (Double precision).
     *
     * @param rawValue Raw sensor reading.
     * @param dtSeconds Optional explicit elapsed time since previous sample in seconds.
     * @return Filtered value.
     */
    open fun filter(rawValue: Double, dtSeconds: Double? = null): Double {
        val current = preprocessInput(rawValue)
        val prev = filteredValue
        val nowMark = TimeSource.Monotonic.markNow()

        if (prev == null) {
            filteredValue = current
            lastTimeMark = nowMark
            return current
        }

        val computedDt = dtSeconds ?: run {
            val mark = lastTimeMark
            if (mark != null) {
                val elapsedNs = (nowMark - mark).inWholeNanoseconds
                if (elapsedNs > 0) elapsedNs / 1e9 else null
            } else null
        }

        val effectiveAlpha = computeAlpha(computedDt)
        val delta = computeDelta(current = current, previous = prev)
        val updated = postprocessUpdated(prev + effectiveAlpha * delta)

        filteredValue = updated
        lastTimeMark = nowMark

        return updated
    }

    /**
     * Preprocesses raw input value before filtering. Default implementation returns input unchanged.
     */
    protected open fun preprocessInput(rawValue: Double): Double = rawValue

    /**
     * Computes the error delta between current and previous values.
     * Default implementation returns `current - previous`.
     */
    protected open fun computeDelta(current: Double, previous: Double): Double = current - previous

    /**
     * Postprocesses updated value after EMA step. Default implementation returns updated value unchanged.
     */
    protected open fun postprocessUpdated(updatedValue: Double): Double = updatedValue

    /**
     * Computes the effective alpha smoothing factor for the current step.
     */
    protected fun computeAlpha(dtSeconds: Double?): Double {
        alpha?.let { return it.coerceIn(0.001, 1.0) }

        if (dtSeconds != null && dtSeconds > 0.0 && dtSeconds < 2.0) {
            val tau = timeConstantSeconds.coerceAtLeast(1e-4)
            return (dtSeconds / (tau + dtSeconds)).coerceIn(0.001, 1.0)
        }

        // Fallback default sample interval if dt is unavailable (~60ms / 16.6 Hz sensor UI delay)
        val defaultDt = 0.06
        val tau = timeConstantSeconds.coerceAtLeast(1e-4)
        return (defaultDt / (tau + defaultDt)).coerceIn(0.001, 1.0)
    }

    companion object {
        /**
         * Creates a filter configured for a specific cutoff frequency in Hz.
         */
        fun fromCutoffFrequency(cutoffFrequencyHz: Double): LowPassFilter {
            require(cutoffFrequencyHz > 0.0) { "Cutoff frequency must be positive" }
            val tau = 1.0 / (2.0 * PI * cutoffFrequencyHz)
            return LowPassFilter(timeConstantSeconds = tau)
        }
    }
}

/**
 * Low-pass filter for scalar sensor values (e.g. ambient light lux, temperature) using a first-order IIR /
 * Exponential Moving Average (EMA) algorithm.
 *
 * @param timeConstantSeconds Target time constant tau in seconds (default: 0.5 seconds).
 * @param alpha Optional direct filter constant (0.0 < alpha <= 1.0).
 */
class ScalarLowPassFilter(
    timeConstantSeconds: Double = 0.5,
    alpha: Double? = null
) : LowPassFilter(timeConstantSeconds, alpha) {

    companion object {
        /**
         * Creates a scalar filter configured for a specific cutoff frequency in Hz.
         */
        fun fromCutoffFrequency(cutoffFrequencyHz: Double): ScalarLowPassFilter {
            require(cutoffFrequencyHz > 0.0) { "Cutoff frequency must be positive" }
            val tau = 1.0 / (2.0 * PI * cutoffFrequencyHz)
            return ScalarLowPassFilter(timeConstantSeconds = tau)
        }
    }
}

/**
 * Low-pass filter for compass headings (in degrees, [0, 360)) using a first-order IIR /
 * Exponential Moving Average (EMA) algorithm.
 *
 * Smooths out high-frequency sensor jitter while handling angular wrap-around at 0° / 360°.
 *
 * @param timeConstantSeconds Target time constant tau in seconds (default: 0.5 seconds).
 * @param alpha Optional direct filter constant (0.0 < alpha <= 1.0). If specified, overrides dynamic time constant calculation.
 */
class CompassLowPassFilter(
    timeConstantSeconds: Double = 0.5,
    alpha: Double? = null
) : LowPassFilter(timeConstantSeconds, alpha) {

    override fun preprocessInput(rawValue: Double): Double {
        return normalizeHeading360(rawValue)
    }

    override fun computeDelta(current: Double, previous: Double): Double {
        return normalizeAngle180(current - previous)
    }

    override fun postprocessUpdated(updatedValue: Double): Double {
        return normalizeHeading360(updatedValue)
    }

    companion object {
        /**
         * Normalizes an angle in degrees to the range [0, 360).
         */
        fun normalizeHeading360(headingDeg: Double): Double {
            var h = headingDeg % 360.0
            if (h < 0.0) h += 360.0
            return h
        }

        /**
         * Normalizes an angular difference in degrees to the range [-180, 180].
         */
        fun normalizeAngle180(angleDeg: Double): Double {
            var a = (angleDeg + 180.0) % 360.0
            if (a < 0.0) a += 360.0
            return a - 180.0
        }

        /**
         * Creates a compass filter configured for a specific cutoff frequency in Hz.
         * For example, cutoffFrequencyHz = 2.0 corresponds to tau ≈ 0.0796 seconds.
         */
        fun fromCutoffFrequency(cutoffFrequencyHz: Double): CompassLowPassFilter {
            require(cutoffFrequencyHz > 0.0) { "Cutoff frequency must be positive" }
            val tau = 1.0 / (2.0 * PI * cutoffFrequencyHz)
            return CompassLowPassFilter(timeConstantSeconds = tau)
        }
    }
}
