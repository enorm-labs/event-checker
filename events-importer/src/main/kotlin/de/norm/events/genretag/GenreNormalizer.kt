package de.norm.events.genretag

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

// Genre normalization utilities shared between the admin API (event module)
// and the scraper pipeline (scraper module).
//
// Parses free-text genre strings scraped from venue websites into canonical
// genre tag names suitable for structured filtering. The raw genre text is
// preserved on the event for display; these normalized tags power the
// frontend's genre filter.

/**
 * Synonym mapping from lowercase variations to canonical genre tag names.
 *
 * Covers common spelling differences, abbreviations, and compound genre labels
 * observed across Berlin venue websites. Canonical names use title case for
 * consistent display (e.g. "Hip Hop", not "hip hop" or "HIP HOP").
 *
 * When adding new venues, extend this map with any new synonyms encountered
 * in their genre labelling.
 */
private val GENRE_SYNONYMS: Map<String, String> =
    mapOf(
        // Hip Hop family
        "hip hop" to "Hip Hop",
        "hip-hop" to "Hip Hop",
        "hiphop" to "Hip Hop",
        "rap" to "Hip Hop",
        "urban" to "Hip Hop",
        "experimental hip-hop" to "Hip Hop",
        // Rock family
        "rock" to "Rock",
        "alternative rock" to "Rock",
        "alternative" to "Alternative",
        "alternative / indie" to "Alternative",
        // Indie family
        "indie" to "Indie",
        "indie pop" to "Indie",
        "indie-pop" to "Indie",
        "indie rock" to "Indie",
        // Pop family
        "pop" to "Pop",
        "pop punk" to "Punk",
        "synthpop" to "Synthpop",
        // Punk
        "punk" to "Punk",
        // Metal
        "metal" to "Metal",
        // Electronic family
        "electronica" to "Electronic",
        "elektro-fusion" to "Electronic",
        "synthesizer instrumental & melodic electronica" to "Electronic",
        "techno" to "Techno",
        "house" to "House",
        // Post-punk / dark wave family
        "post punk" to "Post-Punk",
        "postpunk" to "Post-Punk",
        "new wave" to "New Wave",
        "darkwave" to "Darkwave",
        "ebm" to "EBM",
        "gothicrock" to "Gothic Rock",
        "gothic rock" to "Gothic Rock",
        // Soul / Funk / R&B family
        "soul" to "Soul",
        "funk" to "Funk",
        "boogaloo" to "Funk",
        "r&b" to "R&B",
        "rnb" to "R&B",
        // Jazz
        "jazz" to "Jazz",
        "jazz-fusion" to "Jazz",
        // Folk
        "folk" to "Folk",
        // Reggae
        "reggae" to "Reggae",
        // Latin family
        "cumbia" to "Latin",
        "salsa" to "Latin",
        "latin roots" to "Latin",
        // World Music
        "world music" to "World Music",
        "indian" to "World Music",
        "urdu rock" to "World Music",
        // Disco
        "disco" to "Disco",
        // Karaoke
        "karaoke" to "Karaoke",
        // Decades / party labels
        "80s" to "80s",
        "90s" to "90s",
        "2000s" to "2000s",
        // Oldschool / Newschool
        "oldschool" to "Old School",
        "newschool" to "New School"
    )

/**
 * Delimiters used to split raw genre strings into individual genre tokens.
 *
 * Handles the variety of separators observed in venue data:
 * - `,` — most common (e.g. "Pop, Rock, Indie")
 * - `//` — Cassiopeia floor descriptions (e.g. "80s Floor // Hip Hop Floor")
 * - `&` — compound genres (e.g. "80s, Disco & Hip Hop")
 * - `/` — slash-separated alternatives (e.g. "Alternative / Indie")
 *
 * Note: `&` and `/` can appear inside genre names (e.g. "R&B"), so we split
 * on them only when surrounded by spaces to avoid false splits.
 */
private val GENRE_DELIMITERS = Regex("""[,]|//|\s[&/]\s""")

/**
 * Suffixes commonly appended to genre names in venue listings that should
 * be stripped before normalization (e.g. "Hip Hop Floor", "Pop Disco Floor").
 */
private val NOISE_SUFFIXES = listOf("floor", "disco floor")

/**
 * Parses a raw genre string into a list of canonical genre tag names.
 *
 * The normalization pipeline:
 * 1. Split on common delimiters (`,`, `//`, ` & `, ` / `).
 * 2. Trim whitespace and strip noise suffixes ("Floor", "Disco Floor").
 * 3. Look up each token in the synonym map (case-insensitive).
 * 4. Deduplicate — multiple raw tokens may map to the same canonical name.
 *
 * Tokens that don't match any synonym are included as-is with title case,
 * so new genres are automatically captured without requiring synonym updates.
 * The synonym map only handles known variations that should be merged.
 *
 * @param rawGenre the free-text genre string from the scraped event, or null.
 * @return a deduplicated list of canonical genre tag names, or empty if input is null/blank.
 *
 * Examples:
 * ```
 * normalizeGenre("Hip Hop")                    → ["Hip Hop"]
 * normalizeGenre("Pop Punk, Indie, Karaoke")   → ["Punk", "Indie", "Karaoke"]
 * normalizeGenre("80s, Rock, New Wave")        → ["80s", "Rock", "New Wave"]
 * normalizeGenre("Postpunk, Gothicrock, Darkwave, EBM und Synthpop etc.")
 *                                              → ["Post-Punk", "Gothic Rock", "Darkwave", "EBM", "Synthpop"]
 * normalizeGenre(null)                         → []
 * ```
 */
fun normalizeGenre(rawGenre: String?): List<String> {
    if (rawGenre.isNullOrBlank()) return emptyList()

    return rawGenre
        .split(GENRE_DELIMITERS)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { stripNoise(it) }
        .filter { it.isNotBlank() }
        // Re-split after noise stripping — stripNoise may introduce new commas
        // (e.g. "EBM und Synthpop" → "EBM, Synthpop")
        .flatMap { it.split(",") }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .flatMap { token -> resolveGenre(token) }
        .distinct()
}

/**
 * Strips noise suffixes and common filler words from a genre token.
 *
 * Handles patterns like "Hip Hop & Urban Disco Floor" → "Hip Hop & Urban"
 * and trailing "etc." from venue listings.
 */
private fun stripNoise(token: String): String {
    var cleaned =
        token
            .replace(Regex("""\s+etc\.?$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+und\s+""", RegexOption.IGNORE_CASE), ", ")
            .trim()

    // Strip "Floor" / "Disco Floor" suffixes (case-insensitive)
    for (suffix in NOISE_SUFFIXES) {
        if (cleaned.endsWith(suffix, ignoreCase = true)) {
            cleaned = cleaned.dropLast(suffix.length).trim()
        }
    }
    return cleaned
}

/**
 * Resolves a single cleaned genre token to a list of canonical genre names.
 *
 * Resolution strategy (in order):
 * 1. Direct synonym lookup (case-insensitive) — returns a single canonical name.
 * 2. Word-level synonym matching — splits the token into words and returns **all**
 *    matched genres. This handles compound freeform labels like "Superheavy Funky
 *    Soul & Boogaloo" → ["Funk", "Soul"] instead of silently discarding matches.
 * 3. No match — returns the original token with title case as a new genre.
 *
 * Tokens that are clearly not genre names (too short, "from …" prefixes) are
 * filtered out by returning an empty list.
 */
@Suppress("ReturnCount") // Multiple early returns improve readability for this cascading lookup
private fun resolveGenre(token: String): List<String> {
    val lower = token.lowercase().trim()

    // Skip tokens that are clearly not genre names
    if (lower.length < 2) return emptyList()
    if (lower.startsWith("all kinds of")) {
        val genre =
            GENRE_SYNONYMS[
                lower
                    .substringAfter("all kinds of")
                    .trim()
                    .split(" ")
                    .first()
            ]
        return listOfNotNull(genre)
    }
    if (lower.startsWith("from ")) return emptyList()

    // Direct synonym match
    GENRE_SYNONYMS[lower]?.let { return listOf(it) }

    // Try matching individual words if the full token didn't match
    // (handles compound freeform labels like "Superheavy Funky Soul & Boogaloo")
    val words = lower.split(Regex("""\s+"""))
    val matched = words.mapNotNull { GENRE_SYNONYMS[it] }.distinct()
    if (matched.isNotEmpty()) return matched

    // No match — use the original token with title case as a new genre
    logger.info { "No synonym match for genre token '$token', using as-is" }
    val titleCased =
        token.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
    return listOf(titleCased)
}
