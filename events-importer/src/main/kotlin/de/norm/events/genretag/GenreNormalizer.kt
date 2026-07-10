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
 * Synonym mapping from a normalized lookup key to canonical genre tag names.
 *
 * Keys are *normalized* via [lookupKey] — lowercased with all separators
 * (spaces, hyphens, slashes, etc.) stripped — so a single entry covers every
 * spelling and spacing of the same label. For example the one key `"hiphop"`
 * matches "Hip Hop", "hip-hop", and "HIPHOP" alike; there is no need to list
 * each variant separately. Keep new keys in normalized form.
 *
 * The map therefore encodes only *semantic* knowledge — deliberate merges that
 * string normalization can't derive (e.g. "rap"/"urban" → Hip Hop, "boogaloo"
 * → Funk, "cumbia"/"salsa" → Latin). Canonical names use title case for
 * consistent display (e.g. "Hip Hop", not "hip hop" or "HIP HOP").
 *
 * When adding new venues, extend this map with any new semantic synonyms
 * encountered in their genre labelling.
 */
private val GENRE_SYNONYMS: Map<String, String> =
    mapOf(
        // Hip Hop family
        "hiphop" to "Hip Hop",
        "rap" to "Hip Hop",
        "deutschrap" to "Hip Hop",
        "urban" to "Hip Hop",
        "experimentalhiphop" to "Hip Hop",
        "trap" to "Hip Hop",
        // Rock family
        "rock" to "Rock",
        "alternativerock" to "Rock",
        "poprock" to "Rock",
        "bluesrock" to "Rock",
        "experimentalrock" to "Rock",
        "kraut" to "Krautrock",
        "krautrock" to "Krautrock",
        "alternative" to "Alternative",
        "alternativeindie" to "Alternative",
        // Indie family
        "indie" to "Indie",
        "indiepop" to "Indie",
        "indierock" to "Indie",
        "shoegaze" to "Shoegaze",
        // Pop family
        "pop" to "Pop",
        "deutschpop" to "Pop",
        "altpop" to "Pop",
        "elektropop" to "Pop",
        "queerpop" to "Pop",
        "poppunk" to "Punk",
        "synthpop" to "Synthpop",
        "synth" to "Synthpop",
        // Punk
        "punk" to "Punk",
        "emo" to "Emo",
        // Metal / heavy
        "metal" to "Metal",
        "metalcore" to "Metalcore",
        "melodichardcore" to "Melodic-Hardcore",
        // Electronic family
        "electronic" to "Electronic",
        "electronica" to "Electronic",
        "elektrofusion" to "Electronic",
        "synthesizerinstrumental&melodicelectronica" to "Electronic",
        "techno" to "Techno",
        "house" to "House",
        "afrohouse" to "House",
        "latinhouse" to "House",
        // Drum & Bass (delimiter-embedding name; see DRUM_AND_BASS_REGEX pre-pass)
        "drumnbass" to "Drum & Bass",
        "dnb" to "Drum & Bass",
        // Post-punk / dark wave family
        "postpunk" to "Post-Punk",
        "newwave" to "New Wave",
        "darkwave" to "Darkwave",
        "ebm" to "EBM",
        "gothicrock" to "Gothic Rock",
        // Soul / Funk / R&B family
        "soul" to "Soul",
        "neosoul" to "Soul",
        "indiesoul" to "Soul",
        "funk" to "Funk",
        "boogaloo" to "Funk",
        "r&b" to "R&B",
        "rnb" to "R&B",
        "altrnb" to "R&B",
        // Jazz / Blues
        "jazz" to "Jazz",
        "jazzfusion" to "Jazz",
        "latinjazz" to "Jazz",
        "blues" to "Blues",
        // Folk
        "folk" to "Folk",
        "americana" to "Americana",
        "singersongwriter" to "Singer-Songwriter",
        "singersongwriterin" to "Singer-Songwriter",
        // Reggae
        "reggae" to "Reggae",
        // Tango
        "tango" to "Tango",
        // Latin family
        "cumbia" to "Latin",
        "salsa" to "Latin",
        "latin" to "Latin",
        "latinroots" to "Latin",
        // Afrobeats
        "afro" to "Afrobeats",
        "afrobeat" to "Afrobeats",
        "afrobeats" to "Afrobeats",
        // World Music
        "world" to "World Music",
        "worldmusic" to "World Music",
        "global" to "World Music",
        "indian" to "World Music",
        "urdurock" to "World Music",
        // Disco
        "disco" to "Disco",
        // Karaoke
        "karaoke" to "Karaoke",
        // Decades / party labels
        "80s" to "80s",
        "90s" to "90s",
        "2000s" to "2000s",
        // Mod
        "mod" to "Mod",
        // Oldschool / Newschool
        "oldschool" to "Old School",
        "newschool" to "New School"
    )

/**
 * Non-genre tokens that some venues push into the genre field — event-format
 * labels, series names, and freeform fragments — which must never become a
 * genre tag. Cassiopeia in particular reuses the genre field for arbitrary
 * event labels ("Immersive Ausstellung", "… Special", "Karaoke"-style labels).
 *
 * Entries are stored as [lookupKey]-normalized keys and matched per word (see
 * [looksLikeGenre]), so a single entry drops the word wherever it appears —
 * "Immersive Ausstellung" and "Ausstellung" both fall out via `ausstellung`.
 * A word here only vetoes a token that has *no* recognised genre in it: a mixed
 * label like "Retro Pop" still resolves to Pop via word-level synonym matching,
 * which runs first. Keep deliberately tight so real genres aren't suppressed;
 * extend it as new non-genre labels surface in the `Dropping non-genre …` logs.
 */
private val NON_GENRE_TOKENS: Set<String> =
    setOf(
        // Event-format / listing labels
        "special",
        "immersive",
        "ausstellung",
        "exhibition",
        "vernissage",
        "lesung",
        "reading",
        "party",
        "festival",
        "show",
        "konzert",
        "concert",
        "presents",
        "present",
        "podcast",
        "markt",
        "support",
        "openair",
        "warmup",
        "aftershow",
        "release",
        "releaseparty",
        // Freeform fragments observed leaking as standalone tags
        "beyond",
        "wave",
        "retro",
        "nontango",
        // Audience / theme / series labels Gretchen pushes into the genre field.
        // Keys are [lookupKey]-normalized: it strips non-ASCII letters, so
        // "Männerparty" → "mnnerparty" (the ä is dropped) and "FLINTA*" → "flinta".
        "flinta",
        "mnnerparty",
        "fetish",
        "berbenautika"
    )

/**
 * Normalizes a raw genre token to a [GENRE_SYNONYMS] lookup key.
 *
 * Lowercases and strips every character that isn't `a-z`, `0-9`, or `&`, so all
 * spelling/spacing variations of one label collapse to a single key
 * (e.g. "Hip Hop", "hip-hop", "HIPHOP" → "hiphop"). `&` is kept so "R&B"/"rnb"
 * keys stay meaningful.
 */
private fun lookupKey(raw: String): String = raw.lowercase().replace(Regex("[^a-z0-9&]"), "")

/**
 * Delimiters used to split raw genre strings into individual genre tokens.
 *
 * Handles the variety of separators observed in venue data:
 * - `, ` — most common (e.g. "Pop, Rock, Indie")
 * - `/` — slash-separated alternatives, spaced or not (e.g. "Alternative / Indie",
 *   "Hip-Hop/Rap", "80s Floor // Hip Hop Floor")
 * - `&` — compound genres (e.g. "80s, Disco & Hip Hop")
 * - ` or ` / ` oder ` / ` vs ` — freeform alternatives (e.g. "Tango or NonTango")
 *
 * Note: `/` always splits (no genre name embeds a bare slash, whereas venues write
 * unspaced alternatives like "Hip-Hop/Rap" that must be torn apart). `&` and the word
 * separators *can* appear inside genre names (e.g. "R&B"), so those split only when
 * surrounded by spaces to avoid false splits. Separators are matched case-insensitively.
 */
private val GENRE_DELIMITERS = Regex("""[,/]|\s(?:&|or|oder|vs)\s""", RegexOption.IGNORE_CASE)

/**
 * Genre names whose canonical spelling embeds a [GENRE_DELIMITERS] character (the
 * " & " in "Drum & Bass", the "'n'" in "Drum'n'Bass") and would otherwise be torn
 * into "Drum" + "Bass". Collapsed to a single delimiter-free token *before* the
 * split so the whole name survives and resolves via [GENRE_SYNONYMS] ("drumnbass").
 */
private val DRUM_AND_BASS_REGEX =
    Regex(
        """\bdrum\s*['’]?\s*n\s*['’]?\s*bass\b|\bdrum\s*&\s*bass\b|\bdrum\s+and\s+bass\b|\bd\s*&\s*b\b|\bdnb\b""",
        RegexOption.IGNORE_CASE
    )

/** Collapses delimiter-embedding genre names to a split-safe spelling before tokenization. */
private fun preNormalize(rawGenre: String): String = rawGenre.replace(DRUM_AND_BASS_REGEX, "Drum'n'Bass")

/**
 * Suffixes commonly appended to genre names in venue listings that should
 * be stripped before normalization (e.g. "Hip Hop Floor", "Pop Disco Floor").
 *
 * Order matters: longer/more-specific suffixes must come first so
 * "Pop Disco Floor" strips to "Pop", not "Pop Disco".
 */
private val NOISE_SUFFIXES = listOf("disco floor", "floor")

/**
 * Parses a raw genre string into a list of canonical genre tag names.
 *
 * The normalization pipeline:
 * 1. Split on common delimiters (`, `, `//`, ` & `, ` / `).
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

    return preNormalize(rawGenre)
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
 * 3. No match — kept as a new genre only if it [looksLikeGenre]; otherwise dropped.
 *
 * Tokens that are clearly not genre names (too short, "from …" prefixes, or that
 * fail the [looksLikeGenre] gate) are filtered out by returning an empty list.
 */
@Suppress("ReturnCount") // Multiple early returns improve readability for this cascading lookup
private fun resolveGenre(token: String): List<String> {
    val lower = token.lowercase().trim()

    // Skip tokens that are clearly not genre names
    if (lower.length < 2) return emptyList()
    if (lower.startsWith("all kinds of")) {
        val genre =
            GENRE_SYNONYMS[
                lookupKey(
                    lower
                        .substringAfter("all kinds of")
                        .trim()
                        .split(" ")
                        .first()
                )
            ]
        return listOfNotNull(genre)
    }
    if (lower.startsWith("from ")) return emptyList()

    // Direct synonym match
    GENRE_SYNONYMS[lookupKey(lower)]?.let { return listOf(it) }

    // Try matching individual words if the full token didn't match
    // (handles compound freeform labels like "Superheavy Funky Soul & Boogaloo")
    val words = lower.split(Regex("""\s+"""))
    val matched = words.mapNotNull { GENRE_SYNONYMS[lookupKey(it)] }.distinct()
    if (matched.isNotEmpty()) return matched

    // No synonym match. Keep the token as a new genre only if it plausibly names
    // one; otherwise drop it so event-format labels and freeform fragments never
    // leak into genre_tag (the raw genre text is preserved on the event).
    if (!looksLikeGenre(token)) {
        logger.info { "Dropping non-genre token '$token'" }
        return emptyList()
    }
    logger.info { "No synonym match for genre token '$token', using as-is" }
    val titleCased =
        token.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
    return listOf(titleCased)
}

/**
 * Heuristic gate for the [resolveGenre] fall-through: whether an unmatched token
 * plausibly names a genre and may be kept as-is, rather than being dropped as a
 * non-genre label.
 *
 * A token qualifies when it:
 * - contains at least one letter (rejects bare punctuation/numbers), and
 * - is at most [MAX_GENRE_WORDS] words long (real genres are short — "Noise",
 *   "Trip-Hop", "New Wave" — whereas leaked labels like "Twenty One Pilots
 *   Special" run long), and
 * - contains no [NON_GENRE_TOKENS] word (drops "Immersive Ausstellung" and the
 *   like even when short), and
 * - does not itself *normalize* to a [NON_GENRE_TOKENS] entry (drops the two-word
 *   spellings of a listed label — "Open Air" → `openair`, "Release Party" →
 *   `releaseparty` — that survive the per-word check because no single word is listed).
 */
private fun looksLikeGenre(token: String): Boolean {
    val words = token.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
    return words.size <= MAX_GENRE_WORDS &&
        words.none { lookupKey(it) in NON_GENRE_TOKENS } &&
        lookupKey(token) !in NON_GENRE_TOKENS &&
        token.any { it.isLetter() }
}

/** Maximum word count for a fall-through token to still be treated as a genre name. */
private const val MAX_GENRE_WORDS = 2
