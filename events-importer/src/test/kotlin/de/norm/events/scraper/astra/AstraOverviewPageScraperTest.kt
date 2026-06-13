package de.norm.events.scraper.astra

import de.norm.events.scraper.ScrapedArtist
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit tests for [AstraOverviewPageScraper].
 *
 * Uses a static HTML fixture with a representative mix of articles: a dateless
 * festival teaser, a plain concert, an empty-kind concert with support acts,
 * a sold-out event, and a three-day festival whose last day Astra mislabeled
 * as a concert.
 */
class AstraOverviewPageScraperTest {
    private val scraper = AstraOverviewPageScraper()
    private val baseUrl = "https://www.astra-berlin.de/"
    private lateinit var events: List<ScrapedEvent>

    @BeforeEach
    fun setUp() {
        val html =
            javaClass.classLoader
                .getResourceAsStream("scraper/astra/astra-overview.html")!!
                .bufferedReader()
                .readText()
        events = scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl)
    }

    private fun event(titleFragment: String): ScrapedEvent = events.first { it.title.contains(titleFragment, ignoreCase = true) }

    @Test
    fun `extracts all articles from the fixture`() {
        events shouldHaveSize 7
    }

    @Test
    fun `parses date, type and resolves the detail URL for a concert`() {
        val angus = event("Angus")
        angus.eventType shouldBe "CONCERT"
        angus.eventDate shouldBe LocalDate.of(2026, 7, 8)
        angus.sourceUrl shouldBe "https://www.astra-berlin.de/events/2026-07-08-angus---julia-stone"
        angus.sourceId shouldBe "astra:2026-07-08-angus---julia-stone"
    }

    @Test
    fun `adds the headliner for a confirmed concert even without support acts`() {
        event("Angus").artists shouldContainExactly listOf(ScrapedArtist("ANGUS & JULIA STONE", "HEADLINER"))
    }

    @Test
    fun `extracts headliner and support acts from the subtitle`() {
        // Green Lung has no kind label, but the "Support:" line confirms the
        // title is the headliner.
        event("Green Lung").artists shouldContainExactly
            listOf(
                ScrapedArtist("GREEN LUNG", "HEADLINER"),
                ScrapedArtist("HIGH ON FIRE", "SUPPORT"),
                ScrapedArtist("GNOME", "SUPPORT")
            )
    }

    @Test
    fun `does not extract artists for a festival`() {
        val festival = event("Berlin Breakout")
        festival.eventType shouldBe "FESTIVAL"
        festival.artists.shouldBeEmpty()
    }

    @Test
    fun `uses the unresolved-date sentinel for the dateless teaser`() {
        // The featured teaser has no date in its markup; the detail page fills it in later.
        event("Berlin Breakout").eventDate shouldBe UNRESOLVED_EVENT_DATE
    }

    @Test
    fun `corrects a festival day that Astra mislabeled as a concert`() {
        // The three "OUT OF LINE WEEKENDER 2027" days share one title; Astra tags
        // Day 1/2 as Festival but Day 3 as Concert. Day 3 should be corrected to
        // FESTIVAL via its festival siblings, and its title-as-headliner dropped.
        val days = events.filter { it.title == "OUT OF LINE WEEKENDER 2027" }
        days shouldHaveSize 3
        days.map { it.eventType }.toSet() shouldBe setOf("FESTIVAL")
        days.forEach { it.artists.shouldBeEmpty() }
    }

    @Test
    fun `flags a sold-out event`() {
        val chapo = event("Chapo")
        chapo.soldOut shouldBe true
        chapo.status shouldBe "SCHEDULED"
    }
}
