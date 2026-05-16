package de.norm.events.scraper

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

// Domain-level mapping utilities for scraped event data.
//
// These functions translate raw text values from venue websites into
// domain model constants (event types, artist roles). They are shared
// across all venue-specific scrapers to ensure consistent classification.

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
 * Builds an artist list from a headliner title and support act names.
 *
 * This encapsulates the common "title = headliner + Support:" pattern used by
 * multiple venue scrapers. The presence of [supportNames] confirms the
 * title-as-headliner convention. Placeholder names (e.g. "TBA", "tbc") are
 * filtered out from the output but still serve as the signal that the pattern applies.
 *
 * @param title the event title, assumed to be the headliner name.
 * @param supportNames support act names extracted from the listing. If empty, returns
 *   an empty list (cannot confirm the title is an artist name).
 * @return ordered list: headliner first, then support acts by appearance order.
 */
fun buildArtistList(
    title: String,
    supportNames: List<String>
): List<ScrapedArtist> {
    if (supportNames.isEmpty()) return emptyList()

    val headliner =
        if (isPlaceholderName(title)) {
            emptyList()
        } else {
            listOf(ScrapedArtist(name = title, role = "HEADLINER"))
        }

    val supportActs =
        supportNames
            .filterNot { isPlaceholderName(it) }
            .map { ScrapedArtist(name = it, role = "SUPPORT") }

    return headliner + supportActs
}
