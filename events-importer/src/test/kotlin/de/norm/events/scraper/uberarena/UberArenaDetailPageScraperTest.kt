package de.norm.events.scraper.uberarena

import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalTime

/**
 * Unit tests for [UberArenaDetailPageScraper].
 *
 * Parses a static snapshot of an Uber Arena `/events/detail/<slug>/<YYYY-MM-DD-HHMM>` page for
 * deterministic, offline-safe testing without HTTP fetching.
 */
class UberArenaDetailPageScraperTest {
    private val scraper = UberArenaDetailPageScraper()
    private val sourceUrl = "https://www.uber-arena.de/events/detail/diljit-dosanjh/2026-08-21-2000"

    private val event: ScrapedEvent by lazy {
        val html =
            javaClass.classLoader
                .getResourceAsStream("scraper/uberarena/uberarena-detail-diljit-dosanjh.html")!!
                .bufferedReader()
                .readText()
        scraper.scrape(Jsoup.parse(html, sourceUrl), sourceUrl)!!
    }

    @Test
    fun `reads the doors time, the only place the venue states one`() {
        event.doorsTime shouldBe LocalTime.of(18, 30)
    }

    @Test
    fun `reads the prose description`() {
        event.description!! shouldContain "Punjabi"
    }

    @Test
    fun `reads the external ticket-shop link`() {
        event.ticketUrl!! shouldStartWith "https://queue-de.axs.com/"
    }

    @Test
    fun `derives the sourceId from the full slug including its date segment`() {
        event.sourceId shouldBe "uber_arena:diljit-dosanjh/2026-08-21-2000"
    }

    @Test
    fun `leaves the date to the listing`() {
        // The detail page renders no date this parser reads; the listing assembled it already.
        event.eventDate shouldBe UNRESOLVED_EVENT_DATE
    }

    @Test
    fun `keeps the venue-suffixed heading for the importer to replace`() {
        // "Diljit Dosanjh in der Uber Arena" — the listing's cleaner act name wins at the merge.
        event.title shouldContain "in der Uber Arena"
        event.artists.shouldBeEmpty()
    }

    @Test
    fun `states no price of its own`() {
        event.pricePresale.shouldBeNull()
        event.startTime.shouldBeNull()
    }

    @Test
    fun `returns null for a page without an event heading`() {
        val document = Jsoup.parse("<html><body><div id='content'></div></body></html>", sourceUrl)
        scraper.scrape(document, sourceUrl).shouldBeNull()
    }
}
