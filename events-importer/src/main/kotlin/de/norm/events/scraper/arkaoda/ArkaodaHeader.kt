package de.norm.events.scraper.arkaoda

import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * The `<b>` run heading an arkaoda event block: the event date and the venue's
 * optional category label.
 *
 * The listing and the detail page render this run identically, so both scrapers read
 * it through [parseArkaodaHeader]. The remaining field rules they share live in
 * [ArkaodaFieldMapping.kt][arkaodaEventType].
 */
data class ArkaodaHeader(
    /** The `DD / MM / YYYY` date, or `null` when the block carries none (a placeholder/unpublished event). */
    val eventDate: LocalDate?,
    /** The `// <label>` category, or `null` when the block is unlabelled — see [arkaodaEventType]. */
    val category: String?
)

/**
 * Reads the [ArkaodaHeader] from an event block's `.excerpt` container.
 *
 * The header is a run of sibling `<b>` elements — date, German weekday, and an
 * optional `// <category>` — with no per-field markup to select on, so each label is
 * classified by its own content rather than its position: the one that parses as a
 * date is the date, the one opening with `//` is the category, and the weekday is
 * ignored (the date already carries a four-digit year, so it needs no
 * disambiguation). A block that gained or lost a label therefore still parses.
 */
fun parseArkaodaHeader(excerpt: Element): ArkaodaHeader {
    val labels = excerpt.select("b").map { it.text().trim() }
    return ArkaodaHeader(
        eventDate = labels.firstNotNullOfOrNull { parseArkaodaDate(it) },
        category = labels.firstNotNullOfOrNull { parseCategoryLabel(it) }
    )
}

/**
 * Parses arkaoda's spaced `DD / MM / YYYY` date (e.g. `"30 / 07 / 2026"`).
 *
 * Whitespace is removed before parsing, so the venue's padded rendering and a
 * hypothetical unpadded one both work. Returns `null` for any label that is not a
 * date (the weekday and category labels run through here too) rather than throwing.
 */
fun parseArkaodaDate(text: String?): LocalDate? {
    if (text.isNullOrBlank()) return null
    return try {
        LocalDate.parse(text.filterNot { it.isWhitespace() }, DATE_FORMATTER)
    } catch (_: DateTimeParseException) {
        null
    }
}

/** Reads a `// <label>` category label, or `null` when the text is not one. */
private fun parseCategoryLabel(label: String): String? =
    CATEGORY_LABEL
        .find(label)
        ?.groupValues
        ?.get(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }

/** arkaoda's spaced date rendering, once whitespace is removed: `30/07/2026`. */
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")

/** A category label, rendered as `// <label>` in its own `<b>` (e.g. `"// Konser"`). */
private val CATEGORY_LABEL = Regex("""^//\s*(.+)$""")
