package de.norm.events.scraper.gartn

import de.norm.events.scraper.EventImporter
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.FetchResult
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ImportResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Website importer for gART.n's single-page Carrd programme.
 *
 * The venue's whole web presence is one Carrd page — no CMS behind it, no REST API, no feed, no
 * embedded structured data, and a `sitemap.xml` listing only that page and the separate Impressum
 * card. Its "UPCOMING" block carries the entire programme inline, including every lineup and
 * ticket link, so the homepage is the source and the pipeline is a single fetch:
 * 1. Fetch the page via [HtmlFetcher] with conditional-request support. Carrd's Apache serves both
 *    a strong `ETag` and a `Last-Modified`, and the page changes only when the programme is
 *    edited, so 304s are reliable and frequent here.
 * 2. Parse every dated event block via [GartnOverviewPageScraper].
 *
 * @see GartnOverviewPageScraper for the HTML parsing logic.
 * @see <a href="https://www.gartn.xyz/">gART.n Berlin</a>
 */
@Component
class GartnWebsiteImporter(
    private val htmlFetcher: HtmlFetcher
) : EventImporter {
    private val logger = KotlinLogging.logger {}

    override val eventSource: EventSource = EventSource.GARTN

    private val overviewPageScraper = GartnOverviewPageScraper()

    override suspend fun importEvents(
        url: String,
        etag: String?,
        lastModified: String?
    ): ImportResult =
        when (val fetchResult = htmlFetcher.fetch(url, etag, lastModified)) {
            is FetchResult.NotModified -> {
                ImportResult.NotModified
            }

            is FetchResult.Success -> {
                val events = overviewPageScraper.scrape(fetchResult.document, url)
                logger.info { "Scraped ${events.size} event(s) from gART.n" }

                ImportResult.Success(
                    events = events,
                    etag = fetchResult.etag,
                    lastModified = fetchResult.lastModified
                )
            }
        }
}
