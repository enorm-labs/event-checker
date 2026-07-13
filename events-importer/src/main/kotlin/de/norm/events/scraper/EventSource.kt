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
    /**
     * Alte Kantine (Kulturbrauerei) Berlin – WordPress site whose upcoming programme is rendered on the
     * homepage as a Content Views grid (`.pt-cv-content-item`, each carrying a `data-pid` post id, a
     * year-less `DD.MM.` date, a start time, and a title link to the `?p=<id>` post). Each post's detail
     * page adds the description, poster image, price, event kind (`Was`), and DJ via a `ul.list-style-6`
     * label/value list. The WP REST API is locked down (iThemes Security 401s), so the two HTML pages are
     * the source.
     */
    ALTE_KANTINE,

    /** Astra Kulturhaus Berlin – Kulturhäuser-platform listing on the homepage with per-event detail pages. */
    ASTRA,

    /** Badehaus Berlin – WordPress/Events-Manager single-page `/events/` listing with status classes. */
    BADEHAUS,

    /** Bi Nuu Berlin – SvelteKit/PocketBase site; SSR data embedded as a JS object literal in the page, with per-event detail pages. */
    BINUU,

    /** Cassiopeia Berlin – Webflow-based event listing at `/club`. */
    CASSIOPEIA,

    /**
     * Clash Berlin – WordPress punk/ska venue; upcoming events rendered on the homepage `#events`
     * section as `.gigs-container .item` blocks (the `event` custom post type is not exposed via the
     * WP REST API), each carrying a `DD.MM.YY` date, a Stager ticket-shop link, and a poster image.
     */
    CLASH,

    /** Duncker Club Berlin – retro hand-coded single-page `start.html` programme table (goth/wave/indie DJ nights), German `DD.MM.` dates without a year. */
    DUNCKER,

    /**
     * Festsaal Kreuzberg Berlin – Nuxt.js SPA backed by a Wagtail headless CMS; events read from the
     * CMS's public JSON REST API (`/api/v2/pages/?type=home.EventPage`) rather than the JS-rendered page.
     */
    FESTSAAL,

    /** Frannz Club Berlin – WordPress single-page homepage listing; events server-rendered with `event_typ-*` classes, no detail pages. */
    FRANNZ,

    /** Gretchen Berlin – retro hand-coded single-page homepage listing; each event a `.gig` block with a `.lineup` performer list. */
    GRETCHEN,

    /**
     * Hole 44 Berlin – WordPress/Events-Manager concert hall; the `/events/` page lists every show as a
     * `li.event-item` (date, start time, title, genre tags, and a `.changes` relocation/cancellation note),
     * each linking to a `/event/<date-slug>/` detail page that adds the promoter, doors time, image, and a
     * schema.org `Event` JSON-LD block (name, description, image). The Events-Manager REST API is not exposed
     * for anonymous reads (it 301-redirects), so the two HTML pages are the source.
     */
    HOLE44,

    /**
     * Junction Bar Berlin – retro hand-coded site imported from the homepage entry, which links to two programs merged
     * into this one source: the live-music listing (`music_html/music.html` → per-month `program/MM_YYYY/MM_YY.html`
     * pages) and the DJ program (`DJ_html/DJ.html`). Each page is a flat sequence of `strong.datum`/`strong.Datum` date
     * bars followed by band blocks (`.Stil1222` name + `p.text` bio + ticket-shop link) or DJ blocks (`p.djane`).
     */
    JUNCTION_BAR,

    /** Lido Berlin – same Kulturhäuser platform as Astra (different theme), homepage listing with detail pages. */
    LIDO,

    /**
     * Loge Berlin – Wix site with a Wix Events widget; the `/event-list` overview embeds every event as
     * structured JSON in the `wix-warmup-data` script (discovery + core fields), and each `/event-details/<slug>`
     * page carries a schema.org `Event` JSON-LD block with the ticket price.
     */
    LOGE,

    /** Madame Claude Berlin – WordPress-based event listing with detail pages. */
    MADAME_CLAUDE,

    /**
     * Mikropol Berlin – WordPress/Events-Manager club in Schöneberg; the `/events/` page lists every show as
     * an `a.event` card (a `DD.MM.YYYY` date, start/doors times, title, an inline support line, and an
     * `Ausverkauft`/`Abgesagt` status class), each linking to an `/event/<date-slug>/` detail page that adds
     * the description (`.eventnotes`), poster image (`a.event-image`), and Eventim ticket link
     * (`.ticket-links`). The theme carries no schema.org JSON-LD and the Events-Manager REST API is not exposed
     * for anonymous reads, so the two HTML pages are the source. Relocated shows encode "verlegt in den … –" in
     * the title itself rather than a status class.
     */
    MIKROPOL,

    /**
     * Monarch Berlin – retro hand-coded PHP bar/club above Kottbusser Tor; the whole programme lives on a single
     * `/programm.php` page as a flat run of `div` blocks (no per-event URLs), each with a leading bold
     * `Weekday DD/MM/YYYY-HH:MM` date line, a `td#td1` title cell (a `(KONZERT)` suffix marks concerts, an
     * `ABGESAGT` prefix marks a cancellation), and an optional external "Ticket Vorverkauf" shop link.
     */
    MONARCH,

    /**
     * Neue Zukunft Berlin – static landing page whose concert programme lives only in an embedded
     * Elfsight "Event Calendar" widget; events read from the widget's public JSON boot API
     * (`core.service.elfsight.com/p/boot/?w=<widgetId>`) rather than the JS-rendered `<div>`.
     */
    NEUE_ZUKUNFT,

    /** Privatclub Berlin – WordPress-based single-page event listing. */
    PRIVATCLUB,

    /** Roadrunner's Paradise Berlin – retro hand-coded single-page `programm.html` listing (rockabilly/roots). */
    ROADRUNNER,

    /** Schokoladen Mitte Berlin – Laravel-based single-page homepage listing; each event a `div.event` block with an ISO `data-event-date`, no detail pages. */
    SCHOKOLADEN,

    /** SO36 Berlin – Ticket-Toaster shop platform; `/tickets` listing with per-event `/produkte/…` detail pages. */
    SO36,

    /**
     * Wild at Heart Berlin – retro hand-coded frameset rockabilly/punk venue in Kreuzberg. The whole concert
     * programme lives on a single `/concerts.php` page (linked from the `topics.htm` nav frame) as a flat
     * `<table>` of `<tr>` rows, each carrying a year-less `Weekday DD.MM.` date (`.datum`), a headliner
     * (`.band`) and optional support acts (`.supportband`) with a `(Genre - Country)` tag (`.stil-country`),
     * an optional DJ (`.dj`), a flyer image (`/uploads/img/…`), and an optional `.headlines` banner that may
     * embed a `Tickets:<url>` link, a `Beginn HH:MM` start time, or an `Eintritt frei` free-entry note.
     */
    WILD_AT_HEART;

    /**
     * Prefix for `sourceId` values, derived from the enum name in lowercase.
     *
     * Used by scrapers to build sourceId strings (e.g. `"cassiopeia:some-event-slug"`).
     * This avoids hard-coding the prefix string in scraper classes.
     */
    val sourceIdPrefix: String get() = "${name.lowercase()}:"
}
