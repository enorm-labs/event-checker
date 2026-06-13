package de.norm.events.scraper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EventMappingExtensionsTest {
    // --- mapEventType ---

    @Test
    fun `mapEventType maps base German and English labels`() {
        mapEventType("Konzert") shouldBe "CONCERT"
        mapEventType("Concert") shouldBe "CONCERT"
        mapEventType("Festival") shouldBe "FESTIVAL"
        mapEventType("Party") shouldBe "PARTY"
        mapEventType("Sonstiges") shouldBe "OTHER"
    }

    @Test
    fun `mapEventType is case-insensitive and trims whitespace`() {
        mapEventType("KONZERT") shouldBe "CONCERT"
        mapEventType("  Konzert  ") shouldBe "CONCERT"
    }

    @Test
    fun `mapEventType returns null for null, blank and unknown labels`() {
        mapEventType(null).shouldBeNull()
        mapEventType("").shouldBeNull()
        mapEventType("   ").shouldBeNull()
        mapEventType("Workshop").shouldBeNull()
    }

    @Test
    fun `mapEventType prefers venue-specific synonyms over the base table`() {
        mapEventType("Live", mapOf("live" to "CONCERT")) shouldBe "CONCERT"
        // extra synonyms take precedence over the base mapping
        mapEventType("Party", mapOf("party" to "CLUB_NIGHT")) shouldBe "CLUB_NIGHT"
    }

    // --- extractSupportFromSubtitle ---

    @Test
    fun `extractSupportFromSubtitle returns empty when no support line`() {
        extractSupportFromSubtitle("Tour 2026").shouldBeEmpty()
        extractSupportFromSubtitle(null).shouldBeEmpty()
        extractSupportFromSubtitle("").shouldBeEmpty()
    }

    @Test
    fun `extractSupportFromSubtitle extracts a single support act`() {
        extractSupportFromSubtitle("Tour 2026 | Support: Luana") shouldContainExactly listOf("Luana")
    }

    @Test
    fun `extractSupportFromSubtitle splits multiple acts on common delimiters`() {
        extractSupportFromSubtitle("Tour + Support: High On Fire & Gnome, Aska") shouldContainExactly
            listOf("High On Fire", "Gnome", "Aska")
    }

    // --- isPlaceholderName ---

    @Test
    fun `isPlaceholderName returns true for TBA variants`() {
        isPlaceholderName("TBA") shouldBe true
        isPlaceholderName("tba") shouldBe true
        isPlaceholderName("TBA.") shouldBe true
        isPlaceholderName("T.B.A.") shouldBe true
    }

    @Test
    fun `isPlaceholderName returns true for TBD variants`() {
        isPlaceholderName("TBD") shouldBe true
        isPlaceholderName("tbd") shouldBe true
        isPlaceholderName("TBD.") shouldBe true
    }

    @Test
    fun `isPlaceholderName returns true for NN variants`() {
        isPlaceholderName("N.N.") shouldBe true
        isPlaceholderName("NN") shouldBe true
        isPlaceholderName("nn") shouldBe true
        isPlaceholderName("NN.") shouldBe true
    }

    @Test
    fun `isPlaceholderName trims whitespace`() {
        isPlaceholderName("  TBA  ") shouldBe true
    }

    @Test
    fun `isPlaceholderName returns false for real artist names`() {
        isPlaceholderName("Aska") shouldBe false
        isPlaceholderName("The Adicts") shouldBe false
        isPlaceholderName("DJ Shadow") shouldBe false
    }

    // --- buildArtistList ---

    @Test
    fun `buildArtistList returns empty when supportNames is empty`() {
        buildArtistList("Headliner", emptyList()).shouldBeEmpty()
    }

    @Test
    fun `buildArtistList returns headliner and supports`() {
        val result = buildArtistList("The Adicts", listOf("Maid of Ace", "Kaos"))
        result shouldHaveSize 3
        result[0] shouldBe ScrapedArtist(name = "The Adicts", role = "HEADLINER")
        result[1] shouldBe ScrapedArtist(name = "Maid of Ace", role = "SUPPORT")
        result[2] shouldBe ScrapedArtist(name = "Kaos", role = "SUPPORT")
    }

    @Test
    fun `buildArtistList excludes placeholder headliner`() {
        val result = buildArtistList("TBA", listOf("Support Act"))
        result shouldHaveSize 1
        result[0] shouldBe ScrapedArtist(name = "Support Act", role = "SUPPORT")
    }

    @Test
    fun `buildArtistList excludes placeholder support names`() {
        val result = buildArtistList("The Adicts", listOf("TBA", "Maid of Ace"))
        result shouldHaveSize 2
        result[0] shouldBe ScrapedArtist(name = "The Adicts", role = "HEADLINER")
        result[1] shouldBe ScrapedArtist(name = "Maid of Ace", role = "SUPPORT")
    }

    @Test
    fun `buildArtistList with all placeholder supports returns only headliner`() {
        val result = buildArtistList("The Adicts", listOf("TBA", "TBD"))
        result shouldHaveSize 1
        result[0] shouldBe ScrapedArtist(name = "The Adicts", role = "HEADLINER")
    }
}
