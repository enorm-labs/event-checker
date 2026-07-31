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

---

## How to extend this doc

When adding or changing an importer, record any *accepted* limitation here (with an impact marker) — capture the current state and *why*, not the fix. If it's
actionable, add the fix to `TODO.md` and point at it (`→ tracked in TODO.md`) rather than describing the fix here, so the two files don't drift. Prefer linking
to the code KDoc that documents the same limitation.
