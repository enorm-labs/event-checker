# Importer — Known Issues & Limitations

A living catalogue of the known gaps, quirks and missing-data limitations of the currently implemented event importers, so they can be picked up later. This is
**not** a bug tracker for regressions — it records *accepted / documented*
limitations of the scrape pipeline as it stands.

Related: [ADR-007 Web Scraping Strategy](adr/ADR-007_WEB_SCRAPING_STRATEGY.md) ·
[EVENT_DATA_SOURCES.md](EVENT_DATA_SOURCES.md) (source inventory & import status) ·
[DATA_QUALITY_STRATEGY.md](DATA_QUALITY_STRATEGY.md) (how these get fixed & prevented) · actionable backlog in [../TODO.md](../TODO.md).

Legend: **impact** — 🔴 user-visible missing/wrong data · 🟠 data-quality/noise · 🟢 cosmetic or edge case.

---

## Cross-cutting (affect several/all importers)

- 🟢 **Concerts extract the title as headliner (all venues).** Every venue now treats a `CONCERT` title as the headliner via `buildArtistsForEventType` /
  `headlinersFromTitle` (Privatclub and Badehaus unconditionally; Cassiopeia too, guarded by the strengthened `isNonArtistName` since its titles are the most
  ambiguous — see per-importer notes). This recovered the ~40% of concerts (148 of 354 in a July 2026 seed) that previously stored no artist. Residual risk is
  the reactive non-artist denylist (next entry): a title that is really an event name but matches no structural filter is minted as a headliner until
  denylisted. **Existing rows are unaffected until re-imported** — a one-off backfill re-scrape is still tracked in `TODO.md`.
- 🟠 **A few non-artist titles still slip through as artists.** The curated filters catch festivals/tours/segments/labels, and `stripArtistSuffix` recovers the
  act from tour/live/anniversary tails and performance-format annotations (`THE BUTLERS - 40 YEARS, SKA & SOULPOWER -` → `The Butlers`, `Avangelic (DJ-Set)` →
  `Avangelic`). Idiosyncratic event-format titles are handled case-by-case as they surface by adding them to the curated denylist (`NON_ARTIST_NAMES`, e.g.
  `Music Quiz`,
  `Open Mic L. J. Fox`) — but this is inherently reactive, so newly-seen titles will slip through until denylisted. A general fix needs a classifier → tracked
  in
  `TODO.md` (AI-assisted data quality).
- 🟠 **Artist display names — de-shouted, with residual cases.** `canonicalArtistName`
  now de-shouts ALL-CAPS act names to a clean display form before they're stored (`GREEN LUNG` → `Green Lung`), so an act isn't frozen SHOUTING by whichever
  venue imported it first. It's *casing-only* — no words are stripped (unsafe for band names). It de-shouts words with attached punctuation too
  (`MURPHY'S LAW` →
  `Murphy's Law`, `(BLACK KRAY)` → `(Black Kray)`), while keeping digit/dotted stylised tokens (`MC5`, `H2O`, `AC/DC`, `HGICH.T`), mixed casing (`DJ Koze`), and
  a curated acronym set (`DJ`, `MC`, `UK`, …). Residual, by design:
    - a genuine all-caps name not in the acronym set is title-cased like any shouted word, whether letters-only (`MUNA` → `Muna`, `MØ` → `Mø`) or stylised with
      an interior symbol other than `.`/`/` (`BIGA*RANX` → `Biga*ranx`) — extend `ACRONYMS` to keep one;
    - 🟢 this only cleans the display name; slugs are case-insensitive, so ALL-CAPS and mixed-case spellings already resolved to one artist row (no
      fragmentation), and rows created before this fix keep their original casing until re-created.
- 🟢 **Genre tags are normalized through a curated map, stop-list, and gated fall-through.** `GenreNormalizer` splits on more separators (now also ` or `,
  ` oder `, ` vs `, so `Tango or NonTango` → `Tango`), resolves tokens against a synonym table, and drops non-genre noise two ways: a `NON_GENRE_TOKENS`
  stop-list removes event-format labels a venue pushed into the genre field (`Immersive Ausstellung`, `Release Party`, freeform fragments like `Beyond` /
  `Wave` / `Retro`), and a `looksLikeGenre` gate keeps an unmatched token only when it plausibly names a genre (≤2 words, has letters, no stop-listed word), so
  long series labels like `Twenty One Pilots Special` no longer leak. The raw genre text is still preserved on the event.
    - 🟢 Vocabulary is gated, not closed: a genuinely new genre that passes the heuristic is still captured as-is, and everything dropped is logged
      (`Dropping non-genre token …`) as the curation queue for growing the synonym map / stop-list. `Karaoke` stays a tag (it is in the synonym map and treated
      as a genre) even where a venue uses it as an event label.
- 🟠 **`eventType` frequently defaults to `OTHER`.** When a source exposes no category, `toEventEntity` maps to `OTHER`. Discovery/filtering by type is therefore
  incomplete for several venues (see per-importer notes).
- 🟢 **First page only — pagination is intentionally not followed** (ADR-007). Far-future events on page 2+ of paginated listings are not imported. Accepted:
  first page = the most relevant upcoming events; multi-page crawling was rejected.
- 🟢 **`priceCurrency` is hard-coded to EUR** (all venues are Berlin). Fine until a non-EUR venue is added.

## Data-quality / entity resolution

- 🟠 **Promoter fragmentation — mostly fixed, with residual cases.**
  `canonicalPromoterName` now merges abbreviated/full variants (`LOFT` /
  `Loft Concerts GmbH` → `Loft`). Remaining, by design:
    - a *leading* descriptor isn't stripped (`Konzertbüro Schoneberg` ≠
      `Schoneberg Konzerte`) unless a curated `NAME_CORRECTIONS` entry is added;
    - source typos and spelling/spacing variants are folded onto one spelling via a curated map (`Trinty` → `Trinity`, `Allrooms`/`ALLROOMS` → `All Rooms`);
      only *known* names are corrected — new ones need an entry in
      `NAME_CORRECTIONS`;
    - 🟢 de-shout lowercases genuine acronyms in the display name (`TV Noir` →
      `Tv Noir`, `Bossa FM` → `Bossa Fm`) — display-only, slugs unaffected.
- **Coverage gap (not a defect):** JS-rendered venues aren't importable yet (Playwright deferred, ADR-007). A cookie wall is not itself a blocker — SO36 is
  imported by scraping its Ticket-Toaster shop (`/tickets`) behind the wall. See the **Blocked / deferred** section of
  [EVENT_DATA_SOURCES.md](EVENT_DATA_SOURCES.md#-blocked--deferred) for which venues remain and what would unblock each.

---

## Per-importer

### Cassiopeia (`scraper/cassiopeia/`) — Webflow, list + detail

- 🟠 **First page only** (Finsweet CMS Load lazy-loads the rest via JS) — ~8 events.
- 🟠 **Title-as-headliner, structurally guarded.** For a `CONCERT` the title is taken as the headliner (support acts still come from `Support:` description
  paragraphs). Cassiopeia titles are the most ambiguous — they may be an artist *or* an event name (e.g. "Grey City Fest Opener") — so the `isNonArtistName`
  festival filter was widened to catch a festival slot/edition (`… Fest <slot>`), not just a bare/year-suffixed festival name. Residual: an event-name title
  that matches no structural filter is still minted as a headliner until denylisted.
- 🟢 Some fields use **positional CSS fallbacks** (`._5`, `._8`) — fragile if the Webflow layout changes.

### Privatclub (`scraper/privatclub/`) — WordPress, single page

- 🟢 **Concerts extract the title as headliner** via `buildArtistsForEventType`:
  a `CONCERT` title is the act unconditionally (support acts from the subtitle's
  `Support:` line), while parties/festivals extract none. A concert titled just
  `20Tokens` now yields a headliner; the ~70 artist-less concerts in a July 2026 seed are recovered (see the cross-cutting entry).
- 🟢 Complex/conditional pricing is stored as free-form `priceNote` rather than structured presale/box-office.

### Madame Claude (`scraper/madameclaude/`) — WordPress, list + detail

- 🟢 Small venue (~11 events); `DD/MM/YY` dates. No major known gaps beyond the cross-cutting ones.
- 🟢 **DJ-set nights are detected from the "(DJ-Set)" title marker.** Such events are typed `PARTY` (the CSS category maps them to `CONCERT`) and their DJ lineup
  is sourced from the title, because the detail pages carry no reliable per-artist `<h3>` (see
  `isDjSetTitle` / `djSetArtistsFromTitle`). The title is split on `+` and guarded
  `&`/`and`/`und` but **not** on `/`, since Madame Claude uses `/` inside a single act name (`Morimoto / Wong duo`). Residual, by design:
    - a `DJ <handle>` name is **not** de-prefixed (`DJ Lichene` stays `DJ Lichene`), so it can fragment from the same DJ listed elsewhere without the prefix. A
      blanket strip is unsafe — it would maim real acts whose name includes "DJ" (`DJ Koze`, `DJ Shadow`) — and there is no reliable structural signal to tell a
      role label from a stage name. The general entity-resolution fix is tracked in `TODO.md` (AI-assisted data quality).

### Astra Kulturhaus (`scraper/astra/`) — Kulturhäuser platform, list + detail

- 🟠 **Festival-day mislabeling** is only best-effort corrected. Astra tags each festival day individually and sometimes labels one "Concert"; normalization
  fixes it only when a correctly-labelled sibling exists on the same page.
- 🟠 **Dateless featured teaser** depends on its detail page for the date; if that fetch fails the event is dropped (e.g. `11FREUNDE WM-QUARTIER` drops each
  run).
- 🟢 Duplicate events on the listing are skipped by `sourceId` (`ERRA + CURRENTS`,
  `VOILÀ`) — expected, but means the site genuinely double-lists some events.
- The detail page carries no artist roster, so artists come only from the overview.

### Lido (`scraper/lido/`) — same Kulturhäuser platform as Astra

- Shares Astra's platform limitations (teaser date fallback, artists from overview).

### SO36 (`scraper/so36/`) — Ticket-Toaster shop, list + detail

- 🔴 **Sold-out is never detected.** The JSON-LD `availability: SoldOut` is unreliable — SO36 sells via external shops that report on-platform availability as
  `SoldOut` even when tickets are freely available — so it's ignored and
  `soldOut` is always `false`. A reliable sold-out signal has not been found.
- 🟠 No genre, promoter, or structured box-office price. `eventType` is limited to what the `supertitle` label exposes (Konzert/Party), else `OTHER`.

### Roadrunner's Paradise (`scraper/roadrunner/`) — retro hand-coded, single page

- 🔴 **No artists, promoters, prices, or event type.** The free-text retro HTML carries none reliably; `eventType` defaults to `OTHER`.
- 🟠 **Very sparse & stale.** Currently ~1 event; the site leaves past events listed. The year is *inferred from the weekday* (the source omits it), which
  resolves stale past dates correctly but yields events that won't show in today-forward feeds.

### Arcanoa (`scraper/arcanoa/`) — 1990s hand-coded, single page

- 🔴 **Only a title, a date and a style line exist.** The page has no per-event URLs, images, ticket links, prices, sold-out or cancellation markers, so those
  fields are always null — the poorest source in the set. The one time it publishes is a per-month "Veranstaltungsbeginn: 20 Uhr" line, stored as `startTime`
  for every event in that month; there is no doors time and no per-event override (the Tuesday jam's own "19-21 Uhr" stays in the subtitle).
- 🟠 **The same recurring night appears under two title spellings.** The venue hand-types each line, so its Monday open stage is written both `ARCANOA-Open
  Stage` and `ARCANOA- Open Stage`; only the second has a dash the parser can pad, so the two normalize differently and produce different slugs. Harmless (the
  dates differ anyway) but visible in a title list.
- 🟠 **Recurring formats are filtered out of the lineup by a venue-local denylist.** Roughly half the programme is a standing format whose "act" is the format
  (open stage, jam session, `SpielleuteSession`, medieval night), and there is no structural signal separating those from a billed act — so
  `RECURRING_FORMAT_PATTERN` drops them by name. Reactive, like the shared `NON_ARTIST_NAMES` denylist: a newly-introduced format is minted as an artist until
  added.
- 🟢 **Style tails are not genres.** The free-text tail (`AfroLatinFolkJazzEthnoBluesSession`, `HellCountryBlues`) is kept as the `subtitle`, deliberately not
  the genre field, so it never seeds bogus genre tags — same call as Duncker.
- 🟢 **Year inferred from the German weekday** (the `DD.MM.` date omits it), which also resolves the passed events the venue leaves listed — like Roadrunner and
  Duncker.
- 🟢 **Private bookings are skipped.** A `geschlossene Gesellschaft` line marks the venue as taken for a private function; it is not a public event and is
  dropped rather than imported as an untyped one.

### Badehaus (`scraper/badehaus/`) — WordPress / Events Manager, list + detail

- 🟢 **Title-as-headliner (from the overview).** The overview scraper now extracts the headliner from the title for inferred `CONCERT` events via
  `buildArtistsForEventType` (the detail page still carries no roster, so the merge falls back to the overview's artists). A concert titled `Anette Olzon` or
  `El Flecha Negra` yields a headliner; the ~72 artist-less concerts in a July 2026 seed are recovered. Depends on the inferred type below — a title
  mis-inferred as non-`CONCERT` yields no artist.
- 🔴 **`eventType` is inferred from the title, not scraped.** Badehaus publishes no machine-readable category anywhere, so the type is a heuristic (quiz /
  party / screening, else `CONCERT`). Non-matching events may be mislabelled.
- 🟠 **Start time (`Beginn`) and promoter are only on *some* detail pages** — many events have doors time only and no promoter.
- 🟢 One venue-side **dead link** (a `%`-encoded Arabic-slug event) `404`s each run and degrades to overview data — correctly handled, but worth knowing.
- 🟢 Heaviest importer: ~90 sequential (throttled) detail fetches, ~40s per run.

### Bi Nuu (`scraper/binuu/`) — SvelteKit / PocketBase, list + detail

Event data comes from the SvelteKit SSR payload embedded in each page (a JS object literal in the `kit.start(...)` bootstrap script), parsed via
`BinuuSvelteKitPayload`
rather than the rendered DOM — so dates carry full four-digit years and fields are structured. Limitations:

- 🔴 **`eventType` is inferred from the title/subtitle, not scraped.** Bi Nuu exposes no category field anywhere on the site, so — like Badehaus — the type is a
  best-effort heuristic (`inferBinuuEventType`): `quiz` → `QUIZ`; a curated recurring party/DJ series (`BINUU_PARTY_SERIES`: GrooveJet, Ultra Night, Boheme
  Sauvage — the same names on the artist `NON_ARTIST_NAMES` denylist, edition number ignored) or a party keyword (`party`/`karaoke`/`dj set`/`club night`/
  `rave`) → `PARTY`; everything else defaults to
  `CONCERT` (Bi Nuu is live-music-leaning). It deliberately does **not** sniff the description for genre words: at this metal/rock venue `dancefloor`/`disco`
  turn up in band tour and album names (e.g. Gutalax's "Shit On The Dancefloor" tour is a death-metal gig), so a description scan mislabels concerts. Being
  curated it is reactive — a newly-seen series is `CONCERT` until added to both lists. The type does **not** gate artist extraction here (that runs off the
  structured `performers` list).
- 🟠 **Event/party-series names can slip in as headliner artists.** Artists come from the site's structured `performers` array (a performer also named in the
  `subtitle_2`
  support line becomes `SUPPORT`, otherwise `HEADLINER`) — more reliable than title-parsing, but when a recurring club/DJ night lists *its own name* as the sole
  performer, that name is minted as a headliner. Known series are on the
  `NON_ARTIST_NAMES` denylist (`GrooveJet Berlin`, `Ultra Night`, `Boheme Sauvage
  N°<n>`); like every curated denylist this is reactive, so a newly-seen series slips through until added → the general classifier fix is tracked in `TODO.md`
  (AI-assisted data quality), shared with the cross-cutting reactive-denylist limitation.
- 🟢 **`eventStatus` is a single-letter code** mapped in `mapBinuuStatus`: `r` →
  `RELOCATED` (carries `locationNew`), `p` → `POSTPONED` (carries the original date in
  `startOld`). Any other/unseen code defaults to `SCHEDULED` and is logged, so a new code surfaces rather than being mismapped. A postponed event stores its new
  date (`start`), not `startOld`.
- 🟢 **No genre, and occasional missing `ticket_url`.** No genre is exposed anywhere; some events sell only via a link buried in the description, so `ticket_url`
  is sometimes null. Timestamps carry a spurious `Z` suffix on local Berlin wall-clock times — read as local, no timezone shift (see `parseBinuuDate`/
  `parseBinuuTime`).

### Gretchen (`scraper/gretchen/`) — retro hand-coded, single page

- 🟠 **`eventType` is inferred from the title, not scraped.** Gretchen exposes no category anywhere, so — like Badehaus and Bi Nuu — the type is a best-effort
  heuristic (`inferEventType`): `quiz` → `QUIZ`; a word-anchored `festival` →
  `FESTIVAL`; a party/club keyword (`party`/`club night`/`rave`/`karaoke`/`dj set`) →
  `PARTY`; everything else defaults to `CONCERT` (Gretchen is live-music-leaning). Only the **title** is scanned, never the genre list (a literal `90's Rave`/
  `House` token would otherwise mislabel a concert). Reactive: a party that names itself without a keyword (`AFRO HAUS`, `TESTOSTERONE`, `GIRLS TOWN`) stays
  `CONCERT` until a signal is added → shares the reactive-heuristic limitation tracked in `TODO.md`.
- 🟢 **Artists come from the `.lineup` stages, not the title** (titles are frequently a party/series name). Inline `feat.`/`ft.` credits are split into main +
  guest, a bare
  `DJ-Set` suffix and a `+<tag>` stylisation are stripped, and floor/section headers, credit lines and prose notes are dropped. Residual: a conjunction-joined
  pair on a *single* lineup line (`Prezident & Jay Baez`) is kept as one act — Gretchen lists genuine co-bills on separate `<br>` lines, so a `&` *within* one
  line is as often a duo's name as two acts, and there is no reliable signal to tell them apart.
- 🟢 **The `NN Years GRETCHEN:` anniversary-series banner is stripped from the display title** (`15 Years GRETCHEN: BOTTICELLI BABY` → `BOTTICELLI BABY`); the
  act name is what remains. Other `NN Years <act>` titles (`Recycle: 15 Years FLEXOUT AUDIO`) are left intact — the strip is anchored on "Gretchen".

### Duncker Club (`scraper/duncker/`) — retro hand-coded, single page

- 🟢 **Every night is typed `PARTY`.** Duncker is a resident-DJ dance club with no other event kind, so the type is a constant, not a scraped/inferred signal.
- 🟢 **No structured genre.** The free-text style line ("Rock, Indie, Alternative, Punk") is kept as the `subtitle`, deliberately **not** the genre field, so it
  never seeds bogus genre tags.
- 🟢 **No show time.** The programme lists only an opening-hour range (`21h-05h`); the opening hour is stored as `doorsTime` and there is no separate start time.
- 🟢 **Year inferred from the German weekday** (the `DD.MM.` date omits it), which also resolves the recently-passed events the venue leaves listed — like
  Roadrunner. DJ names are prefixed `DJ`/`Djs` and split on separators; a `DJ <handle>` is **not**
  de-prefixed (shared with Madame Claude's limitation).

### Urban Spree (`scraper/urbanspree/`) — MODX, descending paginated list + detail

Verified against a full live import (45 upcoming events, July 2026 seed): description, poster, start time and promoter on 45/45; ticket link and presale price
on 44/45 (the one gap is a free-entry night with no shop).

- 🟢 **The headliner is derived from the title, which the venue writes free-form.** There is no structured artist anywhere on the site — across a 45-event seed
  the detail pages expose only `Address` / `Promoter` / `Date` / `FB Event` / `Website` rows, no JSON-LD, no `og:` tags, and the ticket link points at 15
  different shops of which exactly one carried an artist path. So the title is the only source, and the shared tail rules in `stripArtistSuffix` do the cleanup:
  a four-digit-year tour tail (`Jawdropped - USA UK EU FALL 2026`), a `- Releaseshow` tail (`Sinem - Hatun - Releaseshow`), and a shouted tour/album tail
  (`Tigercub - NETS TO CATCH THE WIND`). A tour tail that is neither shouted nor keyword-marked would still leak.
- 🟢 **A label leading the title is suppressed by a curated entry, not a rule.** `aufnahme + wiedergabe - Fünfzehn Jahre + Zweiter Akt` is the label's own
  fifteen-year night with no performer in the title, handled by a `NON_ARTIST_TITLE_LEADS` entry. It has to be curated: deriving it from the promoter field is
  **not** viable, because Urban Spree's promoter is frequently the band itself (`WISBORG`, `Pure Obsessions & Red Nights` both promote their own shows), so a
  "title starts with its promoter" rule would delete correct artists. New labels that title their own events need an entry as they surface.
- 🟢 **Country/genre tags stay attached to act names** — `ANEMONE (NL)`, `NIGHT NAIL (Dark Wave US/DE)`. The shared `stripArtistSuffix` only strips *format*
  annotations in parentheses (`(DJ-Set)`, `(Live)`); a parenthesised alias is deliberately kept (`Sickboyrari (Black Kray)`), and the two are not
  distinguishable without a country/genre vocabulary.
- 🟢 **No doors time, genre, or box-office price.** None are structured on the page. An `Einlass: HH:MM` line appears in *some* descriptions, but the description
  is free-form pasted copy where such a line may belong to another show mentioned in the blurb, so it is deliberately not parsed.
- 🟢 **Pagination is bounded at 20 pages.** The listing is sorted descending across a 200+-page archive, so the importer walks `?page=N` until a page reaches the
  past. If the venue ever books beyond ~180 upcoming events, or changes the sort order, the cap truncates the import — this is logged as a warning.

### arkaoda (`scraper/arkaoda/`) — hand-coded PHP, list + detail

Assessed against an 82-event walk of the venue's `?/default/detail/id=<n>` range (ids 1240–1321, roughly March–July 2026): every event has a date, a title and a
flyer; 27 of the 82 carry the `Konser` category and 55 carry none at all.

- 🔴 **No times, prices, sold-out state or genre — for any event.** The venue has no structured field for them anywhere in its markup. Where it mentions them at
  all they sit in free prose ("€10 Entry on the door", "Live set at 22:00, DJs until 6:00", "tickets at the door"), with no delimiter and no guarantee the value
  belongs to the event rather than a record being plugged, so they are deliberately not parsed. Every arkaoda event therefore lands with null times and prices.
  The one prose value that *is* extracted is a `Tickets:`-labelled shop link (in practice Resident Advisor) — a URL is unambiguous once labelled.
- 🟠 **Artists are derived from the title, and the rule drops as many as it keeps.** There is no lineup markup, no JSON-LD and no `og:` performer field, so the
  title is the only source — but arkaoda titles are dominated by series, label and collaboration names rather than billings. `arkaodaArtists` therefore refuses
  any title that still reads as a compound event label after the promoter/series framing is stripped (a spaced dash, an ` x ` collaboration marker, or a
  `release`/`takeover`/`fundraiser`/`market` word). That is the intended trade — it keeps `Remise Takeover` out of the artist table at the cost of losing the
  real acts in `Grumpy Pieces release; Harmonious Thelonious (Live) + Saeko Killy (Live)` and `Brokenchord X Luna Vega`. Across the 27 `Konser` events the rule
  yields artists for roughly half.
- 🟢 **An unlabelled night is typed by title keyword or `OTHER`, never `CONCERT`.** `Konser` is the only category the site emits, so its absence is the signal
  that the night is not a plain gig — but it does not say *what* it is. A club night whose title carries no `party`/`rave`/`clubnight` cue (`MNJM`, `FOAM`,
  `Bar Night: Bent (DJ)`, `Cover`) lands as `OTHER` rather than `PARTY`. Defaulting these to `CONCERT` was rejected: it would also mint each event name as a
  headliner.
- 🟢 **A single-act title cannot be told from a series name.** With no dash, ` x ` or event word to reject on, a one-line title is taken as the act — correct for
  `Juana Aguirre` and `ddwy`, wrong for `Italian Dance Wave` or `Radiant Reciprocity`, which are programme names. This is the same reactive-denylist residue as
  the cross-cutting entry above.
- 🟢 **Country tags survive on some act names.** A trailing all-caps code group is stripped (`Marta Warelis (PL/USA)` → `Marta Warelis`), but a spelled-out or
  mixed form is not (`Apichat Pakwan (Thailand- Live)`) — the same limitation as Urban Spree, for the same reason.
- 🟢 **Conditional requests never fire.** The server sends neither ETag nor Last-Modified and answers `Cache-Control: no-store, no-cache`, so every run is a full
  fetch of the listing plus one detail page per event. This is cheap only because the listing shows *upcoming* events alone and is usually a handful of blocks;
  a season-long listing on this platform would need reconsidering.

### ÆDEN (`scraper/aeden/`) — WordPress, entry page → month pages

Assessed against the July–October 2026 month pages (16 nights). Every night has a date, a start time, a poster and a ticket link; 15 of the 16 carry a genre and
12 name their DJs.

- 🟠 **No prices and no doors time — for any event.** The month page has no field for either and the venue does not mention them in the blurb; tickets are sold
  on Resident Advisor / Weeztix, so the price only exists behind the ticket link. Every ÆDEN event therefore lands with null prices and a start time only.
- 🟠 **DJs are read only from a `Lineup:` paragraph.** The lineup block is prose, and the roster is recognised only when the venue opens a paragraph with
  `Lineup:` and puts one act per `<br>` line — the format it uses consistently today. A night that announces its acts inside the blurb text instead lands with
  no artists, and a roster still being built (`Lineup: TBA soon..`, `More TBA soon…`) yields none by design rather than storing the placeholder.
- 🟢 **Every night is typed `PARTY`.** ÆDEN is a techno club whose programme is DJ nights, and the site emits no category at all, so the type is hardcoded like
  AMT's. The occasional live-music night (`Bleach Berlin with Deer park & Patch`, a shoegaze bill) is therefore filed as `PARTY` rather than `CONCERT`. Typing
  from the genre field was rejected: the genres are dance styles (`Techno`, `Trance`) on both kinds of night.
- 🟢 **`sourceId` is date + slugified title, not a URL.** The month page links no per-event page (the `/aeden/<slug>/` posts exist but are not linked from the
  programme), so there is no canonical per-event path to key on. A night that is renamed *and* moved would therefore be imported as a second event; a rename
  alone keeps the date, and a date change alone keeps the title, so a full identity change is the only collision case.
- 🟢 **Only the club programme is imported.** The site has two further post types for its other spaces (`aeve`, `oel` — the bar/kitchen programme, e.g. "Taco
  Wednesday", "Afterwork Sunset Garten"). Neither appears on the `/month/` pages, which render club nights only, so they are out of scope.
- 🟢 **Conditional requests never fire.** Like AMT, the entry page's ETag changes only when a month is added, not when a night inside a month is edited, so
  caching is deliberately disabled and every run re-fetches the entry plus one page per listed month (4 pages today).

### Club der Visionäre / Sonnenraum / MS Hoppetosse (`scraper/clubdervisionaere/`) — WordPress, one listing for three rooms

Verified against a live import (August 2026): 16 upcoming club nights and 3 Sonnenraum nights, all with a date, a title and a lineup; the boat imported 0 (see
the seasonality note below). All three rooms share one page and one parser, filtered by the colour class on the title.

- 🔴 **No times, prices, tickets, images, genre or description — for any event, in any of the three rooms.** The programme page carries a date, a title and a
  lineup and nothing else; there are no per-event pages to enrich from (the WordPress posts have permalinks but the programme never links them), and the
  `ra.co` links inside the lineup are DJ *profiles*, not ticket shops. Every event therefore lands with those fields null. The only time the page ever prints is
  a per-act set time (`// The Omniversal Earkestra LIVE from 21:00`), which is deliberately **not** stored as the event's start time — it is when that act
  plays, not when the night opens.
- 🟠 **The room is a CSS class, so a theme restyle silently empties a source.** `.cdvRed` / `.sonnenraumYellow` / `.hoppetosseYellow` are the only signal
  separating three venues on one page — there is no per-room page, category or data attribute, and the WordPress REST API exposes no room either. If the theme
  renames a class, that room's importer starts returning zero events rather than failing, which is indistinguishable from the off-season case below.
- 🟠 **Zero events is a normal result, and the venue is seasonal.** The open-air club runs in summer and the boat in winter, so at any time of year one of the
  two legitimately has no nights on the page (the August import above returned 0 for MS Hoppetosse; the January 2026 snapshot in the tests has 0 for Club der
  Visionäre). A genuinely broken selector therefore looks exactly like a closed season — the winter fixture test is what distinguishes them.
- 🟠 **The date of a shared day is inherited, not stated.** The theme prints the date only on the first block of a given day, so a second night on the same date
  (a boat party and the club afterparty, or the Monday Sonnenraum residency beside a club night) carries an empty date cell and takes the preceding block's
  date. This is inferred from the page's chronological ordering; a block whose date cell is empty *and* has nothing dated before it is skipped rather than
  guessed at.
- 🟠 **Every night is typed `PARTY`.** The site emits no category, and the programme is DJ nights across all three rooms. The Sonnenraum's Monday live-band
  residency is therefore filed as `PARTY` rather than `CONCERT`; the act itself is still billed `HEADLINER` off the venue's own `LIVE` marker.
- 🟢 **A lineup line is one act unless it splits unambiguously.** Acts are split at a `b2b` marker and at `&`/`and`/`und` boundaries, but never inside
  parentheses — `Los Refrescos (Dandy Jack & Argenis Brito)` is one act billed with its members and `Naima (2)` is a Resident Advisor disambiguator, both kept
  whole. The trade-off cuts both ways: a duo written without brackets is split into two artists (`Foehn & Jerome`, `Alex & Laetitia Katapult` → `Alex` +
  `Laetitia Katapult`), and a live-project suffix stays attached (`Gwenan (Phase Space Live)` is stored beside plain `Gwenan` as a second artist row, because
  only a format-word parenthetical like `(Live)` is stripped — the same limitation as Urban Spree's country tags).
- 🟢 **An act billed twice on one night is kept once.** A live-band member who also plays a DJ set later in the evening (Remain In Love) would otherwise produce
  two `event_artist` rows for the same pair and hit its unique constraint; the first billing wins, keeping the earlier position and its role.
- 🟢 **The WordPress REST API is unusable for this venue.** Upcoming events are `future`-status posts, which `/wp-json/wp/v2/posts` omits entirely (and 401s per
  id), so the rendered page is the only source despite a JSON API being present — the reverse of the usual ADR-007 preference.
- 🟢 **Conditional requests never fire.** The server sends neither ETag nor Last-Modified, so every run is a full fetch of the one listing. That is cheap: one
  page per room, and the page is a single short programme.

### Columbia Theater (`scraper/columbiatheater/`) — WordPress, homepage listing + detail pages

Verified against a live import (August 2026): 98 upcoming events, dates 2026-08-03 → 2027-11-14, none in the past. Coverage is otherwise strong — every event
has an image and an artist, 97 of 98 have doors *and* start times, 92 have a ticket link and 89 a description.

- 🟠 **No prices, no genre and no sold-out state — for any event.** The venue publishes none of the three anywhere: the detail page's ticket box holds only the
  shop button and a standing "oder bei anderen Vorverkaufsstellen" line, with no price phase, no `Ausverkauft` badge and no style tag. All 98 events therefore
  land with null prices, null genre and `sold_out = false`, and the price only exists behind the Eventim link.
- 🟠 **A rescheduled show stays `POSTPONED` at its new date.** The venue keeps flagging a moved show "Verschoben / Rescheduled" (`data-p`) indefinitely, even
  once the replacement date is confirmed and ticketed — 6 of the 98 events. The stored date is the new one, so the status reads as a *history* marker rather
  than "no date yet"; the venue publishes no signal for "rescheduled and now settled".
- 🟢 **The date comes from the permalink, so a slug without one is dropped.** Both rendered date blocks (`03` / `Aug` on the card, `So. 16.08.` in the header)
  omit the year unless the event is more than a season away, so the `YYYYMMDD` prefix of `/event/YYYYMMDD-<slug>/` is the only unambiguous date — all 98 current
  events carry it, as does every entry of the venue's whole sitemap archive. An event published without that prefix resolves no date and is dropped with a
  warning rather than having its year guessed.
- 🟢 **Only the first URL of a doubled ticket `href` is kept.** The CMS occasionally emits two shop links concatenated into one attribute
  (`…&utm_medium=dphttps://www.eventim.de/…`, 2 of 15 pages sampled). Everything from the second `http(s)://` on is dropped, which would also truncate a
  legitimate href carrying an un-encoded redirect URL — none has been seen.
- 🟢 **Media presenters are stored as promoters; the actual local promoter is not.** The `präsentiert von …` credit names magazines and radio stations (`DIFFUS`,
  `MusikBlog`, `FluxFM`), which are stored per the `Promoter` model's "promoter *or presenter*" definition — the same choice as Astra/Lido. The real booking
  agency appears only as a bare "Örtlicher Veranstalter" **link** (`trinitymusic.de`, `semmel.de`), with no name to store, so it is skipped rather than guessed
  from the domain.
- 🟢 **A billing row's acts are split on `+` only, never on commas.** The venue writes a guest's band affiliations in parentheses
  (`Budgie (SIOUXSIE & THE BANSHEES, THE SLITS)`), so the shared comma-splitting of `splitSupportActs` is deliberately not used and a genuinely comma-separated
  support line would land as one artist. No such line exists today — every row uses `+`.
- 🟢 **Conditional requests never fire.** The server sends neither ETag nor Last-Modified, so every run is a full fetch of the homepage plus one detail page per
  listed event (98 today, serialised by the 200 ms per-host throttle).
- 🟢 **`ELLE & L's Festival` mints a spurious `Elle` headliner.** The title is promoted to `FESTIVAL` at the persistence boundary, but the lineup was already
  derived from the scraper's own `CONCERT` inference, so the `&`-split leaves `Elle` behind (`L's Festival` is filtered out). Pre-existing and cross-cutting —
  Clash and Gretchen have the same shape — → tracked in `TODO.md`.

### Columbiahalle (`scraper/columbiahalle/`) — Contao, single page

Verified against a live import (August 2026): 87 upcoming events, dates 2026-08-07 → 2030-12-28, none in the past. The richest listing of any source so far —
every event has a date, **both** doors and start times, an image and a promoter; 85 of 87 carry a price and 85 an artist.

- 🟠 **No genre for any event.** The Contao event template has no style/category field at all, and the venue never states one in the blurb. All 87 events land
  with a null genre, and their type is inferred from the title alone (concert hall → `CONCERT` unless a title keyword says otherwise, which is how the two
  afterparty nights become `PARTY`).
- 🟢 **A card carries no month or year of its own.** It states only a weekday and a day of month ("Freitag", "07"); the month comes from the
  `.eventlist_monat` heading preceding it in document order. A heading the parser cannot read therefore voids the month rather than carrying the previous one
  forward — those events are skipped with a warning instead of being filed a month early, which is the safer failure but still a silent loss.
- 🟢 **No per-event page, so identity is the Contao event id.** The "Kalender-Eintrag" links (`veranstaltung/<alias>.html`) serve an **iCal download**, not an
  HTML page, and carry strictly less than the listing (no prices, promoter, tickets or sold-out state), so they are not followed. Both `sourceId` and
  `sourceUrl` come from the `div.event_inhalt[id=event_<n>]` id — the same key the venue's own iCal uses as its `UID` — with the listing anchor
  (`…/veranstaltungen.html#event_9743`) as the URL. An event re-created in the CMS under a new id would import as a second event.
- 🟢 **A tiered price is stored as its lowest tier.** `VVK: ab 74,99 €` yields `pricePresale = 74.99`, with the raw text kept as the `priceNote` so the
  "from" qualifier is not lost — a filter on price sees the cheapest ticket, which is the useful reading, but the number alone understates what most seats cost.
  The same applies to the venue's "zzgl. Gebühr" (plus booking fee) footnote, which is likewise only in the note.
- 🟢 **25 events have no ticket link and 2 no price at all.** The venue simply omits the button on those cards (typically shows sold elsewhere or already sold
  out); nothing is parseable that the parser is missing.
- 🟢 **`OFF DAYS 2026` is minted as a headliner.** It is an event name, not an act, but it matches no structural non-artist filter (no `fest`/`festival`
  marker) — the reactive-denylist limitation recorded in the cross-cutting section. `Elle & L's Festival` mints `Elle` for the separate, cross-cutting reason
  tracked in `TODO.md`.
- 🟢 **Conditional requests never fire.** The server sends neither ETag nor Last-Modified (its Contao page cache answers with `contao-cache: fresh` and an
  `age` header instead), so every run is a full fetch — of exactly one page, which is cheap.

### Golden Gate (`scraper/goldengate/`) — WordPress / Elementor, single page

Verified against a live import (August 2026): the homepage announced three nights, of which **one** was still upcoming and stored. Everything the venue does
publish is captured — date, start time, night name and full DJ roster — but that is nearly all it publishes.

- 🔴 **Only the current Thursday–Saturday block is announced — at most three nights, often one.** The venue keeps no forward calendar: the homepage carries
  exactly the current week's Thu/Fri/Sat and nothing beyond. Past nights stay up until the block rolls over and are dropped at persistence time
  (`EventUpsertService`), so an import late in the week legitimately stores a single event, and a run after the weekend before the page is updated can store
  none. Zero events is therefore **not** proof of a broken selector here — the fixture test is what distinguishes the two.
- 🟠 **No prices, tickets, genre, description or images — for any night.** The venue sells at the door only (the page says so once, as a standalone page-level
  heading rather than per event, so it is not stored as a `priceNote`), links no ticket shop, and publishes no per-event page. The single image in each night's
  container is a decorative flame divider shared by all three, so no `imageUrl` is taken rather than storing the same GIF on every event.
- 🟠 **Every night is typed `PARTY`.** Golden Gate is a techno club whose whole programme is DJ nights and which emits no category at all, so the type is fixed
  like ÆDEN's and AMT's rather than inferred from the night's name (`Klubnacht`, `Donnerdogge` — none of which is an artist).
- 🟢 **The parser anchors on heading *content*, because Elementor leaves nothing else.** Every element is named with a per-element hash
  (`elementor-element-7b3297a`) that changes whenever the page is edited, and the theme adds no venue-semantic classes — so a night is recognised by its heading
  matching the German date pattern, with the next two headings taken as its title and lineup. A restyle that changes the *date wording* (not just the markup)
  would empty the source; a restyle that only re-hashes the elements will not.
- 🟢 **`sourceId` is date + slugified title, not a URL.** The venue publishes no per-event page, so a night renamed on the same date imports as a new event and
  the old row is removed by the stale-event cleanup (it falls inside the scraped date range). Two nights sharing a title on different dates stay distinct.
- 🟢 **A back-to-back billing is split into two DJs.** `Nyna Curtis & Kisling` becomes two artists — the same accepted trade-off as Club der Visionäre, since
  nothing in the markup distinguishes a b2b pair from a duo whose name contains an `&`.

### Heimathafen Neukölln (`scraper/heimathafen/`) — WordPress REST + ACF, JSON source

Verified against a live import (August 2026): 95 upcoming events, dates 2026-09-01 → 2027-09-22, none in the past. The best-covered source so far — every event
has a date, start time, image, description and a price; 93 of 95 have a doors time and 90 a ticket link.

- 🟠 **No genre for any event.** The venue *does* tag its events, but the `events_tag` vocabulary is 560 terms mixing real genres (Soul, Cumbia, Folktronica)
  with formats and access notes (Konzert, Premiere, Live Podcast, Buchvorstellung, Gebärdensprache), and the REST payload carries only term **ids** — resolving
  them to names costs six more requests per import for a field that would then need its own stop-list. `class_list` inlines the slugs, but slugs are lossy (`rb`
  for R&B, `gebaerdensprache`), so `genre` is deliberately left null. Resolving the taxonomy once and caching it is what would unblock this.
- 🟠 **Only concerts get artists.** The event type comes from the venue's own category, and `buildArtistsForEventType` mints a headliner from the title only for
  a `CONCERT`. That is correct here — a theatre or reading title (`DIE KLIMA-MONOLOGE`, `LEBEN GROPIUSSTADT GEBRAUCHSANWEISUNG`) names a production, not a
  performer — but it means 65 of 95 events store no artist, and the actual cast, which the venue writes into the prose blurb and an unused `acf.event_cast`
  field, is not extracted.
- 🟢 **One post expands into many dated events, keyed by date *and* time.** `acf.event_performances` is an array (a run reaches 30 entries), so `sourceId` is
  `<postId>-<date>-<HHmm>`. The time is part of the key because a run legitimately plays twice on one day; the cost is that correcting a start time re-keys that
  performance, creating a new row while the stale-event cleanup removes the old one.
- 🟢 **Concession tiers never reach the price columns.** The venue prices by audience and labels its social tiers with the sales channel too — `Mit Berlin-Pass
  (Abendkasse)` is €3 and `Für Geflüchtete (Abendkasse)` is €0. Matching `Abendkasse` anywhere in the label stored €3 as *the* door price during the first smoke
  test (and the €0 tier would have marked the event free), so both label matches are start-anchored and an explicit concession list — including the
  pay-it-forward `ZUGABE TICKET`, which is priced *above* general admission — is excluded first. Every tier survives verbatim in the `priceNote`.
- 🟢 **The room is parsed away.** Performance notes read `Einlass ab 18:45 Uhr (Studio)`, naming the venue's two spaces (Saal / Studio). Only the doors time is
  read; the model has no event-level room field (`ScrapedArtist.stage` is per-artist), so which space a show plays in is lost.
- 🟢 **Paging a mutable ordered list can miss an event.** The endpoint returns the whole archive ordered by *post* date, and the event date is an ACF field
  WordPress cannot filter or sort on, so all ~5 pages are walked. If a post is edited mid-walk it shifts position and can be skipped — capturing the five pages
  minutes apart during development yielded 96 upcoming performances against the live import's 95, for exactly this reason. Harmless in practice: the walk takes
  about a second, the import runs daily, and upserts are idempotent by `sourceId`.
- 🟢 **A promoter is read only from one phrasing.** `acf.event_organiser` is free prose: `"Eine Veranstaltung von X"` names a promoter, but `"Eine Veranstaltung
  des Heimathafen Neukölln in Kooperation mit …"` credits the venue itself and `"Heimathafen Neukölln mit Sophia Keßen und Margret Schütz"` names *performers*.
  Only the first form is read, so most events store no promoter rather than minting partners and performers as one.
- 🟢 **Conditional requests are unused.** The REST endpoint sends no ETag or Last-Modified, so every run re-walks the archive; upserts are idempotent by
  `sourceId`.

### Huxleys Neue Welt (`scraper/huxleys/`) — WordPress / Events-Manager, list + detail

Verified against a live import (August 2026): 107 upcoming events, dates 2026-08-02 → 2028-05-19, none in the past. Coverage is close to complete — **every**
event has a date, a start time *and* a doors time; 106 have an image and an artist, 103 a ticket link, 94 a description, 59 a genre.

- 🟠 **Prices are almost never published.** 105 of 107 events state none: the venue sells through Eventim and prints a price only occasionally, as a labelled
  line in the detail page's `Details` box ("VVK: 28 € (zzgl. Gebühr)" — one of eleven sampled pages). That line is parsed when present, with the booking-fee
  qualifier kept as the note, but for almost every show the price exists only behind the ticket link.
- 🟠 **A relocated show stays on the listing after moving to another house.** Seven events are flagged `RELOCATED`, and the venue uses that note for moves in
  *both* directions — "Das Konzert wird ins Hole44 verlegt" (leaving Huxleys) and "Die Show wird aus dem Metropol ins Huxleys verlegt" (arriving). The former
  are stored as Huxleys events with a `RELOCATED` status, which is what the source says, but they will happen elsewhere — and if the receiving venue also has an
  importer, the same show lands twice under two venues. The note text is not stored, so the destination is lost.
- 🟢 **The change note is the only signal for two statuses.** Sold-out and cancelled come from a CSS class on the list item (`Ausverkauft` / `Abgesagt`) with a
  matching badge, but a relocation or a new date is announced solely in the listing's free-text `.anderungen` note, which the detail page omits entirely — so
  the merge deliberately keeps the *overview's* status whenever it is non-default. Notes that are not status changes ("Zusatzshow", "Nachholtermin", "Eintritt
  ab 18 Jahren!") correctly leave the status alone, but only because none of them contains a status keyword; a future note that does would be misread.
- 🟢 **The detail page has no heading, so the listing owns the title.** The act's name appears there only in the document title with " - Huxleys Neue Welt"
  appended. The detail scraper strips that suffix so it can stand alone when the listing is unavailable, but a successful merge keeps the listing's
  `.eventname`.
- 🟢 **Genre and promoter are read from CSS classes.** Both are WordPress taxonomies the theme emits as slugs on the `article` element (`event-tags-electronic`,
  `promoters-trinity-music`), which costs no extra request but means de-slugified display names: a legal form comes back title-cased word by word
  (`Concert Concept Veranstaltungs Gmbh`) and a stylised genre loses its punctuation (`kpop` → `Kpop`, not `K-Pop`). The sibling `presenters-*` taxonomy (media
  partners) is deliberately not read as a promoter — de-slugifying a domain-shaped term yields a mangled `Laut De`.
- 🟢 **`Corrupted Blood Club Show` is typed `PARTY` and loses its headliner.** The shared title classifier treats `club` as a party keyword, so this one show (of
    107) is mistyped and, being a party, has no artist derived from its title. The cross-cutting reactive-keyword limitation, not specific to this venue.

### Kater (`scraper/kater/`) — WordPress, single page

Verified against a live import (August 2026): 26 upcoming events, dates 2026-08-01 → 2026-11-20, none in the past. Every event has a date and a start time; the
ten structured club nights carry a full per-floor lineup.

- 🔴 **No prices and no images — for any event.** The homepage carries neither, the `/event/<slug>` pages render only a heading, and the REST route returns an
  empty `acf` object, so there is nowhere else to look. Ticket links (Resident Advisor) are present for 19 of 26 events and are the only route to a price.
- 🟠 **Artists are extracted only from a marked lineup.** The venue draws a `____________` rule above each floor name, and only lines beneath such a rule are
  read as acts — ten of twenty-six nights. The remaining sixteen summaries are prose (a garden evening's blurb, a film synopsis, a residency's "every tuesday *
  18:00 – 01:00 *" schedule notes) and yield **no** artists at all. That is deliberate: without the rule there is no structural signal separating a DJ name from
  a schedule note, and reading every line would mint "free entry till 20:00" and a film plot as performers. The cost is that a genuinely billed act mentioned
  only in prose (e.g. the garden evenings' "with Nat Gohl") is missed.
- 🟠 **`Nomadenkino` is typed `PARTY`.** The venue emits no category, so every night defaults to a party unless a title keyword says otherwise — and the shared
  screening keyword is word-anchored (`\bkino\b`), which a German compound like *Nomadenkino* does not match. The anchor is deliberate (it protects real act
  names such as "Alkinoos Ioannidis"), so this monthly film night stays mistyped rather than loosening it.
- 🟢 **Only the start of the opening time span is stored.** Summaries read `Sa. 01.08 22:00 — So. 02.08 10:00`; the model has no end-time field, so the closing
  half — usually the following morning, and for the long weekend parties two days later — is dropped.
- 🟢 **The `by <presenter>` tail is stripped from a floor name.** `ACID BOGEN by GOOEY` and plain `ACID BOGEN` are the same floor on different nights, so the
  tail is removed to make `stage` group across the programme; which collective curated that floor is consequently not stored.
- 🟢 **A parenthesised act is never split.** `Double Penetration (FLOWWW b2b Joe Cleen)` is one billing whose brackets hold its members, so a line containing
  `(` is left whole while an unbracketed `X b2b Y` splits into two DJs — the reverse order to Club der Visionäre's split, which would break this name.
- 🟢 **Free entry is read from the blurb, and only when unqualified.** A summary saying "Free entry summer evenings" marks the event free; the Tuesday
  residency's "free entry till 20:00" deliberately does **not**, since entry is free for two hours rather than all night. The shared
  [detectFree][de.norm.events.scraper.detectFree] is not used here because its bare `free` token would trip on any mention of free drinks.
- 🟢 **Dates carry no year.** Every date is a `DD.MM` with a weekday, so the year comes from [inferYearForWeekday]; the programme currently spans August to
  November without printing a year anywhere.

### Max-Schmeling-Halle / Velodrom / UFO im Velodrom (`scraper/velomax/`) — TYPO3, one listing for three halls

Verified against a live import (August 2026): 17 + 22 + 10 events across the three halls, none in the past. The best-structured source in the project — every
event has a date, a start time, an image and a promoter, and the detail pages carry schema.org Microdata rather than needing CSS selectors.

- 🔴 **Sport is not imported at all.** The halls' biggest strand is handball, volleyball and basketball — 32 of the listing's 85 current entries, 25 of them in
  the Max-Schmeling-Halle — and the model has no `SPORT` event type. Those entries are skipped rather than filed as `OTHER`, which would bury the concerts among
  them. So this venue's imported count is *by design* far below what its programme page shows, and a user looking for a Füchse Berlin fixture will not find it.
- 🟠 **A show that plays twice in one day keeps only its first session.** The stored event slug is date + venue + title and the column is `UNIQUE`, so a second
  same-day session cannot be inserted — the run would fail the entire import with a duplicate-key error. "Disney On Ice" plays three sessions on 13 March and
  "Berlin Tattoo" two on 7 November; each day keeps its earliest, and the later sessions are dropped with a log line. The venue itself distinguishes them
  (sometimes by permalink, sometimes only by start time), so the loss is ours, not the source's → tracked in `TODO.md`.
- 🟢 **No prices for any event.** The halls sell exclusively through Eventim and print no price on either page; the ticket link is the only route to one.
- 🟢 **No genre for any event.** The listing's only classification is the three-way `data-type` that decides concert / show / sport.
- 🟢 **The listing's event type outranks the detail page.** Concert-vs-show is stated *only* as `data-type` on the listing entry; the Microdata says nothing
  about it, so the merge keeps the listing's value rather than the detail page's title-based inference.
- 🟢 **A hall's own configuration is trusted over its slug.** "UFO im Velodrom" is a smaller setup inside the Velodrom, and one event's permalink reads
  `…-ufo-im-velodrom-…` while the listing labels it `Velodrom`. The listing's label wins, so that show is imported as a Velodrom event.
- 🟢 **Conditional requests cover the shared page only.** All three sources fetch the same `velomax.de/events` URL, so each carries its own ETag for the same
  document; a change anywhere on the page re-runs all three. That is cheap and correct, just redundant.

### Renate (`scraper/renate/`) — WordPress, single page

Verified against a live import (August 2026): 13 upcoming nights, none in the past, every one with a ticket link and 11 of 13 with a full per-floor lineup — 84
DJs across the venue's GARDEN, GREEN, BLACK, RED and SECRET floors.

- 🔴 **No times, prices, images or descriptions — for any night.** The programme prints a date and a title and nothing else structured; door times appear only
  inside floor headings ("GREEN (from 22:00)") and are per floor rather than per event, so no `startTime` is stored. The prose that does exist is club policy
  repeated verbatim on every night (cashless, safer-space, awareness team), so no description is stored either — storing it would put four identical paragraphs
  on all 13 events.
- 🟠 **Artists are extracted only beneath a recognised floor heading.** The venue reuses `<strong>` for its slogan (`Garten für alle!`), for host credits
  (`hosted by Neer`) and for festival blurbs, so a heading opens a floor only when it *starts with a floor name* — `GARDEN`, `GREEN`, `BLACK`, `RED`, `SECRET`,
  `TOP SECRET`. Note the German `GARTEN` is deliberately **not** a floor name: it only ever appears in the slogan, the floor itself being spelled `GARDEN`. Two
  nights whose text is pure prose (a festival, a themed party) therefore yield no artists at all.
- 🟠 **An act line is rejected unless it looks like a name.** The same paragraph run carries a workshop timetable, policy sentences and `+ more tba`
  placeholders alongside the DJs, none of them marked up differently — so a line is taken only when it is at most six words, carries no clock time, is not a
  `hosted by …` credit and is not a placeholder. A genuinely wordy billing is dropped rather than mangled.
- 🟢 **An act billed on two floors is kept once.** `event_artist` is `UNIQUE (event_id, artist_id)`, so a label hosting both the garden and the green floor
  (Remoto Records) would produce two rows for one pair and fail the *entire* import — the first billing wins and keeps its floor. The same constraint Club der
  Visionäre documents.
- 🟢 **The markup is not consistent between nights.** Most put each floor heading and act in its own paragraph; some pack a whole night, headings included, into
  one paragraph split by `<br>`. The parser flattens both to one line stream, which is why a floor is switched on any *line* naming a floor rather than on a
  paragraph boundary.
- 🟢 **One night's lineup is mis-split by the venue's own formatting.** On 22 August the source runs a floor heading and its acts together
  (`Red hosted by Dub & Dal mgt Uta …`), so the management abbreviation `mgt` is stored as an act; on 1 August a name is line-broken mid-way
  (`Mr. Sian b2b Aidan` / `Harrison`), yielding two fragments. Nothing in the markup distinguishes these from real names.
- 🟢 **Free entry is left to the shared detector.** Floor headings advertise time-limited offers ("free until 20:00", "FREE til 20:00!") which must not mark a
  night free; only an unqualified `(Free Entry)` in the title does, via the shared `detectFree`.

### Tempodrom (`scraper/tempodrom/`) — schema.org JSON-LD, single page

Verified against a live import (August 2026): 141 events stored from 145 published, dates 2026-08-27 → 2027-12-10, none in the past. The richest structured
source in the project — every event has a date and an image, 140 of 145 have a start *and* a doors time, 86 a price, and 3 arrive already flagged cancelled.

- 🟠 **Four events a year are lost to same-day runs.** A show that plays twice in one day (`Die Unfassbaren`, `Roncalli`, `Flying Mozart`) is published as two
  JSON-LD objects with the same title and date, and the shared `deduplicateScrapedEvents` keys on exactly that pair — so the later session is dropped with a
  warning. This is the same limitation Velomax hits and the reason the event slug needs a start time → tracked in `TODO.md`.
- 🟠 **No genre and no description for any event.** The JSON-LD carries neither; its `description` field holds the tour or edition name ("The Ca$ino Tour",
  "Jungle Vibes Edition"), which is stored as the **subtitle** rather than as a blurb, because that is what it is.
- 🟢 **The Große / Kleine Arena split is not represented.** `location.name` is "Tempodrom Berlin" on all 145 events and the arena appears nowhere else in the
  listing — only in the site navigation — so the house is imported as one venue. The source inventory previously expected two.
- 🟢 **`performer` is ignored.** It is a copy of the event `name` on all 145 events rather than an act, so artists are derived from the title like any other
  concert hall's, and a title that is an event name rather than a performer (`GeoGuessr World Championship`) mints one anyway — the cross-cutting
  reactive-denylist limitation.
- 🟢 **A price range is kept in the note.** `offers` publishes `lowPrice` and `highPrice`, and 68 of the 86 priced events span a range; the low price alone would
  understate what most seats cost, so the range is recorded as `65.00 – 70.75 EUR` alongside it.
- 🟢 **A multi-day run is stored on its opening day.** A congress or esports final publishes a date-only `startDate` with an `endDate` days later; the model has
  no end date, so five such runs are stored as single events with no start time.
- 🟢 **The machine-readable fields need their own parsing.** `startDate` / `doorTime` carry seconds (`2026-09-01T20:30:00`) and `lowPrice` carries no currency
  sign, so the shared `parseIsoTime` and `parsePriceValue` — which expect the `HH:mm` and `… €` a page *renders* — both return null on them. A first pass
  silently imported every event with no time and no price because of it.

### Tresor (`scraper/tresor/`) — WordPress, list + detail

Verified against a live import (August 2026): 30 events stored, dates 2026-08-01 → 2026-09-30, none in the past, 152 artist billings across 149 distinct acts,
every one of them filed on the floor it plays — `Tresor` (91), `Globus` (52) or `Aurora Bar` (9). The 30 permalinks match the live listing exactly.

- 🟠 **Only 6 of 30 events have a start time.** The venue publishes no doors or start time at all; the only clock it gives is the per-artist set slot
  (`23:00-02:00`) on the event page, and it fills that in only for the nearest nights — the rest of the programme is announced with a lineup but no times. The
  night's opening set is taken as the event's start time, so a night whose slots are still blank keeps a null one rather than a guessed 23:00.
- 🟠 **No price, no ticket link, no image and no genre for any event.** The listing and the event pages carry none of them; tickets are sold off-site and the
  poster art is a CSS background rather than an `<img>`. Every event is typed `PARTY`, which is what all 30 are.
- 🟠 **Five nights have no lineup.** The four `Singularity` Mondays and one `Tresor New Faces` are published with an empty lineup block (`event-floors-0`) or a
  `???` placeholder slot. Both are left empty rather than guessed at — the placeholder names nobody.
- 🟢 **A floor label is reduced to the room it names.** The venue brands the label with the night hosted there (`Globus x Black Rave Culture`,
  `Tresor New Faces hosted by Grab The Groove / 23h`), which would mint a new stage per event. A label opening with one of the three real rooms is reduced to
  that room; anything else is kept verbatim, so a genuinely new space still comes through — but the hosting collective is then only visible in the title.
- 🟢 **Set-format notes and host credits are dropped from the lineup.** The venue decorates an act with its format both bracketed (`Ngly [LIVE]`,
  `The Ghost [All Night Long]`) and bare (`Shackleton Live`), and bills the curating collective among the DJs (`hosted by HARD WAX`). The note is stripped so
  one DJ is one artist across nights, and the host credit is dropped because it names a host rather than a performer. Both vocabularies are curated, so a new
  spelling would come through as part of an act name until it is added.
- 🟢 **A `b2b` pair is split into both DJs.** `pschukk b2b Robert We` becomes two artists on the same floor, matching Kater, Renate and Club der Visionäre. An
  act billed on two floors of one night is kept once — `event_artist` is `UNIQUE (event_id, artist_id)` and a second row would fail the whole import.
- 🟢 **The event page repeats the whole programme in its footer**, in the same markup the listing uses, so the detail parser is scoped to the event's own
  `main.main-content`. A first pass without that scope filed the entire month's 152 billings under the one night whose own lineup was empty.
- 🟢 **The standing policy text is cut from the description.** Every event page appends several screens of guest and ticket policy below an underscore rule,
  identical on all 30 nights; only the blurb above the rule is stored, and 4 events have no blurb of their own at all.

### Admiralspalast (`scraper/admiralspalast/`) — Contao, listing + per-production schedules

Verified against a live import (August 2026): 201 performances stored from 100 productions, dates 2026-08-14 → 2027-12-03, none in the past. Every event has a
start time, a poster and a category; 14 are flagged sold out and 2 postponed. The 100 production links match the live listing exactly.

- 🟠 **No prices and no descriptions at all.** The venue publishes neither anywhere — tickets are sold on Eventim, and a production page carries only its
  schedule, the venue's standing admission policy and its address. 185 of 201 performances do carry a per-date Eventim link.
- 🟠 **No doors time.** Only the performance start time is published (`Mo, 19:30`).
- 🟠 **Only concerts get an artist.** The shared `buildArtistsForEventType` mints a headliner from the title for a `CONCERT` and stays silent otherwise, which is
  right for `TV Noir - die emotionale Musik- Talk- und Spieleshow` but loses the performer of a comedy solo show (`Bülent Ceylan - Diktatürk`). The venue bills
  no lineup of its own, so the title is the only candidate — 66 of 201 events have an artist.
- 🟢 **The A–Z listing is discovery only.** Every one of its 100 tiles renders the same `ab DD.MM.YY` run-start date, so the listing cannot date anything; the
  schedule is read from each `/veranstaltung/<slug>.html` page instead. That makes an import one request per production plus one per category — 121 in total,
  spaced by the shared per-host throttle.
- 🟢 **The category costs 20 extra fetches.** `eventkategorie` filter pages are the only place the venue states a category, so each is walked and its productions
  take its label. A production listed under several categories keeps the first in the venue's own alphabetical order — stable between imports, but arbitrary. 11
  distinct categories are in use; `Kultur`, `Diskussion` and `Podcast` name a framing rather than a form and fall through to the theatre's `SHOW`
  default.
- 🟢 **`AUSVERKAUFT` replaces the ticket link rather than accompanying it.** A sold-out performance drops its Tickets button entirely, so the cell text is the
  only signal — a first pass read the missing link as "not ticketed" and left all 14 unflagged.
- 🟢 **The venue's two reschedule notes mean opposite things.** `verschoben auf <date>` sits on the abandoned date (postponed), `verlegt vom <date>` on its
  replacement (going ahead). The shared `parseEventStatus` reads any "verlegt" as `RELOCATED`, which would mark the one certain date as moved, so that idiom is
  checked first. Both notes are also kept as the event's subtitle.
- 🟢 **Poster paths resolve against the site root, not the page.** Contao writes them relative (`assets/images/8/…`) under a page-wide
  `<base href="https://www.admiralspalast.theater/">` — which it emits **unterminated**, so the tag cannot be relied on. Resolving against the production URL
  instead yields `/veranstaltung/assets/…`, which 404s.

### Humboldthain Club (`scraper/humboldthain/`) — Elfsight Event Calendar widget, JSON source

Verified against a live import (August 2026): 10 events stored, 2026-08-01 → 2026-08-25, none in the past, all with a start time and a poster. The rendered
widget's own upcoming list — six one-off nights plus four Tuesdays of the resident night — matches the stored rows one for one, in the same order.

- 🔴 **No artists on the currently-upcoming nights.** The venue marks up a lineup only as `ra.co/dj/<slug>` links inside the HTML description, which it uses on
  roughly a fifth of its nights (15 of 77 in the captured calendar, yielding up to 14 DJs each). The rest write the roster as prose under headings that change
  every night (`Lineup/Musik`, `Line-up Live:`, `❤️‍🔥 POP:`), interleaved with door policy and awareness notes, so nothing is minted from it — the same call as
  Kater's unmarked prose. All ten currently-upcoming nights happen to fall in the unmarked group.
- 🟠 **No prices, doors time, genre or per-event page — for any night.** Prices exist only inside the prose, in as many spellings as there are promoters
  (`Abendkasse - 18€`, `--- 13€ Tickets available at the box office ---`, `12€ Early Bird` / `15€ Normal`), so none is parsed. The widget has no genre field, no
  doors field and no per-event URL, so every event links back to the venue's landing page.
- 🟠 **Every night is typed `PARTY`.** Humboldthain is a techno club whose whole programme is DJ nights, so the type is fixed like Golden Gate's and ÆDEN's. The
  one exception is the venue's own `KONZERT:` title prefix (one night in 77), which types the event `CONCERT` and bills the rest of the title as its headliner.
- 🟠 **Nothing marks a night sold out, cancelled or moved.** The widget has no status field and the venue writes no badge, so every event stays `SCHEDULED`. A
  single archived night mentions "verlegt" in its prose, which is deliberately not read as a status.
- 🟢 **The resident night is a recurrence rule, expanded here.** `OPEN DECKS & TISCHTENNIS` is stored as *one* calendar entry with a weekly repeat rule that the
  widget expands in the browser; reading only `start.date` would import it once at the series' opening date (long past) and lose every upcoming Tuesday. Weekly
  rules are expanded into one event per occurrence over a rolling 26-week horizon, bounded by the rule's own end date or occurrence count — which is why
  `sourceId` is `<widgetId>-<date>` rather than the bare id. Elfsight's monthly `nthDayInMonth` rules are **not** expanded (this venue uses none) and contribute
  their start date alone.
- 🟢 **Conditional requests are deliberately unused.** The Elfsight API does send an ETag, but honouring it would freeze the recurrence horizon — the derived
  occurrences are anchored on "today", so a 304 on an unchanged calendar would stop the window advancing (the same reason Havanna re-fetches every run). Every
  import re-reads the payload and relies on idempotent `sourceId` upserts; a second import of the same calendar stored the same 10 rows.
- 🟢 **The widget's own `eventType` field is not a category and is ignored.** The venue has filled it with weekday/time labels (`Samstag, 14:00`,
  `Donnerstag, 18:00`) rather than event kinds, and they can disagree with the event's own `start.time` — `HUMBI BLEIBT` is labelled `SAMSTAG, 18:00` but
  carries
  `start.time` 17:00, and the structured field is what is stored.

### Neue Zukunft (`scraper/neuezukunft/`) — Elfsight Event Calendar widget, JSON source

- 🟠 **Monthly recurring entries are imported once, at their start date only.** The same Elfsight payload shape as Humboldthain above, but Neue Zukunft's
  recurring entries use Elfsight's *monthly* rules (`repeatPeriod: nthDayInMonth`, and one `custom`/`monthly`) — 4 of 44 entries in the captured fixture,
  including the `Future Bash Reloaded` and `Jazz After Dark` series. `NeueZukunftApiScraper` reads `start.date` alone, so each such series contributes a single
  event and its later occurrences are missing. Unblocking it needs monthly-rule expansion *and* a `sourceId` change (the scraper keys on the bare widget id,
  which cannot distinguish occurrences) — the latter re-mints every Neue Zukunft event → tracked in `TODO.md`.

### LARK (`scraper/lark/`) — WordPress REST + ACF, JSON source

Verified against a live import (August 2026): 20 events stored, 2026-10-08 → 2027-02-27, none in the past, every one with a doors time, a poster, a ticket link
and a headliner. The 20 titles and dates match the venue's rendered `/events/` list exactly, in the same order.

- 🟠 **No start time — only doors.** The venue publishes a single time and labels it `Doors` (its detail page renders "Doors 18:30"), so that is what is stored
  and `start_time` stays empty for every event. The ACF field that *looks* like the answer, `event_doors_time`, is a dead default — `19:00` on 613 of 623 posts
  regardless of the real time, and later than the start on the 18:30 shows — so it is deliberately unread.
- 🟠 **No genre and no prices at all.** `event_music_genre` is empty on all 623 posts, `event_entrance_fee` reads `None` throughout, and `event_price` is filled
  on exactly one. Tickets are sold off-site (Resident Advisor, Eventim, DICE, Loft and several more shops) and the venue quotes no price of its own anywhere.
- 🟢 **The event type is inferred from the title for the current programme.** `acf.event_type` is filled on only 263 of 623 posts — and on none of the 20
  upcoming — so those fall through to the shared live-music-venue inference (default `CONCERT`). The venue's own label is trusted when present, with `Live` →
  concert and `Club` / `Dance` → party.
- 🟢 **The tour tail is stripped before the event is classified.** `LEILA – 20 SOMETHING CLUB TOUR` is a gig, but the shared keyword classifier matches the bare
  `club` in its *tour* name and returned `PARTY` — which, being a party, then discarded the headliner too. Classifying the act rather than the tour fixes it
  here; the shared `PARTY_TITLE_KEYWORDS` entry is the underlying issue → tracked in `TODO.md`.
- 🟢 **Support acts come from the title, not the act repeater.** The ACF schema has six act slots (name, country, genre, type, links and description per act) and
  the venue has filled slot 1 on exactly one of 623 posts, so the lineup is read from the title's own `<act> + <act> (support)` idiom instead. Its other co-bill
  spelling, `w/` (`FEUCHT w/ BELLA, Agua con gas & SENERGI`), is **not** split — the shared splitter does not treat `w/` as a separator and a comma suppresses
  conjunction splitting — so such a title yields one long "artist". None of the currently-upcoming events use it.
- 🟢 **An event-name title is still minted as a headliner.** `Zascha HOT MESS Debut at Lark` becomes an artist of that name: the venue types nothing and bills no
  lineup, so the title is the only candidate. This is the cross-cutting reactive-denylist limitation recorded at the top of this file, not a LARK-specific rule.
- 🟢 **The programme is short and can have gaps.** At capture the venue listed 20 shows but nothing at all between 1 August and 8 October, so a low event count
  is not on its own evidence of a broken importer — the fixture test is what distinguishes the two.

### MAXXIM (`scraper/maxxim/`) — Wix Events warmup JSON, single page

Verified against a live import (1 August 2026): 18 events stored, 2026-08-01 → 2026-08-18, none in the past, every one with a start time, a poster, a
description and a price. All 18 dates and start times match the venue's own rendered strings in the same payload (`1. August 2026`, `22:00`) exactly.

- 🟠 **Only the upcoming ~2.5 weeks are imported.** The Wix Events widget serves a first page and reports `hasMore: true`; loading the rest needs the
  authenticated widget API (the `_api/wix-one-events-server/…` paths 404 for an anonymous client), so the import stops at whatever the page ships — 18 nights at
  capture. The site's `event-pages-sitemap.xml` lists 1762 event pages, but those are overwhelmingly the archive, so it is deliberately not crawled as a
  workaround. This is the cross-cutting first-page-only limitation recorded at the top of this file.
- 🟢 **No doors time, genre, ticket URL or lineup — the venue publishes none of them.** The payload carries one time per night (stored as `start_time`), no
  category and no genre field, and tickets are sold on the Wix event page itself rather than an external shop, so `ticket_url` stays empty and `source_url`
  already points at the checkout. A live guest is named only inside the free-text title (`Queens Night - SHERY M live`), which is too unreliable to split, so no
  artists are derived.
- 🟢 **Every night is typed `PARTY`.** MAXXIM is a club open nightly with a DJ programme and exposes no per-event category, so the type is set by the importer
  rather than read from the source. Correct for the current programme; a one-off concert would be mistyped.
- 🟢 **Cancellation, sold-out and tiered-price handling is fixture-only so far.** The live programme showed none of them at capture (`status: 0`, `soldOut:
  false`, one price tier throughout), so the branches are covered by the hand-crafted `maxxim-overview-edge-cases.html` variant rather than by observed data.
  The numeric `status` mapping follows Wix's documented enum (`3` = canceled); an unexpected value degrades to `SCHEDULED` rather than mislabelling a night.

### Metropol (`scraper/metropol/`) — WordPress / Events-Manager, list + detail

Verified against a live import (1 August 2026): 41 events stored, 2026-08-04 → 2027-03-14, none in the past, 0 suspicious rows. The count, the titles and the
dates match the venue's `/events` listing exactly, in the same order, and the statuses reproduce the three the page marks by hand (1 cancelled, 2 relocated).

- 🔴 **A show relocated *out of* the house is still imported, under Metropol.** The venue keeps a moved show on its listing with a `"Verlegt ins <venue> –"`
  title prefix; the prefix is stripped and the event stored with status `RELOCATED` (2 of 41 at capture — BRKN, now at Bi Nuu, and Kitty, Daisy & Lewis, now at
  Huxleys). Both of those houses are themselves imported, so the show exists twice: once correctly under its real venue and once under Metropol with the
  relocated flag. This follows the Mikropol precedent — an event's venue comes from its `event_source` row, so a per-event venue cannot be resolved (the same
  model limitation that defers the promoter sources in `EVENT_DATA_SOURCES.md`).
- 🟠 **The relocation prose is deliberately not read as a status.** A show that moved *into* Metropol carries the same "verlegt" wording in its
  `.changes` / `.alert-blue` note ("Die Show wird vom Gretchen ins Metropol verlegt") but does take place here, so only the `.attention` / `.alert-red` badge
  and the title prefix set the status. The consequence is that a relocation the venue announces *only* in that prose, with no badge and no title prefix, is
  stored as
  `SCHEDULED`.
- 🟠 **No prices and no sold-out state anywhere on the site.** Neither the listing nor the detail pages quote a price or mark a show as sold out; tickets are
  sold off-site (Eventim for most, the promoter's own shop for the party). `price_presale`, `price_box_office` and `sold_out` are therefore empty for all 41
  events, and a sold-out show is indistinguishable from an available one.
- 🟠 **No genre on any event.** The venue types events only as `Konzert` / `Party` (mapped to `CONCERT` / `PARTY`) and publishes no genre field, so the genre-tag
  filters never see a Metropol event.
- 🟢 **The venue transposes its own doors/start labels on some shows.** Two of 41 read `Einlass: 20:00 // Beginn: 19:00`; the scrapers report both times verbatim
  and the shared `orderDoorsBeforeStart` guard swaps them at the persistence boundary, exactly as it does for SO36.
- 🟢 **An unset start time is written as `0:00` and dropped.** Shadow of Intent lists `Einlass: 18:00 // Beginn: 0:00`; storing that as midnight would be worse
  than storing nothing, because `orderDoorsBeforeStart` would then read the 18:00 doors as "later than the start" and swap the two, inventing an 18:00 start.
  The event keeps its doors time and no start time.
- 🟢 **Some detail pages are empty shells.** 9 of 41 render an `.event-text` block with no prose, 3 an `.event-image` block with no `<img>`, and 7 carry no
  ticket link — all genuine gaps in the venue's own CMS, verified against the live pages, not selector failures.

### Modus Berlin (`scraper/modus/`) — hand-built site, list + detail

Verified against a live import (1 August 2026): 17 events stored, 2026-08-27 → 2027-04-13, none in the past, 0 suspicious rows, every one with a start time and
a poster. The count, titles and dates match the venue's `/events` page exactly, in the same order.

- 🟠 **The slug's date is stale for a moved show and is deliberately not read.** Every event URL encodes a `DDMMYY` date, but the slug is minted once and keeps
  the *original* date: `160426-LunaSimao` renders as `13.04.2027` under the title "Luna Simao (verschoben aus 2026)". The importer therefore reads the rendered
  date and uses the slug only as the stable `sourceId` — the opposite priority from Mikropol and Metropol, whose slugs track the real date. The consequence is
  that if the venue ever stopped rendering a date, the event would be dropped rather than filed under the slug's date.
- 🟠 **A move is announced only in the title prose.** There is no status class or badge anywhere on the site; `"(verschoben aus 2026)"` in the title is the whole
  signal, read as `POSTPONED` from the raw title before `cleanEventTitle` strips it. A cancellation or relocation the venue words differently would be stored as
  `SCHEDULED`.
- 🟠 **No prices, no sold-out state and no genre.** The venue quotes no price anywhere and sells through third parties (Eventim, its own shop, or the
  promoter's — Landstreicher Konzerte for several). It publishes no category either, so the type is inferred from the title: the programme is mostly touring
  concerts, with the recurring "Spree vom Weizen — Poetry Slam & Stand Up Show" correctly recovered as `READING` by the shared title classifier.
- 🟢 **The doors time is prose, not markup.** The detail `h2` carries the start time, but doors — where given at all — is a line inside the description
  (`"Doors: 19:30"`, `"Beginn 20:00 // Einlass 19:00"`), so both spellings are read from there. 2 of 17 events name no doors time at all.
- 🟢 **Modus and Ritter Butzke share a codebase.** The Modus page still ships the Ritter Butzke logo asset and `alt` text, and the Ready list records Ritter
  Butzke as "same shape as Modus Berlin". `ModusOverviewPageScraper` / `ModusDetailPageScraper` should be the template when that venue is implemented — quite
  possibly with the same parsers behind a second `EventSource`.

### OHM (`scraper/ohm/`) — hand-themed WordPress, single page

Verified against a live import (1 August 2026): the page listed 2 nights, the scraper parsed both, and **1 was stored** — the other was the previous night,
which the shared pipeline drops (`Dropped 1 past event(s)`, `EventUpsertService.dropPastEvents`). The stored row matches the live listing exactly: `Animalia`,
2026-08-01, 23:59, DJs C3D-E (live) / Kia / livwutang / LoLo.

- 🔴 **The venue publishes only 1–3 nights, so this source will normally hold one or two rows.** The rest of the programme moves to `/archives` once past, which
  is deliberately not crawled (it holds only past events). A low count is therefore *expected* here and is not evidence of a broken importer — the fixture tests
  are what distinguish the two. This is the thinnest source in the inventory; the venue is nonetheless active (76 nights in the archive at capture).
- 🟠 **The date has no year and no weekday.** It is resolved with `inferYearForWeekday`'s weekday-less path — the occurrence nearest today wins — which keeps a
  night that has just happened in the current year instead of rolling it twelve months forward. A date more than ~6 months out would resolve to the wrong year,
  but the venue's horizon is days, so that case cannot arise in practice.
- 🟠 **No prices, images, ticket links, descriptions or per-event URLs at all.** The listing is the whole source; `sourceUrl` is the home page for every event
  and `sourceId` is built from the resolved date plus the slugified title. The WordPress REST API answers 401, so there is no JSON source to prefer.
- 🟢 **The title is never an artist.** OHM titles are party/collective names (`Ouch x FemmeDecks`, `Animalia`); only the `<br>`-separated `.event-lineup` entries
  are stored, as `DJ`s.
- 🟢 **A lineup entry keeps its performance-format suffix** — `C3D-E (live)` is stored verbatim, so it will not resolve to the same artist row as a plain
  `C3D-E` elsewhere. This is the existing convention for DJ lineups across every club importer (AMT, ÆDEN, Renate, Duncker), not an OHM rule:
  `stripArtistSuffix`
  is applied only to headliners derived from a title → tracked in `TODO.md`.

### Parkbühne Wuhlheide (`scraper/wuhlheide/`) — October CMS, list + detail

Verified against a live import (1 August 2026): 16 events stored, 2026-08-01 → 2027-09-11, none in the past, 0 suspicious rows, **every** event with doors and
start times, a poster, a promoter and a headliner. The count, titles and dates match the live `/programm` page exactly, and the 9 sold-out flags line up exactly
with the 9 `Ausverkauft` badges — those same 9 are the only events without a price or a ticket link, because the venue drops both when a show sells out.

- 🟠 **The programme is seasonal, so the listing empties out in winter.** This is an open-air amphitheatre; `/programm` carries the summer season split into one
  block per year, and outside it the page can be nearly empty. A low count is expected off-season and is not evidence of a broken importer.
- 🟠 **No descriptions and no genres at all.** The detail page's prose block is house rules ("Bitte beachten Sie die ausgewiesenen Zeiten…"), identical on every
  page, so it is deliberately not stored as a description — an event-specific text does not exist anywhere on the site. The venue publishes no genre either.
- 🟢 **A sold-out show has no price.** The `Preis` row and the ticket button are both dropped once a show sells out, so `price_presale` is empty for 9 of 16 —
  absent rather than wrong. The price that *is* published is a single figure (the cheapest ticket), stored as presale; the venue quotes no box-office price.
- 🟢 **The detail page's `h3` is not a tour name.** A show can put an admin notice there ("Bitte die Altersbeschränkungen beachten:"), so the subtitle is taken
  from the listing only, and the detail heading is never read. The consequence is that a tour name published *only* on the detail page would be missed — none
  currently is.
- 🟢 **An act name broken by a `<wbr>` hint is restored.** The markup is `AnnenMay<wbr>Kantereit`; Jsoup renders the hint away, so the act is stored as the
  unbroken `AnnenMayKantereit` rather than split into two words. Verified in the live import, not just the fixture.
- 🟢 **Promoter names are canonicalised by the shared normaliser.** `Landstreicher Konzerte` → `Landstreicher` and `Trinity Music` → `Trinity`, which is the
  documented cross-cutting behaviour recorded at the top of this file, not a Wuhlheide rule.

### Quasimodo (`scraper/quasimodo/`) — WordPress / Events-Manager, list + detail

Verified against a live import (1 August 2026): 26 events stored, 2026-08-01 → 2026-12-19, none in the past, 0 suspicious rows. This is the most complete source
in the inventory — **every** event has a start time, a doors time, a poster, a ticket link, a description and a presale price, and only 3 lack a genre. Types
split 16 `CONCERT` / 10 `PARTY`, matching the venue's own category tagging.

- 🟠 **Three concert titles are event names and are minted as artists.** `Marcos Coll – Album Release Concert` (the act is Marcos Coll), `Soul Night ft. Frankie
  Balou` and `Jazz Night ft. Flow Rea (Est)` (the acts are the names after `ft.`), and `Berlin Beat Invasion No 8` ×2 (a series name). A venue-specific "split
  on
  `ft.`" rule was considered and rejected: the same idiom is used for a *guest* on a headliner's own show, where splitting would discard the real headliner.
  This is the cross-cutting reactive-denylist limitation recorded at the top of this file, not a Quasimodo rule.
- 🟢 **The programme is on the `.club` domain.** `quasimodo.de` is a splash page with no listing; the source URL must be `quasimodo.club/events`.
- 🟢 **The date comes from the card's *mobile* block.** Each card renders two date blocks for the two breakpoints: the desktop one abbreviates to `01.` / `Aug.`
  and leans on the month heading, while `.event-data.visible-xs .date` carries a complete `DD.MM.YYYY - HH:mm`. Reading the mobile block avoids both the
  abbreviation and the heading. If the venue ever drops the mobile markup, the date is lost rather than mis-parsed — the event is skipped, not guessed at.
- 🟢 **The category lives only on the detail page**, as an `event-categories-<slug>` class on its `<article>`. The venue marks DJ nights `party`, leaves most
  concerts untagged, and a night can carry both (`Disco Inferno` is `concerts party`) — so `party` wins and an untagged event falls back to title inference. An
  event whose detail page fails to fetch therefore keeps the listing's guess, which can leave a DJ night typed `CONCERT` with its event name as an artist.
- 🟢 **The presale price is a "from" figure.** The venue writes `ab 30€ (zzgl. Gebühr)`; the numeric field holds 30 and `priceNote` keeps the venue's own
  wording, so the "from" and the booking-fee caveat are not silently lost. A `Tageskasse` box-office price is published for some nights and stored separately.

### Säälchen (`scraper/saalchen/`) — Drupal, one shared calendar filtered by location

Verified against a live import (1 August 2026): 8 events stored, 2026-08-14 → 2026-11-27, none in the past, 0 suspicious rows, every one with a doors time, a
start time, a poster, a ticket link, a genre and price information. Types split 6 `CONCERT` / 1 `FESTIVAL` / 1 `PARTY`.

- 🟠 **The calendar is the whole Holzmarkt site's, not the venue's.** `/kalender` mixes Säälchen with the Marktplatz flea markets and the Holzmarkt 25 grounds (8
  of 13 rows were Säälchen at capture), so rows are filtered on the `.location` span. A rename of that label upstream would silently empty the import — the
  fixture test asserts the filter on a snapshot that still contains the other locations, so a rename fails the build rather than passing quietly.
- 🟠 **No descriptions at all.** The AddToCalendar payload is metadata-only for every Säälchen event; the venue writes prose for its Holzmarkt 25 market rows but
  not for this room, so `description` is empty for all 8.
- 🟠 **The `.doors` CMS field is unreliable and only a fallback.** The editors fill that single time field inconsistently — it holds the *Einlass* on
  `Opening Party` (18:00) but the *Beginn* on `Voodoo Jürgens` (20:00, against a 19:00 Einlass) — so the explicitly labelled `Einlass:` / `Beginn:` lines in the
  notice prose are authoritative. An event whose notice omits a label falls back to that ambiguous span.
- 🟠 **The price line is free-form, and a tiered price stores no number.** `Eintritt:` is hand-typed: `17,00 €`, `€40 + fees`, a bare `30,00`, and a three-tier
  `15€ ermäßigt … 25€ Normalpreis … 35€ Förderticket`. A number is stored **only when the line names exactly one amount** — taking the first of three would file
  the concession price as the ticket price — so 2 of 8 events keep only the verbatim `priceNote`.
- 🟢 **Two concert titles are event names rather than acts.** `Stegreif Orchester – freeEroica #1` and `#2` store the whole title as the artist (the act is
  Stegreif Orchester), and `10 Jahre "The Big Brassers" – Jubiläumskonzert & Party` now stores **no** artist: the co-bill splitter cut it into
  `Jubiläumskonzert`
  and `Party`, both of which were added to the shared `NON_ARTIST_NAMES` denylist, and the real act sits inside quotes where the splitter does not reach. No
  artist is better than a wrong one; this is the cross-cutting reactive-denylist limitation recorded at the top of this file.
- 🟢 **The date comes from the AddToCalendar UTC timestamp.** `atc_date_start` is emitted in UTC (`atc_timezone` says so), so a 20:00 Berlin night reads
  `19:00:00` in winter and `18:00:00` in summer; it is converted to `Europe/Berlin`. The venue's own `Datum:` line is deliberately not read — one event writes
  it in English (`October 5, 2026`).

### Ritter Butzke (`scraper/ritterbutzke/`) — Modus codebase, own template, list + detail

Verified against a live import (3 August 2026): 29 events stored, 2026-08-07 → 2027-04-10, none in the past, 0 suspicious rows, every one with a start time, a
poster and a ticket link, and 26 of 29 with a DJ lineup.

- 🟠 **Same codebase as Modus, but no shared selectors.** The venue runs the same hand-built platform (Modus even ships this venue's logo asset) and the same
  `/event/DDMMYY-<Name>` URL shape, but a different template — Bootstrap grid cards against Modus's `figcaption` tiles — so the two importers share no parsing
  code. What they do share is the trap: **the slug keeps the original date when a show moves**, proven live here by
  `310726-DeeportamentCommunityw-NicoMorano-OpenAir-Indoor`, which renders `04.09.2026`. The rendered date is authoritative; the slug is identity only.
- 🟠 **No doors time, description, genre or price anywhere.** The venue publishes a single `ab HH:mm` opening time (stored as the start time, leaving
  `doors_time` empty for all 29), no prose, no genre field and no price. Two events are nonetheless flagged free, because their titles say so
  ("… - free entry until 9pm") and the shared `detectFree` reads the phrase.
- 🟢 **The DJ lineup is read from a presentational selector.** The `Line Up:` block has no class; its rows are distinguished from the refund notice and video
  embed that follow only by an inline `padding-left` style. ADR-007 would normally rule that out, but it is the sole discriminator the template offers — a
  label-only scope collects the legal boilerplate instead. A restyle of that block would silently empty the lineup, which is what the fixture tests guard.
- 🟢 **Three events store no artist, all correctly.** Community-Rave's lineup is literally `TBA` (dropped by the shared placeholder filter), and Bunte Träumerei
  and Hippie New Year have no lineup block at all. The event *title* is a night/series name (`House of Rave w/ …`) and is never minted as an act.
- 🟢 **Several events share a date routinely.** The club runs multiple floors, so two or three nights per date is normal (three on 2026-08-08); the slug's
  trailing name keeps them apart. `/calendarfile/<id>` is `Disallow`ed by robots.txt and is never fetched.

### Uber Arena (`scraper/uberarena/`) — AEG CMS, list + detail

Verified against a live import (3 August 2026): the listing carried 128 rows, 88 survived the sport filter, and **85 were stored** — the three lost are
second sessions of a same-day double bill (see below). Range 2026-08-21 → 2028-01-29, none in the past, 0 suspicious rows; every event has a start time and a
poster, 64 are `CONCERT` and 21 `SHOW`.

- 🔴 **A production playing twice in one day loses its second session.** `Feuerwerk der Turnkunst` (14:00 + 19:00) and `CAVALLUNA` on two consecutive days
  (14:00 + 19:00, 13:00 + 17:30) each collide on the stored `event.slug`, which is built from date + venue + title. `EventUpsertService` skips the duplicate with
  a warning rather than failing the import, so the count is 85 not 88. This is the cross-cutting slug limitation already tracked in `TODO.md` ("A show cannot
  play twice in one day") — Uber Arena is now the third venue to hit it after Velomax and Bar jeder Vernunft, and the only fix is to include the start time in
  the slug at the persistence boundary.
- 🟠 **Sport is deliberately not imported.** The arena is home to ALBA Berlin and the Eisbären: 40 of the 128 rows are `eishockey`, `basketball` or `sport`, and
  filed as `OTHER` they would bury the 88 concerts, shows and comedy nights they sit among — the same decision as the Velomax halls. Those rows are also the only
  ones carrying a `00:00 Uhr` placeholder start, so the filter removes that noise too.
- 🟠 **No genre on any event.** The venue types events only as `Konzert` / `Show` / `Comedy` (mapped to `CONCERT` / `SHOW`) and publishes no genre field, so the
  genre-tag filters never see an Uber Arena event.
- 🟢 **A `SHOW` stores no artist, by design.** All 21 are production names (`CAVALLUNA - Die Farben des Lebens`, `Feuerwerk der Turnkunst`), not acts, so the
  shared `buildArtistsForEventType` correctly derives none; the 64 concerts all carry a headliner.
- 🟢 **Three events have no price.** The `6K UNITED!` dates render a non-breaking space where the `ab NN,NN €` from-price normally sits; that becomes neither a
  zero nor an empty note. The price the venue does publish is the cheapest ticket, stored as presale with the "ab" wording kept in `priceNote`.
- 🟢 **A detail fetch that degrades costs only the extras.** Two of the 88 detail pages returned no heading during the live run (both parse fine on retry, so it
  was transient), and those events were stored from listing data alone — losing the doors time, description and ticket link, but nothing else. 6 of 85 have no
  doors time and 5 no ticket link for this and similar reasons.

---

## How to extend this doc

When adding or changing an importer, record any *accepted* limitation here (with an impact marker) — capture the current state and *why*, not the fix. If it's
actionable, add the fix to `TODO.md` and point at it (`→ tracked in TODO.md`) rather than describing the fix here, so the two files don't drift. Prefer linking
to the code KDoc that documents the same limitation.
