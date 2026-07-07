# Importer — Known Issues & Limitations

A living catalogue of the known gaps, quirks and missing-data limitations of the
currently implemented event importers, so they can be picked up later. This is
**not** a bug tracker for regressions — it records *accepted / documented*
limitations of the scrape pipeline as it stands.

Related: [ADR-007 Web Scraping Strategy](adr/ADR-007_WEB_SCRAPING_STRATEGY.md) ·
[EVENT_DATA_SOURCES.md](EVENT_DATA_SOURCES.md) (per-venue field analysis) ·
[DATA_QUALITY_STRATEGY.md](DATA_QUALITY_STRATEGY.md) (how these get fixed & prevented) ·
actionable backlog in [../TODO.md](../TODO.md).

Legend: **impact** — 🔴 user-visible missing/wrong data · 🟠 data-quality/noise ·
🟢 cosmetic or edge case.

---

## Cross-cutting (affect several/all importers)

- 🟢 **Concerts extract the title as headliner (all venues).** Every venue now
  treats a `CONCERT` title as the headliner via `buildArtistsForEventType` /
  `headlinersFromTitle` (Privatclub and Badehaus unconditionally; Cassiopeia too,
  guarded by the strengthened `isNonArtistName` since its titles are the most
  ambiguous — see per-importer notes). This recovered the ~40% of concerts (148 of
  354 in a July 2026 seed) that previously stored no artist. Residual risk is the
  reactive non-artist denylist (next entry): a title that is really an event name
  but matches no structural filter is minted as a headliner until denylisted.
  **Existing rows are unaffected until re-imported** — a one-off backfill re-scrape
  is still tracked in `TODO.md`.
- 🟠 **A few non-artist titles still slip through as artists.** The curated
  filters catch festivals/tours/segments/labels, and `stripArtistSuffix` recovers the
  act from tour/live/anniversary tails and performance-format annotations
  (`THE BUTLERS - 40 YEARS, SKA & SOULPOWER -` → `The Butlers`, `Avangelic (DJ-Set)` →
  `Avangelic`). Idiosyncratic event-format titles are handled case-by-case as they
  surface by adding them to the curated denylist (`NON_ARTIST_NAMES`, e.g. `Music Quiz`,
  `Open Mic L. J. Fox`) — but this is inherently reactive, so newly-seen titles will
  slip through until denylisted. A general fix needs a classifier → tracked in
  `TODO.md` (AI-assisted data quality).
- 🟠 **Artist display names — de-shouted, with residual cases.** `canonicalArtistName`
  now de-shouts ALL-CAPS act names to a clean display form before they're stored
  (`GREEN LUNG` → `Green Lung`), so an act isn't frozen SHOUTING by whichever venue
  imported it first. It's *casing-only* — no words are stripped (unsafe for band
  names). It de-shouts words with attached punctuation too (`MURPHY'S LAW` →
  `Murphy's Law`, `(BLACK KRAY)` → `(Black Kray)`), while keeping digit/dotted stylised
  tokens (`MC5`, `H2O`, `AC/DC`, `HGICH.T`), mixed casing (`DJ Koze`), and a curated
  acronym set (`DJ`, `MC`, `UK`, …). Residual, by design:
    - a genuine all-caps name not in the acronym set is title-cased like any shouted word,
      whether letters-only (`MUNA` → `Muna`, `MØ` → `Mø`) or stylised with an interior
      symbol other than `.`/`/` (`BIGA*RANX` → `Biga*ranx`) — extend `ACRONYMS` to keep one;
    - 🟢 this only cleans the display name; slugs are case-insensitive, so ALL-CAPS and
      mixed-case spellings already resolved to one artist row (no fragmentation), and
      rows created before this fix keep their original casing until re-created.
- 🟢 **Genre tags are normalized through a curated map, stop-list, and gated
  fall-through.** `GenreNormalizer` splits on more separators (now also ` or `,
  ` oder `, ` vs `, so `Tango or NonTango` → `Tango`), resolves tokens against a
  synonym table, and drops non-genre noise two ways: a `NON_GENRE_TOKENS`
  stop-list removes event-format labels a venue pushed into the genre field
  (`Immersive Ausstellung`, `Release Party`, freeform fragments like `Beyond` /
  `Wave` / `Retro`), and a `looksLikeGenre` gate keeps an unmatched token only
  when it plausibly names a genre (≤2 words, has letters, no stop-listed word),
  so long series labels like `Twenty One Pilots Special` no longer leak. The raw
  genre text is still preserved on the event.
    - 🟢 Vocabulary is gated, not closed: a genuinely new genre that passes the
      heuristic is still captured as-is, and everything dropped is logged
      (`Dropping non-genre token …`) as the curation queue for growing the synonym
      map / stop-list. `Karaoke` stays a tag (it is in the synonym map and treated
      as a genre) even where a venue uses it as an event label.
- 🟠 **`eventType` frequently defaults to `OTHER`.** When a source exposes no
  category, `toEventEntity` maps to `OTHER`. Discovery/filtering by type is
  therefore incomplete for several venues (see per-importer notes).
- 🟢 **First page only — pagination is intentionally not followed** (ADR-007).
  Far-future events on page 2+ of paginated listings are not imported. Accepted:
  first page = the most relevant upcoming events; multi-page crawling was rejected.
- 🟢 **`priceCurrency` is hard-coded to EUR** (all venues are Berlin). Fine until
  a non-EUR venue is added.

## Data-quality / entity resolution

- 🟠 **Promoter fragmentation — mostly fixed, with residual cases.**
  `canonicalPromoterName` now merges abbreviated/full variants (`LOFT` /
  `Loft Concerts GmbH` → `Loft`). Remaining, by design:
    - a *leading* descriptor isn't stripped (`Konzertbüro Schoneberg` ≠
      `Schoneberg Konzerte`) unless a curated `NAME_CORRECTIONS` entry is added;
    - source typos and spelling/spacing variants are folded onto one spelling via
      a curated map (`Trinty` → `Trinity`, `Allrooms`/`ALLROOMS` → `All Rooms`);
      only *known* names are corrected — new ones need an entry in
      `NAME_CORRECTIONS`;
    - 🟢 de-shout lowercases genuine acronyms in the display name (`TV Noir` →
      `Tv Noir`, `Bossa FM` → `Bossa Fm`) — display-only, slugs unaffected.
- **Coverage gap (not a defect):** JS-rendered / cookie-walled venues aren't
  importable yet (Playwright deferred, ADR-007). See EVENT_DATA_SOURCES.md for
  which venues remain.

---

## Per-importer

### Cassiopeia (`scraper/cassiopeia/`) — Webflow, list + detail

- 🟠 **First page only** (Finsweet CMS Load lazy-loads the rest via JS) — ~8 events.
- 🟠 **Title-as-headliner, structurally guarded.** For a `CONCERT` the title is
  taken as the headliner (support acts still come from `Support:` description
  paragraphs). Cassiopeia titles are the most ambiguous — they may be an artist
  *or* an event name (e.g. "Grey City Fest Opener") — so the `isNonArtistName`
  festival filter was widened to catch a festival slot/edition (`… Fest <slot>`),
  not just a bare/year-suffixed festival name. Residual: an event-name title that
  matches no structural filter is still minted as a headliner until denylisted.
- 🟢 Some fields use **positional CSS fallbacks** (`._5`, `._8`) — fragile if the
  Webflow layout changes.

### Privatclub (`scraper/privatclub/`) — WordPress, single page

- 🟢 **Concerts extract the title as headliner** via `buildArtistsForEventType`:
  a `CONCERT` title is the act unconditionally (support acts from the subtitle's
  `Support:` line), while parties/festivals extract none. A concert titled just
  `20Tokens` now yields a headliner; the ~70 artist-less concerts in a July 2026
  seed are recovered (see the cross-cutting entry).
- 🟢 Complex/conditional pricing is stored as free-form `priceNote` rather than
  structured presale/box-office.

### Madame Claude (`scraper/madameclaude/`) — WordPress, list + detail

- 🟢 Small venue (~11 events); `DD/MM/YY` dates. No major known gaps beyond the
  cross-cutting ones.

### Astra Kulturhaus (`scraper/astra/`) — Kulturhäuser platform, list + detail

- 🟠 **Festival-day mislabeling** is only best-effort corrected. Astra tags each
  festival day individually and sometimes labels one "Concert"; normalization
  fixes it only when a correctly-labelled sibling exists on the same page.
- 🟠 **Dateless featured teaser** depends on its detail page for the date; if that
  fetch fails the event is dropped (e.g. `11FREUNDE WM-QUARTIER` drops each run).
- 🟢 Duplicate events on the listing are skipped by `sourceId` (`ERRA + CURRENTS`,
  `VOILÀ`) — expected, but means the site genuinely double-lists some events.
- The detail page carries no artist roster, so artists come only from the overview.

### Lido (`scraper/lido/`) — same Kulturhäuser platform as Astra

- Shares Astra's platform limitations (teaser date fallback, artists from overview).

### SO36 (`scraper/so36/`) — Ticket-Toaster shop, list + detail

- 🔴 **Sold-out is never detected.** The JSON-LD `availability: SoldOut` is
  unreliable — SO36 sells via external shops that report on-platform availability
  as `SoldOut` even when tickets are freely available — so it's ignored and
  `soldOut` is always `false`. A reliable sold-out signal has not been found.
- 🟠 No genre, promoter, or structured box-office price. `eventType` is limited to
  what the `supertitle` label exposes (Konzert/Party), else `OTHER`.

### Roadrunner's Paradise (`scraper/roadrunner/`) — retro hand-coded, single page

- 🔴 **No artists, promoters, prices, or event type.** The free-text retro HTML
  carries none reliably; `eventType` defaults to `OTHER`.
- 🟠 **Very sparse & stale.** Currently ~1 event; the site leaves past events
  listed. The year is *inferred from the weekday* (the source omits it), which
  resolves stale past dates correctly but yields events that won't show in
  today-forward feeds.

### Badehaus (`scraper/badehaus/`) — WordPress / Events Manager, list + detail

- 🟢 **Title-as-headliner (from the overview).** The overview scraper now extracts
  the headliner from the title for inferred `CONCERT` events via
  `buildArtistsForEventType` (the detail page still carries no roster, so the merge
  falls back to the overview's artists). A concert titled `Anette Olzon` or
  `El Flecha Negra` yields a headliner; the ~72 artist-less concerts in a July 2026
  seed are recovered. Depends on the inferred type below — a title mis-inferred as
  non-`CONCERT` yields no artist.
- 🔴 **`eventType` is inferred from the title, not scraped.** Badehaus publishes
  no machine-readable category anywhere, so the type is a heuristic
  (quiz / party / screening, else `CONCERT`). Non-matching events may be mislabelled.
- 🟠 **Start time (`Beginn`) and promoter are only on *some* detail pages** — many
  events have doors time only and no promoter.
- 🟢 One venue-side **dead link** (a `%`-encoded Arabic-slug event) `404`s each run
  and degrades to overview data — correctly handled, but worth knowing.
- 🟢 Heaviest importer: ~90 sequential (throttled) detail fetches, ~40s per run.

---

## How to extend this doc

When adding or changing an importer, record any *accepted* limitation here (with an
impact marker) — capture the current state and *why*, not the fix. If it's actionable,
add the fix to `TODO.md` and point at it (`→ tracked in TODO.md`) rather than describing
the fix here, so the two files don't drift. Prefer linking to the code KDoc that documents
the same limitation.
