package de.norm.events.scraper

import de.norm.events.event.EventType

// Domain-level mapping utilities for scraped event data.
//
// These functions translate raw text values from venue websites into
// domain model constants (event types, artist roles). They are shared
// across all venue-specific scrapers to ensure consistent classification.

/**
 * Base synonym table mapping common German/English event category labels to
 * [EventType] names. Keys are lowercase; matching is case-insensitive.
 *
 * Venue-specific labels (e.g. Madame Claude's WordPress CSS class names) are
 * passed as `extraSynonyms` to [mapEventType] rather than polluting this table.
 */
private val BASE_EVENT_TYPE_SYNONYMS: Map<String, String> =
    mapOf(
        "konzert" to EventType.CONCERT.name,
        "concert" to EventType.CONCERT.name,
        "festival" to EventType.FESTIVAL.name,
        "party" to EventType.PARTY.name,
        "quiz" to EventType.QUIZ.name,
        "show" to EventType.SHOW.name,
        "sonstiges" to EventType.OTHER.name,
        "other" to EventType.OTHER.name
    )

/**
 * Maps a raw venue category/kind label to an [EventType] name, or `null` when
 * the label is missing or unrecognized.
 *
 * Returning `null` (rather than defaulting to `"OTHER"`) lets callers fall back
 * to another data source via `?:`, and lets the persistence boundary
 * ([ScrapedEvent.toEventEntity]) apply the `OTHER` default. This is the single
 * shared mapper used by every venue scraper — venue-specific labels are supplied
 * via [extraSynonyms], which take precedence over [BASE_EVENT_TYPE_SYNONYMS].
 *
 * Example:
 * ```kotlin
 * mapEventType("Konzert")                              // "CONCERT"
 * mapEventType("Festival")                             // "FESTIVAL"
 * mapEventType("Workshop")                             // null
 * mapEventType("Live", mapOf("live" to "CONCERT"))     // "CONCERT" (venue-specific)
 * ```
 */
fun mapEventType(
    label: String?,
    extraSynonyms: Map<String, String> = emptyMap()
): String? {
    val key = label?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
    return extraSynonyms[key] ?: BASE_EVENT_TYPE_SYNONYMS[key]
}

/**
 * Extracts support act names from a subtitle's `"… + Support: A & B"` pattern.
 *
 * Splits the captured names on `,`, `&`, and `+` so multiple support acts are
 * returned individually. Returns an empty list when no support line is present.
 * Shared across venue scrapers (e.g. Privatclub, Astra) whose subtitles follow
 * this convention.
 *
 * Example:
 * ```kotlin
 * extractSupportFromSubtitle("Tour 2026 | Support: Luana")          // ["Luana"]
 * extractSupportFromSubtitle("Tour + Support: High On Fire & Gnome") // ["High On Fire", "Gnome"]
 * extractSupportFromSubtitle("Tour 2026")                            // []
 * ```
 */
@Suppress("ReturnCount") // Guard clauses for blank subtitle and missing support line are clearer than nesting
fun extractSupportFromSubtitle(subtitle: String?): List<String> {
    if (subtitle.isNullOrBlank()) return emptyList()
    val match = SUPPORT_PATTERN.find(subtitle) ?: return emptyList()
    return match.groupValues[1]
        .split(Regex("[,&+]"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

/** Matches "Support: <names>" anywhere in a subtitle, capturing to end of line. */
private val SUPPORT_PATTERN = Regex("""[Ss]upport:\s*(.+)""")

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
