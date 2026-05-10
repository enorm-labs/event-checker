package de.norm.events.scraper

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element
import java.net.URI
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}

/**
 * Extracts trimmed text content from the first child element matching [cssQuery].
 *
 * Returns `null` if no element matches or the text is blank.
 * This is the most common extraction pattern in scrapers — wraps the
 * verbose `selectFirst(...)?.text()?.trim()?.takeIf { ... }` chain.
 *
 * Example:
 * ```kotlin
 * val title = content.textAt("h1.event-title")
 * val genre = item.textAt("[fs-cmsfilter-field=genre]")
 * ```
 */
fun Element.textAt(cssQuery: String): String? =
    selectFirst(cssQuery)
        ?.text()
        ?.trim()
        ?.takeIf { it.isNotBlank() }

/**
 * Extracts an attribute value from the first child element matching [cssQuery].
 *
 * Returns `null` if no element matches or the attribute value is blank.
 *
 * Example:
 * ```kotlin
 * val href = content.attrAt("a.ticket-link", "href")
 * val style = item.attrAt(".image-wrapper", "style")
 * ```
 */
fun Element.attrAt(
    cssQuery: String,
    attributeKey: String
): String? =
    selectFirst(cssQuery)
        ?.attr(attributeKey)
        ?.takeIf { it.isNotBlank() }

/**
 * Extracts an absolute image URL from the `src` attribute of the first
 * `<img>` element matching [cssQuery].
 *
 * Returns `null` if no element matches, the `src` is blank, or the URL
 * is not absolute (does not start with `http`). This filters out
 * placeholder values like empty strings or relative paths.
 *
 * Example:
 * ```kotlin
 * val imageUrl = content.imgSrcAt("img.eventpage-image")
 * ```
 */
fun Element.imgSrcAt(cssQuery: String): String? =
    attrAt(cssQuery, "src")
        ?.takeIf { it.startsWith("http") }

/**
 * Extracts an absolute URL from the `href` attribute of the first
 * anchor element matching [cssQuery].
 *
 * Returns `null` if no element matches, the `href` is blank, or the URL
 * is not absolute (does not start with `http`).
 *
 * Example:
 * ```kotlin
 * val ticketUrl = content.hrefAt("a.ticket-link")
 * ```
 */
fun Element.hrefAt(cssQuery: String): String? =
    attrAt(cssQuery, "href")
        ?.takeIf { it.startsWith("http") }

/**
 * Checks whether any child element matching [cssQuery] is visible in the
 * Webflow CMS conditional visibility system and contains the given [text].
 *
 * Webflow CMS uses the `w-condition-invisible` class to hide elements
 * that don't meet a CMS condition. This is commonly used for status flags
 * (e.g. "Sold-Out", "Cancelled") that are always present in the DOM but
 * conditionally shown based on a CMS toggle field.
 *
 * A flag is considered "visible" if it does **not** have the
 * `w-condition-invisible` class and its text content contains [text]
 * (case-insensitive).
 *
 * Example:
 * ```kotlin
 * val isSoldOut = item.hasVisibleWebflowFlag(".event-detail.sold-out", "Sold-Out")
 * val isCancelled = item.hasVisibleWebflowFlag(".event-detail.sold-out", "Cancelled")
 * ```
 */
fun Element.hasVisibleWebflowFlag(
    cssQuery: String,
    text: String
): Boolean =
    select(cssQuery)
        .any { !it.hasClass("w-condition-invisible") && it.text().contains(text, ignoreCase = true) }

/** Standard 24-hour time format (HH:mm) used by most Berlin venue websites. */
val HH_MM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Attempts to parse [text] as a [LocalTime] using the given [formatter].
 *
 * Returns `null` if [text] is null, blank, or cannot be parsed — rather
 * than throwing an exception. This is the expected behavior for scrapers
 * where missing or malformed time values should degrade gracefully.
 *
 * Example:
 * ```kotlin
 * val doorsTime = parseTime("19:00", HH_MM_FORMATTER)  // LocalTime.of(19, 0)
 * val invalid = parseTime("TBA", HH_MM_FORMATTER)      // null
 * ```
 */
fun parseTime(
    text: String?,
    formatter: DateTimeFormatter = HH_MM_FORMATTER
): LocalTime? {
    if (text.isNullOrBlank()) return null
    return try {
        LocalTime.parse(text.trim(), formatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Maps common German event category names to [de.norm.events.event.EventType]
 * enum value strings.
 *
 * Berlin venue websites predominantly use German category labels ("Konzert",
 * "Party", "Sonstiges"). This shared mapping avoids duplicating the same
 * `when` expression in every venue-specific scraper.
 *
 * Returns `"OTHER"` for unknown or null categories.
 *
 * Example:
 * ```kotlin
 * mapGermanCategory("Konzert")   // "CONCERT"
 * mapGermanCategory("party")     // "PARTY" (case-insensitive)
 * mapGermanCategory("Workshop")  // "OTHER"
 * ```
 */
fun mapGermanCategory(category: String?): String =
    when (category?.trim()?.lowercase()) {
        "konzert" -> "CONCERT"
        "party" -> "PARTY"
        "sonstiges" -> "OTHER"
        null, "" -> "OTHER"
        else -> "OTHER".also { logger.debug { "Unknown German category: '$category', defaulting to OTHER" } }
    }

/**
 * Common placeholder names used by venues when the artist has not been
 * announced yet (e.g. "TBA", "TBD", "N.N."). These should not be
 * created as artist entries in the database.
 *
 * Comparison is case-insensitive and ignores surrounding whitespace
 * and trailing punctuation (dots).
 */
private val PLACEHOLDER_NAMES =
    setOf("tba", "tbd", "tba.", "tbd.", "nn", "n.n.", "nn.")

/**
 * Checks whether [name] is a placeholder rather than a real artist name.
 *
 * Returns `true` for common "to be announced" abbreviations like
 * "TBA", "TBD", "N.N." (case-insensitive, ignoring trailing dots).
 *
 * Example:
 * ```kotlin
 * isPlaceholderName("TBA")    // true
 * isPlaceholderName("t.b.a.") // true
 * isPlaceholderName("N.N.")   // true
 * isPlaceholderName("Aska")   // false
 * ```
 */
fun isPlaceholderName(name: String): Boolean {
    val trimmed = name.trim().lowercase()
    val dotFree = trimmed.replace(".", "")
    // Check both with and without dots to handle "TBA", "T.B.A.", "N.N." etc.
    return dotFree in PLACEHOLDER_NAMES || trimmed in PLACEHOLDER_NAMES
}

/**
 * Resolves a potentially relative [href] against the [baseUrl].
 *
 * If the href is already absolute (starts with "http"), it is returned as-is.
 * Otherwise, it is resolved against the base URL using [URI.resolve].
 *
 * This is a common operation when scraping venue pages that use relative
 * links for event detail pages.
 *
 * Example:
 * ```kotlin
 * resolveUrl("https://venue.com/events", "/event/foo")  // "https://venue.com/event/foo"
 * resolveUrl("https://venue.com/events", "https://other.com/bar")  // "https://other.com/bar"
 * ```
 */
fun resolveUrl(
    baseUrl: String,
    href: String
): String {
    if (href.startsWith("http")) return href
    return URI(baseUrl).resolve(href).toString()
}
