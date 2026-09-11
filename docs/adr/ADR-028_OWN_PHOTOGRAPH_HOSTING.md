# ADR-028: Our own photographs live in a public bucket of their own

## Status

**Accepted (2026-09-11) — a photograph we take ourselves lives in a second, public object storage bucket. Its
credit links to a Photographs section on the About page. The existing image bucket stays private.**

**Does not supersede anything.** [ADR-019](ADR-019_VENUE_IMAGE_DELIVERY.md) decided how a third-party image reaches a
visitor, and this decision keeps every part of it. [ADR-020](ADR-020_IMAGE_PROCESSING.md) decided how an image is
processed, and an own photograph is derived the same way. Neither considered material we hold the copyright in, because
none existed.

**Not implemented yet.** Three things follow this document. The bucket, the About section, and the
`own-photograph` row the fetch script has to learn.
[#1285](https://github.com/enorm-labs/event-junkie/issues/1285) is where the photographs are collected.
`scripts/README.md` names the script that writes them.

## Context

Two automated rounds sourced a photograph for 44 of the 86 venues. Commons confirmed 40 of 86, and Openverse
confirmed 4 of the 46 that were left. The hit rates fall as the venues get harder.

| Round                       | Reviewed | Confirmed | Hit rate |
| --------------------------- | -------- | --------- | -------- |
| Wikimedia Commons           | 86       | 40        | 47%      |
| Openverse, over the 46 left | 46       | 4         | 9%       |

42 venues have no photograph. `docs/venue-images/README.md` records why they are the hard half: a room in a
courtyard, a bar with no frontage, a rooftop over a car park. Few people photograph them, and two archives say so.
Walking to a venue with a camera is the remaining route, and it makes us the copyright holder for the first time.

### The constraints a candidate had to satisfy

**The pipeline re-fetches every image.** `findDueForRefresh` in `CachedImageRepository.kt` asks each cached image's
`source_url` again on a cycle. An image whose URL stops answering becomes a failure and then a dead source. So the
file must sit at a stable `https` URL that answers indefinitely.

**The image bucket is private, for a stated reason.** `infra/bootstrap/storage.tf` says it holds third-party material
we serve, so a public bucket would also publish an origin whose URLs we do not control. ADR-019 §2.2 makes the BFF its
only reader.

**A credit must link to a licence statement.** `V020` makes `image_source_url` `NOT NULL` whenever an image is set,
and `ImageCreditLine` renders it as the link under the picture. Every row today points at a Commons file page or a
Flickr photo page, which is a page where somebody states the licence.

## Candidate options

**Upload the bytes to an admin endpoint.** The importer would store an original directly and invent a `source_url` for
it. It fails the first constraint: the refresh pass would ask that invented URL on its next cycle, fail, and eventually
treat the image as gone. Making the refresh skip such rows adds a second class of cached image to every query that
touches one.

**Put the originals in the repository.** `raw.githubusercontent.com` answers indefinitely and costs nothing to set up.
It also puts tens of megabytes of binaries in a repository that holds none, and every clone pays for them forever.

**Open the existing bucket.** One line in `storage.tf`, and it publishes other people's material at an origin we
advertise. The reason that bucket is private is written down, and this would overrule it for a case it never covered.

**A second bucket, public, for our own work.** One more resource, and the separation is visible: one bucket holds other
people's photographs and stays closed, one holds ours and is open.

## Decision

**A second bucket.** The reason that settled it is that the existing bucket's privacy has two justifications and
neither applies here. The material is not third-party, and we control its URL completely. A rule that reads
"private because third-party" must not be widened to cover material that is ours. The honest move is a second bucket
whose different rule is visible in `storage.tf`, not a loosened rule on the first.

`image_url` points at the object. The importer fetches, hashes, derives and serves it exactly as it does a Commons
thumbnail, with no new code path.

**The credit links to a Photographs section on the About page.** `image_source_url` points at a stable anchor there,
where we state who took the pictures and under what licence. That makes it a licence statement we publish, which is
what the column holds for every other row. A link back to the venue's own page on this site would be circular and
would state nothing.

**`found_by` gets the value `own-photograph`.** It says which rows are not third-party, which is the first question
anyone auditing the credits will ask.

**The licence is chosen per photograph and recorded in `REVIEWED.tsv`,** the same as any other row. This ADR does not
fix one. It must be an identifier the `LICENCES` map in `events-frontend/src/lib/imageCredit.ts` can render.

## Consequences

**A public bucket is a new thing to get wrong.** Anything written to it is world-readable the moment it lands. The
existing bucket's orphan sweep does not cover it, so an object removed from `REVIEWED.tsv` stays until somebody deletes
it.

**A published licence is irrevocable.** A Creative Commons grant cannot be withdrawn from copies already made. The
choice is a permanent one about our own work, which is why this ADR leaves it to a person per photograph.

**The About page becomes load-bearing.** It carries a link that a licence obligation depends on, in both languages. A
restructure that moves the anchor breaks the credit on every own photograph, and nothing currently tests that.

**Two buckets need two sets of credentials and two lifecycle answers.** The existing bucket deliberately has no expiry
rule, and this one needs the same decision made again rather than inherited.

**We become a copyright holder, and take on what that means.** A photograph of a venue can show people, which is a
personality-rights question the archives handled for us. `docs/SCRAPING_POSITION.md` covers material we republish, not
material we create.

## When to revisit

**If a third party ever asks us to host their photograph directly.** The rule here is "ours is public, theirs is
private", and a donated picture from a venue under #808 is neither. That case needs its own answer rather than a
stretched reading of this one.

## References

- [#1285](https://github.com/enorm-labs/event-junkie/issues/1285) — the 42 venues, the fill-out table, and the comment this ADR records
- [#1277](https://github.com/enorm-labs/event-junkie/issues/1277) — the archive rounds that reached 44 venues
- [#808](https://github.com/enorm-labs/event-junkie/issues/808) — asking the venues, the other route to the hard half
- [ADR-019](ADR-019_VENUE_IMAGE_DELIVERY.md) — how a third-party image reaches a visitor
- [ADR-020](ADR-020_IMAGE_PROCESSING.md) — how an image is processed
- `infra/bootstrap/storage.tf` — the three buckets and why each has the access it has
- `docs/venue-images/README.md` — the two rounds and their hit rates
