package de.norm.events.scraper

import de.norm.events.event.EventStatus
import java.math.BigDecimal
import java.time.LocalTime

// Field-level mapping for scraped events: status badges, doors/start ordering,
// title cleanup, and free-entry detection. Event-type and artist-name mapping live
// in EventTypeMapping.kt and ArtistNameMapping.kt.

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
 * Returns the (doors, start) pair with doors never later than start.
 *
 * Doors open no later than the show begins, so when a source lists the two in the
 * wrong order — e.g. SO36's `"Einlass: 19:30, Beginn: 19:00"` — the labels were
 * transposed at the source; swapping them recovers the intended times. Only
 * reorders when **both** times are present and doors is strictly after start; a
 * single time, equal times, or an already-valid pair is returned unchanged. Applied
 * once at the [ScrapedEvent.toEventEntity] persistence boundary, so every venue is
 * covered without each scraper repeating the check.
 *
 * Example:
 * ```kotlin
 * orderDoorsBeforeStart(LocalTime.of(19, 30), LocalTime.of(19, 0)) // (19:00, 19:30) — swapped
 * orderDoorsBeforeStart(LocalTime.of(19, 0), LocalTime.of(20, 0))  // (19:00, 20:00) — unchanged
 * orderDoorsBeforeStart(null, LocalTime.of(20, 0))                 // (null, 20:00) — unchanged
 * ```
 */
fun orderDoorsBeforeStart(
    doors: LocalTime?,
    start: LocalTime?
): Pair<LocalTime?, LocalTime?> = if (doors != null && start != null && doors > start) start to doors else doors to start

/**
 * Trailing noise venues append to an *event title* that must not become part of the stored
 * title (nor of a title-derived headliner artist):
 * - a "Nachholtermin vom <date>" reschedule note or a "Hochverlegung" relocation note,
 * - a "(ausverkauft)" / "ausverkauft" sold-out annotation — a status, not a name; Frannz in
 *   particular never derives sold-out from prose, so it is pure noise here, and stripping it
 *   keeps "… (ausverkauft)" and its non-sold-out twin from splitting into two artists,
 * - any stray trailing dash.
 *
 * Each alternative is word-/end-anchored, so mid-title text (e.g. an "ausverkauften" mention
 * that only ever reaches descriptions) is never touched. This is the title-level counterpart
 * of the tail [ARTIST_SUFFIX_PATTERN] strips off an artist name.
 */
private val TITLE_NOISE_PATTERN =
    Regex(
        """\s+[-–—]?\s*(?:nachholtermin|hochverlegung)\b.*$""" +
            """|\s+[-–—(]*\s*ausverkauft!?\s*\)?\s*$""" +
            """|\s+[-–—]\s*$""",
        RegexOption.IGNORE_CASE
    )

/**
 * Strips a trailing rescheduled-show note and stray trailing dash from an event title
 * so the stored, user-visible title stays clean — "Iggi Kelly Nachholtermin vom
 * 28.04.26-" → "Iggi Kelly". Returns the input unchanged when there is no such tail, or
 * when stripping would leave nothing.
 *
 * Example:
 * ```kotlin
 * cleanEventTitle("Iggi Kelly Nachholtermin vom 28.04.26-")     // "Iggi Kelly"
 * cleanEventTitle("Singalong -Das Mitsing-Event (ausverkauft)") // "Singalong -Das Mitsing-Event"
 * cleanEventTitle("The Adicts")                                 // "The Adicts"
 * ```
 */
fun cleanEventTitle(title: String): String {
    val stripped = title.trim().replace(TITLE_NOISE_PATTERN, "").trim()
    return stripped.ifBlank { title.trim() }
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
