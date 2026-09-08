# ADR-027: Translation follows the display rule, so silence permits it

## Status

**Accepted (2026-09-08) — an event description may be machine-translated wherever it may be displayed. Only
`PROHIBITED` withholds, exactly as it does for the display of the text itself. Silence and `UNCLEAR` permit both.**

**Supersedes rule 3 of [ADR-026](ADR-026_MULTILINGUAL_EVENT_TEXT.md), taken on the same day.** That rule required a
per-source grant before any description was translated. Rules 1, 2 and 4 of ADR-026 stand. Its § 23 UrhG reasoning is
not withdrawn. We accept that risk knowingly rather than avoid it. ADR-026 keeps the reasoning in full, which is why
this is a separate document rather than an edit.

**Implemented in [#1213](https://github.com/enorm-labs/event-junkie/pull/1213).** The gate is the same
`translation_licence` column, and the same admin `PATCH` writes it. What changed is the value an operator sets on 84
sources, and the position behind it.

## Context

ADR-026 decided this on the same day, and its reasoning still stands. What changed is the weight of a consistency
argument that ADR-026 acknowledged and set aside.

**The project already reads silence as permitting the display of a description.** That is #283's fail-open rule.
`docs/SCRAPING_POSITION.md` §3.1 states it plainly: only `PROHIBITED` withholds, and both `UNCLEAR` and an unreviewed
source display. All 86 sources were read on 2026-08-28. The result was 83 `UNCLEAR`, 2 `PROHIBITED` and no `PERMITTED`.
So the site publishes 84 venues' promotional prose under § 16 and § 19a UrhG with no per-source justification. That is
deliberate: blanking every unreviewed source would remove material nobody objected to.

**ADR-026 treated translation differently, and this ADR removes that asymmetry.** Under that rule a German
description could be shown to an English-speaking visitor, because silence permitted the display. It could not be
translated for them, because silence did not permit the adaptation. The practical effect: #470 would translate nothing
for an indefinite period. Zero sources hold a grant, and #808's first batch is ten mails.

### The argument for keeping the asymmetry, which this ADR rejects

It is a real argument and ADR-026 makes it. § 23 (1) UrhG requires the author's consent for an adaptation, and a
translation is the textbook example. The e-recht24 boilerplate on most German venue sites names `Bearbeitung`
explicitly. § 51, § 44a and § 44b do not reach this use. A translation is therefore a third act on top of two the
project already cannot justify per source. It is a weaker position, not an equivalent one.

**Nothing in this ADR disputes that.** The reasoning above is why this document exists at all. The risk is taken
knowingly. A reader who wants the case against it should read ADR-026, not a summary here.

### What makes the risk bounded

Four things, and they were already true:

1. **The venue opt-out is same-day, and finer than the whole listing.** `SCRAPING_POSITION.md` §5 is the remedy.
   Since #805 a venue that minds one field loses only that field. `PROHIBITED` on `translation_licence` removes every
   machine translation for that source at once, and #1213 wires that clearing.
2. **The translation is disclosed, in both locales, with a link to the source.** ADR-026 rule 4 fixed the wording, and
   #1211 shipped it. A reader is never told the venue wrote this.
3. **The original is the record, and the translation is derived.** It carries the engine, its version and a hash of
   the source text. So it is regenerable and deletable, without touching what the venue published.
4. **The exposure is one more act against a corpus already published.** A venue that objects to a translation almost
   certainly objects to the text being there at all. §5 answers that objection either way.

## Decision

**Translation follows the display rule.** A description may be machine-translated when it may be displayed, which
means everywhere except a source whose `description_licence` or `translation_licence` is `PROHIBITED`.

- `translation_licence` stays as a column and as a per-source control. It is what a venue's objection is recorded in,
  and what #808 writes when a venue answers. Its default meaning changes, not its mechanism.
- **`PROHIBITED` still withholds and still clears.** Both fields do: prohibiting the description removes the
  translation with it, because the translation is derived from a text we then hold no justification for.
- The 84 sources that display a description are granted translation on both clusters, by `PATCH`, as the operator's
  act rather than as a migration. A licence verdict is data an operator sets, and #283 built the endpoint for it.

**The reason that settled it** is consistency. The project cannot hold two answers to "may we use this venue's
prose", one per act. It already answered the harder question yes, and built a same-day remedy for being wrong.

## Consequences

**Accepted costs:**

- **The site publishes derivative works of 84 venues' text without consent.** That is the whole of what this ADR
  decides. ADR-026 §The legal reading is the case against it. `SCRAPING_POSITION.md` §3.1 calls this the weakest point
  of the position, and it now is by a wider margin.
- **A first translation pass costs one run over the whole corpus.** About 2.7 million characters, a few dollars once
  per cluster, then under a million characters a month. `app.translation.max-per-run` bounds a single source's share.
- **Quality is now a public-facing risk rather than a theoretical one.** The plausibility checks in #1213 reject a
  summary and a lost proper noun. They do not catch a translation that is merely poor.
- **The go-live checklist gains a row.** A maintainer reads some of the German output before the flip, not after.

**Deliberately unchanged:**

- **Rules 1, 2 and 4 of ADR-026.** Titles are never translated, the publisher's own second language wins over a
  machine one, and every description declares the language it is written in.
- **`PROHIBITED` semantics**, in both directions.
- **#808.** Asking is still how an `UNCLEAR` becomes a `PERMITTED`, and a real grant is still worth more than this
  ADR's reading of silence.

## When to revisit

- **A venue objects to a translation.** This ADR is a bet against that. One objection is the opt-out working. A
  second one, or one that disputes the whole position rather than one listing, makes this worth reopening.
- **A qualified legal opinion on § 23**, through #279 or otherwise. This is our own reasoning and not a legal opinion,
  as `SCRAPING_POSITION.md` says of itself.
- **The project takes money for the listings.** Commercial use changes the balance for every act in
  `SCRAPING_POSITION.md`, and this is the weakest of them.

## References

- [ADR-026](ADR-026_MULTILINGUAL_EVENT_TEXT.md) — the decision this supersedes in part, and the § 23 UrhG reasoning
  kept intact there
- [SCRAPING_POSITION.md](../SCRAPING_POSITION.md) §3.1 and §5 — the display rule this now follows, and the opt-out
  that bounds it
- [#283](https://github.com/enorm-labs/event-junkie/issues/283) — the licence columns and the fail-open rule ·
  [#805](https://github.com/enorm-labs/event-junkie/issues/805) — the per-field remedy
- [#470](https://github.com/enorm-labs/event-junkie/issues/470) · [#1213](https://github.com/enorm-labs/event-junkie/pull/1213)
  — the pipeline this unblocks
- [#808](https://github.com/enorm-labs/event-junkie/issues/808) — the mail that still turns silence into a real answer
- [UrhG § 23](https://www.gesetze-im-internet.de/urhg/__23.html) · [§ 16](https://www.gesetze-im-internet.de/urhg/__16.html)
  · [§ 19a](https://www.gesetze-im-internet.de/urhg/__19a.html)
