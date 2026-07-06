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

- 🔴 **Multi-artist titles are stored as a single headliner.** Titles like
  `TOTAL CHAOS + RUMKICKS + THE DOLLHEADS`, `LAGWAGON / THE VIRGINMARYS`,
  `FLIEHENDE STÜRME / DER FLUCH / …`, `BLACK STAR RIDERS & TYKETTO` become one
  artist entity instead of several. Affects SO36, Astra, Lido and anywhere the
  source packs a lineup into the title. Needs a shared, safe title-splitting
  strategy (separators vary; band names legitimately contain `&`/`and`).
- 🟠 **Artist names are not canonicalized.** Unlike promoters (see below),
  artists are stored as-scraped, so the same act fragments across venues —
  ALL-CAPS on one site (`GREEN LUNG`, `MUNA`) vs. mixed case on another
  (`Green Lung`). A de-shout/normalize pass (keeping acronyms) is on the backlog;
  stripping words from band names is unsafe, so it must be casing-only.
- 🟠 **Genre tags are only partially normalized.** `GenreNormalizer` maps a
  synonym table, but many tokens fall through as-is (`No synonym match for genre
  token …`), creating noisy/duplicated tags — and naive splitting yields
  fragments (`Beyond`, `Wave`, `Retro`, `Tango or NonTango`). The raw genre text
  is preserved, but the structured tags need broader synonyms and better tokenization.
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
  - spacing/abbreviation variants can't merge (`ALLROOMS` vs `All Rooms`);
  - a *leading* descriptor isn't stripped (`Konzertbüro Schoneberg` ≠
    `Schoneberg Konzerte`);
  - source typos stay distinct (`Trinty` vs `Trinity`);
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
- 🟠 **Artists only for concerts** (title = headliner + `Support:` line); other
  event types get none.
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
- 🔴 **Multi-artist titles** → single headliner (see cross-cutting).
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
impact marker) and, if it's actionable soon, add a matching item to `TODO.md`.
Prefer linking to the code KDoc that documents the same limitation.
