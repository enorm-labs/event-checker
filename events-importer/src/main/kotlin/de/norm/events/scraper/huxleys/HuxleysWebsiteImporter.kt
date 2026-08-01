package de.norm.events.scraper.huxleys

import de.norm.events.event.EventStatus
import de.norm.events.scraper.AbstractTwoPageWebsiteImporter
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.HtmlFetcher
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

/**
 * Website importer for Huxleys Neue Welt's WordPress/Events-Manager concert listing.
 *
 * The venue's Events-Manager `event` post type is not exposed over the WordPress REST API (only the
 * stock post types are registered) and the theme embeds no schema.org JSON-LD, so it is scraped as
 * two HTML pages:
 * 1. Fetches the `/events` overview via [HtmlFetcher] with conditional-request support.
 * 2. Parses the cards via [HuxleysOverviewPageScraper] — the source for the discovery list, date,
 *    title, times, status, sold-out flag and support acts.
 * 3. For each event, fetches and parses its detail page via [HuxleysDetailPageScraper] — the source
 *    for the tour name, poster, ticket URL, description, genre and promoter.
 *
 * @see HuxleysOverviewPageScraper for overview parsing (discovery, date, times, status, fallback).
 * @see HuxleysDetailPageScraper for detail parsing (tour name, image, tickets, genre, promoter).
 * @see <a href="https://huxleysneuewelt.de/events">Huxleys Neue Welt</a>
 */
@Component
class HuxleysWebsiteImporter(
    htmlFetcher: HtmlFetcher
) : AbstractTwoPageWebsiteImporter(htmlFetcher) {
    override val eventSource: EventSource = EventSource.HUXLEYS

    private val overviewPageScraper = HuxleysOverviewPageScraper()
    private val detailPageScraper = HuxleysDetailPageScraper()

    override fun scrapeOverview(
        document: Document,
        url: String
    ): List<ScrapedEvent> = overviewPageScraper.scrape(document, url)

    override fun scrapeDetail(
        document: Document,
        url: String
    ): ScrapedEvent? = detailPageScraper.scrape(document, url)

    /**
     * Merges detail-page data ([primary]) with overview-page data ([fallback]).
     *
     * The detail page is authoritative for the fields the overview lacks (tour name, image, ticket
     * URL, description, genre, promoter), and the fields both carry (date, times, sold-out) prefer
     * it and fall back to the listing.
     *
     * Two fields deliberately keep the **overview's** value instead:
     * - **`title`**, because the detail page renders no heading at all — its only title is the
     *   document title with the site name appended, so the listing's `.eventname` is the cleaner
     *   source (and the reason [HuxleysDetailPageScraper] derives one only to stand alone).
     * - **`status`**, whenever the overview found a non-default one, because a relocation or a new
     *   date is announced solely in the listing's `.anderungen` note, which the detail page omits.
     *
     * `subtitle` and `artists` combine both pages: the tour name comes from the detail page and the
     * support acts from the listing, so a support line is never lost to a show that also has a tour
     * title.
     */
    override fun fillGapsFromOverview(
        primary: ScrapedEvent,
        fallback: ScrapedEvent
    ): ScrapedEvent {
        val subtitle = listOfNotNull(primary.subtitle, fallback.subtitle).distinct().joinToString(SUBTITLE_SEPARATOR).takeIf { it.isNotBlank() }
        return primary.copy(
            title = fallback.title,
            eventDate = primary.eventDate.takeIf { it != UNRESOLVED_EVENT_DATE } ?: fallback.eventDate,
            subtitle = subtitle,
            doorsTime = primary.doorsTime ?: fallback.doorsTime,
            startTime = primary.startTime ?: fallback.startTime,
            soldOut = primary.soldOut || fallback.soldOut,
            status = fallback.status.takeIf { it != EventStatus.SCHEDULED.name } ?: primary.status,
            artists = fallback.artists.ifEmpty { primary.artists }
        )
    }

    private companion object {
        /** Separator joining the detail page's tour name to the listing's support line. */
        const val SUBTITLE_SEPARATOR = " | "
    }
}
