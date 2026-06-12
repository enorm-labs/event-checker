package de.norm.events.scraper.madameclaude

import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.imgSrcAt
import de.norm.events.scraper.parseShortDate
import de.norm.events.scraper.resolveUrl
import de.norm.events.scraper.textAt
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.time.Clock
import java.time.LocalDate

/**
 * Pure HTML parser for Madame Claude's WordPress-based event overview page.
 *
 * Madame Claude lists all upcoming events on `/events/` as a grid of cards.
 * Each card contains minimal data (date, title, category, image, link to
 * detail page). The overview scraper extracts just enough to build a
 * preliminary [ScrapedEvent] with the `sourceUrl` — the detail page scraper
 * provides the full enrichment (times, description, artists, price).
 *
 * Each event card is an `<article>` element containing:
 * - `.event-card` div with a CSS class indicating the category (e.g. `MusicQuiz`, `Live`, `Experimontag`)
 * - `.thumbnail a` with the detail page link and `img.screen` for the image
 * - `.title h4` with the date and title in `<font>` elements: `"DD/MM"` + `" Day - "` + title text
 * - `p.day-name` with category/subtitle text (e.g. "Experimontag / Concert")
 *
 * This class performs **no I/O** — it operates solely on a pre-fetched
 * Jsoup [Document], making it easy to test with static HTML fixtures.
 *
 * @see MadameClaudeDetailPageScraper for detail page parsing (primary data source).
 * @see MadameClaudeWebsiteImporter for the HTTP fetch orchestrator.
 * @see <a href="https://madameclaude.de/events/">Madame Claude Events</a>
 */
class MadameClaudeOverviewPageScraper(
    /** Clock used for year inference. Defaults to system clock; override in tests. */
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Parses all events from the Madame Claude events overview page.
     *
     * @param document the parsed Jsoup document of the events page.
     * @param baseUrl the URL the document was fetched from, used for resolving relative links.
     * @return a list of [ScrapedEvent] instances with basic data from the overview.
     */
    fun scrape(
        document: Document,
        baseUrl: String
    ): List<ScrapedEvent> {
        val articles = document.select("article.entry-card")
        logger.info { "Found ${articles.size} event card(s) on overview page" }

        @Suppress("TooGenericExceptionCaught") // Intentional: skip individual malformed events without aborting the entire import
        return articles.mapNotNull { article ->
            try {
                parseEventCard(article, baseUrl)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to parse event card, skipping" }
                null
            }
        }
    }

    /**
     * Parses a single event card `<article>` into a [ScrapedEvent].
     */
    @Suppress("ReturnCount") // Guard clauses for missing required fields
    private fun parseEventCard(
        article: Element,
        baseUrl: String
    ): ScrapedEvent? {
        val eventCard = article.selectFirst(".event-card") ?: return null

        // Detail page URL from the thumbnail link
        val detailHref = eventCard.selectFirst(".thumbnail a")?.attr("href")
        if (detailHref.isNullOrBlank()) {
            logger.warn { "Event card has no detail link, skipping" }
            return null
        }
        val sourceUrl = resolveUrl(baseUrl, detailHref)
        val slug = extractSlug(sourceUrl)

        // Title from the h4 element — strip the date/day prefix
        val titleElement = eventCard.selectFirst(".title h4")
        val title = extractTitle(titleElement)
        if (title.isNullOrBlank()) {
            logger.warn { "Event card has no title, skipping" }
            return null
        }

        // Date from the first <font> inside h4 (format: "DD/MM" or "DD/MM/YY")
        val dateText = titleElement?.selectFirst("font")?.text()?.trim()
        val eventDate = parseOverviewDate(dateText)
        if (eventDate == null) {
            logger.warn { "Could not parse date for '$title' from '$dateText', skipping" }
            return null
        }

        val imageUrl = eventCard.imgSrcAt("img.screen")

        // Category from the CSS class on .event-card (e.g. "MusicQuiz", "Live", "Experimontag")
        val category = extractCategoryFromClasses(eventCard)

        // Subtitle/category from p.day-name (e.g. "Experimontag / Concert")
        val dayNameText = eventCard.textAt("p.day-name")

        // Map category to event type
        val eventType = mapMadameClaudeCategory(category, dayNameText)

        return ScrapedEvent(
            title = title,
            eventType = eventType,
            eventDate = eventDate,
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            sourceId = "${EventSource.MADAME_CLAUDE.sourceIdPrefix}$slug"
        )
    }

    /**
     * Extracts the event title from the h4 element, stripping the date/day prefix fonts.
     *
     * The h4 contains: `<font>DD/MM</font> <font> Day - </font> Title Text`
     * We need to extract just the "Title Text" portion after the font elements.
     */
    private fun extractTitle(h4: Element?): String? {
        if (h4 == null) return null
        // Remove all <font> elements and get remaining text
        val clone = h4.clone()
        clone.select("font").remove()
        return clone.text().trim().takeIf { it.isNotBlank() }
    }

    /**
     * Parses date from overview format "DD/MM" or "DD/MM/YY".
     *
     * Full dates ("DD/MM/YY") delegate to [parseShortDate]. Date-only strings ("DD/MM")
     * infer the year: current year, or next year if the date has already passed.
     */
    @Suppress("ReturnCount", "MagicNumber", "TooGenericExceptionCaught")
    private fun parseOverviewDate(dateText: String?): LocalDate? {
        if (dateText.isNullOrBlank()) return null

        val parts = dateText.split("/")
        if (parts.size < 2) return null

        // Full date with year — delegate to shared parseShortDate
        if (parts.size >= 3 && parts[2].trim().isNotBlank()) return parseShortDate(dateText)

        // Date-only (DD/MM) — infer year from clock
        return try {
            val day = parts[0].trim().toInt()
            val month = parts[1].trim().toInt()
            val now = LocalDate.now(clock)
            val candidate = LocalDate.of(now.year, month, day)
            val year = if (candidate.isBefore(now)) now.year + 1 else now.year
            LocalDate.of(year, month, day)
        } catch (e: Exception) {
            logger.debug(e) { "Could not parse overview date: '$dateText'" }
            null
        }
    }

    /**
     * Extracts the category from the CSS classes on `.event-card`.
     *
     * The card has classes like "event-card bw card- MusicQuiz" — the last
     * class (not "event-card", "bw", or anything starting with "card-") is
     * the category. Using a prefix check on "card-" avoids picking up
     * future variants like "card-default" as the category.
     */
    private fun extractCategoryFromClasses(eventCard: Element): String? {
        val classes = eventCard.classNames()
        return classes.firstOrNull { it !in IGNORED_CLASSES && !it.startsWith("card-") && it.isNotBlank() }
    }

    companion object {
        /** CSS classes on .event-card that are not category identifiers. */
        private val IGNORED_CLASSES = setOf("event-card", "bw")
    }
}

/**
 * Maps Madame Claude category names to event type strings.
 *
 * Categories are derived from CSS classes (e.g. "Live", "MusicQuiz", "Experimontag")
 * and/or the `p.day-name` text (e.g. "Experimontag / Concert").
 */
@Suppress("ReturnCount") // Null guard + keyword check + when-branch are all distinct return paths
internal fun mapMadameClaudeCategory(
    cssCategory: String?,
    dayNameText: String?
): String? {
    if (cssCategory == null && dayNameText == null) return null

    // Check the day-name text for "Concert" keyword
    val lowerDayName = dayNameText?.lowercase() ?: ""
    if (lowerDayName.contains("concert")) return "CONCERT"

    // Unknown CSS categories return null so fillGapsFromOverview can fall back
    // to the other page's value via `?:`. Only return "OTHER" when the source
    // explicitly classifies the event as such.
    return when (cssCategory?.lowercase()) {
        "live", "experimontag", "openmic", "loveoriginals", "jeudifoster" -> "CONCERT"
        "musicquiz" -> "QUIZ"
        "freakyfriday", "habemussamstag" -> "PARTY"
        "other" -> "OTHER"
        else -> null
    }
}

/**
 * Extracts the event slug from a Madame Claude detail page URL.
 *
 * Example: `https://madameclaude.de/event/drekka-btong/` → `drekka-btong`
 *
 * @throws IllegalArgumentException if the URL path does not start with `/event/`.
 *   Catching this upstream is preferable to silently producing a malformed
 *   `sourceId` like `"madame_claude:/events/foo"`.
 */
internal fun extractSlug(url: String): String {
    val path = URI(url).path
    require(path.startsWith("/event/")) { "Unexpected Madame Claude detail URL: $url" }
    return path.removePrefix("/event/").trimEnd('/')
}
