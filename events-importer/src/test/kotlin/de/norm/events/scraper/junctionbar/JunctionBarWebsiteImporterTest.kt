package de.norm.events.scraper.junctionbar

import de.norm.events.event.EventType
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ImportResult
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for [JunctionBarWebsiteImporter].
 *
 * Mocks [HtmlFetcher] so the whole multi-page fetch (homepage → music listing → monthly pages, and
 * homepage → DJ page) runs offline against the saved fixtures. The clock is pinned to 2026-06-01 —
 * before the DJ fixture's earliest date (5.6.) — so weekday-based year inference stays deterministic.
 */
class JunctionBarWebsiteImporterTest {
    private lateinit var importer: JunctionBarWebsiteImporter
    private val htmlFetcher: HtmlFetcher = mockk()
    private val clock: Clock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC)

    private val homepageUrl = "https://www.junction-bar.de/index.html"
    private val musicListingUrl = "https://www.junction-bar.de/music_html/music.html"
    private val djUrl = "https://www.junction-bar.de/DJ_html/DJ.html"

    private fun fixture(
        name: String,
        baseUrl: String
    ): Document =
        Jsoup.parse(
            javaClass.classLoader
                .getResourceAsStream("scraper/junctionbar/$name")!!
                .bufferedReader()
                .readText(),
            baseUrl
        )

    @BeforeEach
    fun setUp() {
        importer = JunctionBarWebsiteImporter(htmlFetcher, clock)
        coEvery { htmlFetcher.fetchDocument(homepageUrl) } returns fixture("junctionbar-homepage.html", homepageUrl)
        coEvery { htmlFetcher.fetchDocument(musicListingUrl) } returns
            fixture("junctionbar-music-listing.html", musicListingUrl)
        coEvery { htmlFetcher.fetchDocument(match { it.contains("07_2026") }) } returns
            fixture("junctionbar-music-07_2026.html", "https://www.junction-bar.de/program/07_2026/07_26.html")
        // The listing also links June; treat it as an empty month so the count stays deterministic.
        coEvery { htmlFetcher.fetchDocument(match { it.contains("06_2026") }) } returns
            Jsoup.parse("<html><body><div class='gridContainer'></div></body></html>", homepageUrl)
        coEvery { htmlFetcher.fetchDocument(djUrl) } returns fixture("junctionbar-dj.html", djUrl)
    }

    @Test
    fun `eventSource matches expected enum value`() {
        importer.eventSource shouldBe EventSource.JUNCTION_BAR
    }

    @Test
    fun `importEvents merges the live-music and DJ programs into one result`() =
        runTest {
            val result = importer.importEvents(homepageUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            // 8 live-music nights + 8 DJ nights.
            result.events shouldHaveSize 16
            result.events.count { it.eventType == EventType.CONCERT.name } shouldBe 8
            result.events.count { it.eventType == EventType.PARTY.name } shouldBe 8
            // Multi-page importer intentionally disables conditional caching.
            result.etag.shouldBeNull()
            result.lastModified.shouldBeNull()
        }

    @Test
    fun `importEvents falls back to conventional program paths when the homepage has no nav links`() =
        runTest {
            // A homepage without the music/DJ nav links — the importer resolves the conventional relative paths.
            coEvery { htmlFetcher.fetchDocument(homepageUrl) } returns
                Jsoup.parse("<html><body><p>Welcome</p></body></html>", homepageUrl)

            val result = importer.importEvents(homepageUrl)
            result.shouldBeInstanceOf<ImportResult.Success>()
            result.events shouldHaveSize 16
        }
}
