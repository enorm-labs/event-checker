package de.norm.events.scraper.uberarena

import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import de.norm.events.scraper.extractEventSlug
import de.norm.events.scraper.hrefAt
import de.norm.events.scraper.parseTime
import de.norm.events.scraper.textAt
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import java.time.LocalTime

/**
 * Pure HTML parser for Uber Arena event detail pages (`/events/detail/<slug>/<YYYY-MM-DD-HHMM>`).
 *
 * The page adds the three things the listing row cannot carry: the `Einlass` doors time, the
 * prose description, and the external AXS ticket-shop link.
 *
 * It deliberately supplies **no title and no date**. Its `h1.summary` appends the venue
 * ("Diljit Dosanjh **in der Uber Arena**"), so the listing's cleaner act name is kept; and the
 * listing already assembled the date from its own span group. Both are left to
 * [UberArenaWebsiteImporter.fillGapsFromOverview].
 *
 * This class performs **no I/O** — it operates solely on a pre-fetched Jsoup [Document],
 * making it easy to test with a static fixture.
 *
 * @see UberArenaOverviewPageScraper for the listing parser (title, date, category, price).
 * @see UberArenaWebsiteImporter for the HTTP fetch orchestrator.
 * @see <a href="https://www.uber-arena.de/events/detail/diljit-dosanjh/2026-08-21-2000">Example detail page</a>
 */
class UberArenaDetailPageScraper {
    private val logger = KotlinLogging.logger {}

    /**
     * Parses an event detail page into a [ScrapedEvent], or `null` when the page carries no
     * event body at all — the marker that the URL did not resolve to a real event.
     *
     * @param document the parsed Jsoup document of the detail page.
     * @param sourceUrl the event's URL, used as [ScrapedEvent.sourceUrl] and to derive the
     *   [ScrapedEvent.sourceId].
     */
    @Suppress("ReturnCount") // A guard clause for the missing heading is clearer than nesting
    fun scrape(
        document: Document,
        sourceUrl: String
    ): ScrapedEvent? {
        val heading = document.textAt("h1.summary")
        if (heading == null) {
            logger.warn { "Uber Arena detail page at $sourceUrl has no heading, skipping" }
            return null
        }
        val slug = extractEventSlug(sourceUrl, "/events/detail/")

        return ScrapedEvent(
            // The listing owns the title: this page's heading carries an "in der Uber Arena" tail.
            title = heading,
            description = document.textAt(".event_body"),
            // The listing owns the date; the sentinel lets it fill this in at the merge.
            eventDate = UNRESOLVED_EVENT_DATE,
            doorsTime = parseDoorsTime(document.textAt(".edp_heading .doors")),
            sourceUrl = sourceUrl,
            sourceId = "${EventSource.UBER_ARENA.sourceIdPrefix}$slug",
            ticketUrl = document.hrefAt("#tickets a.btn-tix[href]")
        )
    }

    /**
     * Parses the doors time from the venue's `", Einlass 18:30 Uhr"` line, the only place a doors
     * time appears at all. Returns `null` when the page states none.
     */
    private fun parseDoorsTime(text: String?): LocalTime? = parseTime(DOORS_PATTERN.find(text.orEmpty())?.groupValues?.get(1))
}

/** Matches the venue's `"Einlass HH:mm Uhr"` doors line. */
private val DOORS_PATTERN = Regex("""Einlass\s+(\d{1,2}:\d{2})""", RegexOption.IGNORE_CASE)
