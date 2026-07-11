package de.norm.events.scraper.lido

import de.norm.events.scraper.ScrapedArtist
import de.norm.events.scraper.ScrapedEvent
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [LidoOverviewPageScraper].
 *
 * Uses a static HTML fixture with a representative mix of `article.event-ticket`
 * blocks: a concert with a support act, a party, a cancelled concert whose
 * subtitle appends a note after the support line, a "Public Viewing" with no
 * mappable type, and a sold-out concert.
 */
class LidoOverviewPageScraperTest {
    private val scraper = LidoOverviewPageScraper()
    private val baseUrl = "https://www.lido-berlin.de/"
    private lateinit var events: List<ScrapedEvent>

    @BeforeEach
    fun setUp() {
        val html =
            javaClass.classLoader
                .getResourceAsStream("scraper/lido/lido-overview.html")!!
                .bufferedReader()
                .readText()
        events = scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl)
    }

    private fun event(titleFragment: String): ScrapedEvent = events.first { it.title.contains(titleFragment, ignoreCase = true) }

    @Test
    fun `extracts all articles from the fixture`() {
        events shouldHaveSize 5
    }

    @Test
    fun `parses date, times, type and resolves the detail URL for a concert`() {
        val sorry = event("Sorry")
        sorry.eventType shouldBe "CONCERT"
        sorry.eventDate shouldBe LocalDate.of(2026, 6, 15)
        sorry.doorsTime shouldBe LocalTime.of(19, 0)
        sorry.startTime shouldBe LocalTime.of(20, 0)
        sorry.sourceUrl shouldBe "https://www.lido-berlin.de/events/2026-06-15-sorry"
        sorry.sourceId shouldBe "lido:2026-06-15-sorry"
    }

    @Test
    fun `extracts the headliner and support act for a concert`() {
        event("Sorry").artists shouldContainExactly
            listOf(
                ScrapedArtist("SORRY", "HEADLINER"),
                ScrapedArtist("SNAKE ORANGE CAKE", "SUPPORT")
            )
    }

    @Test
    fun `parses the presenter as a promoter`() {
        event("Sorry").promoters shouldContainExactly listOf("Puschen")
    }

    @Test
    fun `does not extract artists for a party`() {
        val party = event("Dynamit")
        party.eventType shouldBe "PARTY"
        party.artists.shouldBeEmpty()
    }

    @Test
    fun `flags a cancelled event and excludes the appended note from the support act`() {
        // The subtitle is "+ Support: JEFF CLARKE <br><br> ABGESAGT. …note…".
        // Only the support line must feed artist extraction — the note must not become an artist.
        val pangea = event("Together Pangea")
        pangea.status shouldBe "CANCELLED"
        pangea.artists shouldContainExactly
            listOf(
                ScrapedArtist("TOGETHER PANGEA", "HEADLINER"),
                ScrapedArtist("JEFF CLARKE", "SUPPORT")
            )
    }

    @Test
    fun `types a public-viewing screening as SCREENING and extracts no artists`() {
        val publicViewing = event("WM 2026")
        // Lido's "Public Viewing" category maps to SCREENING, so the football screening
        // is never treated as a concert and no headliner is minted from its title.
        publicViewing.eventType shouldBe "SCREENING"
        publicViewing.artists.shouldBeEmpty()
    }

    @Test
    fun `flags a sold-out event without changing its status`() {
        val soldOut = event("Public Enemy")
        soldOut.soldOut shouldBe true
        soldOut.status shouldBe "SCHEDULED"
    }
}
