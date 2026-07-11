package de.norm.events.scraper.madameclaude

import de.norm.events.event.EventType
import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedArtist
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.imgSrcAt
import de.norm.events.scraper.isNonArtistName
import de.norm.events.scraper.isScreeningTitle
import de.norm.events.scraper.mapEventType
import de.norm.events.scraper.parseShortDate
import de.norm.events.scraper.resolveUrl
import de.norm.events.scraper.splitSegmentOnConjunctions
import de.norm.events.scraper.stripArtistSuffix
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

        // A "(DJ-Set)" title marker denotes a DJ party — take precedence over the CSS
        // category (which labels these Experimontag/Live → CONCERT). See mapMadameClaudeCategory.
        val eventType =
            if (isDjSetTitle(title)) EventType.PARTY.name else mapMadameClaudeCategory(category, dayNameText, title)

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
 *
 * The "Concert" keyword in the day-name text takes precedence; otherwise the CSS
 * class is resolved via the shared [mapEventType] with Madame Claude's
 * venue-specific synonyms. When the category is unknown, an unambiguous screening
 * [title][isScreeningTitle] (e.g. "SHORTIES FILMS SCREENING #28") types the event
 * [SCREENING][EventType.SCREENING] rather than letting it fall to the `OTHER`
 * default. Failing all of these, returns `null` so `fillGapsFromOverview` can fall
 * back to the other page's value.
 */
@Suppress("ReturnCount") // Keyword check + delegated lookup + title net are distinct return paths
internal fun mapMadameClaudeCategory(
    cssCategory: String?,
    dayNameText: String?,
    title: String? = null
): String? {
    if (dayNameText?.lowercase()?.contains("concert") == true) return EventType.CONCERT.name
    mapEventType(cssCategory, MADAME_CLAUDE_CATEGORY_SYNONYMS)?.let { return it }
    if (title != null && isScreeningTitle(title)) return EventType.SCREENING.name
    return null
}

/** Matches the "(DJ-Set)" / "(DJ Set)" marker Madame Claude appends to a DJ-night title. */
private val DJ_SET_TITLE_MARKER = Regex("""\(\s*dj[\s-]?set\s*\)""", RegexOption.IGNORE_CASE)

/** "+" separator between co-billed DJs in a Madame Claude title (a "/" belongs to a single duo name). */
private val DJ_ACT_SEPARATOR = Regex("""\s*\+\s*""")

/**
 * Whether [title] carries Madame Claude's "(DJ-Set)" marker, identifying a DJ-set night.
 *
 * Used by both scrapers to type the event [PARTY][EventType.PARTY] and (on the detail
 * page) to source its lineup from the title rather than the sparse per-artist `<h3>`
 * headings those pages lack. Shared so the overview-only degraded path types the event
 * the same way the detail page would.
 */
internal fun isDjSetTitle(title: String): Boolean = DJ_SET_TITLE_MARKER.containsMatchIn(title)

/**
 * Derives the DJ lineup from a "(DJ-Set)" [title].
 *
 * DJ-set detail pages are sparse — no per-artist `<h3>`, so the h3 heuristic yields
 * nothing, a stray heading, or an unsplit co-bill. The title is the reliable source:
 * Madame Claude bills co-DJs with `+` and uses `/` **inside** a single act name
 * (`Morimoto / Wong duo`). So the "(DJ-Set)" suffix is stripped, acts are split on `+`
 * then on guarded `&`/`and`/`und` ([splitSegmentOnConjunctions]) — never on `/` —
 * per-act tour/format suffixes are stripped, and non-artists (placeholders, `Open Mic
 * L. J. Fox`, `DJ-Set / Berlin`) are dropped. All are role `DJ`, in billing order.
 *
 * Example: `"Lichene & Neue K (DJ-Set)"` → `[Lichene(DJ), Neue K(DJ)]`;
 * `"Matthew Ryals + Morimoto / Wong duo (DJ-Set)"` → `[Matthew Ryals(DJ), Morimoto / Wong duo(DJ)]`.
 */
internal fun djSetArtistsFromTitle(title: String): List<ScrapedArtist> =
    stripArtistSuffix(title)
        .split(DJ_ACT_SEPARATOR)
        .flatMap { splitSegmentOnConjunctions(it) }
        .map { stripArtistSuffix(it.trim()) }
        .filterNot { it.isBlank() || isNonArtistName(it) }
        .distinct()
        .map { ScrapedArtist(name = it, role = "DJ") }

/** Madame Claude's WordPress CSS class names mapped to [EventType] values. */
private val MADAME_CLAUDE_CATEGORY_SYNONYMS: Map<String, String> =
    mapOf(
        "live" to EventType.CONCERT.name,
        "experimontag" to EventType.CONCERT.name,
        "openmic" to EventType.CONCERT.name,
        "loveoriginals" to EventType.CONCERT.name,
        "jeudifoster" to EventType.CONCERT.name,
        "musicquiz" to EventType.QUIZ.name,
        "freakyfriday" to EventType.PARTY.name,
        "habemussamstag" to EventType.PARTY.name
    )

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
