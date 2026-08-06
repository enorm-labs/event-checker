package de.norm.events.scraper.monsterronsons

import de.norm.events.scraper.EventSource
import de.norm.events.scraper.FetchResult
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ImportResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Unit tests for [MonsterRonsonsWebsiteImporter].
 *
 * Uses saved snapshots of the listing and one night page, with [HtmlFetcher] mocked so no real HTTP
 * requests are made. The clock is pinned to the capture date (2026-08-06) so the listing's year-less
 * dates resolve deterministically.
 */
class MonsterRonsonsWebsiteImporterTest {
    private lateinit var importer: MonsterRonsonsWebsiteImporter
    private val htmlFetcher: HtmlFetcher = mockk()

    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-06T09:00:00Z"), ZoneId.of("Europe/Berlin"))
    private val sourceUrl = "https://www.karaokemonster.de/events"

    @BeforeEach
    fun setUp() {
        importer = MonsterRonsonsWebsiteImporter(htmlFetcher, clock)

        coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns
            FetchResult.Success(
                document = Jsoup.parse(loadFixture("monsterronsons-overview.html"), sourceUrl),
                etag = "\"events-1\"",
                lastModified = "Thu, 06 Aug 2026 06:00:00 GMT"
            )
        // Every night page returns the hosted snapshot; the opener is asserted on by name.
        coEvery { htmlFetcher.fetchDocument(any()) } answers {
            val url = firstArg<String>()
            Jsoup.parse(loadFixture("monsterronsons-detail-hosted.html"), url)
        }
    }

    private fun loadFixture(name: String): String =
        javaClass.classLoader
            .getResourceAsStream("scraper/monsterronsons/$name")!!
            .bufferedReader()
            .readText()

    @Test
    fun `imports the listing window and propagates cache headers`() =
        runTest {
            val result = importer.importEvents(sourceUrl, null, null).shouldBeInstanceOf<ImportResult.Success>()

            result.events shouldHaveSize 11
            result.etag shouldBe "\"events-1\""
            result.lastModified shouldBe "Thu, 06 Aug 2026 06:00:00 GMT"
        }

    @Test
    fun `enriches each night with its detail page`() =
        runTest {
            val result = importer.importEvents(sourceUrl, null, null).shouldBeInstanceOf<ImportResult.Success>()

            val opener = result.events.first { it.sourceId == "monster_ronsons:2026-08-06-sing-with-fauxpas-2" }
            opener.description.shouldNotBeNull() shouldContain "IVANKA TRAMP"
            opener.priceBoxOffice shouldBe BigDecimal("5")
            // The card's own fields survive the merge.
            opener.title shouldBe "SING WITH IVANKA TRAMP"
        }

    @Test
    fun `fetches one night page per distinct url`() =
        runTest {
            importer.importEvents(sourceUrl, null, null)

            // 11 events, 11 distinct night URLs — the closure card never reaches a fetch.
            coVerify(exactly = 11) { htmlFetcher.fetchDocument(any()) }
            coVerify(exactly = 0) { htmlFetcher.fetchDocument("https://www.karaokemonster.de/posts/sorry-we-are-closed") }
        }

    @Test
    fun `keeps the listing data when a night page fails`() =
        runTest {
            coEvery { htmlFetcher.fetchDocument(any()) } throws RuntimeException("connection reset")

            val result = importer.importEvents(sourceUrl, null, null).shouldBeInstanceOf<ImportResult.Success>()

            result.events shouldHaveSize 11
            val opener = result.events.first { it.sourceId == "monster_ronsons:2026-08-06-sing-with-fauxpas-2" }
            opener.title shouldBe "SING WITH IVANKA TRAMP"
            opener.description.shouldBeNull()
        }

    @Test
    fun `returns NotModified when the listing is unchanged`() =
        runTest {
            coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns FetchResult.NotModified

            importer.importEvents(sourceUrl, "\"events-1\"", null).shouldBeInstanceOf<ImportResult.NotModified>()
            coVerify(exactly = 0) { htmlFetcher.fetchDocument(any()) }
        }

    @Test
    fun `returns no events for an empty listing`() =
        runTest {
            coEvery { htmlFetcher.fetch(sourceUrl, any(), any()) } returns
                FetchResult.Success(
                    document = Jsoup.parse("<html><body><div class='grid-container'></div></body></html>", sourceUrl),
                    etag = null,
                    lastModified = null
                )

            val result = importer.importEvents(sourceUrl, null, null).shouldBeInstanceOf<ImportResult.Success>()
            result.events.shouldBeEmpty()
        }

    @Test
    fun `reports its event source`() {
        importer.eventSource shouldBe EventSource.MONSTER_RONSONS
    }
}
