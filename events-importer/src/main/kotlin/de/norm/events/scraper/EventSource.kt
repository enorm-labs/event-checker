package de.norm.events.scraper

/**
 * Enumeration of known event import sources.
 *
 * Each value corresponds to a venue-specific [EventImporter] implementation
 * that knows how to fetch and parse events from that venue's website.
 * Using an enum instead of a String key provides compile-time safety —
 * every registered source must have a matching importer bean.
 */
enum class EventSource {
    /** Cassiopeia Berlin – Webflow-based event listing at `/club`. */
    CASSIOPEIA;

    /**
     * Prefix for `sourceId` values, derived from the enum name in lowercase.
     *
     * Used by scrapers to build sourceId strings (e.g. `"cassiopeia:some-event-slug"`).
     * This avoids hard-coding the prefix string in scraper classes.
     */
    val sourceIdPrefix: String get() = "${name.lowercase()}:"
}
