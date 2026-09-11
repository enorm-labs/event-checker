package de.norm.events.promoter

import de.norm.events.common.deshoutWord
import de.norm.events.common.isShortInitialism

// Promoter-name canonicalization for the scraper pipeline.
//
// The same real-world promoter is written many ways across venue websites: an abbreviated label on
// one site ("LOFT"), a fuller trading name on another ("Loft Concerts GmbH"). Resolving promoters by
// `slugify(name)` alone therefore fragments one promoter into several rows. [canonicalPromoterName]
// reduces those variants to a shared canonical form *before* slugging.
//
// The transform is deliberately conservative and deterministic:
//   1. Strip a *trailing* run of legal-form and generic-descriptor words (GmbH, UG, …, "Concerts",
//      "Konzerte", "Music", "Events", …). Only trailing words, so a name whose descriptor is
//      load-bearing and not at the end — "Concert Concept" — is left intact.
//   2. De-shout ALL-CAPS words ("SIMPLY QUIZ" → "Simply Quiz") for an order-independent display
//      name, with the [deshoutWord] the artist normalizer uses, so its acronym list and stylised
//      tokens apply here too ("TV NOIR" → "TV Noir"); intentional mixed casing ("GreyZone") is
//      preserved.
//   3. Fold known source typos and spacing variants onto one canonical spelling via a curated map
//      ("Trinty" → "Trinity", "Allrooms" → "All Rooms"). The lookup key is punctuation- and
//      space-insensitive, so one entry covers "All Rooms", "Allrooms" and "ALLROOMS" alike. Only
//      exact (normalized) matches are corrected — fuzzy matching would risk merging genuinely
//      distinct promoters.
//
// At least one word is always kept, and stripping never leaves a residue nobody would search for:
// "Records" keeps its single word, "36 Concerts" does not collapse to the bare number "36", and
// "HB Music" does not collapse to the bare initialism "HB". That last residue is also kept in its
// capitals rather than de-shouted to "Hb", which is what the strip-then-de-shout order used to do.
//
// Accepted: a *leading* descriptor is not stripped, so "Konzertbüro Schoneberg" does not merge with
// "Schoneberg Konzerte" without an explicit correction-map entry. Artists are deliberately not
// normalized this way — stripping words from band names is unsafe.

/**
 * Returns the canonical form of a promoter [raw] name (see file header). Falls
 * back to the trimmed input when normalization would leave nothing.
 */
fun canonicalPromoterName(raw: String): String {
    // Drop a trailing parenthetical annotation ("Mind Enterprises GmbH (wf)" → "Mind
    // Enterprises GmbH") before tokenizing, so the descriptor-strip below can reach the
    // real legal-form/descriptor words that the annotation was shielding.
    val withoutAnnotation =
        raw
            .trim()
            .replace(TRAILING_PAREN_REGEX, "")
            .trim()
            .ifBlank { raw.trim() }
    val tokens =
        withoutAnnotation
            .split(WHITESPACE_REGEX)
            .filter { it.isNotBlank() }
            .toMutableList()
    if (tokens.isEmpty()) return raw.trim()

    // Drop the trailing run of legal-form / descriptor / connector tokens, but always keep a
    // usable name: stripping "Concerts" off "36 Concerts" would leave the bare number "36", and
    // stripping "Music" off "HB Music" the bare initialism "HB".
    while (tokens.size > 1 && tokens.last().isStrippableTrailingWord() && tokens.dropLast(1).isUsableName()) {
        tokens.removeAt(tokens.lastIndex)
    }

    // The initialism the guard above kept a descriptor for is an initialism, so it keeps its
    // capitals ("HB Music", not "Hb Music"). Every other token de-shouts as usual.
    val keepFirst = tokens.first().isShortInitialism() && tokens.drop(1).all { it.isStrippableTrailingWord() }
    val canonical =
        tokens
            .mapIndexed { index, token -> if (index == 0 && keepFirst) token else token.deshoutWord() }
            .joinToString(" ")
            .ifBlank { raw.trim() }
    return NAME_CORRECTIONS[canonical.normalizedKey()] ?: canonical
}

/** Whether these tokens still name something: a letter-bearing word that is not a lone initialism. */
private fun List<String>.isUsableName(): Boolean = any { word -> word.any(Char::isLetter) } && !(size == 1 && first().isShortInitialism())

/**
 * Whether [raw] is not a real promoter but a bare generic label — a name that, once a
 * trailing parenthetical is dropped, consists *entirely* of legal-form and descriptor
 * words ([STRIP_WORDS]) or punctuation, with no distinctive token left. Filters junk
 * like "Event." or "Konzert" that a source drops into the promoter slot, before
 * [canonicalPromoterName] (which always keeps one word and so can't drop them itself).
 * A name with any distinctive word ("Concert Concept", "Loft Concerts GmbH") is kept.
 */
fun isNonPromoterName(raw: String): Boolean {
    val tokens =
        raw
            .trim()
            .replace(TRAILING_PAREN_REGEX, "")
            .split(WHITESPACE_REGEX)
            .filter { it.isNotBlank() }
    return tokens.isEmpty() || tokens.all { it.isStrippableTrailingWord() }
}

/** Lowercased, punctuation-free lookup key for a name (matches [String.isStrippableTrailingWord]'s scheme). */
private fun String.normalizedKey(): String = lowercase().replace(NON_WORD_REGEX, "")

/** A token is strippable if, stripped of punctuation, it is empty (a connector like "&") or a known word. */
private fun String.isStrippableTrailingWord(): Boolean {
    val key = lowercase().replace(NON_WORD_REGEX, "")
    return key.isEmpty() || key in STRIP_WORDS
}

private val WHITESPACE_REGEX = Regex("""\s+""")

/** A trailing parenthetical annotation (" (wf)", " (GSA)") appended to a promoter name. */
private val TRAILING_PAREN_REGEX = Regex("""\s*\([^)]*\)\s*$""")

/** Everything except letters (incl. German umlauts) and digits — used to normalize a token for lookup. */
private val NON_WORD_REGEX = Regex("""[^a-z0-9äöüß]""")

/**
 * Trailing words removed during canonicalization: German/English legal forms plus
 * generic promoter descriptors. Kept intentionally tight to limit accidental merges;
 * extend it as new venues surface new descriptor conventions.
 */
private val STRIP_WORDS: Set<String> =
    setOf(
        // Legal forms
        "gmbh",
        "mbh",
        "ug",
        "gbr",
        "kg",
        "ohg",
        "ag",
        "ev",
        "ou",
        "oü",
        "ltd",
        "llc",
        "inc",
        "co",
        // Generic promoter descriptors
        "concert",
        "concerts",
        "konzert",
        "konzerte",
        "music",
        "musik",
        "events",
        "event",
        "booking",
        "agency",
        "agentur",
        "promotion",
        "promotions",
        "entertainment",
        "live",
        "records",
        "production",
        "productions",
        // Presenter *verbs* a promoter appends to its own name when it heads a billing
        // ("porcupine records & little league shows prsnt:"). Punctuation is stripped before the
        // lookup, so the trailing colon is already handled.
        //
        // The plain English "presents" is deliberately **absent**: it is a brand word as often as a
        // verb — "AEG Presents" is the company's actual name, and stripping it would leave "Aeg".
        "prsnt",
        "prsnts",
        "presenting",
        "präsentiert",
        "präsentieren"
    )

/**
 * Known name corrections — source typos and spelling/spacing variants — keyed on the
 * [String.normalizedKey] of the canonicalized name and mapped to the correct display
 * spelling. The key is punctuation- and space-insensitive, so a single entry folds every
 * spacing/casing variant with the same letters (e.g. "All Rooms" / "Allrooms" / "ALLROOMS").
 * Only add entries that are unambiguously the same real promoter — this map merges promoter
 * entities, so a wrong entry silently collapses two distinct promoters into one.
 *
 * Because the correction runs *after* descriptor-stripping, an entry can also pin a fuller
 * display form that includes a stripped descriptor: "LOFT", "Loft Concerts" and
 * "Loft Concerts GmbH" all reduce to "Loft" first, so the single "loft" entry restores the
 * preferred brand name "Loft Concerts" for every variant.
 */
private val NAME_CORRECTIONS: Map<String, String> =
    mapOf(
        // "Music" is a stripped descriptor, so "Trinity Music" and "Trinity" both reduce to this
        // key; the entry restores the agency's trading name, as "loft" does below (#1139).
        "trinity" to "Trinity Music",
        "trinty" to "Trinity Music",
        // Huxleys' taxonomy slug drops the first word of "Konzertbüro Schoneberg"; folded here so
        // rows minted from the slug resolve to the same promoter as the visible credit.
        "schoneberg" to "Konzertbüro Schoneberg",
        "konzertbüroschoneberg" to "Konzertbüro Schoneberg",
        "radioactve" to "Radioactive",
        "allrooms" to "All Rooms",
        "loft" to "Loft Concerts",
        // Three sources, three spellings — "FluxFM" (Columbia Theater, Frannz), "fluxfm"
        // (Heimathafen) and "Flux FM" (Zitadelle), the last of which de-shouts to "Flux Fm".
        // All four share this key, so one entry folds them onto the station's own casing.
        "fluxfm" to "FluxFM",
        // The station spells itself in one lowercase word; one venue writes it "Radio Eins".
        // Both share this key, so the entry folds them onto the broadcaster's own branding.
        "radioeins" to "radioeins",
        // "tipBerlin" (one venue), "tip Berlin" (another) and the bare "Tip" Zitadelle prints are
        // the city magazine, and no other promoter in the corpus is called "Tip" (#304).
        "tipberlin" to "tip Berlin",
        "tip" to "tip Berlin",
        // The tour agency appears both abbreviated and under its full trading name. The shared
        // acronym list keeps "KKT" in its capitals; the first entry still catches a "Kkt" that
        // an earlier import stored before it did (#304).
        "kkt" to "KKT",
        "kktgmbhkikiskleinertourneeservice" to "KKT"
    )
