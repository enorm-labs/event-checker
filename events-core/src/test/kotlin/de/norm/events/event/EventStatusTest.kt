package de.norm.events.event

import kotlin.test.Test
import kotlin.test.assertEquals

class EventStatusTest {
    @Test
    fun `parseOrDefault returns SCHEDULED for exact match`() {
        assertEquals(EventStatus.SCHEDULED, EventStatus.parseOrDefault("SCHEDULED"))
    }

    @Test
    fun `parseOrDefault is case-insensitive`() {
        assertEquals(EventStatus.SCHEDULED, EventStatus.parseOrDefault("scheduled"))
        assertEquals(EventStatus.RELOCATED, EventStatus.parseOrDefault("Relocated"))
        assertEquals(EventStatus.CANCELLED, EventStatus.parseOrDefault("cAnCeLlEd"))
    }

    @Test
    fun `parseOrDefault trims whitespace`() {
        assertEquals(EventStatus.POSTPONED, EventStatus.parseOrDefault("  POSTPONED  "))
        assertEquals(EventStatus.CANCELLED, EventStatus.parseOrDefault("\tCANCELLED\n"))
    }

    @Test
    fun `parseOrDefault returns all valid enum values`() {
        assertEquals(EventStatus.SCHEDULED, EventStatus.parseOrDefault("SCHEDULED"))
        assertEquals(EventStatus.RELOCATED, EventStatus.parseOrDefault("RELOCATED"))
        assertEquals(EventStatus.CANCELLED, EventStatus.parseOrDefault("CANCELLED"))
        assertEquals(EventStatus.POSTPONED, EventStatus.parseOrDefault("POSTPONED"))
    }

    @Test
    fun `parseOrDefault returns SCHEDULED for unknown values`() {
        assertEquals(EventStatus.SCHEDULED, EventStatus.parseOrDefault("UNKNOWN"))
        assertEquals(EventStatus.SCHEDULED, EventStatus.parseOrDefault("verlegt"))
        assertEquals(EventStatus.SCHEDULED, EventStatus.parseOrDefault(""))
    }
}
