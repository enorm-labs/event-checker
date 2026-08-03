package de.norm.events.scraper.uberarena

import de.norm.events.event.EventType
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import de.norm.events.scraper.buildArtistsForEventType
import de.norm.events.scraper.cleanEventTitle
import de.norm.events.scraper.extractEventSlug
import de.norm.events.scraper.imgSrcAt
import de.norm.events.scraper.mapEventType
import de.norm.events.scraper.parseGermanDate
import de.norm.events.scraper.parsePriceValue
import de.norm.events.scraper.parseTime
import de.norm.events.scraper.resolveUrl
import de.norm.events.scraper.textAt
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.LocalDate

/**
 * Pure HTML parser for Uber Arena's `/events/all` listing page.
 *
 * The page renders the whole programme server-side and unpaginated as `div[data-categoryname]`
 * rows. Each carries the venue's own category, a `.m-date__*` span group (weekday, day, month,
 * year and an `HH:mm Uhr` start), an `.event-title`, an `ab NN,NN €` from-price, a thumbnail, and
 * a link to `/events/detail/<slug>/<YYYY-MM-DD-HHMM>` — whose trailing path segment is what makes
 * the `sourceId` unique per performance, since a tournament reuses one slug across many dates.
 *
 * **Sport rows are dropped** ([SPORT_CATEGORIES]) rather than stored as `OTHER`; see the
 * [EventSource.UBER_ARENA] KDoc for why.
 *
 * This class performs **no I/O** — it operates solely on a pre-fetched Jsoup [Document],
 * making it easy to test with a static fixture.
 *
 * @see UberArenaDetailPageScraper for the detail-page data (doors, description, ticket link).
 * @see UberArenaWebsiteImporter for the HTTP fetch orchestrator.
 * @see <a href="https://www.uber-arena.de/events/all">Uber Arena programme</a>
 */
class UberArenaOverviewPageScraper {
    private val logger = KotlinLogging.logger {}

    /**
     * Parses every non-sport row from the listing page document.
     *
     * @param document the parsed Jsoup document of the `/events/all` page.
     * @param baseUrl the URL the document was fetched from, used to resolve the per-event
     *   detail links.
     * @return a list of [ScrapedEvent] instances, one per imported row.
     */
    fun scrape(
        document: Document,
        baseUrl: String
    ): List<ScrapedEvent> {
        val rows = document.select("div[data-categoryname]")
        val (sport, ours) = rows.partition { it.attr("data-categoryname").lowercase() in SPORT_CATEGORIES }
        logger.info { "Found ${ours.size} event row(s) on Uber Arena overview, skipping ${sport.size} sport fixture(s)" }

        @Suppress("TooGenericExceptionCaught") // Intentional: skip individual malformed rows without aborting the import
        return ours.mapNotNull { row ->
            try {
                parseRow(row, baseUrl)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse Uber Arena event row, skipping" }
                null
            }
        }
    }

    /** Parses a single listing row into a [ScrapedEvent], or `null` when it has no link or title. */
    @Suppress("ReturnCount") // Guard clauses for the required href/title are clearer than nesting
    private fun parseRow(
        row: Element,
        baseUrl: String
    ): ScrapedEvent? {
        val href = row.selectFirst("h3.event-title a[href]")?.attr("href")?.takeIf { it.isNotBlank() } ?: return null
        val sourceUrl = resolveUrl(baseUrl, href)
        // The slug keeps its trailing "/YYYY-MM-DD-HHMM" segment: one production runs many dates.
        val slug = extractEventSlug(sourceUrl, "/events/detail/")

        val title = row.textAt("h3.event-title")?.let { cleanEventTitle(it) }
        if (title.isNullOrBlank()) {
            logger.warn { "Uber Arena row '$slug' has no title, skipping" }
            return null
        }
        val category = row.attr("data-categoryname").takeIf { it.isNotBlank() }
        val eventType = mapEventType(category, EXTRA_CATEGORY_SYNONYMS)
        val price = row.textAt(".buttons .price")

        return ScrapedEvent(
            title = title,
            eventType = eventType,
            eventDate = parseRenderedDate(row) ?: UNRESOLVED_EVENT_DATE,
            startTime = parseTime(row.textAt(".m-date__hour")?.substringBefore(CLOCK_SUFFIX)?.trim()),
            imageUrl = row.imgSrcAt(".thumb img"),
            sourceUrl = sourceUrl,
            sourceId = "${EventSource.UBER_ARENA.sourceIdPrefix}$slug",
            // The venue quotes a cheapest-ticket price ("ab 83,50 €"); the note keeps the "from".
            pricePresale = parsePriceValue(price),
            priceNote = price,
            artists = buildArtistsForEventType(title, subtitle = null, eventType = eventType)
        )
    }

    /**
     * Assembles the date from the row's `.m-date__day` / `.m-date__month` / `.m-date__year` spans,
     * each of which carries its own trailing punctuation (`21.`, `08.`, `2026,`). Returns `null`
     * when any part is missing or the combination is not a real date.
     */
    private fun parseRenderedDate(row: Element): LocalDate? {
        val day = row.textAt(".m-date__day")?.trim('.', ' ')
        val month = row.textAt(".m-date__month")?.trim('.', ' ')
        val year = row.textAt(".m-date__year")?.trim(',', ' ')
        if (day == null || month == null || year == null) return null
        return parseGermanDate("$day.$month.$year")
    }
}

/**
 * The venue's own category names for sport fixtures, dropped rather than imported. The arena is
 * home to ALBA Berlin (basketball) and the Eisbären (ice hockey), which together outnumber every
 * other format on some months.
 */
private val SPORT_CATEGORIES = setOf("eishockey", "basketball", "sport")

/** Venue category labels the shared table does not carry. A comedy night is a staged show. */
private val EXTRA_CATEGORY_SYNONYMS = mapOf("comedy" to EventType.SHOW.name)

/** The trailing `Uhr` the venue appends to its start time. */
private const val CLOCK_SUFFIX = "Uhr"
