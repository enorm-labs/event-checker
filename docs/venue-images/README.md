# Venue images

Which of the 86 venues has a photograph, where it came from, and who said so.

`REVIEWED.tsv` is the record. `scripts/commons-venue-images.py` reads it and writes the confirmed rows through the admin API. Everything below is why the file
exists rather than what it contains.

## The short version

- **A person judged every row.** No heuristic decided any of these. A file that a search returned, a coordinate agreed with and a name matched is still, very
  often, the building next door.
- **`CONFIRMED` means somebody looked at the picture and recognised the venue.** `REJECTED` means they looked and did not. Both are results. The second is what
  stops a search re-proposing the same wrong file.
- **The licence is not taken from this file.** The script reads it from the Commons API on every run. `licence_at_review` is here so a file relicensed since the
  review stops that venue instead of being written with a stale credit.

## Where the candidates came from

Two rounds. Wikimedia Commons ran first, over all 86 venues. Openverse ran second, over the 46 Commons could not picture.

**Round 1 — Wikimedia Commons.** Three searches, each weaker than the one before, and the hit rates say by how much.

| Found by         | Method                                                            | Reviewed | Confirmed | Hit rate |
| ---------------- | ----------------------------------------------------------------- | -------- | --------- | -------- |
| `wikidata-p18`   | The venue's Wikidata item, property `P18`                         | 22       | 17        | **77%**  |
| `commons-title`  | A Commons file whose title names the venue, constrained to Berlin | 42       | 21        | **50%**  |
| `commons-nearby` | A Commons file photographed within 150 m of the venue             | 22       | 2         | **9%**   |
|                  |                                                                   | **86**   | **40**    | 47%      |

**Proximity is worth about a ninth of a name.** That is the finding worth keeping. A coordinate is a weak way to find a picture of a building, and that
argues against a coordinate-only archive next.

**Round 2 — Openverse.** One search over the 46 venues round 1 left empty. Openverse aggregates Flickr, Europeana, Smithsonian and more behind one API,
so it answers "is there supply outside Commons" in a single pass.

| Found by           | Method                                                       | Reviewed | Confirmed | Hit rate |
| ------------------ | ------------------------------------------------------------ | -------- | --------- | -------- |
| `openverse-flickr` | An Openverse file whose title names the venue, plus `Berlin` | 46       | 4         | **9%**   |

**Flickr is not empty, but it is thin.** The probe found a candidate for 13 of the 46, and 127 candidates in total. A person kept 4. Most of the rest are
gig photographs. A band on the stage answers a different question than a venue card asks.

**Both denominators are the venues a round reviewed, not what `found_by` now counts.** Four venues were rejected in round 1 and confirmed in round 2, so their
rows moved to `openverse-flickr`. The column records where the picture in use came from, not every candidate a venue had. `Tresor` moved the same way
inside round 1. It had a `P18` image, Commons names no author for it, and a title search found the file used instead.

**Commons is exhausted.** All three searches it supports ran, and a person read every result. A fourth pass returns what these three rejected.

## What the columns hold

| Column              | Meaning                                                                                                                     |
| ------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `venue`             | The venue name as `http/importer/dev-seed.http` seeds it                                                                    |
| `decision`          | `CONFIRMED` or `REJECTED`. Every venue has one                                                                              |
| `file`              | How the archive names the picture: a Commons file name without the `File:` prefix, a Flickr photo id. Empty for a rejection |
| `licence_at_review` | The licence as the archive stated it, worded the way the `SPDX` map in the script keys on                                   |
| `file_page`         | The file's description page, which the rendered credit links to                                                             |
| `found_by`          | Which search proposed it                                                                                                    |

## What is still open

- **Two licences have no SPDX identifier the script accepts.** `FAL` on `Admiralspalast` and `Parkbühne Wuhlheide`, and a bare `Attribution` template on
  `Velodrom`. The script stops on each rather than guessing, so those three venues stay without a picture until the licences are read. `FAL` is copyleft and its
  share-alike is **not** the CC 4.0 § 2(a)(4) reading that ADR-019 and #1276 rely on. Owned by
  [#1281](https://github.com/enorm-labs/event-junkie/issues/1281).
- **The script writes Commons rows only.** An `openverse-flickr` row stops with a message saying so. Flickr states a licence through its own API, which
  needs a key. Openverse cannot answer instead, because it indexes Flickr rather than speaks for it, and ADR-019 asks for the licence at write time. The
  four confirmed Flickr pictures stay unwritten until that is built.
- **One Flickr picture carries the Public Domain Mark.** `Klunkerkranich`. The mark says a work is out of copyright, which a 2015 rooftop photograph is not, so
  the uploader more probably meant "take it". The credit names the photographer and links the source either way, which is why it is recorded rather than
  refused.
- **42 venues have nothing.** They are the hard half — a room in a courtyard, a bar with no frontage, a rooftop over a car park. Few people photograph them,
  and two archives say so. Asking the venues is [#808](https://github.com/enorm-labs/event-junkie/issues/808).
