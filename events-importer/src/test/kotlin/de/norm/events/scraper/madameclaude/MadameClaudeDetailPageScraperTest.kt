package de.norm.events.scraper.madameclaude

import de.norm.events.scraper.ScrapedEvent
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [MadameClaudeDetailPageScraper].
 *
 * Uses a static HTML fixture that mirrors a real Madame Claude event detail
 * page for deterministic, offline-safe testing.
 */
class MadameClaudeDetailPageScraperTest {
    private val scraper = MadameClaudeDetailPageScraper()
    private val sourceUrl = "https://madameclaude.de/event/drekka-btong-zimmermann-lienhard/"
    private lateinit var event: ScrapedEvent

    @BeforeEach
    fun setUp() {
        val html =
            javaClass.classLoader
                .getResourceAsStream("scraper/madameclaude/madameclaude-detail-concert.html")!!
                .bufferedReader()
                .readText()
        event = scraper.scrape(Jsoup.parse(html, sourceUrl), sourceUrl)!!
    }

    @Nested
    inner class BasicFields {
        @Test
        fun `parses title from h2`() {
            event.title shouldBe "drekka + b°tong + Zimmermann / Lienhard"
        }

        @Test
        fun `parses date from DD-MM-YY format`() {
            event.eventDate shouldBe LocalDate.of(2026, 9, 21)
        }

        @Test
        fun `parses event type from category label`() {
            event.eventType shouldBe "CONCERT"
        }

        @Test
        fun `generates correct sourceId from slug`() {
            event.sourceId shouldBe "madame_claude:drekka-btong-zimmermann-lienhard"
        }
    }

    @Nested
    inner class TimesAndPrice {
        @Test
        fun `parses doors time`() {
            event.doorsTime shouldBe LocalTime.of(19, 0)
        }

        @Test
        fun `parses start time`() {
            event.startTime shouldBe LocalTime.of(20, 0)
        }

        @Test
        fun `parses donation as price note`() {
            event.priceNote shouldBe "Donation"
        }
    }

    @Nested
    inner class ArtistParsing {
        @Test
        fun `extracts three artists from h3 headings`() {
            event.artists shouldHaveSize 3
        }

        @Test
        fun `first artist is headliner`() {
            event.artists[0].name shouldBe "drekka"
            event.artists[0].role shouldBe "HEADLINER"
        }

        @Test
        fun `subsequent artists are support`() {
            event.artists[1].name shouldBe "b°tong"
            event.artists[1].role shouldBe "SUPPORT"

            event.artists[2].name shouldBe "Zimmermann / Lienhard"
            event.artists[2].role shouldBe "SUPPORT"
        }
    }

    @Nested
    inner class NonArtistHeadings {
        private fun scrapeFixture(resource: String): ScrapedEvent {
            val html =
                javaClass.classLoader
                    .getResourceAsStream(resource)!!
                    .bufferedReader()
                    .readText()
            return scraper.scrape(Jsoup.parse(html, sourceUrl), sourceUrl)!!
        }

        @Test
        fun `does not create an artist for a denylisted event-title h3`() {
            // "Music Quiz" appears as both the h2 title and an h3 heading; it is on the
            // NON_ARTIST_NAMES denylist, so the h3 must not become an artist.
            val quiz = scrapeFixture("scraper/madameclaude/madameclaude-detail-music-quiz.html")

            quiz.title shouldBe "Music Quiz"
            quiz.artists.shouldBeEmpty()
        }

        @Test
        fun `still collects the description from a filtered non-artist h3`() {
            val quiz = scrapeFixture("scraper/madameclaude/madameclaude-detail-music-quiz.html")

            quiz.description shouldContain "winning team"
        }
    }

    @Nested
    inner class DescriptionParsing {
        @Test
        fun `extracts description with artist bios`() {
            event.description shouldContain "drekka:"
            event.description shouldContain "composer Michael Anderson"
            event.description shouldContain "b°tong:"
            event.description shouldContain "hostile soundscapes"
        }
    }

    @Test
    fun `returns null for page without primary-info-single-event`() {
        val emptyHtml = "<html><body><main></main></body></html>"
        val document = Jsoup.parse(emptyHtml, sourceUrl)

        scraper.scrape(document, sourceUrl) shouldBe null
    }

    @Test
    fun `returns null for page without title`() {
        val noTitleHtml =
            """
            <html><body><main>
                <div class="primary-info-single-event">
                    <div class="date"><p class="numbers"><font>21/09/26</font></p></div>
                </div>
            </main></body></html>
            """.trimIndent()
        val document = Jsoup.parse(noTitleHtml, sourceUrl)

        scraper.scrape(document, sourceUrl) shouldBe null
    }
}
