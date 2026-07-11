package de.norm.events.scraper.madameclaude

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Unit tests for [MadameClaudeOverviewPageScraper].
 *
 * Uses a static HTML fixture that mirrors the real Madame Claude WordPress
 * page structure for deterministic, offline-safe testing.
 */
class MadameClaudeOverviewPageScraperTest {
    private val baseUrl = "https://madameclaude.de/events/"
    private lateinit var html: String

    /** Fixed clock at 2026-06-12 for deterministic year inference. */
    private val fixedClock: Clock =
        Clock.fixed(
            ZonedDateTime.of(2026, 6, 12, 12, 0, 0, 0, ZoneId.of("Europe/Berlin")).toInstant(),
            ZoneId.of("Europe/Berlin")
        )

    private val scraper = MadameClaudeOverviewPageScraper(fixedClock)

    @BeforeEach
    fun setUp() {
        html =
            javaClass.classLoader
                .getResourceAsStream("scraper/madameclaude/madameclaude-overview.html")!!
                .bufferedReader()
                .readText()
    }

    @Test
    fun `scrape extracts all events from fixture`() {
        val document = Jsoup.parse(html, baseUrl)
        val events = scraper.scrape(document, baseUrl)

        events shouldHaveSize 4
    }

    @Nested
    inner class MapMadameClaudeCategory {
        @Test
        fun `maps a known CSS category`() {
            mapMadameClaudeCategory("MusicQuiz", null, "Music Quiz #12") shouldBe "QUIZ"
        }

        @Test
        fun `types an unknown category with a screening title as SCREENING`() {
            // Madame Claude publishes no screening category, so the title is the only
            // signal — otherwise this film night would fall to the OTHER default.
            mapMadameClaudeCategory("Shorties", null, "SHORTIES FILMS SCREENING #28") shouldBe "SCREENING"
        }

        @Test
        fun `returns null for an unknown category and a non-screening title`() {
            // Null lets fillGapsFromOverview fall back to the other page's value.
            mapMadameClaudeCategory("Unknown", null, "Some Live Act") shouldBe null
        }
    }

    @Nested
    inner class ExperimontagConcert {
        @Test
        fun `parses experimontag concert card`() {
            val document = Jsoup.parse(html, baseUrl)
            val events = scraper.scrape(document, baseUrl)
            val event = events.first { it.title.contains("drekka") }

            event.title shouldBe "drekka + b°tong + Zimmermann / Lienhard"
            event.eventDate shouldBe LocalDate.of(2026, 9, 21)
            event.eventType shouldBe "CONCERT"
            event.sourceUrl shouldBe "https://madameclaude.de/event/drekka-btong-zimmermann-lienhard/"
            event.sourceId shouldBe "madame_claude:drekka-btong-zimmermann-lienhard"
            event.imageUrl shouldContain "58168777e0a7ee5c"
        }
    }

    @Nested
    inner class MusicQuizEvent {
        @Test
        fun `parses music quiz as QUIZ event type`() {
            val document = Jsoup.parse(html, baseUrl)
            val events = scraper.scrape(document, baseUrl)
            val event = events.first { it.title == "Music Quiz" }

            event.eventType shouldBe "QUIZ"
            event.eventDate shouldBe LocalDate.of(2026, 7, 22)
            event.sourceId shouldBe "madame_claude:music-quiz-149"
        }
    }

    @Nested
    inner class LiveConcert {
        @Test
        fun `parses live concert with full date DD-MM-YY`() {
            val document = Jsoup.parse(html, baseUrl)
            val events = scraper.scrape(document, baseUrl)
            val event = events.first { it.title.contains("Wild Ones") }

            event.eventDate shouldBe LocalDate.of(2026, 8, 15)
            event.eventType shouldBe "CONCERT"
            event.sourceId shouldBe "madame_claude:the-wild-ones"
        }
    }

    @Nested
    inner class OtherEvent {
        @Test
        fun `parses film screening as OTHER`() {
            val document = Jsoup.parse(html, baseUrl)
            val events = scraper.scrape(document, baseUrl)
            val event = events.first { it.title.contains("SHORTIES") }

            event.eventType shouldBe "OTHER"
            event.eventDate shouldBe LocalDate.of(2026, 9, 17)
        }
    }

    @Test
    fun `scrape returns empty list for page without events`() {
        val emptyHtml = "<html><body><main></main></body></html>"
        val document = Jsoup.parse(emptyHtml, baseUrl)
        val events = scraper.scrape(document, baseUrl)

        events shouldHaveSize 0
    }

    @Test
    fun `date without year infers current year for future dates`() {
        val document = Jsoup.parse(html, baseUrl)
        val events = scraper.scrape(document, baseUrl)

        // All dates in the fixture are after June 12, 2026
        events.forEach { event ->
            event.eventDate.year shouldBe 2026
        }
    }

    @Test
    fun `date without year rolls forward when day-month already past`() {
        // Synthetic card with date "10/05" — already past the fixed clock (2026-06-12),
        // so the scraper should infer 2027.
        val pastDateHtml =
            """
            <html><body><main><div class="entries">
                <article class="entry-card">
                    <div class="event-card bw card- Live">
                        <div class="thumbnail">
                            <a href="https://madameclaude.de/event/past-date/">
                                <img class="screen" src="https://example.com/img.jpg"/>
                            </a>
                        </div>
                        <div class="text"><div class="title">
                            <h4>
                                <font>10/05</font>
                                <font> Mon - </font>
                                Past Date Event
                            </h4>
                        </div></div>
                    </div>
                </article>
            </div></main></body></html>
            """.trimIndent()
        val document = Jsoup.parse(pastDateHtml, baseUrl)
        val events = scraper.scrape(document, baseUrl)

        events shouldHaveSize 1
        events[0].eventDate shouldBe LocalDate.of(2027, 5, 10)
    }
}
