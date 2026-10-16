package com.radiometry.toolbox

import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

data class SpaLocation(
    val latitudeDeg: Double,
    val longitudeDeg: Double
) {
    init {
        require(latitudeDeg in -90.0..90.0) { "Latitude must be between -90 and 90 degrees." }
        require(longitudeDeg in -180.0..180.0) { "Longitude must be between -180 and 180 degrees." }
    }
}

data class SpaDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Double = 0.0,
    val timeZoneOffsetHours: Double = 0.0
) {
    init {
        require(month in 1..12) { "Month must be between 1 and 12." }
        require(day in 1..31) { "Day must be between 1 and 31." }
        require(hour in 0..23) { "Hour must be between 0 and 23." }
        require(minute in 0..59) { "Minute must be between 0 and 59." }
        require(second in 0.0..<60.0) { "Second must be in range [0, 60)." }
        require(timeZoneOffsetHours in -18.0..18.0) { "Time zone offset must be between -18 and +18 hours." }
    }
}

data class SpaResult(
    val zenithAngleDeg: Double,
    val elevationAngleDeg: Double,
    val azimuthAngleDeg: Double,
    val declinationDeg: Double,
    val rightAscensionDeg: Double,
    val localHourAngleDeg: Double,
    val equationOfTimeMinutes: Double,
    val sunDistanceAu: Double
)

private data class Term(val a: Double, val b: Double, val c: Double)

private data class NutationTerm(
    val y0: Int, val y1: Int, val y2: Int, val y3: Int, val y4: Int,
    val aPsi: Double, val bPsi: Double, val aEps: Double, val bEps: Double
)

object SpaCalculator {

    private fun Double.toRadians(): Double = this * (PI / 180.0)
    private fun Double.toDegrees(): Double = this * (180.0 / PI)

    private fun normalizeDegrees(angleDeg: Double): Double {
        var a = angleDeg % 360.0
        if (a < 0.0) a += 360.0
        return a
    }

    private fun normalizeDegrees180(angleDeg: Double): Double {
        var a = normalizeDegrees(angleDeg)
        if (a > 180.0) a -= 360.0
        return a
    }

    private fun evaluateTerms(terms: Array<Term>, jme: Double): Double {
        var sum = 0.0
        for ((a, b, c) in terms) {
            sum += a * cos(b + c * jme)
        }
        return sum
    }

    /**
     * Calculates the solar position and angles using the NREL Solar Position Algorithm (SPA).
     *
     * @param latitudeDeg Latitude in WGS84 decimal degrees (-90.0 to 90.0).
     * @param longitudeDeg Longitude in WGS84 decimal degrees (-180.0 to 180.0, positive East).
     * @param year Year (e.g. 2024).
     * @param month Month (1-12).
     * @param day Day of month (1-31).
     * @param hour Hour of day (0-23).
     * @param minute Minute of hour (0-59).
     * @param second Second of minute (0.0 to <60.0).
     * @param timeZoneOffsetHours Offset from UTC in hours (e.g. -5.0 for EST, +2.0 for EET).
     * @param deltaTSeconds Difference between Terrestrial Time and UT1 (default: 69.0 seconds).
     * @return [SpaResult] containing topocentric zenith, elevation, azimuth, declination, RA, hour angle, EOT, and distance.
     */
    fun calculateSolarPosition(
        latitudeDeg: Double,
        longitudeDeg: Double,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Double = 0.0,
        timeZoneOffsetHours: Double = 0.0,
        deltaTSeconds: Double = 69.0
    ): SpaResult {
        val location = SpaLocation(latitudeDeg, longitudeDeg)
        val dateTime = SpaDateTime(year, month, day, hour, minute, second, timeZoneOffsetHours)
        return calculateSolarPosition(location, dateTime, deltaTSeconds)
    }

    /**
     * Calculates the solar position and angles using the NREL Solar Position Algorithm (SPA).
     * Assumes 0 meters ellipsoid elevation.
     *
     * @param location WGS84 position.
     * @param dateTime Date and time of day.
     * @param deltaTSeconds Delta T = TT - UT1 in seconds (default: 69.0s).
     * @return [SpaResult] containing solar zenith, elevation, azimuth, declination, right ascension, local hour angle, EOT, and distance.
     */
    fun calculateSolarPosition(
        location: SpaLocation,
        dateTime: SpaDateTime,
        deltaTSeconds: Double = 69.0
    ): SpaResult {
        val ut = dateTime.hour + (dateTime.minute / 60.0) + (dateTime.second / 3600.0) - dateTime.timeZoneOffsetHours

        var y = dateTime.year
        var m = dateTime.month
        if (m <= 2) {
            y -= 1
            m += 12
        }

        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)

        val jd = floor(365.25 * (y + 4716.0)) + floor(30.6001 * (m + 1.0)) + dateTime.day + (ut / 24.0) + b - 1524.5
        val jde = jd + (deltaTSeconds / 86400.0)

        val jc = (jd - 2451545.0) / 36525.0
        val jce = (jde - 2451545.0) / 36525.0
        val jme = jce / 10.0

        val jme2 = jme * jme
        val jme3 = jme2 * jme
        val jme4 = jme3 * jme
        val jme5 = jme4 * jme

        val l0 = evaluateTerms(L0_TERMS, jme)
        val l1 = evaluateTerms(L1_TERMS, jme)
        val l2 = evaluateTerms(L2_TERMS, jme)
        val l3 = evaluateTerms(L3_TERMS, jme)
        val l4 = evaluateTerms(L4_TERMS, jme)
        val l5 = evaluateTerms(L5_TERMS, jme)

        val lRad = (l0 + l1 * jme + l2 * jme2 + l3 * jme3 + l4 * jme4 + l5 * jme5) / 100000000.0
        val lDeg = normalizeDegrees(lRad.toDegrees())

        val b0 = evaluateTerms(B0_TERMS, jme)
        val b1 = evaluateTerms(B1_TERMS, jme)

        val bRad = (b0 + b1 * jme) / 100000000.0
        val bDeg = bRad.toDegrees()

        val r0 = evaluateTerms(R0_TERMS, jme)
        val r1 = evaluateTerms(R1_TERMS, jme)
        val r2 = evaluateTerms(R2_TERMS, jme)
        val r3 = evaluateTerms(R3_TERMS, jme)
        val r4 = evaluateTerms(R4_TERMS, jme)

        val rAu = (r0 + r1 * jme + r2 * jme2 + r3 * jme3 + r4 * jme4) / 100000000.0

        val thetaDeg = normalizeDegrees(lDeg + 180.0)
        val betaDeg = -bDeg

        val jce2 = jce * jce
        val jce3 = jce2 * jce

        val x0 = 297.85036 + 445267.111480 * jce - 0.0019142 * jce2 + jce3 / 189474.0
        val x1 = 357.52911 + 35999.050290 * jce - 0.0001536 * jce2 + jce3 / 2449000.0
        val x2 = 134.96340 + 477198.867505 * jce + 0.0086972 * jce2 + jce3 / 56250.0
        val x3 = 93.27209 + 483202.017538 * jce - 0.0036825 * jce2 + jce3 / 327270.0
        val x4 = 125.04452 - 1934.136261 * jce + 0.0020708 * jce2 + jce3 / 450000.0

        var deltaPsiSum = 0.0
        var deltaEpsSum = 0.0

        for (term in NUTATION_TERMS) {
            val thetaN = (term.y0 * x0 + term.y1 * x1 + term.y2 * x2 + term.y3 * x3 + term.y4 * x4).toRadians()
            val sinN = sin(thetaN)
            val cosN = cos(thetaN)
            deltaPsiSum += term.aPsi * sinN + term.bPsi * cosN
            deltaEpsSum += term.aEps * cosN + term.bEps * sinN
        }

        val deltaPsiDeg = deltaPsiSum / 36000000.0
        val deltaEpsDeg = deltaEpsSum / 36000000.0

        val deltaTauDeg = -20.4898 / (3600.0 * rAu)
        val lambdaDeg = thetaDeg + deltaPsiDeg + deltaTauDeg

        val uArcsec = 84381.448 - 46.8150 * jme - 0.00059 * jme2 + 0.001813 * jme3
        val eps0Deg = uArcsec / 3600.0
        val epsDeg = eps0Deg + deltaEpsDeg

        val nu0Deg = normalizeDegrees(280.46061837 + 360.98564736629 * (jd - 2451545.0) + 0.000387933 * (jc * jc) - (jc * jc * jc) / 38710000.0)
        val nuDeg = nu0Deg + deltaPsiDeg * cos(epsDeg.toRadians())

        val lambdaRad = lambdaDeg.toRadians()
        val epsRad = epsDeg.toRadians()
        val betaRad = betaDeg.toRadians()

        val raRad = atan2(sin(lambdaRad) * cos(epsRad) - tan(betaRad) * sin(epsRad), cos(lambdaRad))
        val raDeg = normalizeDegrees(raRad.toDegrees())

        val decRad = asin(sin(betaRad) * cos(epsRad) + cos(betaRad) * sin(epsRad) * sin(lambdaRad))

        val hDeg = normalizeDegrees(nuDeg + location.longitudeDeg - raDeg)

        val phiRad = location.latitudeDeg.toRadians()
        val uRad = atan(0.99664719 * tan(phiRad))
        val xRad = 0.99664719 * sin(uRad)
        val yRad = cos(uRad)

        val xiRad = (8.794100 / (3600.0 * rAu)).toRadians()

        val hRad = hDeg.toRadians()

        val deltaRaRad = atan2(-yRad * sin(xiRad) * sin(hRad), cos(decRad) - yRad * sin(xiRad) * cos(hRad))
        val hPrimeRad = hRad - deltaRaRad
        val decPrimeRad = atan2((sin(decRad) - xRad * sin(xiRad)) * cos(deltaRaRad), cos(decRad) - yRad * sin(xiRad) * cos(hRad))

        val e0Rad = asin(sin(phiRad) * sin(decPrimeRad) + cos(phiRad) * cos(decPrimeRad) * cos(hPrimeRad))
        val e0Deg = e0Rad.toDegrees()

        val deltaEDeg = if (e0Deg >= -0.56) {
            val pressureMbar = 1013.25
            val tempC = 15.0
            val arg = e0Deg + (10.3 / (e0Deg + 5.11))
            val deltaEArcmin = (pressureMbar / 1010.0) * (283.0 / (273.0 + tempC)) * (1.02 / tan(arg.toRadians()))
            deltaEArcmin / 60.0
        } else {
            0.0
        }

        val eDeg = e0Deg + deltaEDeg
        val zenithDeg = 90.0 - eDeg

        val gammaRad = atan2(sin(hPrimeRad), cos(hPrimeRad) * sin(phiRad) - tan(decPrimeRad) * cos(phiRad))
        val azimuthDeg = normalizeDegrees(gammaRad.toDegrees() + 180.0)

        val mDeg = normalizeDegrees(280.466456 + 36000.7698277 * jce + 0.0003032028 * jce2 + (jce3 / 499310.0) - (jce2 * jce2 / 15300000.0) - (jce2 * jce3 / 2000000000.0))
        val eotDeg = normalizeDegrees180(mDeg - 0.0057183 - raDeg + deltaPsiDeg * cos(epsRad))
        val eotMinutes = eotDeg * 4.0

        return SpaResult(
            zenithAngleDeg = zenithDeg,
            elevationAngleDeg = eDeg,
            azimuthAngleDeg = azimuthDeg,
            declinationDeg = decPrimeRad.toDegrees(),
            rightAscensionDeg = raDeg,
            localHourAngleDeg = normalizeDegrees180(hPrimeRad.toDegrees()),
            equationOfTimeMinutes = eotMinutes,
            sunDistanceAu = rAu
        )
    }

    /**
     * Calculates the solar incidence angle on a tilted flat surface.
     *
     * @param zenithAngleDeg Solar zenith angle in degrees.
     * @param azimuthAngleDeg Solar azimuth angle in degrees (clockwise from North).
     * @param surfaceTiltDeg Surface slope/tilt angle in degrees (0 = horizontal, 90 = vertical).
     * @param surfaceAzimuthDeg Surface azimuth angle in degrees (clockwise from North, e.g. 180 = facing South).
     * @return Solar incidence angle in degrees.
     */
    fun calculateSolarIncidenceAngle(
        zenithAngleDeg: Double,
        azimuthAngleDeg: Double,
        surfaceTiltDeg: Double,
        surfaceAzimuthDeg: Double
    ): Double {
        val zRad = zenithAngleDeg.toRadians()
        val betaRad = surfaceTiltDeg.toRadians()
        val aRad = azimuthAngleDeg.toRadians()
        val awRad = surfaceAzimuthDeg.toRadians()

        val cosIncidence = cos(zRad) * cos(betaRad) + sin(zRad) * sin(betaRad) * cos(aRad - awRad)
        val clampedCos = cosIncidence.coerceIn(-1.0, 1.0)
        return acos(clampedCos).toDegrees()
    }

    private val L0_TERMS = arrayOf(
        Term(175347046.0, 0.0, 0.0), Term(3341656.0, 4.6692568, 6283.07585),
        Term(34894.0, 4.6261, 12566.1517), Term(3497.0, 2.7441, 5753.3849),
        Term(3418.0, 2.8289, 3.5231), Term(3136.0, 3.6277, 77713.7715),
        Term(2676.0, 4.4181, 7860.4194), Term(2343.0, 6.1352, 3930.2097),
        Term(1324.0, 0.7425, 11506.7698), Term(1273.0, 2.0371, 529.691),
        Term(1199.0, 1.1096, 1577.3435), Term(990.0, 5.233, 5884.927),
        Term(902.0, 2.045, 26.298), Term(857.0, 3.508, 398.149),
        Term(780.0, 1.179, 5223.694), Term(753.0, 2.533, 5507.553),
        Term(505.0, 4.583, 18849.228), Term(492.0, 4.205, 775.523),
        Term(357.0, 2.92, 0.067), Term(317.0, 5.849, 11790.629),
        Term(284.0, 1.899, 796.298), Term(271.0, 0.315, 10977.079),
        Term(243.0, 0.345, 5486.778), Term(206.0, 4.806, 2544.314),
        Term(205.0, 1.869, 5573.143), Term(202.0, 2.458, 6069.777),
        Term(156.0, 0.836, 213.299), Term(132.0, 3.411, 2942.463),
        Term(126.0, 1.083, 20.775), Term(115.0, 0.845, 0.98),
        Term(103.0, 0.847, 4694.003), Term(102.0, 0.963, 0.323),
        Term(102.0, 2.507, 1061.022), Term(99.0, 2.133, 2281.338),
        Term(98.0, 4.551, 20110.128), Term(86.0, 5.981, 1230.134),
        Term(85.0, 1.288, 5202.918), Term(85.0, 4.152, 2471.773),
        Term(80.0, 3.58, 298.114), Term(79.0, 0.88, 117.81),
        Term(75.0, 2.87, 8530.13), Term(75.0, 4.86, 73.06),
        Term(74.0, 2.37, 2415.54), Term(74.0, 2.01, 12028.18),
        Term(70.0, 2.11, 14.54), Term(62.0, 5.1, 7575.22),
        Term(61.0, 2.39, 1113.88), Term(58.0, 1.15, 606.58),
        Term(56.0, 5.09, 21329.28), Term(56.0, 2.21, 22.81),
        Term(52.0, 0.9, 11210.8), Term(52.0, 5.57, 3175.7),
        Term(51.0, 4.93, 11.23), Term(49.0, 5.82, 1231.11),
        Term(48.0, 3.14, 5323.52), Term(45.0, 4.4, 14018.1),
        Term(45.0, 1.25, 272.22), Term(44.0, 2.35, 27.52),
        Term(42.0, 2.2, 59.98), Term(38.0, 2.04, 3915.67),
        Term(38.0, 0.17, 825.26), Term(37.0, 1.62, 7780.2),
        Term(36.0, 1.61, 75.82), Term(36.0, 2.38, 9437.76)
    )

    private val L1_TERMS = arrayOf(
        Term(628331966700.0, 0.0, 0.0), Term(206059.0, 2.678235, 6283.07585),
        Term(4303.0, 2.6351, 12566.1517), Term(425.0, 1.59, 3.523),
        Term(119.0, 5.796, 7860.419), Term(109.0, 2.966, 1577.344),
        Term(93.0, 2.59, 18849.23), Term(72.0, 1.14, 529.69),
        Term(68.0, 1.87, 3930.21), Term(67.0, 4.41, 5753.38),
        Term(59.0, 2.89, 5884.93), Term(56.0, 2.17, 5507.55),
        Term(45.0, 0.39, 5223.69), Term(36.0, 0.47, 0.07),
        Term(29.0, 2.27, 11506.77), Term(23.0, 1.22, 11790.63),
        Term(23.0, 5.27, 77713.77), Term(21.0, 3.75, 775.52),
        Term(18.0, 0.84, 10977.08), Term(17.0, 3.82, 26.3),
        Term(14.0, 0.38, 5486.78), Term(13.0, 0.86, 2544.31),
        Term(13.0, 2.12, 5573.14), Term(13.0, 0.31, 6069.78),
        Term(12.0, 2.45, 1061.02), Term(10.0, 2.08, 20110.13),
        Term(10.0, 3.83, 2471.77), Term(10.0, 0.98, 2942.46),
        Term(10.0, 1.28, 796.3), Term(9.0, 3.29, 0.98),
        Term(9.0, 2.71, 4694.0), Term(9.0, 3.59, 1230.13),
        Term(8.0, 1.47, 5202.92), Term(8.0, 0.32, 8530.13)
    )

    private val L2_TERMS = arrayOf(
        Term(52919.0, 0.0, 0.0), Term(8720.0, 0.6844, 6283.0758),
        Term(309.0, 0.641, 12566.152), Term(27.0, 3.14, 0.0),
        Term(16.0, 3.62, 7860.42), Term(16.0, 0.89, 1577.34),
        Term(10.0, 0.6, 18849.23), Term(9.0, 2.42, 529.69),
        Term(7.0, 5.41, 3930.21), Term(5.0, 2.42, 5753.38),
        Term(4.0, 0.9, 5884.93), Term(4.0, 0.18, 5507.55),
        Term(3.0, 4.4, 5223.69), Term(3.0, 6.28, 0.07),
        Term(3.0, 0.82, 11506.77), Term(2.0, 3.23, 11790.63),
        Term(2.0, 3.29, 775.52), Term(1.0, 2.85, 10977.08),
        Term(1.0, 5.83, 26.3), Term(1.0, 2.39, 5486.78)
    )

    private val L3_TERMS = arrayOf(
        Term(289.0, 5.844, 6283.076), Term(35.0, 0.0, 0.0),
        Term(17.0, 5.49, 12566.15), Term(3.0, 2.38, 7860.42),
        Term(1.0, 2.6, 1577.34), Term(1.0, 2.5, 18849.2),
        Term(1.0, 1.1, 529.7)
    )

    private val L4_TERMS = arrayOf(
        Term(114.0, 3.142, 0.0), Term(8.0, 4.55, 6283.08),
        Term(1.0, 4.16, 12566.15)
    )

    private val L5_TERMS = arrayOf(
        Term(1.0, 1.88, 6283.08)
    )

    private val B0_TERMS = arrayOf(
        Term(280.0, 3.199, 84334.662), Term(102.0, 5.422, 5507.553),
        Term(80.0, 3.88, 5223.69), Term(44.0, 3.7, 23528.66),
        Term(32.0, 4.0, 1577.34), Term(28.0, 0.47, 10977.08),
        Term(25.0, 2.51, 775.52), Term(24.0, 0.35, 15773.44),
        Term(21.0, 0.28, 207.77), Term(21.0, 4.82, 11506.77),
        Term(20.0, 0.78, 11790.63), Term(17.0, 0.02, 2678.85),
        Term(13.0, 0.86, 2281.34), Term(13.0, 5.98, 20110.13),
        Term(12.0, 2.4, 1061.02), Term(10.0, 0.17, 7215.38),
        Term(10.0, 0.1, 8530.13), Term(10.0, 3.92, 14018.1),
        Term(9.0, 5.07, 1230.13)
    )

    private val B1_TERMS = arrayOf(
        Term(9.0, 3.9, 5507.55), Term(6.0, 2.35, 5223.69),
        Term(4.0, 2.18, 23528.66), Term(3.0, 2.47, 1577.34),
        Term(2.0, 5.17, 10977.08), Term(2.0, 0.98, 775.52),
        Term(2.0, 5.06, 15773.44), Term(1.0, 4.99, 207.77)
    )

    private val R0_TERMS = arrayOf(
        Term(100013989.0, 0.0, 0.0), Term(1670700.0, 3.0984635, 6283.07585),
        Term(13956.0, 3.05525, 12566.1517), Term(3084.0, 5.1985, 77713.7715),
        Term(1628.0, 1.1739, 5753.3849), Term(1576.0, 2.8469, 7860.4194),
        Term(925.0, 5.453, 11506.77), Term(542.0, 4.564, 3930.21),
        Term(472.0, 3.661, 529.69), Term(346.0, 0.964, 5884.93),
        Term(329.0, 5.9, 5507.55), Term(307.0, 0.209, 5223.69),
        Term(243.0, 4.273, 5573.14), Term(212.0, 5.847, 11790.63),
        Term(186.0, 5.022, 775.52), Term(175.0, 3.012, 18849.23),
        Term(110.0, 5.055, 20110.13), Term(98.0, 0.89, 20.78),
        Term(86.0, 5.69, 10977.08), Term(86.0, 1.27, 1577.34),
        Term(65.0, 0.27, 5486.78), Term(63.0, 0.92, 6069.78),
        Term(57.0, 2.01, 0.07), Term(56.0, 5.24, 2544.31),
        Term(49.0, 3.25, 1061.02), Term(47.0, 2.58, 2471.77),
        Term(45.0, 5.54, 2942.46), Term(43.0, 6.01, 1230.13),
        Term(42.0, 1.85, 796.3), Term(40.0, 3.83, 0.98),
        Term(37.0, 1.48, 4694.0), Term(37.0, 5.24, 8530.13),
        Term(36.0, 4.07, 5202.92), Term(36.0, 2.42, 2415.54),
        Term(33.0, 0.2, 12028.18), Term(30.0, 3.83, 11210.8),
        Term(29.0, 3.1, 7575.22), Term(29.0, 0.3, 1113.88),
        Term(26.0, 1.13, 117.81), Term(25.0, 1.56, 1231.11)
    )

    private val R1_TERMS = arrayOf(
        Term(103019.0, 1.10749, 6283.07585), Term(1721.0, 1.0644, 12566.1517),
        Term(702.0, 3.1416, 0.0), Term(32.0, 1.02, 18849.23),
        Term(31.0, 2.84, 5753.38), Term(25.0, 1.32, 7860.42),
        Term(18.0, 5.0, 529.69), Term(10.0, 1.42, 3930.21),
        Term(9.0, 5.91, 5507.55), Term(9.0, 1.42, 5884.93)
    )

    private val R2_TERMS = arrayOf(
        Term(4359.0, 5.7846, 6283.0758), Term(124.0, 5.579, 12566.152),
        Term(12.0, 3.14, 0.0), Term(9.0, 3.63, 77713.77),
        Term(6.0, 1.87, 5573.14), Term(3.0, 5.47, 18849.23)
    )

    private val R3_TERMS = arrayOf(
        Term(145.0, 4.223, 6283.076), Term(7.0, 4.0, 12566.15)
    )

    private val R4_TERMS = arrayOf(
        Term(4.0, 2.68, 6283.08)
    )

    private val NUTATION_TERMS = arrayOf(
        NutationTerm(0, 0, 0, 0, 1, -171996.0, -174.2, 92025.0, 8.9),
        NutationTerm(-2, 0, 0, 2, 2, -13187.0, -1.6, 5736.0, -3.1),
        NutationTerm(0, 0, 0, 2, 2, -2274.0, -0.2, 977.0, -0.5),
        NutationTerm(0, 0, 0, 0, 2, 2062.0, 0.2, -895.0, 0.5),
        NutationTerm(0, 1, 0, 0, 0, 1426.0, -3.4, 54.0, -0.1),
        NutationTerm(0, 0, 1, 0, 0, 712.0, 0.1, -7.0, 0.0),
        NutationTerm(-2, 1, 0, 2, 2, -517.0, 1.2, 224.0, -0.6),
        NutationTerm(0, 0, 0, 2, 1, -386.0, -0.4, 200.0, 0.0),
        NutationTerm(0, 0, 1, 2, 2, -301.0, 0.0, 129.0, 0.0),
        NutationTerm(-2, -1, 0, 2, 2, 217.0, -0.5, -95.0, 0.3),
        NutationTerm(-2, 0, 1, 0, 0, -158.0, 0.0, -1.0, 0.0),
        NutationTerm(-2, 0, 0, 2, 1, 129.0, 0.1, -70.0, 0.0),
        NutationTerm(0, 0, -1, 2, 2, 123.0, 0.0, -53.0, 0.0),
        NutationTerm(2, 0, 0, 0, 0, 63.0, 0.0, -2.0, 0.0),
        NutationTerm(0, 0, 1, 0, 1, 63.0, 0.1, -33.0, 0.0),
        NutationTerm(2, 0, -1, 2, 2, -59.0, 0.0, 26.0, 0.0),
        NutationTerm(0, 0, -1, 0, 1, -58.0, -0.1, 32.0, 0.0),
        NutationTerm(0, 0, 1, 2, 1, -51.0, 0.0, 27.0, 0.0),
        NutationTerm(-2, 0, 2, 0, 0, 48.0, 0.0, 1.0, 0.0),
        NutationTerm(0, 0, -2, 2, 1, 46.0, 0.0, -24.0, 0.0),
        NutationTerm(2, 0, 0, 2, 2, -38.0, 0.0, 16.0, 0.0),
        NutationTerm(0, 0, 2, 2, 2, -31.0, 0.0, 13.0, 0.0),
        NutationTerm(0, 0, 2, 0, 0, 29.0, 0.0, -1.0, 0.0),
        NutationTerm(-2, 0, 1, 2, 2, 29.0, 0.0, -12.0, 0.0),
        NutationTerm(0, 0, 0, 2, 0, 26.0, 0.0, -1.0, 0.0),
        NutationTerm(-2, 0, 0, 2, 0, -22.0, 0.0, 0.0, 0.0),
        NutationTerm(0, 0, -1, 2, 1, 21.0, 0.0, -10.0, 0.0),
        NutationTerm(0, 2, 0, 0, 0, 17.0, -0.1, 0.0, 0.0),
        NutationTerm(2, 0, -1, 0, 1, 16.0, 0.0, -8.0, 0.0),
        NutationTerm(-2, 2, 0, 2, 2, -16.0, 0.1, 7.0, 0.0),
        NutationTerm(0, 1, 0, 0, 1, -15.0, 0.0, 9.0, 0.0),
        NutationTerm(-2, 0, 1, 0, 1, -13.0, 0.0, 7.0, 0.0),
        NutationTerm(0, -1, 0, 0, 1, -12.0, 0.0, 6.0, 0.0),
        NutationTerm(0, 0, 2, -2, 0, 11.0, 0.0, 0.0, 0.0),
        NutationTerm(2, 0, -1, 2, 1, -10.0, 0.0, 5.0, 0.0),
        NutationTerm(2, 0, 1, 2, 2, -8.0, 0.0, 3.0, 0.0),
        NutationTerm(0, 1, 0, 2, 2, 7.0, 0.0, -3.0, 0.0),
        NutationTerm(-2, 1, 1, 0, 0, -7.0, 0.0, 0.0, 0.0),
        NutationTerm(0, -1, 0, 2, 2, -7.0, 0.0, 3.0, 0.0),
        NutationTerm(2, 0, 0, 2, 1, -7.0, 0.0, 3.0, 0.0),
        NutationTerm(2, 0, 1, 0, 0, 6.0, 0.0, 0.0, 0.0),
        NutationTerm(-2, 0, 2, 2, 2, 6.0, 0.0, -3.0, 0.0),
        NutationTerm(-2, 0, 1, 2, 1, 6.0, 0.0, -3.0, 0.0),
        NutationTerm(2, 0, -2, 2, 2, -6.0, 0.0, 3.0, 0.0),
        NutationTerm(2, 0, 0, 0, 1, -6.0, 0.0, 3.0, 0.0),
        NutationTerm(0, -1, 1, 0, 0, 5.0, 0.0, 0.0, 0.0),
        NutationTerm(-2, -1, 0, 2, 1, -5.0, 0.0, 3.0, 0.0),
        NutationTerm(-2, 0, 0, 0, 1, -5.0, 0.0, 3.0, 0.0),
        NutationTerm(0, 0, 2, 2, 1, -5.0, 0.0, 3.0, 0.0),
        NutationTerm(-2, 0, 2, 0, 1, 4.0, 0.0, 0.0, 0.0),
        NutationTerm(-2, 1, 0, 2, 1, 4.0, 0.0, -2.0, 0.0),
        NutationTerm(0, 0, 1, -2, 0, 4.0, 0.0, 0.0, 0.0),
        NutationTerm(-1, 0, 1, 0, 0, -4.0, 0.0, 0.0, 0.0),
        NutationTerm(-2, 1, 0, 0, 0, -4.0, 0.0, 0.0, 0.0),
        NutationTerm(1, 0, 0, 0, 0, -3.0, 0.0, 0.0, 0.0),
        NutationTerm(0, 0, 1, 2, 0, 3.0, 0.0, 0.0, 0.0),
        NutationTerm(0, 0, -2, 2, 2, -3.0, 0.0, 1.0, 0.0),
        NutationTerm(-1, -1, 1, 0, 0, -3.0, 0.0, 0.0, 0.0),
        NutationTerm(0, 1, 1, 0, 0, -3.0, 0.0, 0.0, 0.0),
        NutationTerm(0, -1, 1, 2, 2, -3.0, 0.0, 1.0, 0.0),
        NutationTerm(2, -1, 0, 0, 0, -3.0, 0.0, 0.0, 0.0),
        NutationTerm(2, 0, -1, 0, 0, 3.0, 0.0, 0.0, 0.0),
        NutationTerm(0, 1, -1, 2, 2, -3.0, 0.0, 1.0, 0.0)
    )
}
