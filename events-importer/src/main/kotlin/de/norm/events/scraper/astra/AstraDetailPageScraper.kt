package de.norm.events.scraper.astra

import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.UNRESOLVED_EVENT_DATE
import de.norm.events.scraper.hrefAt
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.math.BigDecimal

/**
 * Pure HTML parser for Astra Kulturhaus event detail pages.
 *
 * The detail page is the **primary data source** for each event. It reuses the
 * same `.event__*` header markup as the overview (parsed via
 * [parseAstraEventBlock]) and adds the fields that only the detail page carries:
 * - promoter(s) (`.promoters__link`)
 * - presale / box-office prices (`.prices .price`)
 * - ticket shop URL (`.purchase-option__button`)
 * - description / artist bio (`.gig__description`, `.detail__description`)
 *
 * The detail page does not render the artist roster, so `artists` is left for
 * the overview page to supply via [AstraWebsiteImporter.fillGapsFromOverview].
 * It may render a `kind` label, but that is the raw per-day value (uncorrected
 * by the overview's festival-day normalization), so the overview type wins in
 * the merge and the detail value is only a fallback.
 *
 * This class performs **no I/O** — it operates solely on a pre-fetched
 * Jsoup [Document], making it easy to test with static HTML fixtures.
 *
 * @see AstraOverviewPageScraper for overview parsing (event type, discovery).
 * @see AstraWebsiteImporter for the HTTP fetch orchestrator.
 * @see <a href="https://www.astra-berlin.de/events/2026-05-18-green-lung">Example detail page</a>
 */
class AstraDetailPageScraper {
    private val logger = KotlinLogging.logger {}

    /**
     * Parses an event detail page into a [ScrapedEvent].
     *
     * Scopes parsing to the `main.page-content` container, which holds the
     * single event. Returns `null` if the container or the event title is
     * missing (an unexpected page structure).
     *
     * @param document the parsed Jsoup document of the detail page.
     * @param sourceUrl the event's URL, used as [ScrapedEvent.sourceUrl] and to
     *   derive the [ScrapedEvent.sourceId].
     */
    @Suppress("ReturnCount") // Guard clauses for missing container and title are clearer than nesting
    fun scrape(
        document: Document,
        sourceUrl: String
    ): ScrapedEvent? {
        val content = document.selectFirst("main.page-content") ?: document.body()
        val block = parseAstraEventBlock(content, sourceUrl)
        if (block == null) {
            logger.warn { "Detail page at $sourceUrl has no event title, skipping" }
            return null
        }

        val (pricePresale, priceBoxOffice) = parsePrices(content)

        return ScrapedEvent(
            title = block.title,
            subtitle = block.subtitle,
            description = parseDescription(content),
            eventType = block.eventType,
            // Detail pages always carry the real date; sentinel only if absent
            // (then the overview value is used via fillGapsFromOverview).
            eventDate = block.eventDate ?: UNRESOLVED_EVENT_DATE,
            doorsTime = block.doorsTime,
            startTime = block.startTime,
            imageUrl = block.imageUrl,
            sourceUrl = sourceUrl,
            sourceId = "${EventSource.ASTRA.sourceIdPrefix}${extractAstraSlug(sourceUrl)}",
            ticketUrl = content.hrefAt(".purchase-option__button"),
            pricePresale = pricePresale,
            priceBoxOffice = priceBoxOffice,
            soldOut = block.soldOut,
            status = block.status,
            promoters = parsePromoters(content)
        )
    }

    /**
     * Extracts promoter names from the `.promoters__link` anchors, deduplicated
     * while preserving order (the markup repeats them for mobile/desktop layouts).
     */
    private fun parsePromoters(content: Element): List<String> =
        content
            .select(".promoters__link")
            .mapNotNull { it.text().trim().takeIf { name -> name.isNotBlank() } }
            .distinct()

    /**
     * Extracts the event description from the artist bio and detail sections.
     *
     * Paragraphs are gathered from `.gig__description` (per-artist bios) and
     * `.detail__description` (event blurb), deduplicated while preserving order
     * since both layouts can repeat content. Returns `null` when no prose exists.
     */
    private fun parseDescription(content: Element): String? =
        content
            .select(".gig__description p, .detail__description p")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
            .takeIf { it.isNotBlank() }

    /**
     * Parses presale and box-office prices from the `.prices` section.
     *
     * Each `.price` carries a `.price__value` (e.g. "39,90€" or "35.20€") and a
     * `.price__label`. Labels containing "Abendkasse" or the standalone "AK" token
     * map to the box-office price; everything else (Vorverkauf / "VVK …") maps to
     * presale. The first value seen for each category wins, so the duplicate price
     * blocks the markup renders for mobile/desktop collapse to a single value.
     *
     * @return a pair of (presale, boxOffice), either of which may be `null`.
     */
    private fun parsePrices(content: Element): Pair<BigDecimal?, BigDecimal?> {
        var presale: BigDecimal? = null
        var boxOffice: BigDecimal? = null

        for (price in content.select(".prices .price")) {
            val value = parsePrice(price.selectFirst(".price__value")?.text()) ?: continue
            val label =
                price
                    .selectFirst(".price__label")
                    ?.text()
                    ?.lowercase()
                    .orEmpty()
            val isBoxOffice = label.contains("abendkasse") || AK_LABEL_PATTERN.containsMatchIn(label)
            if (isBoxOffice) {
                boxOffice = boxOffice ?: value
            } else {
                presale = presale ?: value
            }
        }

        return presale to boxOffice
    }

    /**
     * Parses the first monetary value from a price string, accepting both German
     * (`39,90€`) and dot (`35.20€`) decimal separators. Returns `null` when no
     * value is found.
     */
    @Suppress("ReturnCount") // Guard clauses for blank input and unparseable value are clearer than nesting
    private fun parsePrice(text: String?): BigDecimal? {
        if (text.isNullOrBlank()) return null
        val match = PRICE_PATTERN.find(text) ?: return null
        return try {
            BigDecimal(match.groupValues[1].replace(",", "."))
        } catch (_: NumberFormatException) {
            null
        }
    }

    companion object {
        /** Matches a price value with an optional decimal part, e.g. "39,90€", "35.20€", "53€". */
        private val PRICE_PATTERN = Regex("""(\d+(?:[.,]\d{1,2})?)\s*€""")

        /** Matches the standalone "AK" (Abendkasse) token, so labels like "VVK" don't false-match on the letters. */
        private val AK_LABEL_PATTERN = Regex("""\bak\b""")
    }
}
