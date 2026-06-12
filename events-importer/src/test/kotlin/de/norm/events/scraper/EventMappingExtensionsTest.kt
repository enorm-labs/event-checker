package de.norm.events.scraper

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EventMappingExtensionsTest {
    // --- mapGermanCategory ---

    @Test
    fun `mapGermanCategory maps Konzert to CONCERT`() {
        mapGermanCategory("Konzert") shouldBe "CONCERT"
    }

    @Test
    fun `mapGermanCategory maps Party to PARTY`() {
        mapGermanCategory("Party") shouldBe "PARTY"
    }

    @Test
    fun `mapGermanCategory maps Sonstiges to OTHER`() {
        mapGermanCategory("Sonstiges") shouldBe "OTHER"
    }

    @Test
    fun `mapGermanCategory is case-insensitive`() {
        mapGermanCategory("KONZERT") shouldBe "CONCERT"
        mapGermanCategory("party") shouldBe "PARTY"
        mapGermanCategory("PARTY") shouldBe "PARTY"
    }

    @Test
    fun `mapGermanCategory trims whitespace`() {
        mapGermanCategory("  Konzert  ") shouldBe "CONCERT"
    }

    @Test
    fun `mapGermanCategory returns OTHER for null`() {
        mapGermanCategory(null) shouldBe "OTHER"
    }

    @Test
    fun `mapGermanCategory returns OTHER for empty string`() {
        mapGermanCategory("") shouldBe "OTHER"
    }

    @Test
    fun `mapGermanCategory returns OTHER for unknown categories`() {
        mapGermanCategory("Workshop") shouldBe "OTHER"
        mapGermanCategory("Lesung") shouldBe "OTHER"
        mapGermanCategory("Theater") shouldBe "OTHER"
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
