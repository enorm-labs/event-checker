@file:Suppress("TooManyFunctions") // Cohesive collection of small, single-purpose scraped-event mapping utilities.

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
 * Delegates to [splitSupportActs], so the captured names are split on commas,
 * `+` and `/`, with `&` / `and` / `und` handled per boundary — a backing-band
 * tail stays attached to its act. Returns an empty list when no support line is
 * present. Shared across venue scrapers (e.g. Privatclub, Astra) whose subtitles
 * follow this convention.
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
    return splitSupportActs(match.groupValues[1])
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
 * announced yet (e.g. "TBA", "TBD", "TBC", "N.N."). These should not be
 * created as artist entries in the database.
 *
 * Comparison is case-insensitive and ignores surrounding whitespace
 * and trailing punctuation (dots).
 */
private val PLACEHOLDER_NAMES =
    setOf("tba", "tbd", "tbc", "tba.", "tbd.", "tbc.", "nn", "n.n.", "nn.")

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
 * A leading lineup **role label** ("Support:", "Special Guest(s):", "div. Supports",
 * "feat.", "featuring", "w/"), optionally followed by a colon. Used both to strip the
 * label off an act ("Special Guest: FUCK" → "FUCK") and, when a chunk is nothing but
 * the label, to recognize it as a non-artist via [isNonArtistLabel]. Shared with the
 * SO36 detail scraper, whose support subtitles carry these inline.
 */
val ROLE_LABEL_PREFIX =
    Regex("""^(?:div\.?\s*supports?|special\s+guests?|supports?|feat\.?|featuring|w/)\s*:?\s*""", RegexOption.IGNORE_CASE)

/**
 * Checks whether [name] is a bare lineup role label ("Special Guest", "Support",
 * "div. Supports", …) rather than a real act name.
 *
 * These leak in when a venue lists an unnamed slot (e.g. a subtitle `"Support:
 * Special Guest"`, where the captured act is just the label). Matching is exact —
 * the whole trimmed value must be the label — so a real name that merely *contains*
 * a label word (`"Special Guest Foo"`, `"Support Act X"`) is kept. Filtered out
 * alongside [isPlaceholderName] wherever support/headliner names are resolved.
 *
 * Example:
 * ```kotlin
 * isNonArtistLabel("Special Guest") // true
 * isNonArtistLabel("Support")       // true
 * isNonArtistLabel("Green Lung")    // false
 * ```
 */
fun isNonArtistLabel(name: String): Boolean {
    val trimmed = name.trim()
    return trimmed.isNotEmpty() && trimmed.replaceFirst(ROLE_LABEL_PREFIX, "").isBlank()
}

private val WHITESPACE = Regex("""\s+""")

/**
 * Curated event-*segment* labels — an aftershow/afterparty/warm-up slot that a
 * venue lists in the lineup, which is a part of the night, not a performer. Any
 * optional leading qualifier is allowed (`ACID AFTERSHOW`, `TECHNO AFTERPARTY`),
 * and `aftershow`/`after show`/`after-show` spellings are accepted.
 */
private val EVENT_SEGMENT_PATTERN =
    Regex("""(?:\S+ )*after[ -]?show(?: party)?|(?:\S+ )*after[ -]?party|warm[ -]?up""", RegexOption.IGNORE_CASE)

/**
 * Checks whether [name] is an event-segment label ("Acid Aftershow", "Warm Up")
 * rather than a performer.
 *
 * Matching is **fully anchored** (the whole trimmed, whitespace-collapsed value
 * must be the segment phrase), so it cannot touch a real band whose name merely
 * contains or resembles a segment word — `"AFTERHOURS"` (a band) and the venue's
 * `"Warm Up im Franken"` are both kept. Curated on purpose: there is no structural
 * signal separating a segment from an act in the flat lineup text, so new families
 * are added to [EVENT_SEGMENT_PATTERN] as they appear.
 *
 * Example:
 * ```kotlin
 * isEventSegmentLabel("ACID AFTERSHOW") // true
 * isEventSegmentLabel("Aftershow Party") // true
 * isEventSegmentLabel("AFTERHOURS")     // false (real band)
 * ```
 */
fun isEventSegmentLabel(name: String): Boolean {
    val normalized = name.trim().replace(WHITESPACE, " ")
    return normalized.isNotEmpty() && EVENT_SEGMENT_PATTERN.matches(normalized)
}

/**
 * Event names that are not performers: a festival ("Shred Fest", "Canarias Calling
 * Festival", optionally year-suffixed) or a festival-ticket label ("… Festivalticket").
 * The `fest`/`festival` markers are word-anchored, so one-word names ("Infest",
 * "Manifest") are safe.
 */
private val NON_ARTIST_EVENT_PATTERN =
    Regex(""".*\bfest\b(?: \d{4})?|.*\bfestival\b(?: \d{4})?|.*\bfestivalticket\b""", RegexOption.IGNORE_CASE)

/**
 * Checks whether [name] is an event label (a festival or festival-ticket) rather
 * than a performer.
 *
 * Matching is **fully anchored** on the whitespace-collapsed value, and the
 * `fest`/`festival` word boundaries keep one-word names safe (`Infest`, `Manifest`
 * are kept). Curated: new event-label families are added to
 * [NON_ARTIST_EVENT_PATTERN] as they appear.
 *
 * Example:
 * ```kotlin
 * isNonArtistEvent("SHRED FEST")                 // true
 * isNonArtistEvent("CANARIAS CALLING FESTIVAL")  // true
 * isNonArtistEvent("Manifest")                   // false
 * ```
 */
fun isNonArtistEvent(name: String): Boolean {
    val normalized = name.trim().replace(WHITESPACE, " ")
    return normalized.isNotEmpty() && NON_ARTIST_EVENT_PATTERN.matches(normalized)
}

/**
 * Trailing suffixes that decorate a real act name, stripped by [stripArtistSuffix]
 * to recover the performer:
 * - a hyphen-separated "… - <tour name> Tour <year>" tail,
 * - a hyphen-separated anniversary tail "… - <n> Years/Jahre …" (e.g.
 *   "THE BUTLERS - 40 YEARS, SKA & SOULPOWER -"),
 * - a trailing "Live" / "Live in <city>", and
 * - a trailing parenthesized performance-format annotation "(DJ-Set)", "(Live)",
 *   "(Acoustic)", "(Solo)", "(Unplugged)".
 * The hyphen tails require a `<space>-<space>` boundary and a recognized marker
 * (`tour`, or a number + `years`/`jahre`), so an undecorated hyphenated name like
 * "BAD COMPANY LEGACY - Dave Colwell" is left intact. A whitespace boundary before
 * "Live" is likewise required, so a bare "Live" (the band) is never matched. The
 * parenthetical is keyed on the format word, so an alias in parentheses (e.g.
 * "Sickboyrari (Black Kray)") is kept.
 */
private val ARTIST_SUFFIX_PATTERN =
    Regex(
        """\s+-\s+(?:\S.*\btour\b|\d+\s+(?:years?|jahre)\b).*$""" +
            """|\s+live(?:\s+in\s+\S.*)?$""" +
            """|\s*\((?:dj[\s-]?set|live|acoustic|akustik|unplugged|solo)\)\s*$""",
        RegexOption.IGNORE_CASE
    )

/**
 * Strips a trailing tour/live/anniversary suffix or performance-format annotation from
 * a scraped act name to recover the performer.
 *
 * Recovers the band from decorated names — `"DOMINIUM - NIGHT IS CALLING TOUR 2026"` →
 * `"DOMINIUM"`, `"AZ LIVE IN BERLIN"` → `"AZ"`, `"HGICH.T LIVE"` → `"HGICH.T"`,
 * `"THE BUTLERS - 40 YEARS, SKA & SOULPOWER -"` → `"THE BUTLERS"`,
 * `"Avangelic (DJ-Set)"` → `"Avangelic"`. Returns the input unchanged when there is no
 * such suffix, or when stripping would leave nothing — so a bare `"Live"` (the band) and
 * a parenthesized alias like `"Sickboyrari (Black Kray)"` are preserved.
 *
 * Example:
 * ```kotlin
 * stripArtistSuffix("HGICH.T LIVE")                     // "HGICH.T"
 * stripArtistSuffix("THE BUTLERS - 40 YEARS, SKA -")    // "THE BUTLERS"
 * stripArtistSuffix("Avangelic (DJ-Set)")               // "Avangelic"
 * stripArtistSuffix("Sickboyrari (Black Kray)")         // "Sickboyrari (Black Kray)"
 * ```
 */
fun stripArtistSuffix(name: String): String {
    val stripped = name.trim().replace(ARTIST_SUFFIX_PATTERN, "").trim()
    return stripped.ifBlank { name.trim() }
}

/**
 * Manually curated one-off titles that are not performers but that no structural
 * rule safely catches — a warm-up slot at a specific room, a package-tour name, a
 * recurring themed night. Entries are lowercase and whitespace-collapsed; a
 * trailing edition number is ignored at match time, so a recurring series matches
 * every edition (`FEMALE-FRONTED IS NOT A GENRE 5`, `… 6`, …). Add exact titles
 * here as they surface.
 */
private val NON_ARTIST_NAMES: Set<String> =
    setOf(
        "warm up im franken",
        "the revival tour",
        "female-fronted is not a genre"
    )

/** A trailing edition number ("… 5") on a recurring event title, ignored when matching [NON_ARTIST_NAMES]. */
private val TRAILING_EDITION = Regex("""\s+\d+$""")

private fun isDenylistedNonArtist(name: String): Boolean =
    name
        .trim()
        .replace(WHITESPACE, " ")
        .lowercase()
        .replace(TRAILING_EDITION, "") in NON_ARTIST_NAMES

/**
 * True when [name] must never be stored as an artist: a placeholder ("TBA"), a bare
 * role label ("Special Guest"), an event-segment label ("Acid Aftershow"), an event
 * label ("Shred Fest"), or a curated one-off non-artist title ("The Revival Tour").
 * The single predicate applied wherever scraped headliner/support names are resolved.
 */
fun isNonArtistName(name: String): Boolean =
    isPlaceholderName(name) || isNonArtistLabel(name) || isEventSegmentLabel(name) ||
        isNonArtistEvent(name) || isDenylistedNonArtist(name)

/**
 * Well-known single acts whose name legitimately contains a conjunction that
 * [splitHeadlinerTitle] would otherwise read as a co-bill delimiter. Matched
 * case-insensitively against the whole trimmed title, so such a title is kept
 * intact as one headliner. `AC/DC` and similar are already protected by the
 * space-padding requirement and need no entry here — this list is only for the
 * ambiguous `" & "` / `" and "` / `" und "` cases the heuristics below can't
 * catch structurally. Entries are written in `&` form; comparison normalizes
 * the title's conjunctions to `&` first, so an `"… and …"` source spelling of a
 * listed act still matches.
 */
private val KNOWN_SINGLE_ACTS: Set<String> =
    setOf(
        "simon & garfunkel",
        "earth, wind & fire",
        "blood, sweat & tears",
        "mumford & sons",
        "hall & oates",
        "above & beyond",
        "sam & dave",
        "chas & dave",
        "angus & julia stone",
        "matt & kim"
    )

/**
 * Leading words that mark the right-hand side of a conjunction as a band-name
 * tail ("X & the Ys", "X and his Ys", "X und die Ys") rather than a second act.
 * Used to suppress splitting for the common "frontman & backing band" pattern.
 */
private val CONJUNCTION_TAIL_ARTICLES: Set<String> =
    setOf("the", "his", "her", "their", "los", "las", "die", "der", "das", "el", "la")

/** Space-padded `/` or `+` — unambiguous co-bill separators once whitespace is required on both sides. */
private val SAFE_TITLE_SEPARATOR = Regex("""\s+[/+]\s+""")

/**
 * Space-padded conjunction (`&`, `and`, `und`) — split only at boundaries that
 * pass the [splitSegmentOnConjunctions] guardrails. The `and`/`und` word forms
 * are space-padded so they match only the standalone conjunction, never a
 * substring (e.g. the "and" in "Portland"), and case-insensitive for `AND`/`UND`.
 */
private val CONJUNCTION_SEPARATOR = Regex("""\s+(?:&|and|und)\s+""", RegexOption.IGNORE_CASE)

/**
 * Splits a title segment into acts at its conjunctions, deciding **per boundary**
 * so a real co-bill still splits even when another conjunction in the same title
 * is a band-name tail. Conservative: a comma anywhere suppresses splitting (the
 * "Earth, Wind & Fire" member-list pattern), and a boundary is kept joined when
 * its right-hand side opens with an article/possessive (the "X and the Ys"
 * pattern) — so `CARL CARLTON & MELANIE WIEGMANN AND THE GREAT BAND` cuts only at
 * the `&`. Each act keeps its original conjunction spelling (no rewrite).
 */
@Suppress("ReturnCount") // Guard clauses for the comma and no-cut cases are clearer than nesting
private fun splitSegmentOnConjunctions(segment: String): List<String> {
    if (segment.contains(',')) return listOf(segment)

    val cuts =
        CONJUNCTION_SEPARATOR
            .findAll(segment)
            .filter { match ->
                segment
                    .substring(match.range.last + 1)
                    .trimStart()
                    .substringBefore(' ')
                    .lowercase() !in
                    CONJUNCTION_TAIL_ARTICLES
            }.map { it.range }
            .toList()
    if (cuts.isEmpty()) return listOf(segment)

    val acts = mutableListOf<String>()
    var start = 0
    for (range in cuts) {
        acts.add(segment.substring(start, range.first))
        start = range.last + 1
    }
    acts.add(segment.substring(start))
    return acts
}

/** Hard separators that always delimit acts in a support/lineup line: comma, plus, slash. */
private val SUPPORT_HARD_SEPARATOR = Regex("""\s*[,+/]\s*""")

/**
 * Splits a support/lineup line into individual act names.
 *
 * Hard separators (comma, `+`, `/`) always delimit acts; the `&` / `and` / `und`
 * conjunctions are then split **per boundary** via [splitSegmentOnConjunctions],
 * the same guardrails [splitHeadlinerTitle] uses — so a backing-band tail stays
 * attached to its act (`Scott Hepple & The Sun Band` is one act via the article
 * guard, while `High On Fire & Gnome` is two). Blank fragments are dropped;
 * role-label stripping and placeholder filtering are left to the caller.
 *
 * Example:
 * ```kotlin
 * splitSupportActs("High On Fire & Gnome, Aska")                    // ["High On Fire", "Gnome", "Aska"]
 * splitSupportActs("Earth Tongue und Scott Hepple & The Sun Band")  // ["Earth Tongue", "Scott Hepple & The Sun Band"]
 * ```
 */
fun splitSupportActs(text: String): List<String> =
    text
        .split(SUPPORT_HARD_SEPARATOR)
        .flatMap { splitSegmentOnConjunctions(it) }
        .map { it.trim() }
        .filter { it.isNotBlank() }

/**
 * Splits a headliner title into its individual co-billed acts.
 *
 * Titles frequently pack a whole lineup into one string
 * (`TOTAL CHAOS + RUMKICKS + THE DOLLHEADS`, `LAGWAGON / THE VIRGINMARYS`,
 * `BLACK STAR RIDERS & TYKETTO`, `Earth Tongue und Scott Hepple`). This splits on
 * unambiguous, space-padded separators only, so band names that legitimately
 * contain these characters survive intact:
 * - `" / "` and `" + "` are treated as co-bill separators (space-padding
 *   protects `AC/DC`, `dance/electronic`, etc.).
 * - a `" & "` / `" and "` / `" und "` conjunction is split per boundary via
 *   [splitSegmentOnConjunctions], and never for a title in [KNOWN_SINGLE_ACTS].
 *   The article-tail guard keeps `X and the Ys` band names (`James and the Cold
 *   Gun`, `Melanie Wiegmann and the Great Band`) whole while still splitting a
 *   real co-bill alongside them.
 *
 * A title with no recognized separator (the common single-act case) returns a
 * singleton list of the trimmed title, so callers see no behavioural change.
 * Placeholder filtering is left to the caller.
 *
 * Example:
 * ```kotlin
 * splitHeadlinerTitle("TOTAL CHAOS + RUMKICKS")       // ["TOTAL CHAOS", "RUMKICKS"]
 * splitHeadlinerTitle("LAGWAGON / THE VIRGINMARYS")   // ["LAGWAGON", "THE VIRGINMARYS"]
 * splitHeadlinerTitle("Earth Tongue und Scott Hepple") // ["Earth Tongue", "Scott Hepple"]
 * splitHeadlinerTitle("Simon & Garfunkel")            // ["Simon & Garfunkel"]  (denylist)
 * splitHeadlinerTitle("James and the Cold Gun")       // ["James and the Cold Gun"]  (article tail)
 * splitHeadlinerTitle("AC/DC")                         // ["AC/DC"]  (no space padding)
 * ```
 */
@Suppress("ReturnCount") // Guard clauses for blank and denylisted titles are clearer than nesting
fun splitHeadlinerTitle(title: String): List<String> {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return listOf(title)
    // Normalize the "and"/"und" word forms to "&" so a title matches the &-spelled denylist.
    if (trimmed.replace(CONJUNCTION_SEPARATOR, " & ").lowercase() in KNOWN_SINGLE_ACTS) return listOf(trimmed)

    val acts =
        trimmed
            .split(SAFE_TITLE_SEPARATOR)
            .flatMap { splitSegmentOnConjunctions(it) }
            .map { it.trim() }
            .filter { it.isNotBlank() }

    return acts.ifEmpty { listOf(trimmed) }
}

/**
 * Turns an event title into its headliner artist entries: split co-billed acts via
 * [splitHeadlinerTitle], strip tour/live suffixes via [stripArtistSuffix] to recover
 * the performer, then drop anything that is not an artist ([isNonArtistName] —
 * placeholders, role labels, segments, festivals). Returned in billing order (title
 * order); the caller appends support acts.
 */
fun headlinersFromTitle(title: String): List<ScrapedArtist> =
    splitHeadlinerTitle(title)
        .map { stripArtistSuffix(it) }
        .filterNot { isNonArtistName(it) }
        .map { ScrapedArtist(name = it, role = "HEADLINER") }

/**
 * Builds an artist list from a headliner title and support act names.
 *
 * This encapsulates the common "title = headliner + Support:" pattern used by
 * multiple venue scrapers. The presence of [supportNames] confirms the
 * title-as-headliner convention. The title is split into co-billed headliners
 * via [headlinersFromTitle]; placeholder names (e.g. "TBA", "tbc") and bare role
 * labels (e.g. "Special Guest") are filtered out from the output but still serve
 * as the signal that the pattern applies.
 *
 * @param title the event title, assumed to be one or more headliner names.
 * @param supportNames support act names extracted from the listing. If empty, returns
 *   an empty list (cannot confirm the title is an artist name).
 * @return ordered list: headliner(s) first, then support acts by appearance order.
 */
fun buildArtistList(
    title: String,
    supportNames: List<String>
): List<ScrapedArtist> {
    if (supportNames.isEmpty()) return emptyList()

    val supportActs =
        supportNames
            .filterNot { isNonArtistName(it) }
            .map { ScrapedArtist(name = it, role = "SUPPORT") }

    return headlinersFromTitle(title) + supportActs
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

    // Concert: the title carries the headliner(s) (co-bills split out), then support acts in listing order.
    val supportActs =
        supportNames
            .filterNot { isNonArtistName(it) }
            .map { ScrapedArtist(name = it, role = "SUPPORT") }
    return headlinersFromTitle(title) + supportActs
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
