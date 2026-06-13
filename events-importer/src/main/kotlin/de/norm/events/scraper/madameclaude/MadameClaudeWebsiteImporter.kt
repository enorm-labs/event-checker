package de.norm.events.scraper.madameclaude

import de.norm.events.scraper.AbstractTwoPageWebsiteImporter
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

/**
 * Website importer for Madame Claude Berlin's WordPress-based event listing.
 *
 * Orchestrates the full fetch → parse pipeline for Madame Claude:
 * 1. Fetches the overview page (`/events/`) via [HtmlFetcher] with conditional
 *    request support (ETag / Last-Modified).
 * 2. Parses event cards from the overview page via [MadameClaudeOverviewPageScraper].
 * 3. For each event, fetches its detail page via [HtmlFetcher].
 * 4. Parses the detail page via [MadameClaudeDetailPageScraper] — this is the
 *    **primary** data source. The overview page provides discovery and fallback
 *    data for fields the detail page cannot supply (e.g. image URL).
 *
 * @see MadameClaudeOverviewPageScraper for overview page parsing (discovery + fallback)
 * @see MadameClaudeDetailPageScraper for detail page parsing (primary data source)
 * @see <a href="https://madameclaude.de/events/">Madame Claude Events</a>
 */
@Component
class MadameClaudeWebsiteImporter(
    htmlFetcher: HtmlFetcher
) : AbstractTwoPageWebsiteImporter(htmlFetcher) {
    override val eventSource: EventSource = EventSource.MADAME_CLAUDE

    private val overviewPageScraper = MadameClaudeOverviewPageScraper()
    private val detailPageScraper = MadameClaudeDetailPageScraper()

    override fun scrapeOverview(
        document: Document,
        url: String
    ): List<ScrapedEvent> = overviewPageScraper.scrape(document, url)

    override fun scrapeDetail(
        document: Document,
        url: String
    ): ScrapedEvent? = detailPageScraper.scrape(document, url)

    /**
     * Fills missing fields in the [primary] (detail page) event with values
     * from the [fallback] (overview page) event.
     *
     * The detail page is authoritative for every field it provides. The
     * overview page only contributes values where the detail page returned
     * null or a default sentinel.
     */
    override fun fillGapsFromOverview(
        primary: ScrapedEvent,
        fallback: ScrapedEvent
    ): ScrapedEvent =
        primary.copy(
            // Use overview date if detail page couldn't parse it (detail returns the sentinel)
            eventDate = primary.eventDate.takeIf { it != UNRESOLVED_EVENT_DATE } ?: fallback.eventDate,
            eventType = primary.eventType ?: fallback.eventType,
            imageUrl = primary.imageUrl ?: fallback.imageUrl,
            description = primary.description ?: fallback.description,
            artists = primary.artists.ifEmpty { fallback.artists }
        )
}
