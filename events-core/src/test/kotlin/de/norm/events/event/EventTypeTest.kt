package de.norm.events.event

import kotlin.test.Test
import kotlin.test.assertEquals

class EventTypeTest {
    @Test
    fun `parseOrDefault returns CONCERT for exact match`() {
        assertEquals(EventType.CONCERT, EventType.parseOrDefault("CONCERT"))
    }

    @Test
    fun `parseOrDefault is case-insensitive`() {
        assertEquals(EventType.CONCERT, EventType.parseOrDefault("concert"))
        assertEquals(EventType.FESTIVAL, EventType.parseOrDefault("Festival"))
        assertEquals(EventType.PARTY, EventType.parseOrDefault("pArTy"))
    }

    @Test
    fun `parseOrDefault trims whitespace`() {
        assertEquals(EventType.QUIZ, EventType.parseOrDefault("  QUIZ  "))
        assertEquals(EventType.CLUB_NIGHT, EventType.parseOrDefault("\tCLUB_NIGHT\n"))
    }

    @Test
    fun `parseOrDefault returns all valid enum values`() {
        assertEquals(EventType.CONCERT, EventType.parseOrDefault("CONCERT"))
        assertEquals(EventType.FESTIVAL, EventType.parseOrDefault("FESTIVAL"))
        assertEquals(EventType.PARTY, EventType.parseOrDefault("PARTY"))
        assertEquals(EventType.QUIZ, EventType.parseOrDefault("QUIZ"))
        assertEquals(EventType.CLUB_NIGHT, EventType.parseOrDefault("CLUB_NIGHT"))
        assertEquals(EventType.SHOW, EventType.parseOrDefault("SHOW"))
        assertEquals(EventType.OTHER, EventType.parseOrDefault("OTHER"))
    }

    @Test
    fun `parseOrDefault returns OTHER for unknown values`() {
        assertEquals(EventType.OTHER, EventType.parseOrDefault("UNKNOWN"))
        assertEquals(EventType.OTHER, EventType.parseOrDefault("rave"))
        assertEquals(EventType.OTHER, EventType.parseOrDefault(""))
    }
}
