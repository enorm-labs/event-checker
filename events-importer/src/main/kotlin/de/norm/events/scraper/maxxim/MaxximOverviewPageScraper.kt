package de.norm.events.scraper.maxxim

import de.norm.events.event.EventStatus
import de.norm.events.event.EventType
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.WixEventsWarmupData
import de.norm.events.scraper.cleanEventTitle
import de.norm.events.scraper.parseWixSchedule
import de.norm.events.scraper.resolveUrl
import de.norm.events.scraper.stringOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import tools.jackson.databind.JsonNode
import java.math.BigDecimal

/**
 * Pure parser for MAXXIM's Wix Events programme page (`/partys`).
 *
 * Every field comes from the embedded `wix-warmup-data` JSON (see
 * [WixEventsWarmupData]) — the rendered cards are never read. Unlike Loge, whose
 * warmup payload omits the price, MAXXIM's `registration.ticketing` block carries
 * the ticket price and the sold-out flag, so the single overview fetch is
 * complete: no `/event-details/<slug>` page is fetched (its `<slug>` is still
 * used for the canonical [ScrapedEvent.sourceUrl] and the stable
 * [ScrapedEvent.sourceId]).
 *
 * This class performs **no I/O** — it operates solely on a pre-fetched Jsoup
 * [Document], making it easy to test with a static fixture.
 *
 * @see MaxximWebsiteImporter for the HTTP fetch orchestrator.
 * @see <a href="https://www.maxxim-berlin.de/partys">MAXXIM programme</a>
 */
class MaxximOverviewPageScraper {
    private val logger = KotlinLogging.logger {}

    /**
     * Parses all events from the programme page's embedded Wix warmup payload.
     *
     * @param document the parsed Jsoup document of the `/partys` page.
     * @param baseUrl the URL the document was fetched from, used to resolve the
     *   per-event `/event-details/<slug>` URLs.
     * @return a list of [ScrapedEvent] instances, one per listed night.
     */
    fun scrape(
        document: Document,
        baseUrl: String
    ): List<ScrapedEvent> {
        val events = WixEventsWarmupData.events(document, EventSource.MAXXIM) ?: return emptyList()
        logger.info { "Found ${events.size()} event(s) in MAXXIM Wix warmup payload" }

        @Suppress("TooGenericExceptionCaught") // Intentional: skip individual malformed events without aborting the import
        return events.mapNotNull { node ->
            try {
                parseEvent(node, baseUrl)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse MAXXIM event, skipping" }
                null
            }
        }
    }

    @Suppress("ReturnCount") // Guard clauses for the required slug, title and date are clearer than nesting
    private fun parseEvent(
        node: JsonNode,
        baseUrl: String
    ): ScrapedEvent? {
        val slug = node.stringOrNull("slug")
        if (slug == null) {
            logger.warn { "MAXXIM event has no slug, skipping" }
            return null
        }
        val title = node.stringOrNull("title")?.let { cleanEventTitle(it) }
        if (title.isNullOrBlank()) {
            logger.warn { "MAXXIM event '$slug' has no title, skipping" }
            return null
        }
        // Unlike Loge there is no detail page to recover a to-be-decided date from, so an event
        // without a resolvable startDate is dropped rather than persisted with a sentinel date.
        val (eventDate, startTime) = parseWixSchedule(node.path("scheduling").path("config"))
        if (eventDate == null) {
            logger.warn { "MAXXIM event '$slug' has no parseable start date, skipping" }
            return null
        }

        val ticketing = node.path("registration").path("ticketing")
        return ScrapedEvent(
            title = title,
            description = node.stringOrNull("description"),
            // MAXXIM publishes no categories: it is a club whose every night is a DJ dance party.
            eventType = EventType.PARTY.name,
            eventDate = eventDate,
            startTime = startTime,
            imageUrl = node.path("mainImage").stringOrNull("url"),
            sourceUrl = resolveUrl(baseUrl, "/event-details/$slug"),
            sourceId = "${EventSource.MAXXIM.sourceIdPrefix}$slug",
            pricePresale = parseTicketPrice(ticketing.path("lowestTicketPrice")),
            priceNote = parsePriceRangeNote(ticketing),
            soldOut = ticketing.path("soldOut").asBoolean(false),
            status = mapWixEventStatus(node.path("status"))
        )
    }

    /**
     * Reads a Wix ticket price node (`{"amount": "12.00", "currency": "EUR"}`).
     * Returns `null` when the amount is absent or not a number — a free or
     * registration-only night carries no ticketing block at all.
     */
    private fun parseTicketPrice(price: JsonNode): BigDecimal? = price.stringOrNull("amount")?.toBigDecimalOrNull()

    /**
     * Builds a price note only when a night has several ticket tiers, i.e. when
     * the lowest and highest formatted prices differ (`"€10 – €25"`). A single-tier
     * night — the normal case — needs no note: [ScrapedEvent.pricePresale] already
     * says everything.
     */
    private fun parsePriceRangeNote(ticketing: JsonNode): String? {
        val lowest = ticketing.stringOrNull("lowestTicketPriceFormatted")
        val highest = ticketing.stringOrNull("highestTicketPriceFormatted")
        return if (lowest != null && highest != null && lowest != highest) "$lowest – $highest" else null
    }
}

/**
 * Maps Wix's numeric event `status` to a domain [EventStatus] name.
 *
 * Wix serialises its `EventStatus` enum as an ordinal in the warmup payload:
 * `0` SCHEDULED, `1` STARTED, `2` ENDED, `3` CANCELED, `4` DRAFT. Only the
 * cancellation is meaningful here — a started/ended night is simply in the past,
 * and a draft is never published to the widget — so everything else keeps the
 * `SCHEDULED` default rather than inventing statuses the model has no place for.
 */
internal fun mapWixEventStatus(status: JsonNode): String =
    if (status.asInt(WIX_STATUS_SCHEDULED) == WIX_STATUS_CANCELED) EventStatus.CANCELLED.name else EventStatus.SCHEDULED.name

private const val WIX_STATUS_SCHEDULED = 0
private const val WIX_STATUS_CANCELED = 3
