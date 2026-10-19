package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CoordinateParserTest {

    @Test
    fun testParseDecimalCoordinates() {
        val c1 = parseCoordinates("59.3293, 18.0686")
        assertNotNull(c1)
        assertEquals(59.3293, c1.latitude, 1e-4)
        assertEquals(18.0686, c1.longitude, 1e-4)

        val c2 = parseCoordinates("39.742476, -105.1786")
        assertNotNull(c2)
        assertEquals(39.742476, c2.latitude, 1e-4)
        assertEquals(-105.1786, c2.longitude, 1e-4)

        val c3 = parseCoordinates("Dropped Pin\nhttps://maps.google.com/?q=59.3293,18.0686")
        assertNotNull(c3)
        assertEquals(59.3293, c3.latitude, 1e-4)
        assertEquals(18.0686, c3.longitude, 1e-4)
    }

    @Test
    fun testParseInvalidCoordinates() {
        assertNull(parseCoordinates("invalid text"))
        assertNull(parseCoordinates("999.0, 18.0686"))
    }
}
