package de.norm.events.scraper.cassiopeia

import de.norm.events.scraper.EventImporter
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.FetchResult
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ImportResult
import de.norm.events.scraper.ScrapedEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Website importer for Cassiopeia Berlin's Webflow-based event listing.
 *
 * Orchestrates the full fetch → parse pipeline for Cassiopeia:
 * 1. Fetches the overview page (`/club`) via [HtmlFetcher] with conditional
 *    request support (ETag / Last-Modified).
 * 2. Parses event listings from the overview page via [CassiopeiaOverviewPageScraper].
 * 3. For each event, fetches its detail page via [HtmlFetcher].
 * 4. Parses the detail page via [CassiopeiaDetailPageScraper] — this is the
 *    **primary** data source. The overview page provides discovery and fallback
 *    data for fields the detail page cannot supply.
 *
 * This importer owns **all** HTTP fetching for Cassiopeia, keeping I/O
 * concerns in a single place. The page scrapers are pure parsers operating
 * on pre-fetched Jsoup Documents, making them easy to test in isolation.
 *
 * @see CassiopeiaOverviewPageScraper for overview page parsing (discovery + fallback)
 * @see CassiopeiaDetailPageScraper for detail page parsing (primary data source)
 * @see <a href="https://cassiopeia-berlin.de/club">Cassiopeia Club page</a>
 */
@Component
class CassiopeiaWebsiteImporter(
    private val htmlFetcher: HtmlFetcher
) : EventImporter {
    private val logger = KotlinLogging.logger {}

    override val eventSource: EventSource = EventSource.CASSIOPEIA

    private val overviewPageScraper = CassiopeiaOverviewPageScraper()
    private val detailPageScraper = CassiopeiaDetailPageScraper()

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
                val overviewEvents = overviewPageScraper.scrape(fetchResult.document, url)
                logger.info { "Scraped ${overviewEvents.size} event(s) from overview page" }

                val events = overviewEvents.map { overview -> parseDetailOrFallback(overview) }

                ImportResult.Success(
                    events = events,
                    etag = fetchResult.etag,
                    lastModified = fetchResult.lastModified
                )
            }
        }

    /**
     * Fetches and parses the detail page for a single event.
     *
     * The detail page is the **primary** data source — it provides richer
     * fields (description, ticket URL, cleaner images) than the overview
     * listing. If the detail page cannot be fetched or parsed, the overview
     * page data is returned as-is as a fallback, so that the event is still
     * imported rather than skipped entirely.
     */
    @Suppress("TooGenericExceptionCaught") // Intentional: degrade to overview data if detail page is unavailable
    private suspend fun parseDetailOrFallback(overview: ScrapedEvent): ScrapedEvent =
        try {
            // Use fetchDocument() to ensure Jsoup parsing runs on Dispatchers.IO
            val detailDoc = htmlFetcher.fetchDocument(overview.sourceUrl)
            val detail = detailPageScraper.scrape(detailDoc, overview.sourceUrl)

            if (detail != null) fillGapsFromOverview(primary = detail, fallback = overview) else overview
        } catch (e: Exception) {
            logger.warn(e) {
                "Failed to fetch detail page for '${overview.title}' (${overview.sourceUrl}), using overview data"
            }
            overview
        }

    /**
     * Fills missing fields in the [primary] (detail page) event with values
     * from the [fallback] (overview page) event.
     *
     * The detail page is authoritative for every field it provides. The
     * overview page only contributes values where the detail page returned
     * null or a default sentinel (e.g. missing genre or "OTHER" event type).
     */
    private fun fillGapsFromOverview(
        primary: ScrapedEvent,
        fallback: ScrapedEvent
    ): ScrapedEvent =
        primary.copy(
            doorsTime = primary.doorsTime ?: fallback.doorsTime,
            startTime = primary.startTime ?: fallback.startTime,
            eventType = primary.eventType.takeIf { it != "OTHER" } ?: fallback.eventType,
            genre = primary.genre ?: fallback.genre,
            imageUrl = primary.imageUrl ?: fallback.imageUrl,
            soldOut = primary.soldOut || fallback.soldOut,
            status = primary.status.takeIf { it != "SCHEDULED" } ?: fallback.status,
            description = primary.description ?: fallback.description,
            ticketUrl = primary.ticketUrl ?: fallback.ticketUrl,
            // Detail page artists include support acts from description paragraphs;
            // overview page only has the headliner. Prefer detail when available.
            artists = primary.artists.ifEmpty { fallback.artists }
        )
}
