package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonResourceTest {

    @Test
    fun testParseTwoColumnCsv() {
        val sampleCsv = """
            # Header line
            380;0.1
            381,0.2
            382, 0.3
        """.trimIndent()

        val data = parseTwoColumnCsv(sampleCsv)
        assertEquals(3, data.x.size)
        assertEquals(380.0, data.x[0])
        assertEquals(0.1, data.y[0])
        assertEquals(381.0, data.x[1])
        assertEquals(0.2, data.y[1])
    }
}
