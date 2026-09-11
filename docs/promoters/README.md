# Promoters — the review behind the names and websites

`REVIEWED.tsv` holds one row per promoter the site has (#328). The rows were read on staging on
2026-09-11, after the merges in `V023__merge_duplicate_promoters.sql`. Each row records what a
person decided about that promoter.

| Column           | What it holds                                                                                                                                                                                                                                                                                               |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `stored_name`    | The display name the row had on staging before V023                                                                                                                                                                                                                                                         |
| `slug`           | The row's slug after V023                                                                                                                                                                                                                                                                                   |
| `kind`           | `promoter`, `media` (a magazine or station that presents), `venue` (a venue crediting itself), `sponsor`, `party` (a series with no company behind it), `artist` (an act credited as its own promoter), `junk` (a fragment a scraper put in the slot), or `unverified` (a single credit nothing else names) |
| `website`        | The promoter's own site, opened and read. Empty where none was found, or none exists                                                                                                                                                                                                                        |
| `name`           | The spelling that site uses. Where it differs from `stored_name`, `PromoterNormalizer` pins it and V023 renames the row                                                                                                                                                                                     |
| `description_de` | One to three sentences on the promoter, in German: since when, what it books, where in Berlin. Our own prose, from the site's about page                                                                                                                                                                    |
| `description_en` | The same text in English, written by hand                                                                                                                                                                                                                                                                   |
| `events`         | Events behind the row on staging when it was read, all dates                                                                                                                                                                                                                                                |
| `note`           | What decided the row                                                                                                                                                                                                                                                                                        |

**What reads it.** `scripts/promoter-websites.py` writes the `website` and the two description
columns onto the promoters through the admin API, and nothing else. German goes in as the
description and English as the alternate, because German is the site's authoritative language
(ADR-013). The `name` column reaches the database through V023 and
the normalizer. A rename through the API changes the slug, and the next import would then mint
the old row again.

**What was checked, and how.** For every `promoter`, `media`, `venue` and `sponsor` row with a
website, that site was opened. Its title, logo text or imprint gave the spelling. The `note` says
where the site and the credit disagree. A `party`, `artist` or `junk` row was read from its events
and its venue, not from a site. An `unverified` row had one search that found nothing to cite. It
stays as the venue wrote it.

**The descriptions are written for the promoters with the most events first.** The first twenty
were drafted from each promoter's own about page and read by a person before they were added
(#328). A row without one shows no description, which is better than a sentence that says
nothing.

**Logos: two sites offer one, none states who may use it.** Every site in the table was read for
a press, brand or partner page (#328). FluxFM (`fluxfm.de/presse`) and Karsten Jahnke
(`kj.de/presse.html`) offer a logo download on a press page and say nothing about its use. The
other press pages take accreditation requests or sit behind a login (rbb). No promoter publishes a
usage grant, so this pass writes no `image_url`. The two offers are in the `note` column, for the
day the image decision in #328 is taken.

**Kinds that are not promoters stay in the table.** A magazine that presents a show is a credit
the venue prints and a reader may search for. The kind is what a later enrichment reads to decide
whether a row gets a description at all. Nothing deletes a row for its kind.

**Not read yet.** Bi Nuu, Lido, Astra and Festsaal ship each promoter's URL in the event JSON
their scrapers already parse. The importer could write that URL itself, for the credits this
table left empty.
