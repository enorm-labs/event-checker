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
  are stored as Huxleys events with a `RELOCATED` status, which is what the source says, but they will happen elsewhere — and if the receiving venue also has
  an importer, the same show lands twice under two venues. The note text is not stored, so the destination is lost.
- 🟢 **The change note is the only signal for two statuses.** Sold-out and cancelled come from a CSS class on the list item (`Ausverkauft` / `Abgesagt`) with a
  matching badge, but a relocation or a new date is announced solely in the listing's free-text `.anderungen` note, which the detail page omits entirely — so
  the merge deliberately keeps the *overview's* status whenever it is non-default. Notes that are not status changes ("Zusatzshow", "Nachholtermin", "Eintritt
  ab 18 Jahren!") correctly leave the status alone, but only because none of them contains a status keyword; a future note that does would be misread.
- 🟢 **The detail page has no heading, so the listing owns the title.** The act's name appears there only in the document title with " - Huxleys Neue Welt"
  appended. The detail scraper strips that suffix so it can stand alone when the listing is unavailable, but a successful merge keeps the listing's
  `.eventname`.
- 🟢 **Genre and promoter are read from CSS classes.** Both are WordPress taxonomies the theme emits as slugs on the `article` element (`event-tags-electronic`,
  `promoters-trinity-music`), which costs no extra request but means de-slugified display names: a legal form comes back title-cased word by word
  (`Concert Concept Veranstaltungs Gmbh`) and a stylised genre loses its punctuation (`kpop` → `Kpop`, not `K-Pop`). The sibling `presenters-*` taxonomy
  (media partners) is deliberately not read as a promoter — de-slugifying a domain-shaped term yields a mangled `Laut De`.
- 🟢 **`Corrupted Blood Club Show` is typed `PARTY` and loses its headliner.** The shared title classifier treats `club` as a party keyword, so this one show
  (of 107) is mistyped and, being a party, has no artist derived from its title. The cross-cutting reactive-keyword limitation, not specific to this venue.

---

## How to extend this doc

When adding or changing an importer, record any *accepted* limitation here (with an impact marker) — capture the current state and *why*, not the fix. If it's
actionable, add the fix to `TODO.md` and point at it (`→ tracked in TODO.md`) rather than describing the fix here, so the two files don't drift. Prefer linking
to the code KDoc that documents the same limitation.
