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
//     "DJ Koze", not "Dj Koze".
//
// Known limits (accepted): a genuine all-caps name that is only letters and isn't
// in [ACRONYMS] ("ABBA", "MGMT", "MUNA", "MØ") is title-cased like any shouted
// word, because it is indistinguishable from one without a lookup table — extend
// [ACRONYMS] when a real act needs its capitals kept. Names stylised with other
// interior symbols ("BIGA*RANX", "OI!STURM") are likewise title-cased. This is
// display-only: slugs are case-insensitive, so the resolved artist row is unaffected.

/**
 * Returns the de-shouted display form of an artist [raw] name (see file header).
 * Casing-only: no words are added or removed. Falls back to the trimmed input
 * when normalization would leave nothing.
 */
fun canonicalArtistName(raw: String): String =
    raw
        .trim()
        .split(WHITESPACE_REGEX)
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.deshoutWord() }
        .ifBlank { raw.trim() }

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
