package de.norm.events.genretag

/**
 * The thirteen families the genre filter offers, each grouping the many [GenreTag]s a venue's
 * free text produces (#363).
 *
 * The [slug] is what the database column, the `family=` query parameter and the frontend's
 * translation keys carry. [ordinal] is the display order — broad and busy first, party labels
 * last — which is why this is an enum rather than a set of strings.
 *
 * Which tag belongs to which family is the importer's knowledge, next to the synonym map; a tag
 * the importer cannot place has no family and appears in the filter under none.
 */
enum class GenreFamily(
    val slug: String
) {
    ELECTRONIC("electronic"),
    HIP_HOP("hip-hop"),
    POP("pop"),
    ROCK("rock"),
    PUNK("punk"),
    METAL("metal"),
    WAVE("wave"),
    SOUL_FUNK("soul-funk"),
    JAZZ_BLUES("jazz-blues"),
    FOLK("folk"),
    LATIN_WORLD("latin-world"),
    CLASSICAL("classical"),
    CHARTS("charts");

    companion object {
        /** The family whose [slug] matches, or `null` for a slug the vocabulary does not know. */
        fun fromSlug(slug: String?): GenreFamily? = entries.firstOrNull { it.slug == slug }
    }
}
