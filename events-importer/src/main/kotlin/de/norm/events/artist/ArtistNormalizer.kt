package de.norm.events.artist

// Artist-name canonicalization for the scraper pipeline.
//
// The same act is written many ways across (and within) venue websites: ALL-CAPS
// on one site ("GREEN LUNG"), mixed case on another ("Green Lung"). Because slugs
// are case-insensitive, these already resolve to a single artist row — but
// whichever import creates the row first also fixes its *display name*, so an act
// can be stored SHOUTING forever. [canonicalArtistName] de-shouts the name to a
// clean, stable display form before it is persisted.
//
// Unlike promoters, artist names are normalized *casing-only* — no words are ever
// stripped, because every word in a band name can be load-bearing ("The The",
// "Wolf Alice", "Girl Band", even "Arcade Fire Concerts"). The transform is
// deterministic and de-shouts each shouted ALL-CAPS word to title case, keeping
// any attached punctuation in place ("GREEN LUNG" -> "Green Lung",
// "MURPHY'S LAW" -> "Murphy's Law", "(BLACK KRAY)" -> "(Black Kray)"), while
// leaving three kinds of token untouched:
//   - tokens that already carry a lowercase letter — intentional styling survives
//     ("DJ Koze", "will.i.am", "GoGo", "El Flecha Negra");
//   - tokens with a digit or an interior "." / "/", i.e. stylised names and dotted
//     initialisms, not plain words ("MC5", "UB40", "H2O", "AC/DC", "R.E.M.");
//   - recognised acronyms in [ACRONYMS] ("DJ", "MC", "UK") — so "DJ KOZE" becomes
//     "DJ Koze", not "Dj Koze";
//   - a name that is a *single* short all-caps token (≤ [SHORT_INITIALISM_MAX_LEN]
//     letters: "JJ", "EV", "YU", "MØ") — a standalone two-letter all-caps name is an
//     initialism/stylisation far more often than a shouted word, and title-casing it
//     to "Jj"/"Ev" reads as a typo. Scoped to the whole name so a short *word* inside
//     a multi-word name still de-shouts ("WARS OF ATTRITION" -> "Wars of Attrition").
//
// Known limits (accepted): a genuine all-caps name of three-plus letters that isn't
// in [ACRONYMS] ("ABBA", "MGMT", "MUNA") is title-cased like any shouted word, because
// it is indistinguishable from one without a lookup table — extend [ACRONYMS] when a
// real act needs its capitals kept. Names stylised with other interior symbols
// ("BIGA*RANX", "OI!STURM") are likewise title-cased. This is display-only: slugs are
// case-insensitive, so the resolved artist row is unaffected.

/**
 * Returns the de-shouted display form of an artist [raw] name (see file header).
 * Casing-only: no words are added or removed. Falls back to the trimmed input
 * when normalization would leave nothing.
 */
fun canonicalArtistName(raw: String): String {
    val trimmed = raw.trim()
    val tokens = trimmed.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
    // A whole name that is a single short all-caps token is an initialism/stylisation
    // ("JJ", "MØ"), not a shouted word — keep it verbatim rather than minting "Jj"/"Mø".
    if (tokens.size == 1 && tokens[0].isShortStandaloneInitialism()) return tokens[0]
    return tokens.joinToString(" ") { it.deshoutWord() }.ifBlank { trimmed }
}

/**
 * Whether the token is a standalone short initialism to keep verbatim: only letters,
 * no lowercase, and at most [SHORT_INITIALISM_MAX_LEN] characters long. Only applied
 * to a single-token name (see [canonicalArtistName]) so it never freezes a short word
 * ("OF", "MY") inside a longer shouted name.
 */
private fun String.isShortStandaloneInitialism(): Boolean =
    length <= SHORT_INITIALISM_MAX_LEN && any { it.isLetter() } && none { it.isLowerCase() || it.isDigit() }

/** Max length of a single-token all-caps name kept as an initialism rather than de-shouted. */
private const val SHORT_INITIALISM_MAX_LEN = 2

/** Title-cases a shouted word (see [isShoutedWord]); returns any other token unchanged. */
private fun String.deshoutWord(): String = if (isShoutedWord()) titleCaseKeepingPunctuation() else this

/**
 * A token is a shouted word — safe to title-case — when it has letters, no lowercase, no
 * digit or interior "." / "/" (which mark stylised names and dotted initialisms), and is not
 * a recognised acronym. Punctuation like apostrophes, parentheses, "!" or "," does not exempt
 * it, so possessives and bracketed words de-shout too ("MURPHY'S" -> "Murphy's").
 */
private fun String.isShoutedWord(): Boolean =
    any { it.isLetter() } &&
        none { it.isLowerCase() } &&
        none { it.isDigit() || it in STYLISED_CHARS } &&
        uppercase() !in ACRONYMS

/** Uppercases the first letter, lowercases the rest, leaving every non-letter character in place. */
private fun String.titleCaseKeepingPunctuation(): String {
    var seenLetter = false
    return buildString {
        for (ch in this@titleCaseKeepingPunctuation) {
            when {
                !ch.isLetter() -> append(ch)
                !seenLetter -> append(ch.uppercaseChar()).also { seenLetter = true }
                else -> append(ch.lowercaseChar())
            }
        }
    }
}

/** Interior characters that mark a token as a stylised name or dotted initialism, not a plain word. */
private val STYLISED_CHARS = setOf('.', '/')

private val WHITESPACE_REGEX = Regex("""\s+""")

/**
 * Acronyms/initialisms kept in their capitals when they appear as a standalone word,
 * so "DJ KOZE" de-shouts to "DJ Koze" rather than "Dj Koze". Deliberately tight —
 * a broader set risks freezing genuine words in caps — and extended as new music
 * acronyms surface. Compared case-insensitively (see [isShoutedWord]).
 */
private val ACRONYMS: Set<String> =
    setOf(
        "DJ",
        "MC",
        "VJ",
        "UK",
        "US",
        "USA",
        "FM",
        "AM",
        "TV",
        "EP",
        "LP",
        "NYC",
        "LA",
        "EDM",
        "DIY",
        "RIP",
        // Act names that are themselves initialisms — kept in caps so they aren't flattened.
        "FKJ",
        "AZ",
        "DBG"
    )
