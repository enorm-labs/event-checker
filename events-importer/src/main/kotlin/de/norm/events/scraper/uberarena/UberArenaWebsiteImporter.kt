package de.norm.events.scraper.uberarena

import de.norm.events.scraper.AbstractTwoPageWebsiteImporter
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

/**
 * Website importer for Uber Arena's programme.
 *
 * Orchestrates the fetch → parse pipeline:
 * 1. Fetch the unpaginated `/events/all` listing via [HtmlFetcher] with conditional request
 *    support (ETag / Last-Modified).
 * 2. Discover every non-sport row via [UberArenaOverviewPageScraper] — title, category, date,
 *    start time, from-price and thumbnail.
 * 3. For each event, fetch its `/events/detail/<slug>/<YYYY-MM-DD-HHMM>` page and parse it via
 *    [UberArenaDetailPageScraper] — the only source for the doors time, description and the
 *    external AXS ticket link.
 *
 * @see UberArenaOverviewPageScraper for listing parsing (title, date, category, price).
 * @see UberArenaDetailPageScraper for detail parsing (doors, description, ticket).
 * @see <a href="https://www.uber-arena.de/events/all">Uber Arena programme</a>
 */
@Component
class UberArenaWebsiteImporter(
    htmlFetcher: HtmlFetcher
) : AbstractTwoPageWebsiteImporter(htmlFetcher) {
    override val eventSource: EventSource = EventSource.UBER_ARENA

    private val overviewPageScraper = UberArenaOverviewPageScraper()
    private val detailPageScraper = UberArenaDetailPageScraper()

    override fun scrapeOverview(
        document: Document,
        url: String
    ): List<ScrapedEvent> = overviewPageScraper.scrape(document, url)

    override fun scrapeDetail(
        document: Document,
        url: String
    ): ScrapedEvent? = detailPageScraper.scrape(document, url)

    /**
     * Merges detail-page data ([primary]) with listing data ([fallback]).
     *
     * The **listing wins on title, date, start time, category, price and thumbnail** — the detail
     * page states none of them cleanly: its heading appends "in der Uber Arena" to the act name,
     * and it renders no date the parser reads. The detail page contributes only the doors time,
     * the description and the ticket link. The artist roster therefore also comes from the
     * listing, which is where the clean title lives.
     */
    override fun fillGapsFromOverview(
        primary: ScrapedEvent,
        fallback: ScrapedEvent
    ): ScrapedEvent =
        primary.copy(
            title = fallback.title,
            eventDate = primary.eventDate.takeIf { it != UNRESOLVED_EVENT_DATE } ?: fallback.eventDate,
            startTime = primary.startTime ?: fallback.startTime,
            eventType = primary.eventType ?: fallback.eventType,
            imageUrl = primary.imageUrl ?: fallback.imageUrl,
            pricePresale = primary.pricePresale ?: fallback.pricePresale,
            priceNote = primary.priceNote ?: fallback.priceNote,
            artists = primary.artists.ifEmpty { fallback.artists }
        )
}
