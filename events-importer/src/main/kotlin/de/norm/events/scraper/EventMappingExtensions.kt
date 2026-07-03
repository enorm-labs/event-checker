package de.norm.events.scraper

import de.norm.events.event.EventStatus
import de.norm.events.event.EventType
import java.math.BigDecimal

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
 * Splits the captured names on `, `, `&`, and `+` so multiple support acts are
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
 * Picks the subtitle line carrying the "Support:" marker from already-split
 * subtitle [lines], or `null` if none.
 *
 * Venues whose subtitle stacks a support line and trailing notes across separate
 * lines (e.g. an "ABGESAGT …" cancellation notice) must isolate the support line
 * before handing it to [extractSupportFromSubtitle]; otherwise the note's text —
 * which `.text()` flattens onto the same line — would be captured as a support
 * act. Pair with [textLinesAt][de.norm.events.scraper.textLinesAt] to obtain the
 * lines. Shared by the Kulturhäuser-platform scrapers (Astra, Lido).
 *
 * Example:
 * ```kotlin
 * supportSubtitleLine(listOf("+ Support: Jeff Clarke", "ABGESAGT. …note…"))  // "+ Support: Jeff Clarke"
 * supportSubtitleLine(listOf("Tour 2026"))                                   // null
 * ```
 */
fun supportSubtitleLine(lines: List<String>): String? = lines.firstOrNull { SUPPORT_PATTERN.containsMatchIn(it) }

/**
 * Maps a venue status-badge text (German or English) to an [EventStatus] name.
 *
 * Matching is case-insensitive. "sold out" / "ausverkauft" is intentionally
 * **not** a status — venues capture it separately as the `soldOut` flag, leaving
 * the status [SCHEDULED][EventStatus.SCHEDULED]. Shared across Kulturhäuser-platform
 * scrapers (Astra, Lido) whose badges use these conventional labels.
 *
 * Example:
 * ```kotlin
 * parseEventStatus("Abgesagt")   // "CANCELLED"
 * parseEventStatus("Verlegt")    // "RELOCATED"
 * parseEventStatus("Ausverkauft") // "SCHEDULED" (sold-out is a flag, not a status)
 * ```
 */
fun parseEventStatus(statusText: String): String {
    val text = statusText.lowercase()
    return when {
        text.contains("abgesagt") || text.contains("cancel") -> EventStatus.CANCELLED.name
        text.contains("verschoben") || text.contains("postpon") -> EventStatus.POSTPONED.name
        text.contains("verlegt") || text.contains("reloc") -> EventStatus.RELOCATED.name
        else -> EventStatus.SCHEDULED.name
    }
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

/**
 * Builds an artist list using the source's own event-type classification, for
 * venues that expose a clean `kind`/type label (the Kulturhäuser platform —
 * Astra, Lido). The strategy keys off [eventType]:
 * - **Festivals / parties** — the title is an event name, not an artist; no
 *   artists are extracted.
 * - **Concerts** — the type confirms the title is the headliner, so it is always
 *   added (plus any support acts), even without a support line.
 * - **Unknown / other** — fall back to the conservative [buildArtistList], which
 *   only treats the title as an artist when a "Support:" line is present.
 *
 * Support acts come from the subtitle's `"… + Support: A & B"` pattern.
 */
@Suppress("ReturnCount") // Guard clauses for the event-type branches are clearer than nesting
fun buildArtistsForEventType(
    title: String,
    subtitle: String?,
    eventType: String?
): List<ScrapedArtist> {
    if (eventType == EventType.FESTIVAL.name || eventType == EventType.PARTY.name) return emptyList()

    val supportNames = extractSupportFromSubtitle(subtitle)
    if (eventType != EventType.CONCERT.name) return buildArtistList(title, supportNames)

    // Concert: the title is the headliner (unless a placeholder), then support acts in listing order.
    val headliner =
        if (isPlaceholderName(title)) emptyList() else listOf(ScrapedArtist(name = title, role = "HEADLINER"))
    val supportActs =
        supportNames
            .filterNot { isPlaceholderName(it) }
            .map { ScrapedArtist(name = it, role = "SUPPORT") }
    return headliner + supportActs
}

/**
 * Free-entry phrases unambiguous enough to detect from any text field (title or
 * price note). Multi-word, so they won't collide with band or festival names.
 */
private val FREE_PHRASES =
    listOf(
        "eintritt frei",
        "freier eintritt",
        "kostenloser eintritt",
        "free entry",
        "free admission"
    )

/**
 * Single-word free markers, only scanned within the pricing-scoped [ScrapedEvent.priceNote]
 * (never the title/subtitle) to avoid false positives from names like "Freedom Festival"
 * or "Freikörperkultur". Word-boundary matched, so "free" won't match "freestyle".
 */
private val FREE_TOKENS = listOf("free", "frei", "gratis", "kostenlos", "umsonst")

private val FREE_PHRASE_PATTERN =
    Regex(FREE_PHRASES.joinToString("|") { Regex.escape(it) }, RegexOption.IGNORE_CASE)

private val FREE_TOKEN_PATTERN =
    Regex("""\b(${FREE_TOKENS.joinToString("|") { Regex.escape(it) }})\b""", RegexOption.IGNORE_CASE)

/**
 * Detects whether an event is free to attend, from its prices and text.
 *
 * A *positive* signal is required — an absent price means the price is **unknown**,
 * not free — so this returns `true` only for:
 * - an explicit €0 presale or box-office price, or
 * - an unambiguous free-entry phrase ([FREE_PHRASES]) in the title or price note, or
 * - a single-word free marker ([FREE_TOKENS]) in the price note (pricing-scoped, so
 *   an artist name in the title can't trigger it).
 *
 * Example:
 * ```kotlin
 * detectFree(priceNote = "Eintritt frei")         // true
 * detectFree(pricePresale = BigDecimal.ZERO)      // true
 * detectFree(title = "Freedom Festival")          // false
 * detectFree(pricePresale = BigDecimal("12.00"))  // false
 * ```
 */
fun detectFree(
    pricePresale: BigDecimal? = null,
    priceBoxOffice: BigDecimal? = null,
    priceNote: String? = null,
    title: String? = null
): Boolean {
    val hasZeroPrice = pricePresale?.signum() == 0 || priceBoxOffice?.signum() == 0
    val phraseInTitle = title?.let { FREE_PHRASE_PATTERN.containsMatchIn(it) } ?: false
    val markerInNote =
        priceNote?.let { FREE_PHRASE_PATTERN.containsMatchIn(it) || FREE_TOKEN_PATTERN.containsMatchIn(it) } ?: false
    return hasZeroPrice || phraseInTitle || markerInNote
}
