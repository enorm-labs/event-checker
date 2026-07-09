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
    /** Astra Kulturhaus Berlin – Kulturhäuser-platform listing on the homepage with per-event detail pages. */
    ASTRA,

    /** Badehaus Berlin – WordPress/Events-Manager single-page `/events/` listing with status classes. */
    BADEHAUS,

    /** Bi Nuu Berlin – SvelteKit/PocketBase site; SSR data embedded as a JS object literal in the page, with per-event detail pages. */
    BINUU,

    /** Cassiopeia Berlin – Webflow-based event listing at `/club`. */
    CASSIOPEIA,

    /** Duncker Club Berlin – retro hand-coded single-page `start.html` programme table (goth/wave/indie DJ nights), German `DD.MM.` dates without a year. */
    DUNCKER,

    /** Frannz Club Berlin – WordPress single-page homepage listing; events server-rendered with `event_typ-*` classes, no detail pages. */
    FRANNZ,

    /** Lido Berlin – same Kulturhäuser platform as Astra (different theme), homepage listing with detail pages. */
    LIDO,

    /** Madame Claude Berlin – WordPress-based event listing with detail pages. */
    MADAME_CLAUDE,

    /** Privatclub Berlin – WordPress-based single-page event listing. */
    PRIVATCLUB,

    /** Roadrunner's Paradise Berlin – retro hand-coded single-page `programm.html` listing (rockabilly/roots). */
    ROADRUNNER,

    /** SO36 Berlin – Ticket-Toaster shop platform; `/tickets` listing with per-event `/produkte/…` detail pages. */
    SO36;

    /**
     * Prefix for `sourceId` values, derived from the enum name in lowercase.
     *
     * Used by scrapers to build sourceId strings (e.g. `"cassiopeia:some-event-slug"`).
     * This avoids hard-coding the prefix string in scraper classes.
     */
    val sourceIdPrefix: String get() = "${name.lowercase()}:"
}
