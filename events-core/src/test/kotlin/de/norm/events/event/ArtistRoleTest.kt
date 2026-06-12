package de.norm.events.event

import kotlin.test.Test
import kotlin.test.assertEquals

class ArtistRoleTest {
    @Test
    fun `parseOrDefault returns HEADLINER for exact match`() {
        assertEquals(ArtistRole.HEADLINER, ArtistRole.parseOrDefault("HEADLINER"))
    }

    @Test
    fun `parseOrDefault is case-insensitive`() {
        assertEquals(ArtistRole.HEADLINER, ArtistRole.parseOrDefault("headliner"))
        assertEquals(ArtistRole.SUPPORT, ArtistRole.parseOrDefault("Support"))
        assertEquals(ArtistRole.DJ, ArtistRole.parseOrDefault("dj"))
    }

    @Test
    fun `parseOrDefault trims whitespace`() {
        assertEquals(ArtistRole.SUPPORT, ArtistRole.parseOrDefault("  SUPPORT  "))
        assertEquals(ArtistRole.DJ, ArtistRole.parseOrDefault("\tDJ\n"))
    }

    @Test
    fun `parseOrDefault returns all valid enum values`() {
        assertEquals(ArtistRole.HEADLINER, ArtistRole.parseOrDefault("HEADLINER"))
        assertEquals(ArtistRole.SUPPORT, ArtistRole.parseOrDefault("SUPPORT"))
        assertEquals(ArtistRole.DJ, ArtistRole.parseOrDefault("DJ"))
    }

    @Test
    fun `parseOrDefault returns HEADLINER for unknown values`() {
        assertEquals(ArtistRole.HEADLINER, ArtistRole.parseOrDefault("UNKNOWN"))
        assertEquals(ArtistRole.HEADLINER, ArtistRole.parseOrDefault("vocalist"))
        assertEquals(ArtistRole.HEADLINER, ArtistRole.parseOrDefault(""))
    }
}
