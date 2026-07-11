@file:Suppress("TooManyFunctions") // Cohesive collection of small, single-purpose scraped-event mapping utilities.

package de.norm.events.scraper

import de.norm.events.event.EventStatus
import de.norm.events.event.EventType
import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalTime

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
        // A football/match screening (Lido labels these "Public Viewing") is a SCREENING, not a concert.
        "public viewing" to EventType.SCREENING.name,
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

/** Title keywords marking a non-music variety show (mapped to [EventType.SHOW]). */
private val SHOW_TITLE_KEYWORDS = listOf("wrestling", "burlesque", "circus")

/**
 * Title keywords marking a film/match screening (mapped to [EventType.SCREENING]):
 * football public-viewings / live screenings — the audience watches a screen rather
 * than a live act. These are long/distinctive enough for a safe substring test; the
 * shorter cinema marker lives in [SCREENING_TITLE_WORD_PATTERN].
 */
private val SCREENING_TITLE_KEYWORDS =
    listOf(
        "public viewing",
        "live-screening",
        "screening",
        "world cup",
        "weltmeisterschaft",
        "fußball",
        "fussball",
        "wm-quartier",
        "11freunde"
    )

/**
 * Whole-word screening keyword too short for a safe substring test: `kino` (cinema)
 * is a substring of real act names like "Alkinoos Ioannidis", so it is matched only
 * as a standalone word.
 */
private val SCREENING_TITLE_WORD_PATTERN = Regex("""\bkino\b""", RegexOption.IGNORE_CASE)

/**
 * Whether [title] names a film/match screening — a football public-viewing / live
 * screening or a cinema night. Exposed for venues (e.g. Madame Claude) that type
 * from a category but want a title-based screening safety net when the category is
 * unknown.
 */
fun isScreeningTitle(title: String): Boolean {
    val haystack = title.lowercase()
    return SCREENING_TITLE_KEYWORDS.any { it in haystack } || SCREENING_TITLE_WORD_PATTERN.containsMatchIn(haystack)
}

/**
 * Title keywords marking a clearly non-musical format with no more specific type
 * (mapped to [EventType.OTHER]): readings/poetry slams and markets.
 */
private val NON_CONCERT_TITLE_KEYWORDS =
    listOf(
        // Readings / poetry slams / spoken word
        "lesung",
        "lesedüne",
        "slam",
        // Markets
        "markt"
    )

/** Title keywords marking a DJ/club night (mapped to [EventType.PARTY]). */
private val PARTY_TITLE_KEYWORDS =
    listOf("aftershow", "afterparty", "after-party", "after party", "party", "club night", "clubnight", "club", "rave", "karaoke")

/**
 * Classifies an event by unambiguous keywords in its [title], or `null` when none
 * match. Curated and reactive: a quiz keyword → [QUIZ][EventType.QUIZ]; a
 * wrestling/burlesque/circus show → [SHOW][EventType.SHOW]; a football screening or
 * cinema night → [SCREENING][EventType.SCREENING]; another non-music format
 * (reading/slam, market) → [OTHER][EventType.OTHER]; a party/club-night keyword →
 * [PARTY][EventType.PARTY].
 */
private fun classifyByTitleKeyword(title: String): String? {
    val haystack = title.lowercase()
    return when {
        "quiz" in haystack -> EventType.QUIZ.name
        SHOW_TITLE_KEYWORDS.any { it in haystack } -> EventType.SHOW.name
        isScreeningTitle(title) -> EventType.SCREENING.name
        NON_CONCERT_TITLE_KEYWORDS.any { it in haystack } -> EventType.OTHER.name
        PARTY_TITLE_KEYWORDS.any { it in haystack } -> EventType.PARTY.name
        else -> null
    }
}

/**
 * Title-based event-type inference for a concert-leaning venue that left an event
 * *entirely unclassified* (no category at all — e.g. Roadrunner has no category
 * field; Astra/Lido/So36 omit the label for some events).
 *
 * These are dedicated live-music venues, so the default is [CONCERT][EventType.CONCERT] —
 * a category-less event is almost always an ordinary gig whose title names the act,
 * and defaulting to CONCERT lets that title be minted as the headliner
 * ([buildArtistsForEventType]). Only an unambiguous [classifyByTitleKeyword] match
 * flips it (keeping quiz/screening/party event-names out of the lineup).
 */
fun inferConcertVenueType(title: String): String = classifyByTitleKeyword(title) ?: EventType.CONCERT.name

/** The generic "unclassified" category labels a venue may emit (as opposed to a specific kind). */
private val GENERIC_OTHER_TYPES = setOf(EventType.OTHER.name)

/**
 * Refines the [mappedType] a concert-leaning venue produced for an event, using
 * title keywords to recover types the venue's own category field got wrong.
 *
 * The venue's category is trusted unless it is *unclassified*:
 *  - a `null` mapping (no category) → [inferConcertVenueType]: default CONCERT for
 *    this live-music venue, since a category-less event is almost always a gig.
 *  - a generic [OTHER][EventType.OTHER] mapping (the venue's own catch-all bucket,
 *    e.g. Astra's "Other" kind) → reclassified by [classifyByTitleKeyword] only,
 *    falling back to OTHER. Here the venue *explicitly* said "not a normal concert",
 *    so — unlike the `null` case — we do **not** default to CONCERT: only a positive
 *    keyword (a wrestling show → SHOW, a party → PARTY) overrides it. A signal-less
 *    catch-all event (e.g. "GWF Summer Smash") stays OTHER.
 *  - any specific type (CONCERT/PARTY/QUIZ/FESTIVAL/SHOW) → trusted as-is.
 */
fun refineConcertVenueType(
    mappedType: String?,
    title: String
): String =
    when (mappedType) {
        null -> inferConcertVenueType(title)
        in GENERIC_OTHER_TYPES -> classifyByTitleKeyword(title) ?: EventType.OTHER.name
        else -> mappedType
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
 * Festival"), a festival slot/edition ("Grey City Fest Opener", "Sommer Festival
 * Special", "Grobes Fest 2026") or a festival-ticket label ("… Festivalticket").
 * The `fest`/`festival` markers are word-anchored and may carry any trailing content
 * (a year, an "Opener"/"Special" slot label, …), so a festival titled with a slot or
 * edition is caught while one-word names ("Infest", "Manifest") and compounds
 * ("Sommerfest" — no standalone `fest` boundary) stay safe.
 */
private val NON_ARTIST_EVENT_PATTERN =
    Regex(""".*\bfest\b.*|.*\bfestival\b.*|.*\bfestivalticket\b.*""", RegexOption.IGNORE_CASE)

/**
 * Checks whether [name] is an event label (a festival, a festival slot/edition, or a
 * festival-ticket) rather than a performer.
 *
 * The whitespace-collapsed value must contain a word-anchored `fest`/`festival`
 * marker, which may carry any trailing slot/edition text (`Grey City Fest Opener`,
 * `Grobes Fest 2026`). The word boundaries keep one-word names (`Infest`, `Manifest`)
 * and compounds (`Sommerfest`) safe. Curated: new event-label families are added to
 * [NON_ARTIST_EVENT_PATTERN] as they appear.
 *
 * Example:
 * ```kotlin
 * isNonArtistEvent("SHRED FEST")                 // true
 * isNonArtistEvent("Grey City Fest Opener")      // true
 * isNonArtistEvent("CANARIAS CALLING FESTIVAL")  // true
 * isNonArtistEvent("Manifest")                   // false
 * ```
 */
fun isNonArtistEvent(name: String): Boolean {
    val normalized = name.trim().replace(WHITESPACE, " ")
    return normalized.isNotEmpty() && NON_ARTIST_EVENT_PATTERN.matches(normalized)
}

/**
 * A title that unambiguously names a festival: a word-anchored `festival` /
 * `festivalticket` marker *anywhere* in the title ("Canarias Calling Festival",
 * "Grossstadtwahnsinn 2026 - Festivalticket"). Deliberately tighter than
 * [NON_ARTIST_EVENT_PATTERN] — it does **not** match a bare `fest`, so an event
 * merely titled "… Fest" is not promoted on this weak signal.
 */
private val FESTIVAL_TITLE_PATTERN =
    Regex("""\bfestivaltickets?\b|\bfestivals?\b""", RegexOption.IGNORE_CASE)

/**
 * Whether [title] unambiguously names a festival (see [FESTIVAL_TITLE_PATTERN]).
 *
 * Used at the persistence boundary ([ScrapedEvent.toEventEntity]) to promote an
 * event whose source under-classified it as `CONCERT`/`OTHER` — a festival day the
 * venue labelled "Konzert", or a title with no source category at all — to
 * `FESTIVAL`. A source that already typed the event `PARTY`/`QUIZ`/`FESTIVAL` is
 * trusted and left unchanged.
 *
 * Example:
 * ```kotlin
 * isFestivalTitle("CANARIAS CALLING FESTIVAL")               // true
 * isFestivalTitle("GROSSSTADTWAHNSINN 2026 - FESTIVALTICKET") // true
 * isFestivalTitle("Manifest")                                 // false
 * ```
 */
fun isFestivalTitle(title: String): Boolean = FESTIVAL_TITLE_PATTERN.containsMatchIn(title)

/**
 * Trailing suffixes that decorate a real act name, stripped by [stripArtistSuffix]
 * to recover the performer:
 * - a hyphen-separated "… - <tour name> Tour <year>" tail,
 * - a hyphen-separated anniversary tail "… - <n> Years/Jahre …" (e.g.
 *   "THE BUTLERS - 40 YEARS, SKA & SOULPOWER -"),
 * - a trailing "Live" / "Live in <city>",
 * - a trailing performance-format annotation, either parenthesized — "(DJ-Set)",
 *   "(Live)", "(Acoustic)", "(Solo)", "(Unplugged)" — or a bare, whitespace-preceded
 *   "DJ-Set" / "DJ Set" tail ("Acid Arab DJ-Set" → "Acid Arab"),
 * - a trailing German relocation/reschedule note — "Nachholtermin vom <date>" or
 *   "Hochverlegung" (e.g. "The Dear Hunter -Nachholtermin vom 30.09.2025.",
 *   "OCT (On Company Time) – Hochverlegung" → "OCT (On Company Time)"),
 * - a trailing "singt <repertoire>" tribute framing ("Tex singt Leonard Cohen" → "Tex"), and
 * - a trailing "<Album/EP/…> Release" / "Release Party" promo tag ("Hawt Coco Album
 *   Release" → "Hawt Coco").
 * The hyphen tails require a `<space>-<space>` boundary and a recognized marker
 * (`tour`, or a number + `years`/`jahre`), so an undecorated hyphenated name like
 * "BAD COMPANY LEGACY - Dave Colwell" is left intact. A whitespace boundary before
 * "Live" is likewise required, so a bare "Live" (the band) is never matched. The
 * relocation/reschedule marker is word-anchored and accepts an optional leading dash
 * (`-`/`–`/`—`), so both "… -Nachholtermin …" and "… Nachholtermin …" spellings are
 * caught. The bare "Release" tag requires a preceding format word (Album/EP/…) or a
 * "Party"/"Show" tail, so a band named just "Release" survives. The parenthetical is
 * keyed on the format word, so an alias in parentheses (e.g. "Sickboyrari (Black Kray)") is kept.
 */
private val ARTIST_SUFFIX_PATTERN =
    Regex(
        """\s+-\s+(?:\S.*\btour\b|\d+\s+(?:years?|jahre)\b).*$""" +
            """|\s+live(?:\s+in\s+\S.*)?$""" +
            """|\s*\((?:dj[\s-]?set|live|acoustic|akustik|unplugged|solo)\)\s*$""" +
            """|\s+dj[\s-]?set$""" +
            """|\s+[-–—]?\s*(?:nachholtermin|hochverlegung)\b.*$""" +
            """|\s+singt\s+\S.*$""" +
            """|\s+(?:album|ep|single|mixtape|record|tape)\s+release(?:\s+(?:party|show|special))?$""" +
            """|\s+release\s+(?:party|show)$""",
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
 * stripArtistSuffix("HGICH.T LIVE")                          // "HGICH.T"
 * stripArtistSuffix("THE BUTLERS - 40 YEARS, SKA -")         // "THE BUTLERS"
 * stripArtistSuffix("Avangelic (DJ-Set)")                    // "Avangelic"
 * stripArtistSuffix("Acid Arab DJ-Set")                      // "Acid Arab"
 * stripArtistSuffix("The Dear Hunter -Nachholtermin vom …")  // "The Dear Hunter"
 * stripArtistSuffix("OCT (On Company Time) – Hochverlegung") // "OCT (On Company Time)"
 * stripArtistSuffix("Tex singt Leonard Cohen")               // "Tex"
 * stripArtistSuffix("Hawt Coco Album Release")               // "Hawt Coco"
 * stripArtistSuffix("Sickboyrari (Black Kray)")              // "Sickboyrari (Black Kray)"
 * ```
 */
fun stripArtistSuffix(name: String): String {
    val stripped = name.trim().replace(ARTIST_SUFFIX_PATTERN, "").trim()
    return stripped.ifBlank { name.trim() }
}

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
 * Manually curated one-off titles that are not performers but that no structural
 * rule safely catches — a warm-up slot at a specific room, a package-tour name, a
 * recurring themed night, or a venue's own party/DJ series that its structured
 * data lists as the "performer" (Bi Nuu). Entries are lowercase, accent-free, and
 * whitespace-collapsed; before matching, a title is normalized the same way
 * ([isDenylistedNonArtist]) — diacritics stripped, and a trailing edition number
 * or `Berlin` locality ignored — so one entry folds every surface form of a series:
 * the plain `… 5` form (`FEMALE-FRONTED IS NOT A GENRE 5`), the `… N°<n>` form
 * (`Boheme Sauvage N°141`), and the accented, city-suffixed form
 * (`Bohème Sauvage Berlin`). Add exact titles here as they surface.
 */
private val NON_ARTIST_NAMES: Set<String> =
    setOf(
        "warm up im franken",
        "the revival tour",
        "female-fronted is not a genre",
        "music quiz",
        "open mic l. j. fox",
        "feinster hiphop",
        "karrera klub",
        "the swag jam",
        "groovejet",
        "ultra night",
        "boheme sauvage"
    )

/**
 * A trailing edition number on a recurring event title, ignored when matching
 * [NON_ARTIST_NAMES]. Covers both the plain `… 5` form and the `… N°141` form
 * (optional `n°`/`nº` before the digits), so every edition of a series folds onto
 * one denylist entry.
 */
private val TRAILING_EDITION = Regex("""\s+(?:n[°º]\s*)?\d+$""", RegexOption.IGNORE_CASE)

/**
 * A trailing `Berlin` locality on a recurring-series title (`GrooveJet Berlin`,
 * `Bohème Sauvage Berlin`), ignored when matching [NON_ARTIST_NAMES] so a series
 * folds onto one city-free entry. Matching-only — a real act merely ending in
 * `Berlin` (`Isolation Berlin`) drops the suffix too but is still absent from the
 * denylist, so it is kept.
 */
private val TRAILING_CITY = Regex("""\s+berlin$""")

/** Combining diacritical marks left by NFD normalization; stripped so accents can't defeat a denylist match. */
private val DIACRITICS = Regex("""\p{Mn}+""")

private fun isDenylistedNonArtist(name: String): Boolean =
    Normalizer
        .normalize(name.trim().replace(WHITESPACE, " ").lowercase(), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .replace(TRAILING_EDITION, "")
        .replace(TRAILING_CITY, "")
        .trim() in NON_ARTIST_NAMES

/**
 * A bare "DJ set" performance-format label, optionally carrying a `/ <origin>` tail
 * — `DJ-Set`, `DJ Set`, `DJ-Set / Berlin`. Venues occasionally push this format/city
 * descriptor into a performer slot (e.g. a Madame Claude detail-page heading), where
 * it must not be minted as an artist. Anchored: the whole trimmed value must be the
 * label (± origin), so a real act whose name merely starts with "DJ Set…" — or any
 * `DJ <handle>` name like `DJ Koze` — is untouched.
 */
private val DJ_SET_LABEL_PATTERN = Regex("""dj[\s-]?set(?:\s*/.*)?""", RegexOption.IGNORE_CASE)

/**
 * Checks whether [name] is a bare `DJ set` format label ("DJ-Set", "DJ-Set / Berlin")
 * rather than a performer. See [DJ_SET_LABEL_PATTERN]; matching is fully anchored so
 * `DJ Koze` / `DJ Set Sail` are kept.
 */
fun isDjSetFormatLabel(name: String): Boolean = DJ_SET_LABEL_PATTERN.matches(name.trim().replace(WHITESPACE, " "))

/**
 * True when [name] must never be stored as an artist: a placeholder ("TBA"), a bare
 * role label ("Special Guest"), an event-segment label ("Acid Aftershow"), an event
 * label ("Shred Fest"), a bare "DJ set" format label ("DJ-Set / Berlin"), or a curated
 * one-off non-artist title ("The Revival Tour"). The single predicate applied wherever
 * scraped headliner/support names are resolved.
 */
fun isNonArtistName(name: String): Boolean =
    isPlaceholderName(name) || isNonArtistLabel(name) || isEventSegmentLabel(name) ||
        isNonArtistEvent(name) || isDjSetFormatLabel(name) || isDenylistedNonArtist(name)

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
 * tail rather than a second act, so the boundary is kept joined. Two families,
 * unioned in [CONJUNCTION_TAIL_MARKERS]:
 * - articles/possessives opening a backing band ("X & **the** Ys", "X and **his**
 *   Ys", "X und **die** Ys"); and
 * - collective nouns naming an unnamed supporting cast ("X & **Friends**", "X &
 *   **Guests**", "X & **Gäste**"), a billing convention where the act is "X",
 *   not a separate act literally called "Friends".
 */
private val CONJUNCTION_TAIL_ARTICLES: Set<String> =
    setOf("the", "his", "her", "their", "los", "las", "die", "der", "das", "el", "la")

private val CONJUNCTION_TAIL_COLLECTIVES: Set<String> = setOf("friends", "guests", "gäste", "freunde")

/** Right-hand-side opener words that keep a conjunction boundary joined — see the two source sets. */
private val CONJUNCTION_TAIL_MARKERS: Set<String> = CONJUNCTION_TAIL_ARTICLES + CONJUNCTION_TAIL_COLLECTIVES

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
 * its right-hand side opens with a [tail marker][CONJUNCTION_TAIL_MARKERS] — an
 * article/possessive (the "X and the Ys" pattern) or a collective like "Friends" —
 * so `CARL CARLTON & MELANIE WIEGMANN AND THE GREAT BAND` cuts only at the `&`.
 * Each act keeps its original conjunction spelling (no rewrite).
 *
 * Splits **only** on `&`/`and`/`und` — never on `/` or `+` — so a venue that uses
 * `/` inside a single act name (e.g. Madame Claude's `Morimoto / Wong duo`) can
 * pre-split its co-bills on its own separator and hand each segment here to safely
 * break just the conjunctions. Public for that reuse; [splitSupportActs] and
 * [splitHeadlinerTitle] apply it after their own hard-separator split.
 *
 * Example:
 * ```kotlin
 * splitSegmentOnConjunctions("Lichene & Neue K")            // ["Lichene", "Neue K"]
 * splitSegmentOnConjunctions("Scott Hepple & The Sun Band") // ["Scott Hepple & The Sun Band"] (article tail)
 * splitSegmentOnConjunctions("Morimoto / Wong duo")         // ["Morimoto / Wong duo"] (no "/" split)
 * ```
 */
@Suppress("ReturnCount") // Guard clauses for the comma and no-cut cases are clearer than nesting
fun splitSegmentOnConjunctions(segment: String): List<String> {
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
                    CONJUNCTION_TAIL_MARKERS
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
 * A leading recurring-series label ending in an edition marker "#<n>:" —
 * "OFF THE RAILS #5: …", "Off the Rails #4: …". The series name is not a performer;
 * the acts follow the colon. Non-greedy up to the first "#<n>:", and requires a
 * non-blank series name before it, so a plain "9:3" or "H2:O" (no `#`) is untouched.
 */
private val SERIES_PREFIX_PATTERN = Regex("""^.+?#\s*\d+\s*:\s*""")

/**
 * Strips a leading "<series> #<n>:" recurring-series label from a title so the acts
 * billed after the colon are what remains — `"OFF THE RAILS #5: Blake Harley &
 * Superior Motive"` → `"Blake Harley & Superior Motive"`. Returns the input unchanged
 * when there is no such prefix, or when stripping would leave nothing.
 */
fun stripSeriesPrefix(title: String): String {
    val stripped = title.trim().replaceFirst(SERIES_PREFIX_PATTERN, "").trim()
    return stripped.ifBlank { title.trim() }
}

/**
 * A leading "A night with" / "An evening with" / "Ein Abend mit" event-framing phrase,
 * stripped from a title-derived headliner so the billed act remains ("A night with
 * GULVØSS II" → "GULVØSS II"). Title-scoped (see [headlinersFromTitle]): these phrases
 * frame a whole event, never appear inside a lineup entry, and the stored event title is
 * left untouched — only the derived artist name is recovered.
 */
private val ARTIST_FRAMING_PREFIX =
    Regex("""^(?:a\s+night\s+with|an\s+evening\s+with|ein\s+abend\s+mit)\s+""", RegexOption.IGNORE_CASE)

/** Strips a leading [ARTIST_FRAMING_PREFIX], keeping the input when stripping would leave nothing. */
private fun stripFramingPrefix(name: String): String {
    val stripped = name.replaceFirst(ARTIST_FRAMING_PREFIX, "").trim()
    return stripped.ifBlank { name.trim() }
}

/**
 * Turns an event title into its headliner artist entries: strip a recurring-series
 * "#<n>:" prefix via [stripSeriesPrefix] so the billed acts remain, split co-billed
 * acts via [splitHeadlinerTitle], strip an "A night with …" framing prefix and any
 * tour/live/note suffix ([stripArtistSuffix]) to recover the performer, then drop
 * anything that is not an artist ([isNonArtistName] — placeholders, role labels,
 * segments, festivals). Returned in billing order (title order); the caller appends
 * support acts.
 */
fun headlinersFromTitle(title: String): List<ScrapedArtist> =
    splitHeadlinerTitle(stripSeriesPrefix(title))
        .map { stripFramingPrefix(stripArtistSuffix(it)) }
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
