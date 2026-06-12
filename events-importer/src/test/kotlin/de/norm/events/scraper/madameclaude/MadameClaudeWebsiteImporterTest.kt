package de.norm.events.scraper.madameclaude

import de.norm.events.scraper.EventSource
import de.norm.events.scraper.FetchResult
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ImportResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [MadameClaudeWebsiteImporter].
 *
 * Uses static HTML fixtures and mocked [HtmlFetcher] for deterministic,
 * offline-safe testing without real HTTP requests.
 */
class MadameClaudeWebsiteImporterTest {
    private lateinit var importer: MadameClaudeWebsiteImporter
    private val htmlFetcher: HtmlFetcher = mockk()
    private val sourceUrl = "https://madameclaude.de/events/"

    @BeforeEach
    fun setUp() {
        importer = MadameClaudeWebsiteImporter(htmlFetcher)

        val overviewHtml =
            javaClass.classLoader
                .getResourceAsStream("scraper/madameclaude/madameclaude-overview.html")!!
                .bufferedReader()
                .readText()
        val overviewDoc = Jsoup.parse(overviewHtml, sourceUrl)

        coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns
            FetchResult.Success(
                document = overviewDoc,
                etag = "\"mc-etag\"",
                lastModified = "Thu, 12 Jun 2026 10:00:00 GMT"
            )

        // Detail pages — return a simple page so we don't need fixtures for all 4
        val detailHtml =
            javaClass.classLoader
                .getResourceAsStream("scraper/madameclaude/madameclaude-detail-concert.html")!!
                .bufferedReader()
                .readText()

        // Mock all detail page fetches — the concert detail page for the first event,
        // empty detail pages for the rest (importer falls back to overview data)
        coEvery { htmlFetcher.fetchDocument("https://madameclaude.de/event/drekka-btong-zimmermann-lienhard/") } returns
            Jsoup.parse(detailHtml, "https://madameclaude.de/event/drekka-btong-zimmermann-lienhard/")

        val emptyDetailHtml = "<html><body><main></main></body></html>"
        coEvery { htmlFetcher.fetchDocument(match { it != "https://madameclaude.de/event/drekka-btong-zimmermann-lienhard/" }) } returns
            Jsoup.parse(emptyDetailHtml, sourceUrl)
    }

    @Test
    fun `importEvents extracts all events from fixture`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            result.events shouldHaveSize 4
        }

    @Test
    fun `importEvents propagates conditional response headers`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            result.etag shouldBe "\"mc-etag\""
            result.lastModified shouldBe "Thu, 12 Jun 2026 10:00:00 GMT"
        }

    @Test
    fun `importEvents returns NotModified when page unchanged`() =
        runTest {
            coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns FetchResult.NotModified

            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.NotModified>()
        }

    @Test
    fun `importEvents returns empty list for page without events`() =
        runTest {
            val emptyHtml = "<html><body><main></main></body></html>"
            val emptyDoc = Jsoup.parse(emptyHtml, sourceUrl)
            coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns
                FetchResult.Success(document = emptyDoc, etag = null, lastModified = null)

            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            result.events shouldHaveSize 0
        }

    @Test
    fun `eventSource matches expected enum value`() {
        importer.eventSource shouldBe EventSource.MADAME_CLAUDE
    }

    @Test
    fun `detail page enriches overview data with times and artists`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()

            val concertEvent = result.events.first { it.title.contains("drekka") }
            // Should have times from detail page
            concertEvent.doorsTime shouldBe java.time.LocalTime.of(19, 0)
            concertEvent.startTime shouldBe java.time.LocalTime.of(20, 0)
            // Should have artists from detail page
            concertEvent.artists shouldHaveSize 3
            concertEvent.priceNote shouldBe "Donation"
        }

    @Test
    fun `falls back to overview data when detail page has no content`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()

            // Music Quiz — detail page returns empty, so overview data is used
            val quizEvent = result.events.first { it.title == "Music Quiz" }
            quizEvent.eventType shouldBe "QUIZ"
            quizEvent.imageUrl shouldBe "https://madameclaude.de/wp-content/uploads/2025/05/quiz-400x400.jpg"
        }
}
