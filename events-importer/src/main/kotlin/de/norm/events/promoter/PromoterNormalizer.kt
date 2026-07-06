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
// At least one word is always kept, so a promoter named purely of descriptor
// words (e.g. "Records") is never reduced to nothing.
//
// Known limits (accepted): a *leading* descriptor is not stripped, so
// "Konzertbüro Schoneberg" does not merge with "Schoneberg Konzerte"; and a
// spacing/abbreviation variant like "ALLROOMS" vs "All Rooms" cannot be merged
// because the token boundaries differ. Artists are intentionally *not*
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

    return tokens.joinToString(" ") { it.deshout() }.ifBlank { raw.trim() }
}

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
        "records",
        "production",
        "productions"
    )
