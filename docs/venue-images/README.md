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

Three searches, each weaker than the one before, and the hit rates say by how much.

| Found by         | Method                                                            | Reviewed | Confirmed | Hit rate |
| ---------------- | ----------------------------------------------------------------- | -------- | --------- | -------- |
| `wikidata-p18`   | The venue's Wikidata item, property `P18`                         | 23       | 18        | **78%**  |
| `commons-title`  | A Commons file whose title names the venue, constrained to Berlin | 41       | 20        | **49%**  |
| `commons-nearby` | A Commons file photographed within 150 m of the venue             | 22       | 2         | **9%**   |
|                  |                                                                   | **86**   | **40**    | 47%      |

**Proximity is worth about a ninth of a name.** That is the finding worth keeping. A coordinate is a weak way to find a picture of a building, and that
argues against a coordinate-only archive next.

**Commons is exhausted.** All three searches it supports ran, and a person read every result. A fourth pass returns what these three rejected.

## What the columns hold

| Column              | Meaning                                                                  |
| ------------------- | ------------------------------------------------------------------------ |
| `venue`             | The venue name as `http/importer/dev-seed.http` seeds it                 |
| `decision`          | `CONFIRMED`, `REJECTED` or `UNSURE`                                      |
| `file`              | The Commons file name, without the `File:` prefix. Empty for a rejection |
| `licence_at_review` | The Commons licence template as it read when the picture was judged      |
| `file_page`         | The file's description page, which the rendered credit links to          |
| `found_by`          | Which of the three searches proposed it                                  |

## What is still open

- **`Theater im Delphi` is `UNSURE`.** One row, neither written nor closed.
- **Two licences have no SPDX identifier the script accepts.** `FAL` on `Admiralspalast` and `Parkbühne Wuhlheide`, and a bare `Attribution` template on
  `Velodrom`. The script stops on each rather than guessing, so those three venues stay without a picture until the licences are read. `FAL` is copyleft and its
  share-alike is **not** the CC 4.0 § 2(a)(4) reading that ADR-019 and #1276 rely on.
- **45 venues have nothing.** They are the hard half — a room in a courtyard, a bar with no frontage, a rooftop over a car park. Few people photograph them,
  which is as true of Flickr as of Commons. Asking the venues is [#808](https://github.com/enorm-labs/event-junkie/issues/808).
