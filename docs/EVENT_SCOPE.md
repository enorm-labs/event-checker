# Event Scope — what belongs in Event Junkie

What kinds of event this app carries, what it deliberately leaves out, and which questions are still open.
The one-line version: **if a Berlin venue puts it on a stage in the evening, it is in scope.**

This document is the standing reference for that question. Related, and deliberately not duplicated here:
[EVENT_DATA_SOURCES.md](EVENT_DATA_SOURCES.md) tracks *which venues* are imported;
[DATA_MODEL.md](DATA_MODEL.md) describes the schema; [../TODO.md](../TODO.md) holds the actionable backlog.

---

## 1. The rule

Scope is decided by **format**, not by genre. A venue's evening programme is in; things that happen to be listed on
the same page but are not a programme are out.

That rule has one deliberate corollary, and it is the most useful thing in this document:

> **The venue decides what kind of night it is.**

Where a venue publishes its own category — Astra's "Konzert", Badehaus's "Quiz", Bar jeder Vernunft's genre —
that label is mapped rather than second-guessed. It stops the importers from encoding one person's taste about
whether a burlesque revue is a `SHOW` or a `CONCERT`, and it means a venue that reclassifies its own programme is
followed automatically. The mapping table lives in
[`EventTypeMapping.kt`](../events-importer/src/main/kotlin/de/norm/events/scraper/EventTypeMapping.kt), with
venue-specific labels passed in per scraper rather than polluting the shared table.

## 2. Event types in the model

Ten values on [`EventType`](../events-core/src/main/kotlin/de/norm/events/event/Event.kt). Every one is in real use —
this is not an aspirational list. Counts are from the development database (3166 events across 86 sources) and are
illustrative of the *mix*, not of coverage:

| Type | Share | What it covers | Where it comes from |
|---|---:|---|---|
| `CONCERT` | ~62% | Live music with a billed lineup, from back rooms to arenas | `konzert` / `concert`, and most venues' default |
| `PARTY` | ~19% | DJ nights, one-off parties | `party` |
| `SHOW` | ~11% | Staged performance — cabaret, burlesque, comedy, musicals, variety | `show` |
| `OTHER` | ~3% | The genuine remainder, plus anything a venue labels `sonstiges` | fallback |
| `READING` | ~2% | Literary readings, spoken word, poetry slams | `lesung` / `reading` |
| `FESTIVAL` | ~1% | Multi-day or multi-stage events | `festival` |
| `EXHIBITION` | ~1% | Gallery shows and openings | `ausstellung` / `exhibition` / `vernissage` |
| `QUIZ` | <1% | Pub quizzes and game nights | `quiz` |
| `SCREENING` | <1% | Film screenings, open-air cinema, football "public viewing" | `screening`, `public viewing` |
| `CLUB_NIGHT` | <1% | A recurring club night distinct from a one-off party | venue-specific labels |

**`OTHER` is a fallback, not a bin.** `parseOrDefault` logs a warning whenever it resolves to `OTHER`, so an
unrecognised label is a signal to extend the mapping rather than something that silently accumulates. The 3% share
is a health metric: if it climbs, a venue has started using vocabulary nobody has mapped.

**`CLUB_NIGHT` is the weakest distinction in the list.** In practice the boundary between it and `PARTY` is drawn by
whichever word a venue happened to use, and only 7 events carry it. Worth either defining sharply or merging into
`PARTY` — noted in §5.

## 3. What is deliberately excluded

Four exclusions, each implemented in exactly one place so it can be revisited without archaeology.

### 3.1 Sport

**Not imported.** There is no `SPORT` event type, and mapping fixtures to `OTHER` would bury the concerts they sit
among. The arenas force the question rather than avoid it:

- **Uber Arena / Uber Eats Music Hall** — home to ALBA Berlin and the Eisbären; roughly a third of the listing is
  basketball and ice hockey. Dropped in `AegOverviewPageScraper.isSport`, which matches both the label and the
  platform's numeric taxonomy.
- **The three Velomax halls** — handball, volleyball and basketball are the biggest strand. `VENUE_EVENT_TYPES`
  simply omits `sport`, so an unmapped row is skipped rather than filed.

The consequence is worth stating plainly: **an arena's imported event count is well below what its own programme
page shows**, and that is correct rather than a bug.

### 3.2 Participation formats

Guided tours, workshops, yoga and qigong sessions, environmental-education slots, drop-in handicraft afternoons.
These are things you *take part in*, not things you *go and see*.

The precedent was set by **Gärten der Welt**: 28 of its 41 upcoming rows were park activities. Importing them would
have swamped the actual programme — the Arena concerts, the open-air cinema, the park festivals — and presented a
concert venue as a tour operator. One predicate,
[`isProgrammeCategory`](../events-importer/src/main/kotlin/de/norm/events/scraper/gaertenderwelt/GaertenDerWeltFieldMapping.kt),
holds the rule, and it is the line to change to revisit it.

Note the deliberate asymmetry: a row with **no** category is kept, because the park files its one-off evening events
(a games night, a quiz show) under no category at all, and dropping uncategorised rows would lose them.

### 3.3 Trade fairs and conferences

Not modelled and not imported. Arena Berlin is the clearest case — all five of its upcoming entries were trade fairs
(deGUT, BUCHBERLIN, Einstieg Berlin), which is why it sits in *Blocked* despite being trivially scrapable. The
blocker there was never the markup.

### 3.4 Classical concerts and orchestras

**Not a taste judgement — a data-model one.** Classical fits the existing `CONCERT` type perfectly well, but the
shape of the data differs: an orchestra or ensemble plus a conductor plus soloists, rather than a headliner with
support. The `ArtistRole` vocabulary and the genre taxonomy both need a decision before an orchestral house can be
imported honestly.

**RBB Sendesaal is the live example.** Its scraping was solved in the 3 August re-check — the ROC calendar is
server-rendered and attributes each concert to a venue, so `.ConcertListItem-location` is the only filter needed —
and it went back to *Blocked* on **scope, not on scraping**. Answer §5's first question and the importer is a short
job.

## 4. What is in scope, and sometimes surprises people

- **Not just live music.** A theatre, a comedy club or an arena-scale room is in scope. **Bar jeder Vernunft** set
  that precedent: its programme is imported, with the venue's own genre deciding whether a night is a `CONCERT` or a
  `SHOW`.
- **Not just techno.** This is the point of the project, and it is worth repeating in a scope document because
  Berlin aggregators have a strong pull in that direction. Punk, jazz, indie, metal, cabaret and singer-songwriter
  nights are as in scope as a Berghain listing.
- **Not just ticketed events.** Free events are detected and badged at import.
- **Venue categories imported today**: clubs (52), bars (35), techno clubs (31), concert halls (30), open-air
  spaces (13), arenas (3), theatres (2), comedy clubs (1).

## 5. Open questions

These are decisions, not tasks — each changes what the app *is*, so none should be settled by an importer PR.
Tracked as checkboxes under **Open questions — coverage scope** in [../TODO.md](../TODO.md).

| Question | Blocked on | Cost of saying yes |
|---|---|---|
| **Classical / orchestras?** (Konzerthaus, Philharmonie, RBB Sendesaal, Berliner Symphoniker) | the artist model — orchestra + conductor + soloists vs. headliner + support | Medium. `ArtistRole` and the genre vocabulary need extending; the scraping is solved for at least one venue |
| **Comedy clubs?** (Comedy Café Berlin, Quatsch Comedy Club, …) | nothing much | **Lowest of the four.** One comedy club is already imported; this is mostly more venues of a category that already exists |
| **Theatres?** (Volksbühne, Schaubühne, Berliner Ensemble, …) | nothing much | Low. Theater im Delphi, Heimathafen and Bar jeder Vernunft are already imported — this is coverage, not a new category |
| **Exhibitions as a first-class thing?** | the time model — a run of weeks, not a start time on one evening | Medium. `EXHIBITION` exists and openings import fine today; a *run* needs a date range in the schema and a display decision |
| **Sport?** | a genuine product decision | High, and answer it **independently** of the three above. New venues, a different audience, and arguably past the point where this is still a music app |

**Two of these are nearly free and two are not.** Comedy and theatre are coverage questions with categories that
already exist. Classical and exhibitions each need a model change first, and sport needs a decision about what the
product is for.

## 6. Changing scope

If you are adding an importer and the venue's programme does not obviously fit:

1. **Check this document first.** If the answer is here, follow it.
2. **If it is a listed open question, do not settle it in an importer PR.** Say so in the PR and leave the venue in
   *Blocked* with the reason. That is exactly what RBB Sendesaal is doing.
3. **If it is genuinely new**, add a row here with the reasoning, and put the implementation behind one named
   predicate — `isSport`, `isProgrammeCategory` — rather than scattering conditions through a parser. Every
   exclusion above is one line to find and one line to change, and that property is worth protecting.
