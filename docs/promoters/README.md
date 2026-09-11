# Promoters — the review behind the names and websites

`REVIEWED.tsv` holds one row per promoter the site has (#328). The rows were read on staging on
2026-09-11, after the merges in `V023__merge_duplicate_promoters.sql`. Each row records what a
person decided about that promoter.

| Column        | What it holds                                                                                                                                                                                                                                                                                               |
| ------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `stored_name` | The display name the row had on staging before V023                                                                                                                                                                                                                                                         |
| `slug`        | The row's slug after V023                                                                                                                                                                                                                                                                                   |
| `kind`        | `promoter`, `media` (a magazine or station that presents), `venue` (a venue crediting itself), `sponsor`, `party` (a series with no company behind it), `artist` (an act credited as its own promoter), `junk` (a fragment a scraper put in the slot), or `unverified` (a single credit nothing else names) |
| `website`     | The promoter's own site, opened and read. Empty where none was found, or none exists                                                                                                                                                                                                                        |
| `name`        | The spelling that site uses. Where it differs from `stored_name`, `PromoterNormalizer` pins it and V023 renames the row                                                                                                                                                                                     |
| `events`      | Events behind the row on staging when it was read, all dates                                                                                                                                                                                                                                                |
| `note`        | What decided the row                                                                                                                                                                                                                                                                                        |

**What reads it.** `scripts/promoter-websites.py` writes the `website` column onto the promoters
through the admin API, and nothing else. The `name` column reaches the database through V023 and
the normalizer. A rename through the API changes the slug, and the next import would then mint
the old row again.

**What was checked, and how.** For every `promoter`, `media`, `venue` and `sponsor` row with a
website, that site was opened. Its title, logo text or imprint gave the spelling. The `note` says
where the site and the credit disagree. A `party`, `artist` or `junk` row was read from its events
and its venue, not from a site. An `unverified` row had one search that found nothing to cite. It
stays as the venue wrote it.

**Kinds that are not promoters stay in the table.** A magazine that presents a show is a credit
the venue prints and a reader may search for. The kind is what a later enrichment reads to decide
whether a row gets a description at all. Nothing deletes a row for its kind.

**Not read yet.** Bi Nuu, Lido, Astra and Festsaal ship each promoter's URL in the event JSON
their scrapers already parse. The importer could write that URL itself, for the credits this
table left empty.
