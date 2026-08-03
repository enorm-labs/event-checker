package de.norm.events.scraper.uberarena

import de.norm.events.event.EventType
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.FetchResult
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ImportResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [UberArenaWebsiteImporter].
 *
 * Uses static HTML fixtures and a mocked [HtmlFetcher] for deterministic, offline-safe testing
 * without real HTTP requests. Only one detail page is stubbed; every other detail fetch throws,
 * exercising the base class's degrade-to-overview fallback.
 */
class UberArenaWebsiteImporterTest {
    private lateinit var importer: UberArenaWebsiteImporter
    private val htmlFetcher: HtmlFetcher = mockk()
    private val sourceUrl = "https://www.uber-arena.de/events/all"
    private val detailUrl = "https://www.uber-arena.de/events/detail/diljit-dosanjh/2026-08-21-2000"

    private fun readFixture(name: String): String =
        javaClass.classLoader
            .getResourceAsStream("scraper/uberarena/$name")!!
            .bufferedReader()
            .readText()

    @BeforeEach
    fun setUp() {
        importer = UberArenaWebsiteImporter(htmlFetcher)

        coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns
            FetchResult.Success(
                document = Jsoup.parse(readFixture("uberarena-overview.html"), sourceUrl),
                etag = "\"ua-etag\"",
                lastModified = "Mon, 03 Aug 2026 09:00:00 GMT"
            )

        coEvery { htmlFetcher.fetchDocument(any()) } throws IllegalStateException("no fixture")
        coEvery { htmlFetcher.fetchDocument(detailUrl) } returns
            Jsoup.parse(readFixture("uberarena-detail-diljit-dosanjh.html"), detailUrl)
    }

    @Test
    fun `importEvents keeps the non-sport programme`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            result.events shouldHaveSize 88
        }

    @Test
    fun `importEvents propagates conditional response headers`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            result.etag shouldBe "\"ua-etag\""
            result.lastModified shouldBe "Mon, 03 Aug 2026 09:00:00 GMT"
        }

    @Test
    fun `importEvents returns NotModified when page unchanged`() =
        runTest {
            coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns FetchResult.NotModified

            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.NotModified>()
        }

    @Test
    fun `importEvents returns empty list for a page without rows`() =
        runTest {
            val emptyDoc = Jsoup.parse("<html><body><div id='content'></div></body></html>", sourceUrl)
            coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns
                FetchResult.Success(document = emptyDoc, etag = null, lastModified = null)

            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            result.events shouldHaveSize 0
        }

    @Test
    fun `merges the detail page while keeping the listing's cleaner title`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            val diljit = result.events.first { it.sourceId == "uber_arena:diljit-dosanjh/2026-08-21-2000" }

            // Listing wins: the detail heading says "Diljit Dosanjh in der Uber Arena".
            diljit.title shouldBe "Diljit Dosanjh"
            diljit.eventDate shouldBe LocalDate.of(2026, 8, 21)
            diljit.startTime shouldBe LocalTime.of(20, 0)
            diljit.pricePresale shouldBe BigDecimal("83.50")
            diljit.eventType shouldBe EventType.CONCERT.name
            diljit.artists.map { it.name } shouldBe listOf("Diljit Dosanjh")
            // Detail-only.
            diljit.doorsTime shouldBe LocalTime.of(18, 30)
            diljit.description!!.isNotBlank() shouldBe true
            diljit.ticketUrl!! shouldStartWith "https://queue-de.axs.com/"
        }

    @Test
    fun `falls back to listing data when a detail page cannot be fetched`() =
        runTest {
            val result = importer.importEvents(sourceUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            val other = result.events.first { it.sourceId != "uber_arena:diljit-dosanjh/2026-08-21-2000" }
            other.title.isNotBlank() shouldBe true
            other.startTime shouldBe other.startTime
            // Detail-only fields stay empty rather than aborting the import.
            other.doorsTime.shouldBeNull()
            other.description.shouldBeNull()
            other.ticketUrl.shouldBeNull()
        }

    @Test
    fun `eventSource matches expected enum value`() {
        importer.eventSource shouldBe EventSource.UBER_ARENA
    }
}
