package de.norm.events.scraper.madameclaude

import de.norm.events.scraper.EventSource
import de.norm.events.scraper.ScrapedArtist
import de.norm.events.scraper.ScrapedEvent
import de.norm.events.scraper.parseShortDate
import de.norm.events.scraper.parseTime
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pure HTML parser for Madame Claude's event detail pages.
 *
 * Detail pages provide the **primary** data source for each event, with
 * rich content including:
 * - Date in `DD/MM/YY` format
 * - Category label (e.g. "Experimontag", "Music Quiz", "Concert")
 * - Doors and start times (e.g. "Doors 19:00 / Start 20:00")
 * - Price note (e.g. "Donation", "5€")
 * - Per-artist descriptions with `<h3>` headings
 * - Artist genre + origin (first line after each h3, e.g. "ambient cinematic soundscapes / Basel, Switzerland")
 * - Artist links (Bandcamp, Instagram, YouTube, etc.)
 *
 * The detail page HTML structure (inside `.container-info-single-event .primary-info-single-event`):
 * ```
 * .date > p.numbers > font (date: "21/09/26")
 * .date > p.days > font (category: "Experimontag")
 * h2 (title)
 * .info > p (times/price line: "Doors 19:00 / Start 20:00 / Donation")
 * .info > p > h3 (artist name)
 * .info > p (artist description paragraphs + links)
 * ```
 *
 * This class performs **no I/O** — it operates solely on a pre-fetched
 * Jsoup [Document], making it easy to test with static HTML fixtures.
 *
 * @see MadameClaudeOverviewPageScraper for overview page parsing (discovery).
 * @see MadameClaudeWebsiteImporter for the HTTP fetch orchestrator.
 * @see <a href="https://madameclaude.de/event/drekka-btong-zimmermann-lienhard/">Example detail page</a>
 */
class MadameClaudeDetailPageScraper {
    private val logger = KotlinLogging.logger {}

    /**
     * Parses the event detail page into a [ScrapedEvent].
     *
     * @param document the parsed Jsoup document of the detail page.
     * @param sourceUrl the canonical URL of the detail page.
     * @return a [ScrapedEvent] with all available data, or null if the page cannot be parsed.
     */
    @Suppress("ReturnCount") // Guard clauses for missing required fields (primaryInfo, title)
    fun scrape(
        document: Document,
        sourceUrl: String
    ): ScrapedEvent? {
        val primaryInfo = document.selectFirst(".primary-info-single-event")
        if (primaryInfo == null) {
            logger.warn { "No .primary-info-single-event found on detail page: $sourceUrl" }
            return null
        }

        // Title from h2
        val title = primaryInfo.selectFirst("h2")?.text()?.trim()
        if (title.isNullOrBlank()) {
            logger.warn { "No title (h2) found on detail page: $sourceUrl" }
            return null
        }

        // Date from .date p.numbers font (format: "DD/MM/YY")
        val dateText = primaryInfo.selectFirst(".date p.numbers font")?.text()?.trim()
        val eventDate = parseShortDate(dateText)

        // Category from .date p.days font or p.jumpe font
        val categoryText =
            primaryInfo.selectFirst(".date p.days font")?.text()?.trim()
                ?: primaryInfo.selectFirst(".date p.jumpe font")?.text()?.trim()
        val eventType = mapMadameClaudeCategory(categoryText, categoryText)

        // Times and price from the .info section
        val infoSection = primaryInfo.selectFirst(".info")
        val (doorsTime, startTime, priceNote) = parseTimesAndPrice(infoSection)

        // Description and artists from the content area
        val (description, artists) = parseArtistsAndDescription(infoSection)

        val slug = extractSlug(sourceUrl)

        return ScrapedEvent(
            title = title,
            eventType = eventType,
            eventDate = eventDate ?: LocalDate.MIN, // Caller should fill from overview if null
            doorsTime = doorsTime,
            startTime = startTime,
            sourceUrl = sourceUrl,
            sourceId = "${EventSource.MADAME_CLAUDE.sourceIdPrefix}$slug",
            priceNote = priceNote,
            description = description,
            artists = artists
        )
    }

    /**
     * Parses times and price from the info section.
     *
     * The first `<p>` in `.info` typically contains the times/price line:
     * "Doors 19:00 / Start 20:00 / Donation"
     * "Doors 19:00 / Start 20:00 / 5€"
     */
    private fun parseTimesAndPrice(infoSection: Element?): Triple<LocalTime?, LocalTime?, String?> {
        if (infoSection == null) return Triple(null, null, null)

        // Find the first paragraph that contains time info
        val timeParagraph =
            infoSection.select("p").firstOrNull { p ->
                val text = p.ownText().ifBlank { p.text() }
                text.contains("Doors", ignoreCase = true) || text.contains("Start", ignoreCase = true)
            }

        val text = timeParagraph?.ownText()?.ifBlank { timeParagraph.text() } ?: ""

        val doorsTime =
            DOORS_PATTERN
                .find(text)
                ?.groupValues
                ?.get(1)
                ?.let { parseTime(it) }
        val startTime =
            START_PATTERN
                .find(text)
                ?.groupValues
                ?.get(1)
                ?.let { parseTime(it) }

        // Price note: everything after the last time, trimmed of separators
        val priceNote = extractPriceNote(text)

        return Triple(doorsTime, startTime, priceNote)
    }

    /**
     * Extracts price note from the times/price line.
     *
     * Splits on "/" and takes the last segment that isn't a time or "Doors"/"Start" prefix.
     * Examples:
     * - "Doors 19:00 / Start 20:00 / Donation" → "Donation"
     * - "Doors 19:00 / Start 20:00 / 5€" → "5€"
     * - "Doors 19:00 / Start 20:00" → null
     */
    private fun extractPriceNote(text: String): String? {
        val segments = text.split("/").map { it.trim() }
        val pricePart =
            segments.lastOrNull { segment ->
                !segment.contains("Doors", ignoreCase = true) &&
                    !segment.contains("Start", ignoreCase = true) &&
                    !TIME_ONLY_PATTERN.containsMatchIn(segment) &&
                    segment.isNotBlank()
            }
        return pricePart?.trim()?.takeIf { it.isNotBlank() }
    }

    /**
     * Parses artist information and description from the detail page content.
     *
     * Artists are identified by `<h3>` headings within the info/content area.
     * Each artist section typically contains:
     * - h3: artist name
     * - First p after h3: genre/origin (e.g. "ambient cinematic soundscapes / Basel, Switzerland")
     * - Following p elements: artist bio and links
     *
     * @return a pair of (combined description text, list of scraped artists)
     */
    @Suppress("ReturnCount") // Guard clause for null section + early return for no h3 headings
    private fun parseArtistsAndDescription(infoSection: Element?): Pair<String?, List<ScrapedArtist>> {
        if (infoSection == null) return null to emptyList()

        val h3Elements = infoSection.select("h3")
        if (h3Elements.isEmpty()) return buildPlainDescription(infoSection) to emptyList()

        val artists = mutableListOf<ScrapedArtist>()
        val descriptionParts = mutableListOf<String>()

        for ((index, h3) in h3Elements.withIndex()) {
            val artistName = h3.text().trim()
            if (artistName.isBlank()) continue

            // First artist is headliner, rest are support
            val role = if (index == 0) "HEADLINER" else "SUPPORT"
            artists.add(ScrapedArtist(name = artistName, role = role))

            // Collect description paragraphs between this h3 and the next h3
            val artistDesc = collectArtistDescription(h3)
            if (artistDesc.isNotBlank()) {
                descriptionParts.add("$artistName: $artistDesc")
            }
        }

        val description = descriptionParts.joinToString("\n\n").takeIf { it.isNotBlank() }
        return description to artists
    }

    /**
     * Collects description text from paragraphs following an h3 element
     * until the next h3 or end of content.
     */
    @Suppress("ReturnCount") // Two early returns for the two distinct traversal strategies
    private fun collectArtistDescription(h3: Element): String {
        // Strategy 1: h3 as a direct child — walk its element siblings until the next h3
        val directParts =
            generateSequence(h3.nextElementSibling()) { it.nextElementSibling() }
                .takeWhile { it.tagName() != "h3" }
                .filter { it.text().isNotBlank() && !isLinkOnlyParagraph(it) }
                .map { it.text().trim() }
                .toList()
        if (directParts.isNotEmpty()) return directParts.joinToString("\n")

        // Strategy 2: h3 wrapped in a <p> — walk following <p> siblings of the parent
        val parentP = h3.parent()?.takeIf { it.tagName() == "p" } ?: return ""
        return generateSequence(parentP.nextElementSibling()) { it.nextElementSibling() }
            .takeWhile { it.tagName() == "p" && it.selectFirst("h3") == null }
            .filter { it.text().isNotBlank() && !isLinkOnlyParagraph(it) }
            .map { it.text().trim() }
            .joinToString("\n")
    }

    /**
     * Checks if a paragraph contains only links (URLs) and no meaningful prose.
     */
    private fun isLinkOnlyParagraph(element: Element): Boolean {
        val text = element.text().trim()
        // If all content is URLs, consider it link-only.
        // Match the scheme strictly to avoid skipping prose starting with words like "httpd".
        return text.isNotBlank() &&
            text.split(Regex("\\s+")).all { it.startsWith("http://") || it.startsWith("https://") }
    }

    /**
     * Builds a plain description when no h3 artist headings are present.
     */
    private fun buildPlainDescription(infoSection: Element): String? {
        val paragraphs =
            infoSection
                .select("p")
                .map { it.text().trim() }
                .filter { it.isNotBlank() }
                // Skip the times/price line
                .filter { !it.contains("Doors", ignoreCase = true) && !it.contains("Start", ignoreCase = true) }

        return paragraphs.joinToString("\n").takeIf { it.isNotBlank() }
    }

    companion object {
        /** Regex to extract doors time from "Doors 19:00" pattern. */
        private val DOORS_PATTERN = Regex("""[Dd]oors\s+(\d{1,2}:\d{2})""")

        /** Regex to extract start time from "Start 20:00" pattern. */
        private val START_PATTERN = Regex("""[Ss]tart\s+(\d{1,2}:\d{2})""")

        /** Regex to match a standalone time value. */
        private val TIME_ONLY_PATTERN = Regex("""\d{1,2}:\d{2}""")
    }
}
