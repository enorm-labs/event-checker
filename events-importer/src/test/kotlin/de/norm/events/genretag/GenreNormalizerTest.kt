package de.norm.events.genretag

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test

/**
 * Tests for [normalizeGenre] — the genre string normalization pipeline.
 *
 * Verifies synonym mapping, delimiter splitting, noise stripping,
 * deduplication, and fallback behavior for unknown genres.
 */
class GenreNormalizerTest {
    @Test
    fun `null input returns empty list`() {
        normalizeGenre(null).shouldBeEmpty()
    }

    @Test
    fun `blank input returns empty list`() {
        normalizeGenre("").shouldBeEmpty()
        normalizeGenre("   ").shouldBeEmpty()
    }

    @Test
    fun `single known genre maps to canonical name`() {
        normalizeGenre("Hip Hop").shouldContainExactly("Hip Hop")
        normalizeGenre("hip-hop").shouldContainExactly("Hip Hop")
        normalizeGenre("Rap").shouldContainExactly("Hip Hop")
    }

    @Test
    fun `comma-separated genres are split and normalized`() {
        normalizeGenre("Pop Punk, Indie, Karaoke")
            .shouldContainExactlyInAnyOrder("Punk", "Indie", "Karaoke")
    }

    @Test
    fun `double-slash delimiter is handled`() {
        normalizeGenre("80s, 90s & 2000s Floor // Pop, Rock & Disco // Hip Hop & Urban Disco Floor // Karaoke Disco Floor")
            .shouldContainExactlyInAnyOrder("80s", "90s", "2000s", "Pop", "Rock", "Disco", "Hip Hop", "Karaoke")
    }

    @Test
    fun `ampersand delimiter splits correctly`() {
        normalizeGenre("80s, Disco & Hip Hop")
            .shouldContainExactlyInAnyOrder("80s", "Disco", "Hip Hop")
    }

    @Test
    fun `post-punk and gothic variants are normalized`() {
        normalizeGenre("Postpunk, Gothicrock, Darkwave, EBM und Synthpop etc.")
            .shouldContainExactlyInAnyOrder("Post-Punk", "Gothic Rock", "Darkwave", "EBM", "Synthpop")
    }

    @Test
    fun `alternative slash indie is normalized`() {
        normalizeGenre("Alternative / Indie")
            .shouldContainExactlyInAnyOrder("Alternative", "Indie")
    }

    @Test
    fun `indie variants map to Indie`() {
        normalizeGenre("Indie Pop").shouldContainExactly("Indie")
        normalizeGenre("Indie-Pop").shouldContainExactly("Indie")
        normalizeGenre("Indie Rock").shouldContainExactly("Indie")
    }

    @Test
    fun `jazz-fusion maps to Jazz`() {
        normalizeGenre("Jazz-Fusion").shouldContainExactly("Jazz")
    }

    @Test
    fun `reggae phrase is handled`() {
        val result = normalizeGenre("All kinds of Reggae - from then till now")
        result.shouldContainExactly("Reggae")
    }

    @Test
    fun `latin genres are normalized`() {
        normalizeGenre("Cumbia, Salsa, Latin Roots")
            .shouldContainExactly("Latin")
    }

    @Test
    fun `electronic variants are normalized`() {
        normalizeGenre("Elektro-Fusion, House,Techno")
            .shouldContainExactlyInAnyOrder("Electronic", "House", "Techno")
    }

    @Test
    fun `duplicates are removed`() {
        normalizeGenre("Indie, Indie Rock, Indie Pop")
            .shouldContainExactly("Indie")
    }

    @Test
    fun `unknown genre is kept with title case`() {
        normalizeGenre("Noise")
            .shouldContainExactly("Noise")
    }

    @Test
    fun `hip hop soul rnb oldschool newschool`() {
        normalizeGenre("Hip Hop, Soul, RnB, Oldschool, Newschool")
            .shouldContainExactlyInAnyOrder("Hip Hop", "Soul", "R&B", "Old School", "New School")
    }

    @Test
    fun `world music variants`() {
        normalizeGenre("World Music, Indian, Urdu Rock")
            .shouldContainExactly("World Music")
    }

    @Test
    fun `single pop returns Pop`() {
        normalizeGenre("Pop").shouldContainExactly("Pop")
    }

    @Test
    fun `single rock returns Rock`() {
        normalizeGenre("Rock").shouldContainExactly("Rock")
    }

    @Test
    fun `funk and boogaloo compound label extracts matching genres`() {
        normalizeGenre("Superheavy Funky Soul & Boogaloo")
            .shouldContainExactlyInAnyOrder("Soul", "Funk")
    }

    @Test
    fun `new wave post punk are distinct`() {
        normalizeGenre("New Wave, Post Punk")
            .shouldContainExactlyInAnyOrder("New Wave", "Post-Punk")
    }

    @Test
    fun `metal is preserved`() {
        normalizeGenre("Metal").shouldContainExactly("Metal")
    }

    @Test
    fun `folk indie comma separated`() {
        normalizeGenre("Folk, Indie")
            .shouldContainExactlyInAnyOrder("Folk", "Indie")
    }

    @Test
    fun `electronica jazz trip-hop`() {
        normalizeGenre("Electronica, Jazz, Trip-Hop")
            .shouldContainExactlyInAnyOrder("Electronic", "Jazz", "Trip-Hop")
    }

    @Test
    fun `arbitrary casing and spacing variants resolve via normalized lookup key`() {
        // These exact spellings are not listed individually — they collapse to
        // the same normalized key (e.g. "hiphop", "postpunk") as the canonical entry.
        normalizeGenre("HIP HOP").shouldContainExactly("Hip Hop")
        normalizeGenre("Hip   Hop").shouldContainExactly("Hip Hop")
        normalizeGenre("POSTPUNK").shouldContainExactly("Post-Punk")
        normalizeGenre("Gothic-Rock").shouldContainExactly("Gothic Rock")
        normalizeGenre("Singer Songwriter").shouldContainExactly("Singer-Songwriter")
    }

    @Test
    fun `punctuation and spacing variants fold into existing canonical tags`() {
        // Variants that previously fell through to as-is, creating duplicate tags
        // (e.g. "World" vs "World Music", "Singer Songwriter" vs "Singer-Songwriter").
        normalizeGenre("Electronic").shouldContainExactly("Electronic")
        normalizeGenre("Post-Punk").shouldContainExactly("Post-Punk")
        normalizeGenre("Singer Songwriter").shouldContainExactly("Singer-Songwriter")
        normalizeGenre("World").shouldContainExactly("World Music")
    }
}
