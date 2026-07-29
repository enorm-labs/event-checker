# Event Data Sources — Berlin

Overview of all venues, clubs, and promoters whose websites are potential sources for importing event data. Sources are grouped by **import status** so the
remaining work is visible at a glance. The **Comment** column records what matters for building or maintaining an importer — the platform, where the data lives,
and the parsing quirks. For an implemented importer, its KDoc and scraper tests are the authoritative field mapping; known defects live in
[IMPORTER_KNOWN_ISSUES.md](IMPORTER_KNOWN_ISSUES.md).

| Status                              | Meaning                                                                              | Count |
|-------------------------------------|--------------------------------------------------------------------------------------|------:|
| ✅ [Imported](#-imported)           | Importer implemented and scheduled                                                   |    28 |
| 🔨 [Ready](#-ready-to-implement)    | Website analyzed, listings are scrapable — these are the next importers to build     |    10 |
| ⛔ [Blocked](#-blocked--deferred)   | Website analyzed, but no usable listings (no programme page, JS-only, or too sparse) |    11 |
| ❓ [Unanalyzed](#-not-analyzed-yet) | No URL recorded yet — website still needs a first look                               |    24 |

"Website analyzed" also means the [data model](DATA_MODEL.md) was checked against that source — to date no source has required a schema change.

## ✅ Imported

| Name                        | URL                                                         | Type         | Comment                                           |
|-----------------------------|-------------------------------------------------------------|--------------|---------------------------------------------------|
| Alte Kantine Kulturbrauerei | https://alte-kantine.eu/                                    | Concert Hall |                                                   |
| AMT                         | https://www.club-amt.berlin                                 | Techno Club  | Webflow; /events → month pages                    |
| Astra Kulturhaus            | https://www.astra-berlin.de/                                | Concert Hall | schema.org `MusicEvent`; presale + door prices    |
| Badehaus                    | https://badehaus-berlin.com/                                | Club         | "AUSVERKAUFT"/"VERLEGT" labels; ticket + FB links |
| Berghain / Panorama Bar     | https://www.berghain.berlin/de/program/                     | Techno Club  | Server-rendered; list + detail                    |
| Bi Nuu                      | https://binuu.de/                                           | Club         | No genre or prices on site; only via ticket link  |
| Cassiopeia                  | https://cassiopeia-berlin.de/                               | Club         | Webflow; genre tags, sold-out / cancelled badges  |
| Clash Club                  | https://clash-berlin.de/                                    | Club         | WordPress; sparse — no times, prices or text      |
| Duncker Club                | https://www.dunckerclub.de/                                 | Club         |                                                   |
| Festsaal Kreuzberg          | https://festsaal-kreuzberg.de/de                            | Concert Hall | Nuxt/Wagtail SSR; `ld+json` empty; no prices      |
| Frannz Club                 | https://frannz.eu/                                          | Club         |                                                   |
| Gretchen                    | https://www.gretchen-club.de/                               | Club         |                                                   |
| Havanna                     | https://www.havanna-berlin.de/                              | Club         | Undated weekly nights; occurrences derived        |
| Hole 44                     | https://hole-berlin.de/                                     | Concert Hall | Events-Manager; "Abgesagt!" / "VERLEGT!" labels   |
| Junction Bar                | https://www.junction-bar.de/                                | Bar          | Static monthly pages; show times vary by weekday  |
| Kantine am Berghain         | https://www.berghain.berlin/de/program/kantine-am-berghain/ | Concert Hall | Shares BERGHAIN importer                          |
| Lido                        | https://www.lido-berlin.de/                                 | Concert Hall | Clean slugs; doors + start; "Ausverkauft" badge   |
| Loge                        | https://www.loge-berlin.org/                                | Club         | Wix; tickets on-site; support via "+" in title    |
| Madame Claude               | https://madameclaude.de/                                    | Bar          | WordPress `event` REST API (ACF)                  |
| Mikropol                    | https://mikropol-berlin.de/                                 | Club         | Events-Manager list + detail; "verlegt in den …"  |
| Monarch                     | https://www.kottimonarch.de/                                | Bar          | PHP /programm.php; type + status inline in title  |
| Neue Zukunft                | https://neue-zukunft.org/                                   | Club         | Elfsight Event Calendar widget API                |
| Privatclub                  | https://privatclub-berlin.de/                               | Club         | Rich detail pages; genre, presale + AK prices     |
| Roadrunner's Paradise       | http://www.roadrunners-paradise.de/                         | Bar          | Retro HTML; rich data; year missing on some dates |
| Schokoladen                 | https://www.schokoladen-mitte.de/                           | Club         | Laravel; anchor-based events; genre inside title  |
| SO36                        | https://www.so36.com/tickets                                | Club         | Cookie wall bypassed via Ticket-Toaster shop      |
| Supamolly                   | https://www.supamolly.de/?p=programm                        | Club         | Retro PHP; row id is the date stamp; no prices    |
| Wild at Heart               | https://www.wildatheartberlin.de/                           | Bar          | Retro frameset; concerts.php; year from weekday   |

27 importer classes cover 28 sources (Kantine am Berghain shares the Berghain importer).

## 🔨 Ready to implement

Analyzed and scrapable — the candidates for the next `/scaffold-importer` runs. **Priority** reflects data richness and effort, not venue importance.

| Priority | Name                  | URL                                | Type     | Why / what it needs                                        |
|:--------:|-----------------------|------------------------------------|----------|------------------------------------------------------------|
|   High   | Urban Spree           | https://www.urbanspree.com/        | Club     | Clean static list + detail; date, time, price, description |
|   High   | Arkaoda               | https://berlin.arkaoda.com/        | Bar      | Clean PHP listings + detail pages, images, RA links        |
|   High   | Trinity Music         | https://trinitymusic.de/           | Promoter | Cross-venue; rich statuses; also populates `promoter`      |
|   High   | Puschen               | https://puschen.net/berlin/        | Promoter | Consistent format; doors/start, sold-out, cross-venue      |
|  Medium  | Zenner                | https://zenner.berlin/             | Club     | Never analyzed in detail — review the site first           |
|  Medium  | Landstreicher Booking | https://landstreicher-booking.de/  | Promoter | Structured tour-date table; needs a Berlin-only filter     |
|  Medium  | Matrix Club Berlin    | https://www.matrix-berlin.de/      | Club     | Recurring nights; dates, times, genres, DJ lineups         |
|  Medium  | Bar jeder Vernunft    | https://www.bar-jeder-vernunft.de/ | Bar      | Multi-day ranges to expand; detail pages for times/prices  |
|   Low    | Soda Club Berlin      | https://www.soda-berlin.de/        | Club     | Recurring nights; list has title/date/image only           |
|   Low    | Arcanoa               | https://www.ssi-media.com/arcanoa/ | Bar      | 1990s HTML; scrapable but very low data quality            |

*Bar jeder Vernunft is theater/cabaret rather than live music — decide whether it belongs in scope before building it.*

## ⛔ Blocked / deferred

Analyzed, but there is nothing worth importing today. Revisit when the blocker changes — a redesigned website, adopting a headless browser (deferred
per [ADR-007](adr/ADR-007_WEB_SCRAPING_STRATEGY.md)), or applying the Havanna-style derived-occurrence approach to undated recurring nights.

| Name             | URL                               | Type     | Blocker                                          | Unblocked by              |
|------------------|-----------------------------------|----------|--------------------------------------------------|---------------------------|
| Fluxbau          | https://www.fluxfm.de/fluxbau     | Club     | Angular SPA; ~1 upcoming event at a time         | Headless browser / API    |
| Sage Club        | https://www.sage-club.de/         | Club     | TYPO3; `/programm/` renders navigation only      | Headless browser          |
| The Pearl        | https://thepearl-berlin.de/       | Club     | `/programm/` is JS-rendered; no events in HTML   | Headless browser          |
| Prince Charles   | https://princecharlesberlin.com/  | Club     | No own listings; links out to Resident Advisor   | RA as a source            |
| Artliners Berlin | https://artliners-berlin.com/     | Club     | Events posted as image flyers only               | Site change / OCR         |
| Prachtwerk       | https://www.prachtwerkberlin.com/ | Bar      | Squarespace landing page; no programme page      | Site change               |
| Panke Culture    | https://www.pankeculture.com/     | Club     | No programme page; events via social/newsletter  | Site change               |
| Wiener Blut      | https://www.wienerblut.org/       | Bar      | Impressum-only page                              | Site change               |
| Paloma           | https://www.palomabar.de/         | Bar      | Party names + DJ lineups but **no dates**        | Havanna-style occurrences |
| Loft             | https://loft.de/                  | Promoter | Very few events; no year on dates, no times      | Site change               |
| Greyzone Tickets | https://www.greyzone-tickets.de/  | Promoter | Contact info only; ticket service, not a listing | —                         |

## ❓ Not analyzed yet

No URL recorded and no website review done. Mostly the techno-club cluster. A first pass should record the URL, check for a server-rendered programme, and move
the row into one of the groups above.

| Name               | Type        |
|--------------------|-------------|
| Sisyfass           | Bar         |
| Bohnengold         | Bar         |
| Zuckerzauber       | ?           |
| Heideglühen        | ?           |
| Maxxim Club        | Club        |
| Strandbad Grünau   | Open Air    |
| C115               | Techno Club |
| Tresor             | Techno Club |
| Sisyphos           | Techno Club |
| Renate             | Techno Club |
| Kater              | Techno Club |
| KitKatClub         | Techno Club |
| Ritter Butzke      | Techno Club |
| ://about blank     | Techno Club |
| RSO                | Techno Club |
| ELSE               | Techno Club |
| Club der Visionäre | Techno Club |
| OHM                | Techno Club |
| OXI & OXI Garten   | Techno Club |
| ÆDEN               | Techno Club |
| Lokschuppen        | Techno Club |
| SchwuZ             | Techno Club |
| MS Hoppetosse      | Techno Club |
| Humboldthain Club  | Techno Club |
| Golden Gate        | Techno Club |

---

## TODO

Source-discovery and new-importer tasks are tracked in the backlog — see the **More importers** section of [../TODO.md](../TODO.md).
