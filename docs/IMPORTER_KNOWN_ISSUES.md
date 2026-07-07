# Importer — Known Issues & Limitations

A living catalogue of the known gaps, quirks and missing-data limitations of the
currently implemented event importers, so they can be picked up later. This is
**not** a bug tracker for regressions — it records *accepted / documented*
limitations of the scrape pipeline as it stands.

Related: [ADR-007 Web Scraping Strategy](adr/ADR-007_WEB_SCRAPING_STRATEGY.md) ·
[EVENT_DATA_SOURCES.md](EVENT_DATA_SOURCES.md) (per-venue field analysis) ·
actionable backlog in [../TODO.md](../TODO.md).

Legend: **impact** — 🔴 user-visible missing/wrong data · 🟠 data-quality/noise ·
🟢 cosmetic or edge case.

---

## Cross-cutting (affect several/all importers)

- 🔴 **Many concerts have no headliner.** ~40% of `CONCERT` events (a July 2026
  seed showed 148 of 354, dominated by **Badehaus** ~72 and **Privatclub** ~70)
  carry no artist at all, because title-as-headliner extraction is deliberately
  conservative: Privatclub and Cassiopeia only treat the title as an artist when a
  `Support:` line confirms it, and Badehaus extracts no artist roster at all
  (see per-importer notes). For these venues the concert title *is* almost always
  the act, so this is user-visible missing data. → Fix tracked in `TODO.md`
  (title-as-headliner extraction — now safer since `isNonArtistName` /
  `stripArtistSuffix` filter non-artist titles; Astra and Lido already do it via
  `buildArtistsForEventType`).
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
- 🟠 **Genre tags are only partially normalized.** `GenreNormalizer` maps a
  synonym table, but many tokens fall through as-is (`No synonym match for genre
  token …`), creating noisy/duplicated tags — and naive splitting yields
  fragments (`Beyond`, `Wave`, `Retro`, `Tango or NonTango`). Non-genre event
  descriptors also leak into `genre_tag` where a venue reuses the genre field for
  a label (`Immersive Ausstellung`, `Twenty One Pilots Special`, `Karaoke` — all
  Cassiopeia). The raw genre text is preserved; broader synonyms, better
  tokenization, and a non-genre stop-list are tracked in `TODO.md`.
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
- 🟠 **Artists rarely extracted.** The title may be an artist *or* an event name
  (e.g. "Grey City Fest Opener"); without a "Support:" signal it's ambiguous, so
  no artists are created to avoid false entries.
- 🟢 Some fields use **positional CSS fallbacks** (`._5`, `._8`) — fragile if the
  Webflow layout changes.

### Privatclub (`scraper/privatclub/`) — WordPress, single page
- 🔴 **Concerts without a `Support:` line get no artist.** `parseArtists` only
  builds a lineup for `CONCERT` events, and even then only when the subtitle
  carries a `Support:` line (the signal that the title is an act) — so a concert
  titled just `20Tokens` yields no headliner. ~70 concerts were artist-less in a
  July 2026 seed (see the cross-cutting entry).
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
- 🔴 **No artist roster at all.** The scraper extracts no artists — a concert
  titled `Anette Olzon` or `El Flecha Negra` yields zero artist entries (~72
  artist-less concerts in a July 2026 seed). The title is almost always the act,
  so title-as-headliner extraction would recover them.
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
