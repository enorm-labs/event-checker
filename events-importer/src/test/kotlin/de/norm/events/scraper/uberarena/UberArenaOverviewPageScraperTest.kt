package de.norm.events.scraper.uberarena

import de.norm.events.event.EventType
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [UberArenaOverviewPageScraper].
 *
 * Parses a static snapshot of Uber Arena's `/events/all` page for deterministic, offline-safe
 * testing without HTTP fetching. The fixture keeps **all 128 rows**, sport included, so the
 * category filter is exercised on real data.
 */
class UberArenaOverviewPageScraperTest {
    private val scraper = UberArenaOverviewPageScraper()
    private val baseUrl = "https://www.uber-arena.de/events/all"

    private val events: List<ScrapedEvent> by lazy {
        val html =
            javaClass.classLoader
                .getResourceAsStream("scraper/uberarena/uberarena-overview.html")!!
                .bufferedReader()
                .readText()
        scraper.scrape(Jsoup.parse(html, baseUrl), baseUrl)
    }

    private fun event(sourceId: String): ScrapedEvent = events.first { it.sourceId == sourceId }

    @Test
    fun `drops the arena's sport fixtures and keeps everything else`() {
        // 128 rows in the fixture: 64 konzert, 15 show, 9 comedy — and 40 eishockey/basketball/sport.
        events shouldHaveSize 88
        events.none { it.title.contains("FIBA") } shouldBe true
    }

    @Test
    fun `maps a fully populated row`() {
        val diljit = event("uber_arena:diljit-dosanjh/2026-08-21-2000")
        diljit.title shouldBe "Diljit Dosanjh"
        diljit.eventType shouldBe EventType.CONCERT.name
        diljit.eventDate shouldBe LocalDate.of(2026, 8, 21)
        diljit.startTime shouldBe LocalTime.of(20, 0)
        diljit.sourceUrl shouldBe "https://www.uber-arena.de/events/detail/diljit-dosanjh/2026-08-21-2000"
        diljit.pricePresale shouldBe BigDecimal("83.50")
        diljit.priceNote shouldBe "ab 83,50 €"
        diljit.imageUrl!! shouldStartWith "https://www.uber-arena.de/assets/img/"
        diljit.artists.map { it.name } shouldBe listOf("Diljit Dosanjh")
    }

    @Test
    fun `assembles the date from the venue's separate day month and year spans`() {
        // Each span carries its own punctuation: "21." / "08." / "2026,".
        event("uber_arena:diljit-dosanjh/2026-08-21-2000").eventDate shouldBe LocalDate.of(2026, 8, 21)
    }

    @Test
    fun `types the venue's own categories`() {
        events.map { it.eventType }.toSet() shouldBe
            setOf(EventType.CONCERT.name, EventType.SHOW.name)
        // 15 "show" + 9 "comedy" rows both resolve to SHOW; the rest are concerts.
        events.count { it.eventType == EventType.SHOW.name } shouldBe 24
        events.count { it.eventType == EventType.CONCERT.name } shouldBe 64
    }

    @Test
    fun `keeps one production's many dates apart by the date segment in its url`() {
        // A run reuses a single slug, so only the trailing "/YYYY-MM-DD-HHMM" makes it unique.
        events.map { it.sourceId }.toSet() shouldHaveSize events.size
    }

    @Test
    fun `parses every kept row into a resolved date and start time`() {
        events.none { it.eventDate == UNRESOLVED_EVENT_DATE } shouldBe true
        events.none { it.startTime == null } shouldBe true
    }

    @Test
    fun `stores no price where the venue has not announced one`() {
        // The three "6K UNITED!" dates render a non-breaking space in place of the price, which
        // must not become a zero or an empty note.
        val priceless = events.filter { it.pricePresale == null }
        priceless.map { it.title }.toSet() shouldBe setOf("6K UNITED!")
        priceless shouldHaveSize 3
        priceless.forEach { it.priceNote.shouldBeNull() }
    }

    @Test
    fun `leaves the detail-only fields empty`() {
        val diljit = event("uber_arena:diljit-dosanjh/2026-08-21-2000")
        diljit.doorsTime.shouldBeNull()
        diljit.description.shouldBeNull()
        diljit.ticketUrl.shouldBeNull()
    }

    @Test
    fun `returns an empty list for a page without rows`() {
        val document = Jsoup.parse("<html><body><div id='content'></div></body></html>", baseUrl)
        scraper.scrape(document, baseUrl).shouldBeEmpty()
    }
}
