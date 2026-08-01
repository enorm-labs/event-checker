# Event Data Sources — Berlin

Overview of all venues, clubs, and promoters whose websites are potential sources for importing event data. Sources are grouped by **import status** so the
remaining work is visible at a glance. The **Comment** column records what matters for building or maintaining an importer — the platform, where the data lives,
and the parsing quirks. For an implemented importer, its KDoc and scraper tests are the authoritative field mapping; known defects live in
[IMPORTER_KNOWN_ISSUES.md](IMPORTER_KNOWN_ISSUES.md).

| Status                              | Meaning                                                                              | Count |
|-------------------------------------|--------------------------------------------------------------------------------------|------:|
| ✅ [Imported](#-imported)           | Importer implemented and scheduled                                                   |    55 |
| 🔨 [Ready](#-ready-to-implement)    | Website analyzed, listings are scrapable — these are the next importers to build     |    16 |
| ⛔ [Blocked](#-blocked--deferred)   | Website analyzed, but no usable listings (no programme page, JS-only, or too sparse) |    35 |
| ❓ [Unanalyzed](#-not-analyzed-yet) | No URL recorded yet — website still needs a first look                               |     0 |

"Website analyzed" also means the [data model](DATA_MODEL.md) was checked against that source — to date no source has required a schema change.

## ✅ Imported

| Name                        | URL                                                         | Type         | Comment                                            |
|-----------------------------|-------------------------------------------------------------|--------------|----------------------------------------------------|
| ÆDEN                        | https://aedenberlin.com/                                    | Techno Club  | WordPress; /events → month pages; no prices        |
| Admiralspalast              | https://www.admiralspalast.theater/                         | Theater      | Contao; one event per performance row; no prices   |
| Alte Kantine Kulturbrauerei | https://alte-kantine.eu/                                    | Concert Hall |                                                    |
| AMT                         | https://www.club-amt.berlin                                 | Techno Club  | Webflow; /events → month pages                     |
| Arcanoa                     | https://www.ssi-media.com/arcanoa/veranst.htm               | Bar          | 1990s HTML; title/date only; year from weekday     |
| arkaoda                     | https://berlin.arkaoda.com/?/default/program                | Bar          | PHP router; only "Konser" typed; RA link in prose  |
| Astra Kulturhaus            | https://www.astra-berlin.de/                                | Concert Hall | schema.org `MusicEvent`; presale + door prices     |
| Badehaus                    | https://badehaus-berlin.com/                                | Club         | "AUSVERKAUFT"/"VERLEGT" labels; ticket + FB links  |
| Bar jeder Vernunft          | https://www.bar-jeder-vernunft.de/de/programm/kalender.html | Bar          | Neos; per-date JSON-LD; one show page per run      |
| Berghain / Panorama Bar     | https://www.berghain.berlin/de/program/                     | Techno Club  | Server-rendered; list + detail                     |
| Bi Nuu                      | https://binuu.de/                                           | Club         | No genre or prices on site; only via ticket link   |
| Cassiopeia                  | https://cassiopeia-berlin.de/                               | Club         | Webflow; genre tags, sold-out / cancelled badges   |
| Clash Club                  | https://clash-berlin.de/                                    | Club         | WordPress; sparse — no times, prices or text       |
| Club der Visionäre          | https://clubdervisionaere.com/programm                      | Techno Club  | WordPress; one listing, 3 rooms by CSS class       |
| Columbia Theater            | https://columbia-theater.de/                                | Concert Hall | WordPress; date in slug; status via `data-*` flag  |
| Columbiahalle               | https://www.columbiahalle.berlin/veranstaltungen.html       | Concert Hall | Contao; one page, month headings carry the year    |
| Duncker Club                | https://www.dunckerclub.de/                                 | Club         |                                                    |
| Festsaal Kreuzberg          | https://festsaal-kreuzberg.de/de                            | Concert Hall | Nuxt/Wagtail SSR; `ld+json` empty; no prices       |
| Frannz Club                 | https://frannz.eu/                                          | Club         |                                                    |
| Golden Gate                 | https://goldengate-berlin.de/                               | Techno Club  | Elementor; current Thu–Sat block only; door-only   |
| Gretchen                    | https://www.gretchen-club.de/                               | Club         |                                                    |
| Havanna                     | https://www.havanna-berlin.de/                              | Club         | Undated weekly nights; occurrences derived         |
| Heimathafen Neukölln        | https://heimathafen-neukoelln.de/                           | Concert Hall | WP REST + ACF; one post, many dated performances   |
| Hole 44                     | https://hole-berlin.de/                                     | Concert Hall | Events-Manager; "Abgesagt!" / "VERLEGT!" labels    |
| Humboldthain Club           | https://www.humboldthain.com/                               | Techno Club  | Elfsight widget API; weekly night expanded         |
| Huxleys Neue Welt           | https://huxleysneuewelt.de/events                           | Concert Hall | Events-Manager; ISO slug date; genre/promoter tags |
| Junction Bar                | https://www.junction-bar.de/                                | Bar          | Static monthly pages; show times vary by weekday   |
| Kantine am Berghain         | https://www.berghain.berlin/de/program/kantine-am-berghain/ | Concert Hall | Shares BERGHAIN importer                           |
| Kater                       | https://www.katerclub.de/                                   | Techno Club  | Homepage programme; ___ floor rules mark lineups   |
| LARK                        | https://larkberlin.com/events/                              | Club         | WP REST + ACF; post date is the event date        |
| Lido                        | https://www.lido-berlin.de/                                 | Concert Hall | Clean slugs; doors + start; "Ausverkauft" badge    |
| Loge                        | https://www.loge-berlin.org/                                | Club         | Wix; tickets on-site; support via "+" in title     |
| Madame Claude               | https://madameclaude.de/                                    | Bar          | WordPress `event` REST API (ACF)                   |
| Matrix Club Berlin          | https://www.matrix-berlin.de/                               | Club         | WordPress; month pages walked; DJs + door prices   |
| Max-Schmeling-Halle         | https://www.velomax.de/events                               | Arena        | Shared VELOMAX listing; no sport imported          |
| Maxxim Club                 | https://www.maxxim-berlin.de/partys                         | Club         | Wix Events warmup JSON; UTC dates; prices inline   |
| Mikropol                    | https://mikropol-berlin.de/                                 | Club         | Events-Manager list + detail; "verlegt in den …"   |
| Monarch                     | https://www.kottimonarch.de/                                | Bar          | PHP /programm.php; type + status inline in title   |
| MS Hoppetosse               | https://hoppetosse.berlin/                                  | Techno Club  | Shares the CdV listing; winter location only       |
| Neue Zukunft                | https://neue-zukunft.org/                                   | Club         | Elfsight Event Calendar widget API                 |
| Privatclub                  | https://privatclub-berlin.de/                               | Club         | Rich detail pages; genre, presale + AK prices      |
| Renate                      | https://www.renate.cc/                                      | Techno Club  | Homepage programme; per-floor lineups, no times    |
| Roadrunner's Paradise       | http://www.roadrunners-paradise.de/                         | Bar          | Retro HTML; rich data; year missing on some dates  |
| Schokoladen                 | https://www.schokoladen-mitte.de/                           | Club         | Laravel; anchor-based events; genre inside title   |
| SO36                        | https://www.so36.com/tickets                                | Club         | Cookie wall bypassed via Ticket-Toaster shop       |
| Soda Club                   | https://www.soda-berlin.de/events                           | Club         | disco2app CMS; `MusicEvent` JSON-LD on details     |
| Sonnenraum                  | https://clubdervisionaere.com/programm                      | Club         | Shares the CdV listing; Monday live residency      |
| Supamolly                   | https://www.supamolly.de/?p=programm                        | Club         | Retro PHP; row id is the date stamp; no prices     |
| Tempodrom                   | https://www.tempodrom.de/programm-und-tickets/              | Concert Hall | schema.org `Event` JSON-LD; whole programme        |
| Tresor                      | https://tresorberlin.com/club/events/                       | Techno Club  | WordPress; floor-grouped lineup; detail pages      |
| UFO im Velodrom             | https://www.velomax.de/events                               | Concert Hall | Shares the VELOMAX listing                         |
| Urban Spree                 | https://www.urbanspree.com/program/                         | Club         | MODX; listing descending + paginated; walks pages  |
| Velodrom                    | https://www.velomax.de/events                               | Arena        | Shares the VELOMAX listing; Microdata details      |
| Wild at Heart               | https://www.wildatheartberlin.de/                           | Bar          | Retro frameset; concerts.php; year from weekday    |
| Zenner                      | https://zenner.berlin/programm                              | Club         | Gatsby/Sanity page-data JSON; UTC dates; archive   |

54 importer classes cover 55 sources (Kantine am Berghain shares the Berghain importer; Club der Visionäre, Sonnenraum and MS Hoppetosse are three thin
importers over one shared listing and parser).

## 🔨 Ready to implement

Analyzed and scrapable — the candidates for the next `/scaffold-importer` runs. **Priority** reflects data richness and effort, not venue importance.

| Priority | Name                 | URL                                      | Type         | Why / what it needs                                        |
|:--------:|----------------------|------------------------------------------|--------------|------------------------------------------------------------|
|  Medium  | Metropol             | https://metropol-berlin.de/              | Concert Hall | One-pager; type + date + time, "VERLEGT"; no prices        |
|  Medium  | Modus Berlin         | https://modus-berlin.de/events           | Club         | Plain list + detail pages; the slug carries the date       |
|  Medium  | OHM                  | https://ohmberlin.com/                   | Techno Club  | dd/MM + time + lineup; year missing, derive from weekday   |
|  Medium  | Parkbühne Wuhlheide  | https://www.wuhlheide.de/programm        | Open Air     | Seasonal; ISO dates and "Ausverkauft", but no times        |
|  Medium  | Quasimodo            | https://quasimodo.club/                  | Club         | Programme is on `.club`; `.de` is a splash page            |
|  Medium  | Ritter Butzke        | https://club.ritterbutzke.com/           | Techno Club  | `/event/DDMMYY-Slug` links — same shape as Modus Berlin    |
|  Medium  | Säälchen             | https://www.holzmarkt.com/kalender       | Concert Hall | Drupal; filter by location + "Konzert"; prices inline      |
|  Medium  | Sisyphos             | https://www.sisyphos-berlin.net/         | Techno Club  | Shopify `/pages/tickets`: name, date, price; ticketed only |
|  Medium  | Uber Arena           | https://www.uber-arena.de/events/all     | Arena        | AEG site, server-rendered; 406 without a browser UA        |
|  Medium  | Uber Eats Music Hall | https://www.uber-eats-music-hall.de/     | Concert Hall | Same AEG platform as Uber Arena — one scraper shape        |
|  Medium  | Zitadelle            | https://citadel-music-festival.de/events | Open Air     | Festival site has the concerts; venue site only tours      |
|   Low    | Cosmic Comedy Club   | https://comedyclubberlin.com/events/     | Comedy Club  | `Event` JSON-LD via The Events Calendar; check scope       |
|   Low    | Heideglühen          | https://heidegluehen.berlin/aktuell/     | Techno Club  | One event at a time; prose date, set times per DJ          |
|   Low    | Humboldtsaal Urania  | https://www.urania.de/kalender/          | Concert Hall | One Urania programme; hall unnamed, mostly lectures        |
|   Low    | Kleistsaal Urania    | https://www.urania.de/kalender/          | Concert Hall | Same programme; the site makes no hall distinction         |
|   Low    | Theater Im Delphi    | https://theater-im-delphi.de/programm/   | Concert Hall | WordPress; page also dumps a PHP `var_dump` of records     |

*Several of the rooms above are theater, comedy, or arena-scale rather than live-music clubs — settle scope before building. Bar jeder Vernunft set the
precedent that such a room is in scope: its programme is imported, with the venue's own genre deciding whether a night is a concert or a staged show.*

## ⛔ Blocked / deferred

Analyzed, but there is nothing worth importing today. Revisit when the blocker changes — a redesigned website, adopting a headless browser (deferred
per [ADR-007](adr/ADR-007_WEB_SCRAPING_STRATEGY.md)), or applying the Havanna-style derived-occurrence approach to undated recurring nights.

**Promoter sources are deferred on a model limitation, not a scraping one.** Puschen, Trinity Music and Landstreicher Booking all publish clean, well-structured
listings that name the venue per event — Puschen's 35 upcoming shows are spread over ~20 houses. But an event's venue comes from its `event_source` row
(`EventUpsertService.upsertAndCleanup(events, venueId, …)`), one venue per source, so a promoter's events cannot be attached to the houses they actually play.
Importing one today would file every show under a pseudo-venue *and* duplicate what the venues' own importers already hold — ~30 of Puschen's 35 are at venues
already imported. Unblocking them means resolving a venue per event and de-duplicating against the venue-level sources; until then the promoter data reaches us
anyway, as the `promoter` field on the venues' own events.

| Name                  | URL                                 | Type         | Blocker                                          | Unblocked by               |
|-----------------------|-------------------------------------|--------------|--------------------------------------------------|----------------------------|
| Fluxbau               | https://www.fluxfm.de/fluxbau       | Club         | Angular SPA; ~1 upcoming event at a time         | Headless browser / API     |
| Sage Club             | https://www.sage-club.de/           | Club         | TYPO3; `/programm/` renders navigation only      | Headless browser           |
| The Pearl             | https://thepearl-berlin.de/         | Club         | `/programm/` is JS-rendered; no events in HTML   | Headless browser           |
| Prince Charles        | https://princecharlesberlin.com/    | Club         | No own listings; links out to Resident Advisor   | RA as a source             |
| Artliners Berlin      | https://artliners-berlin.com/       | Club         | Events posted as image flyers only               | Site change / OCR          |
| Prachtwerk            | https://www.prachtwerkberlin.com/   | Bar          | Squarespace landing page; no programme page      | Site change                |
| Panke Culture         | https://www.pankeculture.com/       | Club         | No programme page; events via social/newsletter  | Site change                |
| Wiener Blut           | https://www.wienerblut.org/         | Bar          | Impressum-only page                              | Site change                |
| Paloma                | https://www.palomabar.de/           | Bar          | Party names + DJ lineups but **no dates**        | Havanna-style occurrences  |
| Loft                  | https://loft.de/                    | Promoter     | Very few events; no year on dates, no times      | Site change                |
| Greyzone Tickets      | https://www.greyzone-tickets.de/    | Promoter     | Contact info only; ticket service, not a listing | —                          |
| Landstreicher Booking | https://landstreicher-booking.de/   | Promoter     | Cross-venue; one venue per source (see note)     | Per-event venue resolution |
| Puschen               | https://puschen.net/berlin/         | Promoter     | Cross-venue; one venue per source (see note)     | Per-event venue resolution |
| Trinity Music         | https://trinitymusic.de/            | Promoter     | Cross-venue; one venue per source (see note)     | Per-event venue resolution |
| Arena Berlin          | https://www.arena.berlin/           | Concert Hall | Calendar lists trade fairs only, no concerts     | Site change / promoter     |
| Frannz Salon          | https://frannz.eu/                  | Club         | Not a separate listing; a floor of Frannz nights | Covered by FRANNZ          |
| Kesselhaus            | https://www.kesselhaus.net/         | Concert Hall | Angular PWA app shell; no JSON endpoint found    | Headless browser           |
| Maschinenhaus         | https://www.kesselhaus.net/         | Concert Hall | Shares the Kesselhaus app — same blocker         | Headless browser           |
| Passionskirche        | —                                   | Concert Hall | No own website (akanthus.de lapsed to spam)      | Site change / promoter     |
| RBB Sendesaal         | https://www.roc-berlin.de/          | Concert Hall | No venue programme; classical dates on RSB / ROC | Scope decision             |
| Theater des Westens   | https://www.stage-entertainment.de/ | Theater      | Stage portal; one musical, dates in ticket shop  | Scope decision             |
| Zentraler Festplatz   | https://berliner-festplatz.de/      | Open Air     | Rental ground; "Events" page is social embeds    | Site change                |
| ://about blank        | https://aboutblank.li/              | Techno Club  | `/next` carries no events in the HTML            | Site change / RA           |
| Bohnengold            | https://bohnengold.de/              | Bar          | Domain redirects to Facebook                     | Site change                |
| C115                  | https://www.c115.club/              | Techno Club  | Mailing-list splash page; no programme           | Site change / RA           |
| ELSE                  | —                                   | Techno Club  | No own website; listings only on RA              | RA as a source             |
| Hamburger Bahnhof     | https://www.smb.museum/             | Open Air     | Museum programme is guided tours, not concerts   | Promoter feed              |
| KitKatClub            | https://www.kitkatclub.org/         | Techno Club  | News-style prose; series live on external sites  | Site change                |
| Lokschuppen           | https://lokschuppen-berlin.com/     | Techno Club  | Readymag site; the content is JS-only            | Headless browser           |
| OXI & OXI Garten      | https://oxi-club.de/                | Techno Club  | Domain redirects to Instagram                    | Site change                |
| RSO                   | https://rso.berlin/                 | Techno Club  | Domain returns 404; no own site found            | Site change / RA           |
| SchwuZ                | https://www.schwuz.de/              | Techno Club  | Between locations; ~2 guest events listed        | New venue / site change    |
| Sisyfass              | —                                   | Bar          | No website; Instagram and RA only                | Site change                |
| Strandbad Grünau      | https://strandbadgruenau.de/        | Open Air     | `/events/` is rental marketing, not a programme  | Promoter feed              |
| Zuckerzauber          | https://zuckerzauber.info/          | Bar          | Domain redirects to Facebook                     | Site change                |

## ❓ Not analyzed yet

Empty — every source recorded so far has been analyzed and sits in one of the groups above. New candidates land here first: record the URL, check for a
server-rendered programme, then move the row into [Ready](#-ready-to-implement) or [Blocked](#-blocked--deferred).

Two source lists were worked through completely and are no longer reproduced here:

- The 48 venues of the **Trinity Music location directory** (<https://trinitymusic.de/locations>) — 17 were already imported, the other 31 are now filed above.
  The venue-level rows are what carries this list now: the **Trinity Music** promoter source itself is deferred (see
  [Blocked](#-blocked--deferred)), and a venue site usually yields richer data than a promoter listing anyway.
- The **techno-club cluster** — 26 clubs and bars, of which 13 turned out to be scrapable. The other 13 publish only through Instagram, Facebook or Resident
  Advisor, which makes **Resident Advisor as a source** the single biggest unblocker left on this list.

---

## TODO

Source-discovery and new-importer tasks are tracked in the backlog — see the **More importers** section of [../TODO.md](../TODO.md).
