package com.radiometry.toolbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentToolsTest {

    @Test
    fun testRecordAndRetrieveRecentTools() {
        val settings = InMemorySettingsManager()

        assertTrue(getRecentToolNames(settings).isEmpty())

        recordToolUsed("Solar Position", settings)
        assertEquals(listOf("Solar Position"), getRecentToolNames(settings))

        recordToolUsed("Sun Events", settings)
        assertEquals(listOf("Sun Events", "Solar Position"), getRecentToolNames(settings))

        recordToolUsed("Solar Position", settings)
        assertEquals(listOf("Solar Position", "Sun Events"), getRecentToolNames(settings))

        recordToolUsed("Blackbody Radiance", settings)
        recordToolUsed("Source Irradiance", settings)

        val recent = getRecentToolNames(settings)
        assertEquals(MAX_RECENT_TOOLS_COUNT, recent.size)
        assertEquals("Source Irradiance", recent[0])
        assertEquals("Blackbody Radiance", recent[1])
        assertEquals("Solar Position", recent[2])
    }
}
