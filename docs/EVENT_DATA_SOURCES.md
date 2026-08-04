# Event Data Sources — Berlin

Overview of all venues, clubs, and promoters whose websites are potential sources for importing event data. Sources are grouped by **import status** so the
remaining work is visible at a glance. The **Comment** column records what matters for building or maintaining an importer — the platform, where the data lives,
and the parsing quirks. For an implemented importer, its KDoc and scraper tests are the authoritative field mapping; known defects live in
[IMPORTER_KNOWN_ISSUES.md](IMPORTER_KNOWN_ISSUES.md).

| Status                              | Meaning                                                                              | Count |
|-------------------------------------|--------------------------------------------------------------------------------------|------:|
| ✅ [Imported](#-imported)           | Importer implemented and scheduled                                                   |    71 |
| 🔨 [Ready](#-ready-to-implement)    | Website analyzed, listings are scrapable — these are the next importers to build     |     8 |
| ⛔ [Blocked](#-blocked--deferred)   | Website analyzed, but no usable listings (no programme page, JS-only, or too sparse) |    49 |
| ❓ [Unanalyzed](#-not-analyzed-yet) | No URL recorded yet — website still needs a first look                               |    48 |

"Website analyzed" also means the [data model](DATA_MODEL.md) was checked against that source — to date no source has required a schema change.

## ✅ Imported

| Name                        | URL                                                         | Type         | Comment                                               |
|-----------------------------|-------------------------------------------------------------|--------------|-------------------------------------------------------|
| ÆDEN                        | https://aedenberlin.com/                                    | Techno Club  | WordPress; /events → month pages; no prices           |
| Admiralspalast              | https://www.admiralspalast.theater/                         | Theater      | Contao; one event per performance row; no prices      |
| Alte Kantine Kulturbrauerei | https://alte-kantine.eu/                                    | Concert Hall |                                                       |
| AMT                         | https://www.club-amt.berlin                                 | Techno Club  | Webflow; /events → month pages                        |
| Arcanoa                     | https://www.ssi-media.com/arcanoa/veranst.htm               | Bar          | 1990s HTML; title/date only; year from weekday        |
| arkaoda                     | https://berlin.arkaoda.com/?/default/program                | Bar          | PHP router; only "Konser" typed; RA link in prose     |
| Astra Kulturhaus            | https://www.astra-berlin.de/                                | Concert Hall | schema.org `MusicEvent`; presale + door prices        |
| Badehaus                    | https://badehaus-berlin.com/                                | Club         | "AUSVERKAUFT"/"VERLEGT" labels; ticket + FB links     |
| Bar jeder Vernunft          | https://www.bar-jeder-vernunft.de/de/programm/kalender.html | Bar          | Neos; per-date JSON-LD; one show page per run         |
| Berghain / Panorama Bar     | https://www.berghain.berlin/de/program/                     | Techno Club  | Server-rendered; list + detail                        |
| Bi Nuu                      | https://binuu.de/                                           | Club         | No genre or prices on site; only via ticket link      |
| Cassiopeia                  | https://cassiopeia-berlin.de/                               | Club         | Webflow; genre tags, sold-out / cancelled badges      |
| Clash Club                  | https://clash-berlin.de/                                    | Club         | WordPress; sparse — no times, prices or text          |
| Club der Visionäre          | https://clubdervisionaere.com/programm                      | Techno Club  | WordPress; one listing, 3 rooms by CSS class          |
| Columbia Theater            | https://columbia-theater.de/                                | Concert Hall | WordPress; date in slug; status via `data-*` flag     |
| Columbiahalle               | https://www.columbiahalle.berlin/veranstaltungen.html       | Concert Hall | Contao; one page, month headings carry the year       |
| Cosmic Comedy Club          | https://comedyclubberlin.com/wp-json/tribe/events/v1/events | Comedy Club  | The Events Calendar REST API; cursor-paged; no prices |
| Duncker Club                | https://www.dunckerclub.de/                                 | Club         |                                                       |
| Festsaal Kreuzberg          | https://festsaal-kreuzberg.de/de                            | Concert Hall | Nuxt/Wagtail SSR; `ld+json` empty; no prices          |
| Frannz Club                 | https://frannz.eu/                                          | Club         |                                                       |
| Golden Gate                 | https://goldengate-berlin.de/                               | Techno Club  | Elementor; current Thu–Sat block only; door-only      |
| Gretchen                    | https://www.gretchen-club.de/                               | Club         |                                                       |
| Havanna                     | https://www.havanna-berlin.de/                              | Club         | Undated weekly nights; occurrences derived            |
| Heideglühen                 | https://heidegluehen.berlin/monatsvorschau/                 | Techno Club  | One month at a time; DJ lineup on /aktuell/           |
| Heimathafen Neukölln        | https://heimathafen-neukoelln.de/                           | Concert Hall | WP REST + ACF; one post, many dated performances      |
| Hole 44                     | https://hole-berlin.de/                                     | Concert Hall | Events-Manager; "Abgesagt!" / "VERLEGT!" labels       |
| Humboldthain Club           | https://www.humboldthain.com/                               | Techno Club  | Elfsight widget API; weekly night expanded            |
| Huxleys Neue Welt           | https://huxleysneuewelt.de/events                           | Concert Hall | Events-Manager; ISO slug date; genre/promoter tags    |
| Junction Bar                | https://www.junction-bar.de/                                | Bar          | Static monthly pages; show times vary by weekday      |
| Kantine am Berghain         | https://www.berghain.berlin/de/program/kantine-am-berghain/ | Concert Hall | Shares BERGHAIN importer                              |
| Kater                       | https://www.katerclub.de/                                   | Techno Club  | Homepage programme; ___ floor rules mark lineups      |
| LARK                        | https://larkberlin.com/events/                              | Club         | WP REST + ACF; post date is the event date            |
| Lido                        | https://www.lido-berlin.de/                                 | Concert Hall | Clean slugs; doors + start; "Ausverkauft" badge       |
| Loge                        | https://www.loge-berlin.org/                                | Club         | Wix; tickets on-site; support via "+" in title        |
| Madame Claude               | https://madameclaude.de/                                    | Bar          | WordPress `event` REST API (ACF)                      |
| Matrix Club Berlin          | https://www.matrix-berlin.de/                               | Club         | WordPress; month pages walked; DJs + door prices      |
| Max-Schmeling-Halle         | https://www.velomax.de/events                               | Arena        | Shared VELOMAX listing; no sport imported             |
| Maxxim Club                 | https://www.maxxim-berlin.de/partys                         | Club         | Wix Events warmup JSON; UTC dates; prices inline      |
| Metropol                    | https://metropol-berlin.de/events                           | Concert Hall | Events-Manager list + detail; no prices; "Verlegt"    |
| Mikropol                    | https://mikropol-berlin.de/                                 | Club         | Events-Manager list + detail; "verlegt in den …"      |
| Modus Berlin                | https://modus-berlin.de/events                              | Club         | List + detail; rendered date wins over stale slug     |
| Monarch                     | https://www.kottimonarch.de/                                | Bar          | PHP /programm.php; type + status inline in title      |
| MS Hoppetosse               | https://hoppetosse.berlin/                                  | Techno Club  | Shares the CdV listing; winter location only          |
| Neue Zukunft                | https://neue-zukunft.org/                                   | Club         | Elfsight Event Calendar widget API                    |
| OHM                         | https://ohmberlin.com/                                      | Techno Club  | Year-less dd/MM; only 1–3 nights listed at a time     |
| Panke Culture               | https://www.pankeculture.com/programme/                     | Club         | WordPress/Divi; upcoming list only; no event pages    |
| Parkbühne Wuhlheide         | https://www.wuhlheide.de/programm                           | Open Air     | October CMS; ISO date in URL; seasonal, sold-out      |
| Privatclub                  | https://privatclub-berlin.de/                               | Club         | Rich detail pages; genre, presale + AK prices         |
| Quasimodo                   | https://quasimodo.club/events                               | Club         | Events-Manager; .club domain; genre tags + prices     |
| Renate                      | https://www.renate.cc/                                      | Techno Club  | Homepage programme; per-floor lineups, no times       |
| Ritter Butzke               | https://club.ritterbutzke.com/events                        | Techno Club  | Modus codebase, own template; stale slug dates        |
| Roadrunner's Paradise       | http://www.roadrunners-paradise.de/                         | Bar          | Retro HTML; rich data; year missing on some dates     |
| Säälchen                    | https://www.holzmarkt.com/kalender                          | Concert Hall | Drupal; shared calendar filtered by location          |
| Schokoladen                 | https://www.schokoladen-mitte.de/                           | Club         | Laravel; anchor-based events; genre inside title      |
| SO36                        | https://www.so36.com/tickets                                | Club         | Cookie wall bypassed via Ticket-Toaster shop          |
| Soda Club                   | https://www.soda-berlin.de/events                           | Club         | disco2app CMS; `MusicEvent` JSON-LD on details        |
| Sonnenraum                  | https://clubdervisionaere.com/programm                      | Club         | Shares the CdV listing; Monday live residency         |
| Supamolly                   | https://www.supamolly.de/?p=programm                        | Club         | Retro PHP; row id is the date stamp; no prices        |
| Tempodrom                   | https://www.tempodrom.de/programm-und-tickets/              | Concert Hall | schema.org `Event` JSON-LD; whole programme           |
| Theater im Delphi           | https://theater-im-delphi.de/programm/                      | Concert Hall | One row per performance; prices only in a leak        |
| Tresor                      | https://tresorberlin.com/club/events/                       | Techno Club  | WordPress; floor-grouped lineup; detail pages         |
| Uber Arena                  | https://www.uber-arena.de/events/all                        | Arena        | AEG CMS; list + detail; no sport imported             |
| Uber Eats Music Hall        | https://www.uber-eats-music-hall.de/events/all              | Concert Hall | Shares the Uber Arena parsers; month names, no cats   |
| UFO im Velodrom             | https://www.velomax.de/events                               | Concert Hall | Shares the VELOMAX listing                            |
| Urania                      | https://www.urania.de/kalender/                             | Concert Hall | One house; no source attributes an event to a hall    |
| Urban Spree                 | https://www.urbanspree.com/program/                         | Club         | MODX; listing descending + paginated; walks pages     |
| Velodrom                    | https://www.velomax.de/events                               | Arena        | Shares the VELOMAX listing; Microdata details         |
| VOID Club                   | https://www.void-club.de/                                   | Techno Club  | Hand-coded Bootstrap; year from weekday; 2 rooms      |
| Wild at Heart               | https://www.wildatheartberlin.de/                           | Bar          | Retro frameset; concerts.php; year from weekday       |
| Zenner                      | https://zenner.berlin/programm                              | Club         | Gatsby/Sanity page-data JSON; UTC dates; archive      |
| Zitadelle                   | https://citadel-music-festival.de/events                    | Open Air     | Festival site; WordPress/EM; summer season only       |

70 importer classes cover 71 sources: only Kantine am Berghain has no class of its own, sharing the Berghain importer outright. Three other groups share a
*listing and parser* while keeping one thin `@Component` per venue, so they do not reduce the count — Club der Visionäre, Sonnenraum and MS Hoppetosse; the
three Velomax halls; and Uber Arena with the Uber Eats Music Hall.

## 🔨 Ready to implement

Analyzed and scrapable — the candidates for the next `/scaffold-importer` runs. **Priority** reflects data richness and effort, not venue importance.

These eight — and VOID Club, since [imported](#-imported) — were analyzed on **4 August 2026**, working down the [Unanalyzed](#-not-analyzed-yet) table in
RA-event-count order: 22 sites were opened, and these are the ones that server-render a dated programme. Each row was confirmed by fetching the raw HTML — no
headless browser, per [ADR-007](adr/ADR-007_WEB_SCRAPING_STRATEGY.md) — and reading the events out of it.

| Priority | Name           | URL                                            | Type         | Why / what it needs                                                                                                                                |
|:--------:|----------------|------------------------------------------------|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
|   High   | Crack Bellmer  | https://www.crackbellmer.de/program/this-month | Bar          | Webflow; 67 events on one page — `this-month` and `next-month` serve identical HTML, so the month nav is client-side and one fetch gets everything |
|   High   | Maaya          | https://maaya.de/                              | Club         | WordPress; "NEXT DATES" on the homepage; dd.MM.yyyy + time range, free/ticketed flag, Xceed ticket link                                            |
|   High   | silent green   | https://www.silent-green.net/                  | Concert Hall | TYPO3 + `ld+json`; month headings, full dd.MM.yyyy, date ranges for multi-day runs                                                                 |
|  Medium  | gART.n         | https://www.gartn.xyz/                         | Techno Club  | "UPCOMING" block — weekday, dd.MM., time range, title, lineup; year-less, derive as Arcanoa does                                                   |
|  Medium  | Club OST       | https://clubost.de/                            | Techno Club  | Homepage programme, dates carry the year + start time; detail text is a "More Infos coming soon" stub, tickets link to RA                          |
|  Medium  | migas          | https://migas.berlin/program/                  | Bar          | Upcoming/past tabs; weekday, dd.MM, time, title, type and description; year-less                                                                   |
|  Medium  | Eschschloraque | https://www.eschschloraque.de/                 | Bar          | Drupal 7; full German dates + `ab HH Uhr` start, bilingual description                                                                             |
|   Low    | Klunkerkranich | https://klunkerkranich.org/                    | Bar          | WordPress + `ld+json`, full dates — but only ~6 days ahead. A short rolling horizon like OHM's, so thin but truthful                               |

*A theater, comedy or arena-scale room is in scope, not just live-music clubs. Bar jeder Vernunft set that precedent — its programme is imported, with the
venue's own genre deciding whether a night is a concert or a staged show. That precedent does **not** extend to classical concerts and orchestras: those are
still an open question in [../TODO.md](../TODO.md), because the data shape differs — orchestra plus conductor plus soloists rather than a headliner with
support — so the artist model needs a decision before any such venue leaves [Blocked](#-blocked--deferred).*

## ⛔ Blocked / deferred

Analyzed, but there is nothing worth importing today. Revisit when the blocker changes — a redesigned website, adopting a headless browser (deferred
per [ADR-007](adr/ADR-007_WEB_SCRAPING_STRATEGY.md)), or applying the Havanna-style derived-occurrence approach to undated recurring nights.

**Last re-checked 3 August 2026**, every entry. Two came out of it: Panke Culture, which now publishes a dated programme and has since been
[imported](#-imported), and the RBB Sendesaal, whose concerts sit in a ROC calendar that turned out to be server-rendered and venue-attributed. The Sendesaal is
back here as of 4 August — **not on scraping, which works, but on scope**: it is an orchestral house, and whether classical concerts belong in this app is still
an open question (see *"add classical concerts / orchestras?"* under **Open questions — coverage scope** in [../TODO.md](../TODO.md), which names this venue).
Answer that question and the importer is a short job; the ROC calendar is server-rendered and attributes each concert to a venue, so
`.ConcertListItem-location` is the only filter needed. Five entries had their blocker *change* without unblocking, which is worth knowing before anyone spends
effort on them:

- **Fluxbau** and **The Pearl** are no longer JS-only — both render their programmes server-side now. Adopting a headless browser would not help either: Fluxbau
  publishes 2 dated events beside undated weekly series, and The Pearl exactly one. They are thin-programme problems now, not rendering ones.
- **Arena Berlin** moved to The Events Calendar, so scraping it would be trivial — but all 5 entries are trade fairs (deGUT, BUCHBERLIN, Einstieg Berlin). The
  blocker was never the markup.
- **Prachtwerk** gained a Programm page that is empty (Squarespace reports `itemCount: 0`). Its gigs are real but reach the web only through Loft's listing,
  which names Prachtwerk more often than any other house.
- **Loft** was blocked on thin, year-less dates; its redesign turned it into a full cross-venue promoter listing, so it now shares the promoter blocker below.

**Artliners Berlin**'s domain stopped resolving altogether. Bohnengold, OXI and Zuckerzauber still redirect to Facebook or Instagram — their HTTPS is broken, so
they answer only over `http://`.

**Promoter sources are deferred on a model limitation, not a scraping one.** Puschen, Trinity Music, Landstreicher Booking, Landstreicher Konzerte and — since
its 2026 redesign — Loft all publish clean, well-structured listings that name the venue per event: Puschen's 35 upcoming shows are spread over ~20 houses,
Loft's 135 over about the same. But an event's venue comes from its `event_source` row (`EventUpsertService.upsertAndCleanup(events, venueId, …)`), one venue
per source, so a promoter's events cannot be attached to the houses they actually play. Importing one today would file every show under a pseudo-venue *and*
duplicate what the venues' own importers already hold — ~30 of Puschen's 35 are at venues already imported. Unblocking them means resolving a venue per event
and de-duplicating against the venue-level sources; until then the promoter data reaches us anyway, as the `promoter` field on the venues' own events.

**A handful of tickets is not a programme.** Sisyphos is the case to reason from: its only web presence is a Shopify merch shop, whose `/pages/tickets`
carried 3 ticketed nights of a single recurring series (`generationS`, one per month) beside the T-shirts, while the club actually runs a programme every
weekend. Importing those 3 would not be a thin-but-truthful import like OHM's — where the venue publishes its real programme on a short rolling horizon — it
would present one series per month as if it were the whole programme. The bar is whether the source reflects what the venue is doing, not whether it yields a
non-zero count.

**The 4 August 2026 analysis of the RA candidates put 13 rows here**, against 9 that reached [Ready](#-ready-to-implement). They are worth reading as a group,
because the failure modes repeat and none of them is "the venue is too small":

- **The busiest venue on RA has no website at all.** Minimal Bar tops the Berlin listing with 58 events, and `minimal-berlin.de` 303-redirects to
  `minimal-berlin.geo.io` — a generic geo.io business-directory page with a stock bar photo and no programme. This is the sharpest case for RA as a source:
  the club's entire published programme exists only there.
- **Squarespace accounts for three of the thirteen.** Bar Neun, Unkompress and Weekend all serve a large page whose event content is client-side only — Bar
  Neun's 1.1 MB of HTML yields no event text at all. Prachtwerk above is the same story.
- **Two sites hand the programme back to RA.** Bulbul Berlin's "Program" button links to `ra.co/clubs/175191`, and its own page carries opening hours plus
  "Special dates (Check: RA)". VOID Club, by contrast, links to RA *for tickets* while still listing the events itself — which is why it is now
  [imported](#-imported).
- **A blog of past parties is not a programme.** Hafenbar Berlin server-renders 61 dated items, all of them write-ups of events that already happened (June,
  May, April 2026); `/events/` and `/veranstaltungen/` both 404.
- **Neue Nationalgalerie repeats the Hamburger Bahnhof result exactly** — the shared SMB TYPO3 calendar renders cleanly and is richly dated, but every entry is
  a Workshop, Gespräch or Öffentliche Führung. Its 11 RA events are concert bookings that never reach the museum's own calendar.

| Name                             | URL                                       | Type         | Blocker                                                  | Unblocked by               |
|----------------------------------|-------------------------------------------|--------------|----------------------------------------------------------|----------------------------|
| Minimal Bar                      | https://minimal-berlin.geo.io/            | Techno Club  | No own site; redirects to a geo.io business page         | RA as a source             |
| Sensorium                        | http://www.sensorium-club.com             | Techno Club  | Domain serves a 229-byte stub page                       | Site change / RA           |
| Insomnia                         | http://www.insomnia-berlin.de             | Club         | WAF returns 403 with an empty body to scripts            | Request headers / RA       |
| Hafenbar Berlin                  | https://www.hafenbar-berlin.de            | Bar          | WordPress blog of *past* parties; no programme           | Site change                |
| Bulbul Berlin                    | https://www.bulbulberlin.de               | Club         | Own site links out to RA for the programme               | RA as a source             |
| Bar Neun                         | http://barneun.de                         | Bar          | Squarespace; 1.1 MB of HTML, no event text               | Headless browser           |
| Unkompress                       | https://www.unkompress.berlin/            | Club         | Squarespace; event content is client-side only           | Headless browser           |
| Weekend                          | https://www.weekendclub.berlin/           | Club         | Squarespace; event content is client-side only           | Headless browser           |
| M-BIA                            | http://www.m-bia.de                       | Techno Club  | WordPress, but no dated content is rendered              | Site change / RA           |
| KREUZWERK                        | https://kreuzwerk.club/                   | Techno Club  | Address and hours only; no dated content                 | Site change / RA           |
| ACUD MACHT NEU                   | https://acudmachtneu.de/programm/         | Club         | Renders 2 exhibitions; club nights are JS-only           | Headless browser           |
| Neue Nationalgalerie             | https://www.smb.museum/                   | Concert Hall | SMB calendar is tours and workshops, not concerts        | Promoter feed              |
| Gestrandet a. d. Jannowitzbrücke | https://www.gestrandet-in-berlin.de/      | Open Air     | Site returns 502                                         | Site change                |
| Fluxbau                          | https://www.fluxfm.de/fluxbau             | Club         | Server-rendered now, but 2 dated events + series         | More events / occurrences  |
| Sage Club                        | https://www.sage-club.de/                 | Club         | TYPO3; `/programm/` renders navigation only              | Headless browser           |
| The Pearl                        | https://thepearl-berlin.de/               | Club         | `/programm/` renders now, but holds one event            | More events                |
| Prince Charles                   | https://princecharlesberlin.com/          | Club         | No own listings; links out to Resident Advisor           | RA as a source             |
| Artliners Berlin                 | —                                         | Club         | Domain no longer resolves; site gone                     | New site                   |
| Prachtwerk                       | https://www.prachtwerkberlin.com/         | Bar          | Has a Programm page now, but it is empty                 | Site change                |
| Wiener Blut                      | https://www.wienerblut.org/               | Bar          | Impressum-only page                                      | Site change                |
| Paloma                           | https://www.palomabar.de/                 | Bar          | Party names + DJ lineups but **no dates**                | Havanna-style occurrences  |
| Loft                             | https://loft.de/                          | Promoter     | Cross-venue; one venue per source (see note)             | Per-event venue resolution |
| Greyzone Tickets                 | https://www.greyzone-tickets.de/          | Promoter     | Contact info only; ticket service, not a listing         | —                          |
| Landstreicher Booking            | https://landstreicher-booking.de/         | Promoter     | Cross-venue; one venue per source (see note)             | Per-event venue resolution |
| Landstreicher Konzerte           | https://landstreicher-konzerte.de/        | Promoter     | Cross-venue, cross-city; also has `/venue/` pages        | Per-event venue resolution |
| Puschen                          | https://puschen.net/berlin/               | Promoter     | Cross-venue; one venue per source (see note)             | Per-event venue resolution |
| Trinity Music                    | https://trinitymusic.de/                  | Promoter     | Cross-venue; one venue per source (see note)             | Per-event venue resolution |
| Arena Berlin                     | https://www.arena.berlin/veranstaltungen/ | Concert Hall | Tribe calendar now, but trade fairs only                 | Site change / promoter     |
| Frannz Salon                     | https://frannz.eu/                        | Club         | Not a separate listing; a floor of Frannz nights         | Covered by FRANNZ          |
| Kesselhaus                       | https://www.kesselhaus.net/               | Concert Hall | Angular PWA app shell; no JSON endpoint found            | Headless browser           |
| Maschinenhaus                    | https://www.kesselhaus.net/               | Concert Hall | Shares the Kesselhaus app — same blocker                 | Headless browser           |
| Passionskirche                   | —                                         | Concert Hall | No own website (akanthus.de lapsed to spam)              | Site change / promoter     |
| Theater des Westens              | https://www.stage-entertainment.de/       | Theater      | Stage portal; one musical, dates in ticket shop          | Scope decision             |
| RBB Sendesaal                    | https://www.roc-berlin.de/kalender/       | Concert Hall | Scrapable; deferred pending the classical scope decision | Scope decision             |
| Zentraler Festplatz              | https://berliner-festplatz.de/            | Open Air     | Rental ground; "Events" page is social embeds            | Site change                |
| ://about blank                   | https://aboutblank.li/                    | Techno Club  | `/next` carries no events in the HTML                    | Site change / RA           |
| Bohnengold                       | https://bohnengold.de/                    | Bar          | Domain redirects to Facebook                             | Site change                |
| C115                             | https://www.c115.club/                    | Techno Club  | Mailing-list splash page; no programme                   | Site change / RA           |
| ELSE                             | —                                         | Techno Club  | No own website; listings only on RA                      | RA as a source             |
| Hamburger Bahnhof                | https://www.smb.museum/                   | Open Air     | Museum programme is guided tours, not concerts           | Promoter feed              |
| KitKatClub                       | https://www.kitkatclub.org/               | Techno Club  | News-style prose; series live on external sites          | Site change                |
| Lokschuppen                      | https://lokschuppen-berlin.com/           | Techno Club  | Readymag site; the content is JS-only                    | Headless browser           |
| OXI & OXI Garten                 | https://oxi-club.de/                      | Techno Club  | Domain redirects to Instagram                            | Site change                |
| RSO                              | https://rso.berlin/                       | Techno Club  | Domain returns 404; no own site found                    | Site change / RA           |
| Sisyphos                         | https://www.sisyphos-berlin.net/          | Techno Club  | Shop-only site; 3 ticketed nights, no programme          | RA as a source             |
| SchwuZ                           | https://www.schwuz.de/                    | Techno Club  | Between locations; ~2 guest events listed                | New venue / site change    |
| Sisyfass                         | —                                         | Bar          | No website; Instagram and RA only                        | Site change                |
| Strandbad Grünau                 | https://strandbadgruenau.de/              | Open Air     | `/events/` is rental marketing, not a programme          | Promoter feed              |
| Zuckerzauber                     | https://zuckerzauber.info/                | Bar          | Domain redirects to Facebook                             | Site change                |

## ❓ Not analyzed yet

New candidates land here first: check for a server-rendered programme, then move the row into [Ready](#-ready-to-implement) or
[Blocked](#-blocked--deferred). **None of the rows below has been opened yet** — the URL is recorded, nothing more. The URL is whatever the source named as the
venue's own site, so some are Instagram or Facebook pages, which the [Blocked](#-blocked--deferred) list already shows to be dead ends; those rows are likely to
land there too.

The 4 August 2026 sweep put **70 candidates here, and the same day's analysis moved 22 of them out** — the 22 with the highest RA event counts and a real own
website. 9 reached [Ready](#-ready-to-implement), 13 went to [Blocked](#-blocked--deferred); a 41 % hit rate, which is the number to expect when working further
down this table. The 48 that remain are mostly the ones whose recorded URL is an Instagram or Facebook page, or none at all.

Where the 70 came from, and what was deliberately left out:

- **Resident Advisor** (<https://de.ra.co/events/de/berlin>) — 1142 events at 201 distinct venues over the 4 August – 30 September 2026 window, read from the
  site's own `eventListings` GraphQL query (Berlin is `areas.eq: 34`). This puts a number on what "RA as a source" is worth, which the techno-club note below
  has called the biggest unblocker left: **66 of the 201 venues are new to this document**, and RA's own busiest Berlin room — Minimal Bar, 58 events — was not
  recorded here at all. Analysis then found Minimal Bar has no website of its own, which makes it the strongest single argument for importing RA directly; it
  now sits in [Blocked](#-blocked--deferred).
- **The promoter listings** — Loft, Puschen, Landstreicher Booking and Trinity Music, re-read the same day. They added 4 venues RA did not surface, all of them
  seated or open-air houses rather than clubs. Trinity Music's 48-venue directory yielded nothing new, as expected: it was worked through in full already.
  Chasing down the Gärten der Welt URL surfaced a promoter this document had missed — **Landstreicher Konzerte**, a separate outfit from Landstreicher Booking,
  now filed under [Blocked](#-blocked--deferred) on the same per-event-venue limitation.

**Excluded on purpose, so a later sweep does not re-litigate them.** RA venues with a single event in the window, unless a promoter listed them too — a one-off
booking is not evidence of a programme, and the [Sisyphos rule](#-blocked--deferred) applies. Also every `TBA …` pseudo-venue (about 60 events: secret
locations, Telegram-only addresses, boat terminals), bare addresses and landmarks used as festival grounds (`Straße des 17. Juni`, `Brandenburger Tor`,
`Tempelhof Airport`), hotels and hostels, and venues outside Berlin that RA files under the Berlin area anyway (Waschhaus in Potsdam, Völklingen Ironworks in
Saarland).

The **Events** column is the RA count for that window — a rough proxy for how much a working importer would return, and the best priority signal available
before analysis. A `—` means the venue came only from a promoter listing.

| Name                             | URL                                            | Type         | Events | Seen on               |
|----------------------------------|------------------------------------------------|--------------|-------:|-----------------------|
| DNA. CLUB — urban Space          | —                                              | Club         |     23 | RA                    |
| Der Weiße Hase                   | —                                              | Club         |     17 | RA                    |
| Giri                             | https://www.instagram.com/giri.berlino/        | Bar          |     17 | RA                    |
| Birgit (Birgit & Bier)           | https://www.facebook.com/BirgitundBier         | Techno Club  |     15 | RA                    |
| Prisma                           | https://www.instagram.com/prisma.berlin        | Club         |     11 | RA                    |
| Spielbank Berlin                 | https://www.spielbank-berlin.de                | Other        |      9 | RA                    |
| Haus der Visionäre               | —                                              | Bar          |      8 | RA                    |
| 8MM                              | http://www.8mmbar.de/                          | Bar          |      5 | RA                    |
| Marmorbar                        | https://www.instagram.com/marmor_bar/          | Bar          |      5 | RA                    |
| Ikii                             | https://www.instagram.com/ikii.berlin/         | Bar          |      5 | RA                    |
| Atemporal                        | https://www.instagram.com/atemporal_projects/  | Club         |      5 | RA                    |
| Morphine Raum                    | http://www.morphinerecords.com                 | Club         |      4 | RA                    |
| Süss war gestern                 | https://www.facebook.com/suesswargestern       | Bar          |      4 | RA                    |
| Monster Ronson's Ichiban Karaoke | https://www.karaokemonster.de/                 | Bar          |      3 | RA                    |
| Wendel                           | https://www.nstp.de/nstp/frameset-wendel.htm   | Bar          |      3 | RA                    |
| Funkhaus Berlin                  | http://www.funkhaus-berlin.net                 | Concert Hall |      3 | RA                    |
| Beate Uwe                        | http://www.beate-uwe.de                        | Club         |      3 | RA                    |
| Jonny Knüppel                    | https://www.facebook.com/jonnyknueppel/        | Bar          |      3 | RA                    |
| Fitzroy                          | https://fitzroy-berlin.de/                     | Club         |      3 | RA                    |
| Backsteinboot                    | https://www.facebook.com/backsteinboot/        | Club         |      3 | RA                    |
| Kaos Berlin                      | http://kaosberlin.de                           | Techno Club  |      3 | RA                    |
| Œlgarten                         | https://www.instagram.com/oel.garten/          | Open Air     |      3 | RA                    |
| Rough Trade Berlin               | https://www.roughtrade.com/en-de/stores/berlin | Other        |      3 | RA                    |
| Rosie's Bar                      | —                                              | Bar          |      3 | RA                    |
| Insel der Jugend                 | http://www.inselberlin.de/                     | Open Air     |      2 | RA                    |
| Kulturbrauerei Open Air          | https://www.kulturbrauerei-berlin.de           | Open Air     |      2 | RA, Landstr. Konzerte |
| Tausend                          | https://www.tausendberlin.de                   | Bar          |      2 | RA                    |
| Emma Pea                         | https://emmapea.com                            | Bar          |      2 | RA                    |
| HÖR Berlin                       | https://www.facebook.com/hoerberlin/           | Other        |      2 | RA                    |
| Bredouille                       | https://bredouille-bar.com/                    | Bar          |      2 | RA                    |
| Mena Berlin                      | —                                              | Club         |      2 | RA                    |
| Atelier Rooftop                  | —                                              | Club         |      2 | RA                    |
| Coco Boule                       | —                                              | Bar          |      2 | RA                    |
| YSY                              | http://ysyberlin.de                            | Club         |      2 | RA                    |
| Phantom Bar Berlin               | —                                              | Bar          |      2 | RA                    |
| The Door Club                    | https://thedoor.club/                          | Club         |      2 | RA                    |
| KINDL                            | https://www.kindl-berlin.com/                  | Concert Hall |      2 | RA                    |
| Containerhafen                   | —                                              | Open Air     |      2 | RA                    |
| Golden Flamingo                  | http://goldenflamingo.de/                      | Open Air     |      2 | RA                    |
| DSTRKT Club Berlin               | http://dstrkt.de                               | Club         |      2 | RA                    |
| FOUND                            | https://foundberlin.com/                       | Club         |      2 | RA                    |
| ROSA                             | —                                              | Club         |      2 | RA                    |
| Beach Neukölln                   | https://www.beach-neukoelln.de/                | Open Air     |      1 | RA, Loft              |
| RAW-Gelände                      | —                                              | Open Air     |      1 | RA, Loft              |
| Gärten der Welt (Arena)          | https://www.gaertenderwelt.de/veranstaltungen/ | Open Air     |      — | Loft, Landstr. Konz.  |
| Colosseum                        | https://www.colosseumberlin.com/               | Concert Hall |      — | Loft                  |
| Kulturhaus Peter Edel            | https://www.peteredel.de/events/               | Concert Hall |      — | Loft                  |
| Genezarethkirche                 | https://www.mlg-neukoelln.de/                  | Concert Hall |      — | Puschen               |

**Type is a guess from the RA listing, not from the venue's own site** — several of these are bars that programme club nights, or the reverse. Fix the type when
the row is analyzed. `Other` marks the four that fit none of the existing types and may not belong in scope at all: Spielbank Berlin is a casino, HÖR Berlin is
a streaming radio booth whose "events" are broadcasts, Rough Trade is a record store with in-store gigs, and KINDL is an art centre.

Two source lists were worked through completely and are no longer reproduced here:

- The 48 venues of the **Trinity Music location directory** (<https://trinitymusic.de/locations>) — 17 were already imported, the other 31 are now filed above.
  The venue-level rows are what carries this list now: the **Trinity Music** promoter source itself is deferred (see
  [Blocked](#-blocked--deferred)), and a venue site usually yields richer data than a promoter listing anyway.
- The **techno-club cluster** — 26 clubs and bars, of which 13 turned out to be scrapable. The other 13 publish only through Instagram, Facebook or Resident
  Advisor, which makes **Resident Advisor as a source** the single biggest unblocker left on this list. The 4 August 2026 sweep above sizes that claim: RA
  carries 1142 Berlin events over eight weeks, and names five [Blocked](#-blocked--deferred) venues whose blocker is precisely "no own site" — ://about blank,
  ELSE, KitKatClub, OXI and RSO — beside 66 venues this document had never recorded. Note that RA is one source, not 66: importing it means one `event_source`
  spanning every venue, so it runs into **the same per-event venue resolution** the promoter listings are deferred on.

---

## TODO

Source-discovery and new-importer tasks are tracked in the backlog — see the **More importers** section of [../TODO.md](../TODO.md).
