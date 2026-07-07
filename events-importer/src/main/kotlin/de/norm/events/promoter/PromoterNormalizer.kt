package de.norm.events.promoter

// Promoter-name canonicalization for the scraper pipeline.
//
// The same real-world promoter is written many ways across (and within) venue
// websites: an abbreviated label on one site ("LOFT"), a fuller trading name on
// another ("Loft Concerts GmbH"). Resolving promoters by `slugify(name)` alone
// therefore fragments one promoter into several rows. [canonicalPromoterName]
// reduces those variants to a shared canonical form *before* slugging, so they
// resolve to a single promoter entity.
//
// The transform is deliberately conservative and deterministic:
//   1. Strip a *trailing* run of legal-form and generic-descriptor words
//      (GmbH, UG, …, "Concerts", "Konzerte", "Music", "Events", …). Only trailing
//      words are stripped, so a name whose descriptor is load-bearing and not at
//      the end — e.g. "Concert Concept" — is left intact.
//   2. "De-shout" ALL-CAPS words ("SIMPLY QUIZ" → "Simply Quiz") for a clean,
//      order-independent display name; intentional mixed casing ("GreyZone") is
//      preserved.
//   3. Fold known source typos and spelling/spacing variants onto a single
//      canonical spelling via an explicit, curated map ("Trinty" → "Trinity",
//      "Allrooms" → "All Rooms"). The lookup key is punctuation- and
//      space-insensitive, so one entry covers "All Rooms", "Allrooms" and
//      "ALLROOMS" alike. Only exact (normalized) matches are corrected — no
//      fuzzy/edit-distance matching, which would risk merging genuinely
//      distinct promoters.
// At least one word is always kept, so a promoter named purely of descriptor
// words (e.g. "Records") is never reduced to nothing.
//
// Known limits (accepted): a *leading* descriptor is not stripped, so
// "Konzertbüro Schoneberg" does not merge with "Schoneberg Konzerte" unless an
// explicit correction-map entry is added. Artists are intentionally *not*
// normalized this way — stripping words from band names is unsafe.

/**
 * Returns the canonical form of a promoter [raw] name (see file header). Falls
 * back to the trimmed input when normalization would leave nothing.
 */
fun canonicalPromoterName(raw: String): String {
    val tokens =
        raw
            .trim()
            .split(WHITESPACE_REGEX)
            .filter { it.isNotBlank() }
            .toMutableList()
    if (tokens.isEmpty()) return raw.trim()

    // Drop the trailing run of legal-form / descriptor / connector tokens, keeping >= 1 word.
    while (tokens.size > 1 && tokens.last().isStrippableTrailingWord()) {
        tokens.removeAt(tokens.lastIndex)
    }

    val canonical = tokens.joinToString(" ") { it.deshout() }.ifBlank { raw.trim() }
    return NAME_CORRECTIONS[canonical.normalizedKey()] ?: canonical
}

/** Lowercased, punctuation-free lookup key for a name (matches [String.isStrippableTrailingWord]'s scheme). */
private fun String.normalizedKey(): String = lowercase().replace(NON_WORD_REGEX, "")

/** A token is strippable if, stripped of punctuation, it is empty (a connector like "&") or a known word. */
private fun String.isStrippableTrailingWord(): Boolean {
    val key = lowercase().replace(NON_WORD_REGEX, "")
    return key.isEmpty() || key in STRIP_WORDS
}

/** Title-cases an ALL-CAPS word (>= 2 letters); leaves any word with lowercase letters untouched. */
private fun String.deshout(): String =
    if (length >= 2 && any { it.isLetter() } && none { it.isLowerCase() }) {
        this[0] + substring(1).lowercase()
    } else {
        this
    }

private val WHITESPACE_REGEX = Regex("""\s+""")

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
        "productions"
    )

/**
 * Known name corrections — source typos and spelling/spacing variants — keyed on the
 * [String.normalizedKey] of the canonicalized name and mapped to the correct display
 * spelling. The key is punctuation- and space-insensitive, so a single entry folds every
 * spacing/casing variant with the same letters (e.g. "All Rooms" / "Allrooms" / "ALLROOMS").
 * Only add entries that are unambiguously the same real promoter — this map merges promoter
 * entities, so a wrong entry silently collapses two distinct promoters into one.
 */
private val NAME_CORRECTIONS: Map<String, String> =
    mapOf(
        "trinty" to "Trinity",
        "radioactve" to "Radioactive",
        "allrooms" to "All Rooms"
    )
