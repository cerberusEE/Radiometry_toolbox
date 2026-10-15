package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IfovGsdCalculatorTest {

    @Test
    fun testIfovCalculationFromFocalLengthAndPitch() {
        val focalLengthMm = 50.0 // 50 mm
        val pixelPitchUm = 10.0  // 10 µm

        val ifovRad = calculateIfovInRadians(focalLengthMm, pixelPitchUm)
        assertNotNull(ifovRad)

        // f = 0.05 m, p = 0.00001 m
        // IFOV = 0.00001 / 0.05 = 0.0002 rad = 0.2 mrad
        assertEquals(0.0002, ifovRad, 1e-9)

        val ifovMrad = convertIfovFromRadians(ifovRad, IfovUnit.MILLIRADIANS)
        assertNotNull(ifovMrad)
        assertEquals(0.2, ifovMrad, 1e-6)

        val ifovUrad = convertIfovFromRadians(ifovRad, IfovUnit.MICRORADIANS)
        assertNotNull(ifovUrad)
        assertEquals(200.0, ifovUrad, 1e-4)
    }

    @Test
    fun testPixelPitchCalculationFromIfovAndFocalLength() {
        val ifovRad = 0.0002 // 0.2 mrad
        val focalLengthMm = 50.0

        val pitchUm = calculatePixelPitchUm(ifovRad, focalLengthMm)
        assertNotNull(pitchUm)
        assertEquals(10.0, pitchUm, 1e-6)
    }

    @Test
    fun testGsdCalculationFromIfovAndDistance() {
        val ifovRad = 0.0002
        val distanceMeters = 100.0

        val gsdMeters = calculateGsdInMeters(ifovRad, distanceMeters)
        assertNotNull(gsdMeters)
        assertEquals(0.02, gsdMeters, 1e-8) // 2 cm

        val areaSqM = calculatePixelFootprintArea(gsdMeters)
        assertNotNull(areaSqM)
        assertEquals(0.0004, areaSqM, 1e-10)
    }

    @Test
    fun testDirectIfovInputAndGsd() {
        // Direct input into IFOV field when pitch is unknown
        val directIfovMrad = 0.5 // 0.5 mrad
        val ifovRad = convertIfovToRadians(directIfovMrad, IfovUnit.MILLIRADIANS)
        assertNotNull(ifovRad)
        assertEquals(0.0005, ifovRad, 1e-9)

        val distanceMeters = 1000.0 // 1000 m range
        val gsdMeters = calculateGsdInMeters(ifovRad, distanceMeters)
        assertNotNull(gsdMeters)
        assertEquals(0.5, gsdMeters, 1e-6) // 0.5 m GSD
    }

    @Test
    fun testIfovUnitConversions() {
        val mradVal = 1.0
        val radFromMrad = convertIfovToRadians(mradVal, IfovUnit.MILLIRADIANS)
        assertNotNull(radFromMrad)
        assertEquals(0.001, radFromMrad, 1e-9)

        val uradVal = 1000.0
        val radFromUrad = convertIfovToRadians(uradVal, IfovUnit.MICRORADIANS)
        assertNotNull(radFromUrad)
        assertEquals(0.001, radFromUrad, 1e-9)

        val degVal = 1.0
        val radFromDeg = convertIfovToRadians(degVal, IfovUnit.DEGREES)
        assertNotNull(radFromDeg)
        assertEquals(kotlin.math.PI / 180.0, radFromDeg, 1e-9)

        // Roundtrip test
        val backToDeg = convertIfovFromRadians(radFromDeg, IfovUnit.DEGREES)
        assertNotNull(backToDeg)
        assertEquals(1.0, backToDeg, 1e-9)
    }

    @Test
    fun testFormattingFunctions() {
        val ifovRad = 0.0002 // 0.2 mrad
        assertEquals("0.20 mrad", formatIfov(ifovRad, IfovUnit.MILLIRADIANS))
        assertEquals("200 µrad", formatIfov(ifovRad, IfovUnit.MICRORADIANS))

        assertEquals("2.50 m", formatGsd(2.5))
        assertEquals("2 cm (2.0e-2 m)", formatGsd(0.02))
        assertEquals("5 mm (5.0e-3 m)", formatGsd(0.005))

        assertEquals("1 m²", formatFootprintArea(1.0))
        assertEquals("4 cm² (4.0e-4 m²)", formatFootprintArea(0.0004))
    }

    @Test
    fun testInvalidAndBoundaryInputs() {
        assertNull(calculateIfovInRadians(0.0, 10.0))
        assertNull(calculateIfovInRadians(-50.0, 10.0))
        assertNull(calculateIfovInRadians(50.0, 0.0))
        assertNull(calculateIfovInRadians(Double.NaN, 10.0))

        assertNull(calculatePixelPitchUm(0.0, 50.0))
        assertNull(calculateGsdInMeters(0.0, 100.0))
        assertNull(calculateGsdInMeters(0.0002, -100.0))

        assertEquals("Invalid Input", formatGsd(-1.0))
        assertEquals("Invalid Input", formatGsd(Double.NaN))
    }
}
