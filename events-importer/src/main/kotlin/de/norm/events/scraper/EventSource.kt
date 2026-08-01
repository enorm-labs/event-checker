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
