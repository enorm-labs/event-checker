package de.norm.events.scraper

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document

/**
 * Abstract base class for venue importers that follow the overview → detail page pattern:
 * 1. Fetch the overview page and discover events.
 * 2. For each discovered event, fetch its detail page for richer data.
 * 3. Merge detail and overview data, preferring the detail page.
 *
 * Subclasses provide venue-specific scrapers and a gap-filling strategy;
 * this class owns the shared fetch orchestration.
 */
abstract class AbstractTwoPageWebsiteImporter(
    private val htmlFetcher: HtmlFetcher
) : EventImporter {
    // Use javaClass.name so logs identify the concrete subclass
    // (Cassiopeia / MadameClaude) rather than this abstract base.
    private val logger = KotlinLogging.logger(javaClass.name)

    /** Parses all events from the overview page HTML. */
    protected abstract fun scrapeOverview(
        document: Document,
        url: String
    ): List<ScrapedEvent>

    /** Parses the detail page for a single event, or null if the page cannot be parsed. */
    protected abstract fun scrapeDetail(
        document: Document,
        url: String
    ): ScrapedEvent?

    /**
     * Fills missing fields in [primary] (detail page data) from [fallback] (overview data).
     *
     * Only called when [scrapeDetail] succeeds. Implementations should fill only fields
     * that the detail page cannot supply (e.g. image URL).
     */
    protected abstract fun fillGapsFromOverview(
        primary: ScrapedEvent,
        fallback: ScrapedEvent
    ): ScrapedEvent

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
                val overviewEvents = scrapeOverview(fetchResult.document, url)
                logger.info { "Scraped ${overviewEvents.size} event(s) from ${eventSource.name} overview" }
                val events = overviewEvents.map { parseDetailOrFallback(it) }
                ImportResult.Success(events, fetchResult.etag, fetchResult.lastModified)
            }
        }

    @Suppress("TooGenericExceptionCaught") // Intentional: degrade to overview data if detail page is unavailable
    private suspend fun parseDetailOrFallback(overview: ScrapedEvent): ScrapedEvent =
        try {
            val detailDoc = htmlFetcher.fetchDocument(overview.sourceUrl)
            val detail = scrapeDetail(detailDoc, overview.sourceUrl)
            if (detail != null) fillGapsFromOverview(primary = detail, fallback = overview) else overview
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch detail page for '${overview.title}' (${overview.sourceUrl}), using overview data" }
            overview
        }
}
