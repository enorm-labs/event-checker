package de.norm.events.genretag

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GenreFamilyTest {
    @Test
    fun `fromSlug resolves every family by its slug`() {
        GenreFamily.entries.forEach { family -> GenreFamily.fromSlug(family.slug) shouldBe family }
    }

    @Test
    fun `fromSlug is null for an unknown or absent slug`() {
        GenreFamily.fromSlug("polka").shouldBeNull()
        GenreFamily.fromSlug(null).shouldBeNull()
    }

    @Test
    fun `slugs are unique and the frontend's order is the declaration order`() {
        // The frontend keeps the same list in `lib/genreFamilies.ts`; this is the side to change first.
        GenreFamily.entries.map { it.slug }.shouldContainExactly(
            "electronic",
            "hip-hop",
            "pop",
            "rock",
            "punk",
            "metal",
            "wave",
            "soul-funk",
            "jazz-blues",
            "folk",
            "latin-world",
            "classical",
            "charts"
        )
    }
}
