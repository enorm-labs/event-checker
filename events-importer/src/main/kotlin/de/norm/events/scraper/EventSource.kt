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
     * ÆDEN Berlin – WordPress techno club whose `/events` entry page is a bare month index: one
     * `a.month-button[data-date]` per upcoming month, linking to a fully server-rendered
     * `/month/?month=YYYY-MM` page. Each month page renders its nights as `.single-accordion` blocks
     * carrying a `.event-title`, a `.event-time` start time, a `.event-date` in `dd/MM/yyyy Weekday`
     * form, a comma-separated `.event-genre` list, a `.event-poster` image, an external ticket link
     * (Resident Advisor / Weeztix) and an `.event-lineup` prose block whose `Lineup:` paragraph lists
     * the DJs `<br>`-separated. The `aeve`/`oel` post types (the venue's other spaces) never appear on
     * the month pages, so only the club programme is imported. The month pages carry no prices and no
     * per-event links, so `sourceId` is built from the date plus the slugified title.
     */
    AEDEN,

    /**
     * Admiralspalast Berlin – the Friedrichstraße variety theatre, on Contao. Its
     * `/veranstaltungsuebersicht.html` A–Z listing is a **discovery list only**: each tile carries a
     * production title and an `ab DD.MM.YY` run-start date, but the real schedule lives on the
     * `/veranstaltung/<slug>.html` page, whose `#eventlist` renders one `.item` row **per
     * performance** — `.evDay` (`25`), `.evMJ` (`Jan 2027`, German month abbreviation), `.evWdT`
     * (`Mo, 19:30`), the production title, a per-date Eventim ticket link and the poster. One
     * production therefore yields several events, so `sourceId` combines the slug with the
     * performance date.
     *
     * `.eventzusatz` carries the venue's own reschedule note, and its two idioms mean opposite
     * things: `verschoben auf <date>` marks the *original* date (postponed), while `verlegt vom
     * <date>` marks the *replacement* date and therefore stays scheduled — the shared
     * [parseEventStatus] would read the latter as `RELOCATED`.
     *
     * The event type and genre come from the `/veranstaltungsuebersicht/eventkategorie/<genre>.html`
     * filter pages, the only place the venue states a category; the listing tiles carry none. No
     * prices are published anywhere (tickets are sold on Eventim).
     */
    ADMIRALSPALAST,

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
     * Bar jeder Vernunft Berlin – the Wilmersdorf Spiegelzelt (cabaret / variety theatre) on a Neos CMS 8.3
     * site. The `/de/programm/kalender.html` calendar is the entry page: it renders one
     * `.card-type-calendar` per **performance date** (a residency show appears once per night), each followed
     * by its own `<script type="application/ld+json">` schema.org `Event` — the authoritative source for the
     * date, start time, poster image, canonical URL, performer and `offers.availability` (ADR-007
     * §"Selector Strategy" priority 1). The card markup adds only the show's sub-line and the ticket-shop
     * link; its date block carries no year, so a card without JSON-LD is skipped rather than guessed at.
     *
     * The programme is a run of shows, so **many dates share one `/programmuebersicht/<show>.html` page**.
     * That page holds the fields the calendar omits — `Genre`, the `Preise` range, and the untruncated blurb
     * — so this importer fetches each *distinct* show page once per run and applies it to every date of that
     * show, instead of the per-event detail fetch [AbstractTwoPageWebsiteImporter] would issue (28 calendar
     * cards currently resolve to 2 pages). The venue stages evening shows rather than gigs, so its own
     * `Genre` decides the type: a music style (Chanson, Swing, A cappella) is a
     * [CONCERT][de.norm.events.event.EventType.CONCERT] whose performer becomes the headliner, while a staged
     * format (Musik-Show, Kabarett, Musical) is a [SHOW][de.norm.events.event.EventType.SHOW] whose name is
     * not an artist. `robots.txt` disallows `/de/ical/`, so the iCal feed is not used.
     */
    BAR_JEDER_VERNUNFT,

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

    /**
     * Club der Visionäre Berlin – the Flutgraben open-air shack, on a hand-coded WordPress theme
     * whose `/programm/` page is the **single programme for three rooms**: the club itself, the
     * adjacent [SONNENRAUM] concert space, and the [MS_HOPPETOSSE] boat (the winter location).
     * `hoppetosse.berlin/program/` serves the byte-identical listing, so which room a night belongs
     * to is carried **only by the colour class on its title** — `.cdvRed`, `.sonnenraumYellow`,
     * `.hoppetosseYellow` — and each room is imported as its own source filtered on that class.
     * The programme is seasonal: the open-air club runs in summer, the boat in winter, so the
     * off-season room legitimately imports zero events.
     *
     * Each night is a `div#programmC > div[id^=post-]` block: a `div.headerTxt` date cell in German
     * `Wd. D.M.` form (**no year** — inferred from the weekday via [inferYearForWeekday]), the
     * `p.headerTxt.<room>` title, and a flat run of `<p>` lineup lines — `// <act>` entries grouped
     * under optional `Main:` / `Chill Floor:` floor headings and `Live Band featuring:` / `DJ Sets:`
     * billing sections. The date cell is **empty when a night shares its date with the block above**
     * (a boat party and its club afterparty), so a dateless block inherits the preceding date.
     *
     * The WordPress REST API is not usable: upcoming events are `future`-status posts, which
     * `/wp-json/wp/v2/posts` omits (and 401s per id), so the rendered page is the only source. The
     * page carries no times, prices, ticket links, images or per-event URLs — the `ra.co` links in
     * the lineup are DJ profiles, not tickets — so the WordPress post id is the stable `sourceId`.
     */
    CLUB_DER_VISIONAERE,

    /**
     * Columbia Theater Berlin – the Tempelhofer Feld concert hall, on a bespoke WordPress theme whose
     * **homepage is the whole upcoming programme**: one `a.item[data-id]` card per night, carrying the
     * poster, the `.item-title` act, an optional `.item-tour-text` tour name, and one
     * `.item-support-row` per billing line (`Support:` / `Opener:` / `Special Guest(s):` / `DJ:`, acts
     * `+`-separated). Every card links to a `/event/YYYYMMDD-<slug>/` detail page, and that
     * `YYYYMMDD` permalink prefix is the event date — the rendered date blocks carry a German month
     * abbreviation and usually no year at all. The detail page adds the `.header-date` line
     * (`Wd. DD.MM.[YY] um HH:mm / Einlass HH:mm`), the prose blurb, the ticket-shop link, and the
     * `präsentiert von …` media presenters. The WordPress REST API is disabled site-wide (every
     * `/wp-json/` route 401s `rest_disabled`), so the two HTML pages are the source.
     *
     * Three quirks drive the parser. Status is carried by the venue's own machine-readable
     * `data-c` / `data-m` / `data-p` flags (cancelled / relocated / postponed) on both the card and
     * the detail page's `.event-content`, with the bilingual `Abgesagt / Canceled` badge text as a
     * fallback. A **rescheduled show is rendered twice** — once as an `X`-prefixed `data-id`
     * placeholder sitting at its *original* date, once at its new one — and both link to the same
     * `/event/` URL, so the placeholder is skipped and only the real entry (flagged `POSTPONED`) is
     * imported. And a ticket `href` sometimes has **two shop URLs concatenated** into one attribute
     * (`…utm_medium=dphttps://www.eventim.de/…`), so everything from the second `http(s)://` on is
     * dropped. The venue publishes no prices, genres or sold-out markers anywhere.
     */
    COLUMBIA_THEATER,

    /**
     * Columbiahalle Berlin – the 3,500-capacity hall at Tempelhofer Feld, on a Contao 5 site whose
     * `/veranstaltungen.html` page carries the **whole upcoming programme inline**: one
     * `.eventlist_event` per night, each with the act (`h2`), an optional tour/support line (`h3`),
     * the booking agency (`.veranstalter`), `Einlass`/`Beginn` times (`.zeit`), `VVK`/`AK` prices
     * with an optional "zzgl. Gebühr" note (`.preis`), a ticket-shop link, a poster, and the
     * untruncated blurb in a collapsed `.bandinfo` panel. Nothing needs a second fetch — the
     * `veranstaltung/<alias>.html` links the cards call "Kalender-Eintrag" serve an **iCal
     * download**, not an HTML page, and carry strictly less than the listing (no prices, promoter,
     * tickets or sold-out state).
     *
     * Two quirks drive the parser. A card states only its **weekday and day of month** ("Freitag",
     * "07"); the month and year come from the `.eventlist_monat` heading ("August 2026") that
     * precedes it in document order, so the two node kinds are walked as one ordered stream and a
     * heading that fails to parse voids the month rather than carrying the previous one forward.
     * And the venue publishes **no per-event page**, so both the identity and the URL come from the
     * Contao event id on `div.event_inhalt[id=event_<n>]` — the same id its iCal export uses as the
     * `UID` — with the listing anchor (`…/veranstaltungen.html#event_9743`) as [ScrapedEvent.sourceUrl].
     *
     * Status is a `.stoerer` sticker (`Ausverkauft` → the sold-out flag, `Abgesagt` → `CANCELLED`,
     * `Zusatzshow` → neither). The site emits no category and no genre, and sends neither ETag nor
     * Last-Modified.
     */
    COLUMBIAHALLE,

    /**
     * Cosmic Comedy Berlin – the English-language stand-up club on Schönhauser Allee, imported
     * entirely from its **The Events Calendar** REST API (`/wp-json/tribe/events/v1/events`).
     *
     * The plugin's own API is preferred over the `Event` JSON-LD the listing page also embeds:
     * the JSON-LD covers only the page's current view (22 events at capture) where the API returns
     * the whole upcoming programme (57), plus the categories, tags, organizers and full
     * descriptions the JSON-LD omits. The API hands back its own `next_rest_url` cursor, which the
     * importer walks rather than building page URLs itself; its default window runs from today to
     * two years out, so no past-date filter is needed.
     *
     * Everything the club programmes is comedy, so every event is a `SHOW`. The `categories` name a
     * format or a language (`Showcase`, `Open Mic`, `Comedy Special`, `English Language`) and never
     * a musical genre, so none is stored. A `Comedy Special` is the club's own marker for a named
     * act rather than the house showcase, and those titles are all `"<Performer> – <Show>"`, so the
     * part before the dash becomes the artist — the recurring nights name no one and get none.
     *
     * The club publishes **no prices at all** (`cost` and `cost_details` are empty on every event).
     * The ticket link is the event's `website` where set, else the Universe listing embedded as a
     * widget `<script>` in the description — the club's season listing, so most nights share one
     * URL. Titles and taxonomy names arrive HTML-escaped and the description is raw HTML opening
     * with that widget script, so both are decoded and flattened before storage.
     */
    COSMIC_COMEDY,

    /** Duncker Club Berlin – retro hand-coded single-page `start.html` programme table (goth/wave/indie DJ nights), German `DD.MM.` dates without a year. */
    DUNCKER,

    /**
     * Festsaal Kreuzberg Berlin – Nuxt.js SPA backed by a Wagtail headless CMS; events read from the
     * CMS's public JSON REST API (`/api/v2/pages/?type=home.EventPage`) rather than the JS-rendered page.
     */
    FESTSAAL,

    /** Frannz Club Berlin – WordPress single-page homepage listing; events server-rendered with `event_typ-*` classes, no detail pages. */
    FRANNZ,

    /**
     * Golden Gate Berlin – the techno club under the S-Bahn arches at Jannowitzbrücke, on a WordPress
     * site built with the Elementor page builder. Its homepage announces only the **current
     * Thursday–Saturday block** — three nights at a time, each an Elementor container holding exactly
     * three headings: a German date line (`Do. 30. Juli 2026 - 23:59`, weekday, full month, year and
     * the door time), the night's name (`Klubnacht`, `Donnerdogge`), and the DJ roster
     * `<br>`-separated. Nights that have passed stay on the page until the block rolls over, so an
     * import late in the week legitimately yields as little as one event once
     * [EventUpsertService][de.norm.events.scraper.EventUpsertService] has dropped the past-dated ones.
     *
     * Elementor names every element with a per-element hash (`elementor-element-7b3297a`) that changes
     * whenever the page is edited, so there is nothing venue-semantic to anchor on. The parser
     * therefore walks the `.elementor-heading-title` headings as one ordered stream and keys off
     * **content**: a heading matching the date pattern opens a night, the next two are its title and
     * lineup, and the next date heading closes it — which also skips the page's trailing non-event
     * headings ("Tickets only available at the door.", "SHOPPING").
     *
     * Every night is a DJ party, so events are typed [PARTY][de.norm.events.event.EventType.PARTY] and
     * the roster is billed [DJ][de.norm.events.event.ArtistRole.DJ]. The venue sells at the door only
     * and publishes no prices, tickets, genres, descriptions, per-event pages or posters — the one
     * image per container is a decorative flame divider shared by all three — so `sourceId` is built
     * from the date plus the slugified title.
     */
    GOLDEN_GATE,

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
     * Heimathafen Neukölln Berlin – the Karl-Marx-Straße Saal/Studio venue, on WordPress with an
     * Advanced Custom Fields `events` post type whose public REST API (`/wp-json/wp/v2/events`)
     * exposes the whole programme as structured JSON — no HTML is scraped (ADR-007 §"Selector
     * Strategy" priority 1).
     *
     * The defining feature is that **one post holds many dated performances**: `acf.event_performances`
     * is an array (a theatre run reaches 30 entries), each with its own US-format
     * `performance_date_time` (`11/27/2026 8:00 p.m.`), ticket link, `Einlass ab HH:mm (Saal|Studio)`
     * note, and `performance_status`. Each performance is expanded into its own event, keyed
     * `<postId>-<date>-<HHmm>` — the time is part of the identity because a run legitimately plays
     * twice on one day (matinee plus evening).
     *
     * The `performance_status` vocabulary carries what the model needs: `ausverkauft` → the sold-out
     * flag, `entfallt` → `CANCELLED`, `verlegt` → `RELOCATED`, `freier_eintritt` → the free flag, with
     * `default`/`premiere`/`restkarten`/`diskussion`/`custom` left `SCHEDULED`. The event type comes
     * from the venue's own `events_cat-*` taxonomy slug, which `class_list` inlines on every post
     * (`musik` → concert, `theater`/`amusemang`/`eigenproduktionen`/… → show, `literatur` → reading),
     * so no second taxonomy request is needed.
     *
     * The endpoint returns the **whole archive** (400+ posts, 800+ performances, of which under a
     * hundred are upcoming) and cannot be filtered on the ACF date server-side, so the importer walks
     * its ~5 pages and drops past performances while parsing. The 560-term `events_tag` vocabulary
     * *would* supply a genre, but it mixes genres with formats and needs six more requests to resolve
     * ids to names, so `genre` is deliberately left null.
     */
    HEIMATHAFEN,

    /**
     * Hole 44 Berlin – WordPress/Events-Manager concert hall; the `/events/` page lists every show as a
     * `li.event-item` (date, start time, title, genre tags, and a `.changes` relocation/cancellation note),
     * each linking to a `/event/<date-slug>/` detail page that adds the promoter, doors time, image, and a
     * schema.org `Event` JSON-LD block (name, description, image). The Events-Manager REST API is not exposed
     * for anonymous reads (it 301-redirects), so the two HTML pages are the source.
     */
    HOLE44,

    /**
     * Humboldthain Club Berlin – the techno club on Hochstraße in Wedding, on WordPress. Its
     * programme is not in the page at all: the site embeds an Elfsight "Event
     * Calendar" widget that renders client-side, so — as for [NEUE_ZUKUNFT] — the source is the
     * widget's public boot API (`core.service.elfsight.com/p/boot/?w=<widgetId>`), which returns
     * the venue's whole calendar as structured JSON (ADR-007 §"Selector Strategy" priority 1).
     * Each entry carries an `id`, `name`, a `start.{date,time}`, an HTML `description`, a
     * `coverImage.url` and `actions[]` (a "Presale Tickets" link).
     *
     * Two things set it apart from the other Elfsight venue. Its resident night ("OPEN DECKS &
     * TISCHTENNIS") is stored as a **single recurring entry** the widget expands in the browser,
     * so a parser reading only `start.date` would import it once at the series' opening date and
     * lose every upcoming Tuesday; weekly recurrences are therefore expanded into one event per
     * occurrence over a rolling horizon, which is also why `sourceId` combines the widget id with
     * the occurrence date. And the venue writes its lineups as **`ra.co/dj/<slug>` links inside
     * the HTML description** — a machine-readable DJ roster, unlike the surrounding prose, whose
     * lineup headings vary from night to night ("Lineup/Musik", "Line-up Live:", "❤️‍🔥 POP:") and
     * from which no artists are derived.
     *
     * Every night is a DJ party, so events are typed [PARTY][de.norm.events.event.EventType.PARTY]
     * unless the venue prefixes the title `KONZERT:`, its only category marker. The widget's own
     * `eventType` vocabulary is not one: the venue has filled it with weekday/time labels
     * ("Samstag, 14:00") that contradict the event's own `start.time`. Prices are quoted only in
     * the prose ("Abendkasse - 18€", "12€ Early Bird"), the venue publishes no per-event page, and
     * nothing on the calendar marks a night sold out, cancelled or moved.
     */
    HUMBOLDTHAIN,

    /**
     * Huxleys Neue Welt Berlin – the Neukölln concert hall (Hasenheide), on WordPress with an
     * Events-Manager `event` post type whose REST API is not exposed (only the stock post types are
     * registered), so its two HTML pages are the source. The `/events` page lists every upcoming show
     * as an `li.event-item` grouped under `.month` headings, each carrying a `.date` day cell, a
     * `.time` line (`Beginn: 20:00 | Einlass: 19:00`), an `.eventname`, an optional `+ Support:` line,
     * an optional `.anderungen` change note, and a link to its `/event/YYYY-MM-DD-<slug>` detail page
     * — whose ISO slug prefix is the authoritative date, since the card itself prints only a day and
     * a month abbreviation.
     *
     * The detail page adds the `.tourtitel` tour name, the poster, the Eventim link, the prose blurb,
     * and — as slugs on its `article` element — the venue's own taxonomies: `event-tags-*` are real
     * music genres (`electronic`, `indietronica`), `promoters-*` the booking agency. It carries no
     * heading of its own, so the overview's `.eventname` stays the title through the merge.
     *
     * Status comes from three places, because the venue uses three: a CSS class on the list item
     * (`Ausverkauft` → the sold-out flag, `Abgesagt` → `CANCELLED`), the matching `.canceledsoldout`
     * badge, and the free-text `.anderungen` note, which is the *only* signal for a show moved to
     * another date (`verschoben` → `POSTPONED`) or another house (`verlegt` → `RELOCATED`) — the
     * latter including shows that have moved **away** from Huxleys but stay on its listing.
     */
    HUXLEYS,

    /**
     * Junction Bar Berlin – retro hand-coded site imported from the homepage entry, which links to two programs merged
     * into this one source: the live-music listing (`music_html/music.html` → per-month `program/MM_YYYY/MM_YY.html`
     * pages) and the DJ program (`DJ_html/DJ.html`). Each page is a flat sequence of `strong.datum`/`strong.Datum` date
     * bars followed by band blocks (`.Stil1222` name + `p.text` bio + ticket-shop link) or DJ blocks (`p.djane`).
     */
    JUNCTION_BAR,

    /**
     * Kater Berlin – the Holzmarkt techno club (formerly Kater Blau), on WordPress with an `event`
     * post type whose REST route *is* public but returns no usable data: ACF is not exposed to REST,
     * so `acf` comes back empty and only id, title and permalink survive. The `/event/<slug>` pages
     * are likewise near-empty (a heading and nothing else). The **homepage** carries the entire
     * programme inline instead, so it is the single source.
     *
     * Each night is an `article.event[id=event-<postId>]` — the WordPress post id, and the event's
     * stable identity, since nothing else about a night is fixed. Its header holds a `.date-header`
     * day and a `.date-title` name; its `.entry-summary` opens with a
     * `Wd. DD.MM HH:mm — Wd. DD.MM HH:mm` span (start and *end*, the latter usually the following
     * morning), then an optional Resident Advisor `a.rsvp` ticket link, then free prose.
     *
     * That prose is only sometimes a lineup, and the venue marks which: a `____________` rule
     * introduces a **floor** (`HOPPER`, `ACID BOGEN`, `EXTRA`, sometimes suffixed `by <presenter>`),
     * and the lines beneath it are that floor's DJs — mapped onto
     * [ScrapedArtist.stage][de.norm.events.scraper.ScrapedArtist.stage]. Roughly ten of twenty-six
     * nights are structured that way; the rest are descriptions (a garden evening, a film night, a
     * residency's schedule notes), and no artists are derived from them rather than minting prose
     * lines like "free entry till 20:00" as acts.
     *
     * Dates carry a weekday but **no year**, so the year comes from [inferYearForWeekday]. The venue
     * publishes no prices, no images anywhere, and no per-event page worth fetching.
     */
    KATER,

    /**
     * LARK Berlin – the Holzmarktstraße live-music club, on WordPress
     * with an Advanced Custom Fields `event` post type whose public REST API
     * (`/wp-json/wp/v2/event`) exposes the whole 600-post archive as structured JSON — no HTML is
     * scraped (ADR-007 §"Selector Strategy" priority 1).
     *
     * The defining quirk is that the venue **overloads WordPress's own post date with the event
     * date**: `post.date` is the show's date and time while `date_gmt` keeps the publish instant.
     * That makes the listing natively sortable and orderable by event date — unlike
     * [HEIMATHAFEN], whose date sits in an unsortable ACF field — so the default
     * newest-first page already carries every upcoming show and paging stops as soon as a page
     * reaches the past. The time on that date is what the venue renders as **`Doors`**, so it is
     * read as the doors time; the separate `acf.event_doors_time` field is a dead default
     * (`19:00` on 613 of 623 posts, later than the start on some) and is deliberately ignored.
     *
     * Status lives **in the title**, not in a field: the venue appends or prefixes `SOLD OUT`,
     * `(ausverkauft)`, `CANCELLED:` or `(abgesagt)` to the post title, while `acf.event_status`
     * reads `Scheduled` on all 623 posts. The marker is read and then stripped from the stored
     * title. Support acts are likewise written into the title, as `<act> + <act> (support)`.
     *
     * Of the remaining ACF fields only `event_type`, `event_organizer` (the promoter),
     * `event_tickets_url` and `event_description` carry data; `event_entrance_fee`
     * (`None` throughout), `event_music_genre`, `event_card_subtitle` and the six-slot act
     * repeater are unused defaults. The poster is a WordPress `featured_media` id, resolved for
     * the upcoming events in one batched `/wp-json/wp/v2/media?include=…` request rather than
     * with `_embed`, which would triple the listing payload.
     */
    LARK,

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
     * Max-Schmeling-Halle Berlin – the Prenzlauer Berg arena, one of three halls run by Velomax and
     * programmed on **one shared listing** at `velomax.de/events`. Each entry there is an
     * `a.ticketWrap` carrying the hall as a CSS class (`msh` / `velodrom` / `ufo`) and its own
     * `.location` label, the venue's own `data-type` (`concert` / `show` / `sport`), the date parts,
     * and a link to the hall's own domain — so the three halls are imported as three sources off the
     * one page, filtered on that class (as [CLUB_DER_VISIONAERE] does for its three rooms).
     *
     * Each `/events/event/<slug>` detail page carries full **schema.org Event Microdata** — an
     * `itemprop` `startDate` and `doorTime` with machine-readable `datetime` attributes, an
     * `eventStatus`, the `performer`, the `organizer`, and the address — which is what the detail
     * parser reads rather than the rendered markup (ADR-007 §"Selector Strategy" priority 1).
     *
     * **Sport is deliberately not imported.** The arena's biggest programme strand is handball,
     * volleyball and basketball (32 of 85 current entries, mostly here), and the model has no
     * `SPORT` event type — importing fixtures as `OTHER` would bury the concerts they sit beside.
     * Only the venue's own `concert` and `show` entries are taken.
     */
    MAX_SCHMELING_HALLE,

    /**
     * MAXXIM Berlin – the Ku'damm party club, on Wix with a Wix Events widget. Its `/partys`
     * programme page renders client-side, but Wix server-side-injects the widget's full event data
     * as strict JSON in a `<script id="wix-warmup-data">` block, so that payload is the source
     * (ADR-007 §"Selector Strategy" priority 1) rather than the rendered cards — see
     * [WixEventsWarmupData], shared with [LOGE]. Each entry carries the title, a teaser
     * `description`, the poster (`mainImage.url`), a `scheduling.config` with a **UTC** `startDate`
     * plus a `timeZoneId`, and a `registration.ticketing` block with the lowest/highest ticket price
     * and a `soldOut` flag. Reading `startDate` naively would shift every night two hours early and
     * roll a 22:00 door onto the previous day, so it is converted to `Europe/Berlin`.
     *
     * The club opens nightly with a DJ programme and publishes no categories, lineups or genres, so
     * every night is typed [PARTY][de.norm.events.event.EventType.PARTY] and no artists are derived
     * (a live guest is named only inside the free-text title). Tickets are sold on the Wix event
     * page itself, so there is no external `ticketUrl`. The widget serves the upcoming window only
     * (~18 nights); the archive in `event-pages-sitemap.xml` is deliberately not crawled.
     */
    MAXXIM,

    /**
     * Metropol Berlin – the Nollendorfplatz concert hall, on WordPress with the same
     * Events-Manager plugin as [MIKROPOL] (and the same `/event/<iso-date-slug>` URL shape, so the
     * date is read from the slug rather than the German rendering). Its whole programme sits
     * unpaginated on `/events` as `li.event` rows: a `.date` block (`04/` + `Aug. 2026` + a
     * `HH:mm` start with an `Einlass: HH:mm` doors time in a nested `<small>`), a category link
     * (`Konzert` / `Party`), an `h2.artist` title with the support acts in a `small.support`
     * child, and a link to the detail page. The detail page adds the promoter
     * (`… presents:`), a `.tour` subtitle, the poster, the prose description and the Eventim
     * ticket link, and re-states the times unambiguously as `Einlass: 19:00 // Beginn: 20:00`.
     *
     * Three quirks drive the parser. The venue **transposes the two time labels** on some shows
     * (`Einlass: 20:00 // Beginn: 19:00`), which the shared `orderDoorsBeforeStart` guard
     * recovers, and writes an unset start as `0:00`, which is dropped rather than stored as
     * midnight. A **cancellation** is the `.attention` / `.alert-red` badge, while a show that
     * moved **out of** the house keeps its listing with a `"Verlegt ins <venue> –"` title prefix
     * (stripped by `stripRelocationPrefix`, status `RELOCATED`) — the adjacent
     * `.changes` / `.alert-blue` prose is deliberately *not* read as a status, because a show
     * moved **into** Metropol carries the same "verlegt" wording and does take place here. The
     * site publishes **no prices and no sold-out state** at all; tickets are sold off-site on
     * Eventim.
     */
    METROPOL,

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
     * Modus Berlin – hand-built, server-rendered club/concert site (the same codebase as
     * [Ritter Butzke](https://club.ritterbutzke.com), down to the shared logo asset). Its `/events`
     * page renders the whole programme as `.event-item` tiles — a `figcaption` holding a German
     * `DD.MM.YYYY` date and an `h2` title, plus a poster — each linking to an
     * `/event/DDMMYY-<Name>` detail page that adds the ticket shop link (Eventim, the venue's own
     * shop, or the promoter's), the poster at full size, an `h2` restating the date with the start
     * time (`24.09.2026 - 20:00`), and a prose description that sometimes carries the doors time as
     * a `Doors: 19:30` / `Beginn 20:00` line.
     *
     * **The date is deliberately *not* read from the slug**, even though it encodes one. The slug is
     * minted once and keeps the original date when a show moves: `160426-LunaSimao` renders as
     * `13.04.2027` under the title "Luna Simao (verschoben aus 2026)". The rendered date is
     * therefore authoritative and the slug is used only as the stable `sourceId`, so a postponed
     * show keeps its identity across the move instead of being re-minted. The venue publishes no
     * categories, prices or sold-out state.
     */
    MODUS,

    /**
     * Monarch Berlin – retro hand-coded PHP bar/club above Kottbusser Tor; the whole programme lives on a single
     * `/programm.php` page as a flat run of `div` blocks (no per-event URLs), each with a leading bold
     * `Weekday DD/MM/YYYY-HH:MM` date line, a `td#td1` title cell (a `(KONZERT)` suffix marks concerts, an
     * `ABGESAGT` prefix marks a cancellation), and an optional external "Ticket Vorverkauf" shop link.
     */
    MONARCH,

    /**
     * MS Hoppetosse Berlin – the moored Spree salon boat that is Club der Visionäre's **winter
     * location**, sharing that venue's WordPress theme, programme page and importer. Its nights are
     * the `.hoppetosseYellow` entries on the one listing (`hoppetosse.berlin/program/` serves the
     * identical page); the boat is dark in summer, so an off-season import legitimately yields no
     * events. See [CLUB_DER_VISIONAERE] for the page structure and its quirks.
     */
    MS_HOPPETOSSE,

    /**
     * Neue Zukunft Berlin – static landing page whose concert programme lives only in an embedded
     * Elfsight "Event Calendar" widget; events read from the widget's public JSON boot API
     * (`core.service.elfsight.com/p/boot/?w=<widgetId>`) rather than the JS-rendered `<div>`.
     */
    NEUE_ZUKUNFT,

    /**
     * OHM Berlin – the small bass/techno club in the Tresor power-station complex, on a
     * hand-themed WordPress. Its whole upcoming programme is a `ul.event-list` on the home page,
     * with **no per-event URLs, prices, images or ticket links** — each `li.event-item` carries
     * only a `.event-date` (`31/07`), a `.event-time` (`23:00`), an `.event-title` and a
     * `<br>`-separated `.event-lineup` of DJs. The WordPress REST API answers 401, so the
     * rendered page is the only source, and `sourceId` is built from the resolved date plus the
     * slugified title.
     *
     * Two properties shape the importer. **The date carries no year and no weekday**, so the year
     * is resolved with [inferYearForWeekday][de.norm.events.scraper.inferYearForWeekday]'s
     * weekday-less path: the occurrence nearest today wins, which keeps a night that has just
     * happened in the current year instead of rolling it twelve months forward. And the venue
     * publishes an extremely **short horizon** — one to three nights, the rest moving to
     * `/archives` once past — so a low event count is normal here and is not evidence of a broken
     * importer; the archive is deliberately not crawled, as it holds only past events.
     *
     * Every night is a DJ programme, so events are typed
     * [PARTY][de.norm.events.event.EventType.PARTY] and the lineup entries are stored as `DJ`
     * artists. The title is a party/collective name (`Ouch x FemmeDecks`), not an act, so it is
     * never minted as an artist.
     */
    OHM,

    /** Privatclub Berlin – WordPress-based single-page event listing. */
    PRIVATCLUB,

    /**
     * Quasimodo Berlin – the Charlottenburg jazz/blues cellar (Berlin's oldest), on WordPress with
     * the same Events-Manager plugin as [METROPOL] and [MIKROPOL]. The programme is on the
     * **`.club` domain**: `quasimodo.de` is only a splash page. `/events` renders the whole
     * season unpaginated as `a.event-item` cards grouped under German month headings.
     *
     * A card carries two date blocks for the two breakpoints, and the **mobile one is the useful
     * one**: `.event-data.visible-xs .date` holds a complete `DD.MM.YYYY - HH:mm`, so neither the
     * month heading nor the abbreviated desktop `.day`/`.month` pair has to be read. It also
     * carries the genre tags (`.event-tags a`, the venue's own taxonomy — Blues, Latin Jazz,
     * Neo-Soul), a background-image poster and the external ticket-shop link (Eventim, ticket.io,
     * tixforgigs). Each links to `/events/<slug>-<postId>`, whose trailing id makes the `sourceId`
     * stable across the venue's recurring series (WE LOVE 80S runs five times, Disco Inferno three).
     *
     * The detail page adds the description, the `… präsentiert:` promoter, the full-size poster and
     * a `<table>` of `Beginn` / `Einlass` / `Vorverkauf` / `Tageskasse` rows — a real presale *and*
     * box-office price, the presale written as `ab 30€ (zzgl. Gebühr)`, whose "from" and booking-fee
     * caveats are kept in `priceNote`. It is also the only page carrying the **category**, as an
     * `event-categories-<slug>` class on its `<article>`: the venue marks its DJ nights `party` and
     * leaves most concerts untagged, and a night can be tagged both (`Disco Inferno` is
     * `concerts party`), so `party` wins and an untagged event falls back to title inference.
     */
    QUASIMODO,

    /**
     * Renate (Wilde Renate) Berlin – the Friedrichshain techno club, on WordPress with no `event`
     * post type in its REST API, so its homepage is the source. Each night is a `.prog-row`
     * carrying a `.prog-day` weekday, a year-less `.prog-date`, a `.prog-title`, the spaces used
     * (`.cat-btn`: `CLUB` / `GARTEN`), a Resident Advisor `.ticket-link`, and a `.prog-text` block
     * holding the per-floor lineup. A trailing `.prog-row.blog-row` carries a news post rather than
     * an event and is excluded by requiring a date.
     *
     * The lineup is the reason to import this venue and the reason it needs care. Floors are
     * `<strong>` headings — `GREEN (from 22:00) hosted by Kollektiv Lost In`, `BLACK`, `RED`,
     * `GARDEN` — with the DJs beneath them, but `<strong>` is also used for the venue's slogan
     * (`Garten für alle!`), for a continuation line (`hosted by Neer`), and for festival blurbs. A
     * heading therefore opens a floor only when it starts with one of the venue's **actual floor
     * names**, and an act line is only taken when it is short enough to be a name — the same prose
     * paragraphs otherwise become artists.
     *
     * Dates carry a weekday but no year, so the year comes from [inferYearForWeekday]. The venue
     * publishes no prices, no images and no per-event page, so `sourceId` is the date plus the
     * slugified title; its prose is club policy repeated verbatim on every night, so no description
     * is stored.
     */
    RENATE,

    /**
     * Ritter Butzke Berlin – the Kreuzberg techno club, on the **same hand-built codebase as
     * [MODUS]** (Modus even ships this venue's logo asset), but a different template: `/events`
     * renders Bootstrap grid cards rather than Modus's `figcaption` tiles, so the two share no
     * selectors. What they *do* share is the trait that matters — every event links to
     * `/event/DDMMYY-<Name>`, and **that slug keeps the original date when a show moves**. One of
     * the 29 events at capture proved it: `310726-DeeportamentCommunityw-NicoMorano-OpenAir-Indoor`
     * renders `04.09.2026` on both the card and the detail page. The rendered date is therefore
     * authoritative and the slug serves only as the stable `sourceId`.
     *
     * A card carries the poster, a `DD.MM.YY` date and the title; the detail page restates the
     * date in full (`DD.MM.YYYY`), adds an `ab HH:mm` start time, the ticket shop (a pretix widget
     * whose `event` attribute holds the URL, or a Resident Advisor button), and a `Line Up:` block
     * listing the night's DJs one per row. Every night is a DJ party, so events are typed
     * [PARTY][de.norm.events.event.EventType.PARTY], the lineup is stored as `DJ` artists, and the
     * title — an event/series name (`House of Rave w/ …`) — is never minted as an act.
     *
     * The club runs several floors, so **two or three events share a date routinely**; the slug's
     * trailing name is what keeps them apart. `/calendarfile/<id>` is `Disallow`ed by robots.txt
     * and is never fetched.
     */
    RITTER_BUTZKE,

    /** Roadrunner's Paradise Berlin – retro hand-coded single-page `programm.html` listing (rockabilly/roots). */
    ROADRUNNER,

    /**
     * Säälchen Berlin – the concert hall on the Holzmarkt riverside grounds, on the Holzmarkt's
     * shared Drupal site. `/kalender` is **one calendar for the whole site**: its `.views-row`
     * entries cover the Marktplatz flea markets and the Holzmarkt 25 grounds as well, so rows are
     * filtered on the `.location` span — only `Säälchen` is this venue. Month tabs are in-page
     * anchors, not extra requests, so one fetch carries the whole programme.
     *
     * Each row embeds an **AddToCalendar** widget whose `<var class="atc_*">` values are the
     * machine-readable part: `atc_date_start` is a **UTC** timestamp (a 20:00 Berlin night reads
     * `19:00:00` in winter, `18:00:00` in summer), so it is converted to `Europe/Berlin` for the
     * date. Its `atc_description` carries a hand-typed `Datum / Einlass / Beginn / Ende / Eintritt
     * / Tickets` block followed by the event's prose, which is the source for the times, the price
     * and the description.
     *
     * **The `.doors` span is not trustworthy and is only a fallback.** The editors fill that single
     * CMS time field inconsistently — it holds the *Einlass* on one night (`Opening Party`, 18:00)
     * and the *Beginn* on another (`Voodoo Jürgens`, 20:00 against a 19:00 Einlass) — so the
     * explicitly labelled prose wins. The `Eintritt:` line is equally free-form (`17,00 €`, `€40 +
     * fees`, `30,00`, and a three-tier `15€ ermäßigt … 25€ Normalpreis … 35€ Förderticket`), so it
     * is kept verbatim in `priceNote` and only converted to a number when it names exactly one
     * amount. Its `Datum:` is not read at all — one event writes it in English (`October 5, 2026`).
     */
    SAALCHEN,

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
     * Sonnenraum Berlin – the indoor concert space next door to Club der Visionäre, sharing that
     * venue's WordPress theme, programme page and importer. Its own `/sonnenraum/` page is a
     * description that points the reader back at the programme ("Angekündigt werden diese
     * Veranstaltungen … unter Programm"), so its nights are simply the `.sonnenraumYellow` entries
     * on the one listing. See [CLUB_DER_VISIONAERE] for the page structure and its quirks.
     */
    SONNENRAUM,

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
     * Tempodrom Berlin – the tented concert hall at Möckernstraße, on a ProcessWire site whose
     * `/programm-und-tickets/` page embeds its **entire** programme as one
     * `<script type="application/ld+json">` array of schema.org `Event` objects — 145 at the time of
     * writing, each with `startDate`, `doorTime`, `image`, `description`, `eventStatus` and an
     * `offers` block carrying `price` / `lowPrice` / `highPrice` and `availability`. The rendered
     * cards add nothing the JSON-LD lacks, so the structured data is the source (ADR-007 §"Selector
     * Strategy" priority 1) and no detail page is fetched.
     *
     * Two fields the venue publishes are deliberately not used as they stand. `performer.name` is
     * always a copy of the event `name` rather than an act, so artists are derived from the title
     * like any other concert hall's. And `location.name` is always "Tempodrom Berlin": the house
     * has a Große and a Kleine Arena, but which one an event uses appears nowhere in the listing —
     * only in the site navigation — so it is imported as a single venue rather than two.
     *
     * A multi-day run (a congress, an esports final) carries an `endDate` days after its
     * `startDate` and a date-only `startDate`; it is stored as one event on its opening day, the
     * model having no end date.
     */
    TEMPODROM,

    /**
     * Theater im Delphi – the 1929 silent-cinema building in Weißensee, now a theatre and concert
     * hall. WordPress; `/programm/` renders the **whole upcoming programme** in one page with no
     * pagination and no archive, as a run of `h2.month` headings each followed by a
     * `table.program_table` whose every `<tr>` is one performance. A production that runs several
     * nights repeats its row per date, so the `sourceId` is `<prod id>/<date>-<clock>` — several
     * productions play a matinee and an evening on the same day.
     *
     * The `?prod=<id>` page belongs to the **production**, not the date: it lists the whole run and
     * describes the show once. Only the full blurb and the full-width photo are taken from it, and
     * each distinct page is fetched once for all its dates, as at [BAR_JEDER_VERNUNFT].
     *
     * The venue's labels are staging **formats** (`Tanz`, `Theater`, `Musiktheater`,
     * `Dialog & Lesung`) rather than musical genres, so they drive the event type — dance and
     * theatre both to `SHOW`, the model having no type of their own. Only `Kammermusik` and
     * `Elektronische Musik` name a genre and are stored as one.
     *
     * **Prices exist only in a leaked debug dump.** The programme page emits a `var_dump()` of each
     * performance's database row into an HTML comment, 23 fields including `event_BetragAb`/`Bis`
     * and an `event_EintrittFrei` flag — the rendered page states no price anywhere. That leak is
     * joined on `(production id, start time)` and used strictly best-effort: the day the venue
     * fixes it these events lose their prices and nothing else. `event_Zeit` is a Unix timestamp of
     * the **Berlin wall clock**, verified against the rendered times across a DST boundary.
     */
    THEATER_IM_DELPHI,

    /**
     * Tresor Berlin – the Köpenicker Straße techno club in the former Heizkraftwerk, on WordPress
     * with its REST API disabled site-wide (`401 rest_disabled`), so its two HTML pages are the
     * source. `/club/events/` lists each night as an `article.event-item` carrying a year-less
     * `Sa 01.08` date, a title, and the night's lineup already grouped **by floor** —
     * `.event-floor` → `.floor-name` (`Tresor`, `Globus`, `Aurora Bar`) → `.floor-artist` — which
     * is the cleanest lineup markup of any venue here and maps straight onto
     * [ScrapedArtist.stage][de.norm.events.scraper.ScrapedArtist.stage]. The date comes from the
     * `YYYYMMDD` prefix of the `/event/YYYYMMDD-<slug>/` permalink rather than the rendered card,
     * which prints no year.
     *
     * Each detail page adds a per-artist **set time** (`23:00-02:00`) and a blurb. The model has no
     * per-artist time, so the night's opening set supplies the event's start time — the venue
     * publishes no doors or start time of its own. The blurb is followed by an underscore rule and
     * then several screens of guest and ticket policy repeated on every night, so only the part
     * above that rule is kept.
     *
     * The venue publishes no prices, no images and no ticket link (tickets are sold on Resident
     * Advisor, mentioned only in the policy prose), and bills an unannounced slot as `???`.
     */
    TRESOR,

    /**
     * Uber Arena Berlin – the Friedrichshain arena (formerly Mercedes-Benz Arena) on AEG's own CMS,
     * the platform it shares with [UBER_EATS_MUSIC_HALL]. Its `/events/all` page renders the whole
     * programme server-side and unpaginated — 128 entries at capture — as `div[data-categoryname]`
     * rows. Each carries the venue's own category, a `.m-date__*` span group holding the weekday,
     * day, month, year and `HH:mm Uhr` start, an `.event-title`, an `ab NN,NN €` from-price, a
     * thumbnail, and a link to `/events/detail/<slug>/<YYYY-MM-DD-HHMM>` whose path segment makes
     * the `sourceId` unique per performance (a tournament runs the same slug over many dates).
     *
     * **Sport is deliberately not imported**, following the [MAX_SCHMELING_HALLE] precedent: the
     * arena is home to ALBA Berlin and the Eisbären, and 40 of the 128 entries are `eishockey`,
     * `basketball` or `sport` — filed as `OTHER` they would bury the 88 concerts, shows and comedy
     * nights they sit among. Those rows are also the only ones carrying a `00:00 Uhr` placeholder
     * start, so the filter removes that noise too.
     *
     * The detail page adds what the row cannot: the `Einlass` doors time, the prose description and
     * the external AXS ticket-shop link. Its `h1` appends "in der Uber Arena" to the act, so the
     * listing's cleaner title is kept instead.
     */
    UBER_ARENA,

    /**
     * Uber Eats Music Hall Berlin – the arena's smaller neighbour, on the **same Carbonhouse
     * tenant** as [UBER_ARENA]: same `/events/all` listing shape, same
     * `/events/detail/<slug>/<YYYY-MM-DD-HHMM>` URLs, same detail-page fields, and even the same
     * asset bucket (its posters are served from `uber-arena.production.carbonhouse.com`). One
     * parser pair therefore serves both, as the Velomax halls share theirs.
     *
     * Two tenant differences are handled in that shared parser. This venue emits only the numeric
     * `data-category`, not the `data-categoryname` the arena publishes, so the platform's numeric
     * taxonomy — decoded from the arena, which emits both — supplies the label. And it abbreviates
     * its month in German (`Sep. `, `Mär `) where the arena writes it numerically (`08.`).
     *
     * Its `#tickets` block also renders a **self-link before** the real AXS shop link with
     * identical classes, so the ticket URL is taken as the first link pointing off the venue's own
     * host rather than simply the first one. Being a music hall it has no resident team, but the
     * shared sport filter still applies.
     */
    UBER_EATS_MUSIC_HALL,

    /**
     * UFO im Velodrom Berlin – the smaller hall configured inside the Velodrom, listed as its own
     * `UFO` location on the shared Velomax programme and served by its own `ufo-velodrom.de` domain.
     * Its nights are the `.ufo` entries on the one listing; see [MAX_SCHMELING_HALLE] for the page
     * structure, the Microdata detail pages and the sport exclusion.
     */
    UFO_IM_VELODROM,

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
     * Velodrom Berlin – the Landsberger Allee arena, the second of the three Velomax halls. Its
     * nights are the `.velodrom` entries on the shared listing; see [MAX_SCHMELING_HALLE] for the
     * page structure, the Microdata detail pages and the sport exclusion. A show staged in the
     * hall's smaller [UFO_IM_VELODROM] configuration is labelled by the venue itself, so an event
     * whose slug mentions "ufo-im-velodrom" but whose `.location` says Velodrom is imported here.
     */
    VELODROM,

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
     * Parkbühne Wuhlheide Berlin – the Köpenick open-air amphitheatre, on October CMS. Its
     * `/programm` page carries the whole season, unpaginated, split into one `.shows` block per
     * year (`Konzerte 2026`, `Konzerte 2027`). Each `.show` holds a German long date
     * (`Samstag, 01. August 2026`), an `h2` act, an `h3` tour name, a poster, an
     * `Ausverkauft` `.statusLabel` and — unless sold out — a Ticketmaster/Eventim link. The
     * **date is read from the URL instead**: every event links to `/programm/<act>/YYYY-MM-DD`,
     * an ISO date that needs no German month parsing and doubles as the stable `sourceId`.
     *
     * The listing carries no times or prices; the detail page's `<table>` adds `Einlass`,
     * `Beginn`, a `NN,NN EUR` price (written with the currency spelled out, so the shared
     * `€`-anchored parser does not apply) and the `Veranstalter` promoter. Two detail-page
     * quirks: its `h3` is **not** reliably the tour name — a show can put an admin notice there
     * ("Bitte die Altersbeschränkungen beachten:") — so the subtitle is taken from the listing
     * only; and a sold-out show simply omits both the price row and the ticket link.
     *
     * An act's name may be broken by a `<wbr>` hint (`AnnenMay<wbr>Kantereit`), which Jsoup
     * renders back as the unbroken `AnnenMayKantereit`. A run of nights by one act is normal
     * here (AnnenMayKantereit ×3, Nina Chuba ×3), which the per-date URL keeps apart.
     * `Ausverkauft` sets the sold-out flag, never a status, per the shared convention.
     */
    WUHLHEIDE,

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
    ZENNER,

    /**
     * Zitadelle Spandau – the Renaissance fortress whose entire event calendar is the **Citadel
     * Music Festival**, the open-air concert series staged in its courtyard each summer. The venue's
     * own site is a museum site; the programme lives at `citadel-music-festival.de`, which is what
     * this imports. The season is small — under a dozen dates, rendered in full with no pagination.
     *
     * WordPress with the Events Manager plugin, but the grid is a hand-written child-theme template
     * (`article.cmf-card`) rather than the plugin's markup: an `h3.cmf-title`, a machine-readable
     * `time[datetime]`, a `.cmf-time` start, a `data-status` badge, and a link to
     * `/event/<YYYY-MM-DD-slug>`. The plugin's `event` post type is **not** exposed through the
     * WordPress REST API, so despite ADR-007's JSON-first preference there is no API to read.
     *
     * The detail page adds the `Einlass` doors time (which may read `tba`), a `.tour-title`
     * subtitle, the `.eventnotes` prose, an external shop link (Eventim or Ticketmaster), and the
     * presenters ("präsentiert von: Flux FM, tip Berlin"), stored as promoters as at the Columbia
     * Theater — the promoter proper is only a taxonomy slug with no display name anywhere.
     *
     * Two traps. Every card's `aria-label` ends "– Ausverkauft" **regardless of the actual state**,
     * a broken template string; `data-status` is what tracks reality. And a relocated show is
     * badged `Abgesagt` with a separate `.aenderungen` line explaining the move ("Wird in die
     * Columbiahalle verlegt"), so that line — not the badge — decides between `CANCELLED` and
     * `RELOCATED`, and is kept at the head of the description so the status says where the show
     * went. The site publishes no prices at all, only shop links.
     */
    ZITADELLE;

    /**
     * Prefix for `sourceId` values, derived from the enum name in lowercase.
     *
     * Used by scrapers to build sourceId strings (e.g. `"cassiopeia:some-event-slug"`).
     * This avoids hard-coding the prefix string in scraper classes.
     */
    val sourceIdPrefix: String get() = "${name.lowercase()}:"
}
