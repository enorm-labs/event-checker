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
     * AMT Club Berlin – Webflow techno club whose `/events` entry page carries no events server-side (the
     * Finsweet CMS-nest list injects them client-side); the programme lives on per-month `/month/<name>`
     * pages instead. Each month page renders every night as a `.div-grid-dan[data-date]` block (a full
     * `MMMM d, yyyy` date, a `.name` title, an optional `.sexpos` theme tag, a `.perf` DJ line, a Resident
     * Advisor/EventJet ticket link, a tiered `min – max €` price, and a `/event/<slug>` detail link). The
     * per-event detail pages add only a prose blurb, so the month pages are the source; conditional caching
     * is disabled because the entry ETag does not change when a night is edited within an existing month.
     */
    AMT,

    /**
     * Alte Kantine (Kulturbrauerei) Berlin – WordPress site whose upcoming programme is rendered on the
     * homepage as a Content Views grid (`.pt-cv-content-item`, each carrying a `data-pid` post id, a
     * year-less `DD.MM.` date, a start time, and a title link to the `?p=<id>` post). Each post's detail
     * page adds the description, poster image, price, event kind (`Was`), and DJ via a `ul.list-style-6`
     * label/value list. The WP REST API is locked down (iThemes Security 401s), so the two HTML pages are
     * the source.
     */
    ALTE_KANTINE,

    /**
     * Arcanoa Berlin – a Kreuzberg live-music bar whose 1990s hand-coded `veranst.htm` is one nested
     * `<font>`/`<table>` soup with no classes, ids or per-event URLs. The only structure worth anchoring on is
     * a `font.gesperrt` month heading ("Juli") followed by a single `<p>` holding that month's whole programme
     * as one run of `<br>`-separated lines — so the parser splits the paragraph's flat text on the German
     * `Mo 22.07.Live:` entry marker rather than on markup. Each line packs the act and a free-text style tail
     * into `"<act> - <style>"`, which is split into title and subtitle (never `genre`: tails like
     * "AfroLatinFolkJazzEthnoBluesSession" would seed junk tags). Dates carry a weekday but **no year**, so the
     * year comes from [inferYearForWeekday]. Everything else the model wants is simply absent: no images, ticket
     * links, prices or per-event pages, and the only time on the page is the shared "Veranstaltungsbeginn: 20 Uhr"
     * line, read once per month block. Two blocks are deliberately ignored — the undated weekly-programme boxes at
     * the top (the dated listing already carries every occurrence) and the "Mittelaltertreffen immer Mittwoch"
     * recap, which repeats Wednesdays already listed above it.
     */
    ARCANOA,

    /**
     * arkaoda Berlin – the Neukölln outpost of the Istanbul bar/club, on a hand-coded PHP site whose router
     * puts the path in the query string (`?/default/program`, `?/default/detail/id=<n>`). The `?/default/program`
     * listing server-renders one `div.box` per **upcoming** event (past events drop off, so it is short — often
     * only a handful of blocks), each carrying a flyer, a `<b>` run with a spaced `DD / MM / YYYY` date, the German
     * weekday and an optional `// Konser` category, the title, and a CSS-truncated excerpt. The `?/default/detail/id=<n>`
     * pages repeat that same block markup and add the **untruncated** description, which is the reason they are
     * fetched at all; the numeric `id` is the stable [ScrapedEvent.sourceId] key. Three quirks drive the parser:
     * `Konser` (Turkish for concert) is the *only* category the venue ever emits, so an unlabelled event is typed
     * from its title rather than defaulted to a concert; PHP `addslashes` escapes leak into the rendered title
     * (`7\" Vinyl`), so they are unescaped before storage; and conditional requests are pointless because the site
     * sends no ETag/Last-Modified and answers `Cache-Control: no-store`. The venue has no structured field for
     * times, prices, sold-out state or genre — those appear, if at all, only inside the prose description, from
     * which just the `Tickets:`-labelled shop link (usually Resident Advisor) is read.
     */
    ARKAODA,

    /** Astra Kulturhaus Berlin – Kulturhäuser-platform listing on the homepage with per-event detail pages. */
    ASTRA,

    /**
     * Berghain Berlin – the club's own server-rendered programme. One importer serves two source rows built on
     * the identical page template: the main `/de/program/` page (the Berghain building floors — Berghain,
     * Panorama Bar, Säule, Halle — typed [PARTY][de.norm.events.event.EventType.PARTY]) and the
     * `/de/program/kantine-am-berghain/` page (the adjacent concert hall, typed
     * [CONCERT][de.norm.events.event.EventType.CONCERT]). Each night is an `a[href^=/de/event/<id>/]` block
     * carrying a German `DD.MM.YYYY` date, an optional `tür` (doors) and a `beginn` (start) time, the event
     * title (`h2`), one or more `h3` floor labels, and an `h4` running-order lineup (each act in its own span,
     * with `Live`/`b2b` markers in uppercase spans). Each `/de/event/<id>/` detail page adds the poster image,
     * the ticket-shop link, presale/box-office (`Abendkasse`) prices with an `ausverkauft` sold-out marker, and
     * a prose description — so the two HTML pages (list + detail) are the source.
     */
    BERGHAIN,

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
     * Havanna Berlin – Squarespace Latin dance club in Schöneberg. The venue publishes **no dated
     * programme**: its `/events` page is a static three-column teaser (one poster plus a "More" button
     * per night) linking to three undated pages — `/wednesday`, `/friday`, `/saturday` — that describe
     * the same weekly resident nights (floor-by-floor genres, start time, door price, dance-lesson
     * note). This importer therefore *derives* dated events, expanding each night page into one
     * occurrence per week over a rolling horizon. Conditional caching is disabled because the pages
     * have not changed since 2016 — a 304 would freeze the horizon and the calendar would never
     * advance. A closure notice on a night page ("… AB DEM 01.07.2026 IN DER SOMMERPAUSE!") suppresses
     * that night's occurrences from the announced date on.
     */
    HAVANNA,

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
     * Matrix Club Berlin – WordPress club at the Warschauer Straße U-Bahn arches that runs a resident
     * night every single day ("Party Every Night"). Its `/parties/<date>-matrix-<weekday>/` posts have
     * per-event pages, but the `/party-in-berlin/` **month** view already carries the identical, fully
     * expanded content for every night of a month in one server-rendered page — so this importer walks
     * the month pages (`?get_month=<m>&get_year=<yyyy>`, discovered from the page's own next-month
     * link) instead of fetching ~90 detail pages per run. Each night is a `div.toggled-item` whose `id`
     * is a machine-readable `DD-MM-YYYY` date and whose `.toggle-review` half holds the flyer, a
     * `Weekday DD.MM.YYYY | HH:mmUhr` line, the title, a `•`-separated genre list, an optional starred
     * promo line, a `<br>`-delimited prose blurb carrying the `► Entry :` door prices, and labelled
     * `Floors:` / `DJs:` / `Specials:` lists — the DJs and specials being the lineup. Conditional
     * requests are disabled: the site sends neither ETag nor Last-Modified, and a 304 on the entry page
     * would say nothing about the later months.
     */
    MATRIX,

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
     * Soda Club Berlin – discotheque in the Kulturbrauerei running resident nights (Famous Friday,
     * Sodalicious, Salsa Sonntag …) on the "disco2app" club CMS. The `/events` page groups the
     * programme under German month headings, each night a `.event-snippet` card carrying the flyer,
     * the title, a `/de/events/<slug>` detail link and a **year-less** weekday/day/month calendar
     * block — so the overview year is inferred from the weekday. Each detail page carries a
     * schema.org `MusicEvent` JSON-LD block (start date + time, image, canonical URL, status, the
     * online offer price) plus labelled info boxes the JSON-LD omits: the `Eintritt` admission
     * price, an `Einlass` **age** limit (not a doors time — the venue publishes none), an
     * "Abendkasse verfügbar" badge marking door sales, and the untruncated prose blurb
     * (`p.event-details`). Every listing is a club night, so events are typed
     * [PARTY][de.norm.events.event.EventType.PARTY] and no artists are derived — the JSON-LD
     * `performer` is always the placeholder "Unbekannt".
     */
    SODA,

    /**
     * Supamolly Berlin – retro hand-coded PHP squat venue in Friedrichshain. The whole programme lives on a
     * single `?p=programm` page (identical to the homepage) as a `<table>` whose event rows carry a
     * `YYYYMMDDHHMM` `id` — a machine-readable date+time stamp that is both the event date and its stable
     * identity. Each row pairs a `td.date` cell (weekday icon, `DD.MM.` date, `HH:MM` time, optional flyer
     * thumbnail) with a `td.evcont` cell holding one `div.even` block per billed act (`.tit` name, optional
     * `.beschr` note, and an artist reference link). There are no detail pages — `index.php?programm=<id>`
     * serves the full-size flyer JPEG, not HTML — and no prices, so the single page is the source. The
     * advertised `rss.php` feed is **not** usable: its item titles render the date as `"Mi 9.2026..09."`,
     * dropping the day of month entirely.
     */
    SUPAMOLLY,

    /**
     * Urban Spree Berlin – MODX-based art gallery and concert venue in the RAW-Gelände. The
     * `/program/` listing is server-rendered, but **descending by date** and paginated nine cards
     * at a time over the venue's whole archive (200+ pages), so this importer walks `?page=N`
     * forward from the entry URL until a page reaches the past — "first page only" would import
     * the farthest-future shows and miss every upcoming one. Each card is an
     * `a.card[data-dateStart]` carrying a machine-readable `YYYY-MM-DD HH:mm:ss` start, a category
     * (`Concerts`, `Events`, …), a CSS-truncated title, a price (or `Free`), the original poster
     * path (`data-imgfeat`), and a link to a `/program/<category>/<slug>.html` detail page. The
     * detail page supplies the untruncated `h1` title, the promoter, the ticket-shop link, and the
     * prose description. Conditional requests are disabled: the site sends no ETag/Last-Modified
     * and answers `Cache-Control: no-store`.
     */
    URBAN_SPREE,

    /**
     * Wild at Heart Berlin – retro hand-coded frameset rockabilly/punk venue in Kreuzberg. The whole concert
     * programme lives on a single `/concerts.php` page (linked from the `topics.htm` nav frame) as a flat
     * `<table>` of `<tr>` rows, each carrying a year-less `Weekday DD.MM.` date (`.datum`), a headliner
     * (`.band`) and optional support acts (`.supportband`) with a `(Genre - Country)` tag (`.stil-country`),
     * an optional DJ (`.dj`), a flyer image (`/uploads/img/…`), and an optional `.headlines` banner that may
     * embed a `Tickets:<url>` link, a `Beginn HH:MM` start time, or an `Eintritt frei` free-entry note.
     */
    WILD_AT_HEART,

    /**
     * Zenner Berlin – the Treptower Park riverside venue (Saal, Klub, Biergarten, Weingarten) on a
     * Gatsby front end backed by a Sanity headless CMS. The rendered `/programm` page is a React
     * shell, but Gatsby publishes the page's own GraphQL result as a static JSON artefact at
     * `/page-data/programm/page-data.json` — the whole programme as structured data, so no HTML is
     * scraped (ADR-007 §"Selector Strategy" priority 1). Each node carries a `typeOfEvent`
     * (Konzert/Concert/Party/Lesung, plus the non-kind labels "Event" and "Open Air"), the `place`
     * (room) it happens in, an ISO `eventDate`, a ticket-shop `linkEvent` (Resident Advisor, DICE,
     * Ticketmaster), a Sanity CDN `image`, and a Portable Text `_rawText` blurb. The `place`
     * disambiguates "Open Air", which is the SIP! DJ day-party series in the Weingarten but an
     * ice-skating session or a festival day in the Biergarten.
     *
     * Three quirks drive the parser. **`eventDate` is a true UTC instant**, not a local wall clock:
     * the site converts it in the browser (a `21:45Z` party renders as `23:45` in Berlin), so it is
     * converted to `Europe/Berlin` — reading it naively would shift every event one to two hours
     * early and roll late nights onto the wrong day. The payload holds the venue's **whole archive**
     * (three years of past events for a handful of upcoming ones), so past dates are dropped during
     * parsing rather than minting a hundred throwaway events per run. And a sibling
     * `queryShowHidePlaces` block carries the venue's own per-room publish flags, which are honoured
     * so a programme Zenner has unpublished is not imported. Conditional requests are unused — the
     * artefact is rebuilt on every content change and upserts are idempotent by `sourceId`.
     */
    ZENNER;

    /**
     * Prefix for `sourceId` values, derived from the enum name in lowercase.
     *
     * Used by scrapers to build sourceId strings (e.g. `"cassiopeia:some-event-slug"`).
     * This avoids hard-coding the prefix string in scraper classes.
     */
    val sourceIdPrefix: String get() = "${name.lowercase()}:"
}
