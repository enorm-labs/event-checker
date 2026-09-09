# Legal review of the German privacy notice

The record of [#279](https://github.com/enorm-labs/event-junkie/issues/279). **This is our own review, not
a qualified legal opinion.** [#788](https://github.com/enorm-labs/event-junkie/issues/788) is where the
paid opinion lives, and this document does not replace it.

## The short version

1. **Read against the statute, article by article**, from [dsgvo-gesetz.de](https://dsgvo-gesetz.de/),
   and against the [Datenschutz-Generator](https://datenschutz-generator.de/) as a second opinion.
   [GENERATOR_INPUT.md](GENERATOR_INPUT.md) records what was entered there.
2. **Two findings.** One is a processor that runs in production and appears in no notice. The other is
   an article the notice never names although it governs most of the personal data on the site.
3. **The Art. 13 checklist holds.** Every mandatory item of Art. 13 (1) and (2) has a home in the
   German notice, and the unit test enforces that per language.
4. **Nothing here is a wording complaint.** The German reads well. What the review found is a gap
   between the document and what the system now does.

## 1. Finding: the translation engine is an undisclosed processor

**Severity: high. It is live on production and no notice mentions it.**

`deploy/clusters/production/helm-release.yaml` sets `translation.engine: anthropic`. Since 2026-09-08
the importer translates event descriptions through `https://api.anthropic.com`. Two kinds of personal
data leave the cluster on that path:

- **The description itself.** Event descriptions routinely name the performing artists. §4 of the
  notice calls such a name personal data.
- **The artist names, on purpose.** `DescriptionTranslationService.protectedTermsFor` collects the
  venue name and every artist on the bill. `AnthropicTranslationEngine` then puts them into the system
  prompt as _"These names must appear unchanged: …"_.

**What the notice says today, and why it is now wrong:**

> Daten werden an genau einen Dienstleister übermittelt — Auftragsverarbeiter mit einem Vertrag nach
> Art. 28 DSGVO

That sentence names Hetzner alone. A second processor is in the path, in a third country. The notice
does not name it. Art. 13 (1) (e) requires the recipients. Art. 13 (1) (f) requires the intention to
transfer to a third country, together with the basis for it.

**Not disclosed anywhere else either.** `docs/LEGAL.md` does not name Anthropic. §7.3a is the scope
declared for the Hetzner Art. 28 contract, and it says in terms that _"processing a category not
listed below means the AVV needs revisiting"_. ADR-026 and ADR-027 decide the copyright question and
say nothing about data protection.

**Closed by [#1233](https://github.com/enorm-labs/event-junkie/issues/1233) on 2026-09-09**, in the
order the finding set out. The translation stays on, which was the first decision.

**The Art. 28 contract turned out to exist already.** Anthropic's Data Processing Addendum is
incorporated into the Commercial Terms of Service that the API runs under. It makes Anthropic the
processor, and it carries the standard contractual clauses for the transfer. §5 of both notices now
names the processor, what reaches it, and Art. 46 (2) (c) as the basis. `LEGAL.md` §7.3a carries the same facts for the
processor forms. `legalViews.spec.ts` asserts both processors and the transfer mechanism in each
language, so a third one cannot arrive unnoticed.

**Nothing was written into the notice before those facts were checked.** A notice that claims a
contract nobody concluded is worse than one that omits the processor.

## 2. Finding: the notice never names Art. 14

**Severity: medium. The substance is present, the citation is not.**

The notice is built against Art. 13, and `legalViews.spec.ts` enforces the twelve items of Art. 13.
Art. 13 applies where the data comes from the data subject. **Most personal data here does not.** An
artist's name is read from a venue's website, and the artist never visited this site. That is Art. 14.

What Art. 14 adds over Art. 13 is one item, and the notice already carries it in prose:

| Art. 14 (2) (f)                                                                                                                  | Where §4 answers it                                                                                              |
| -------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| _"aus welcher Quelle die personenbezogenen Daten stammen und gegebenenfalls ob sie aus öffentlich zugänglichen Quellen stammen"_ | "sammelt öffentlich zugängliche Informationen … von den Websites der Locations, Veranstalter und Ticketanbieter" |

**Art. 14 (3) is the harder half, and Art. 14 (5) (b) is the answer.** The information is due within a
month, or at the first communication with the person. There is no communication with an artist and no
address to write to. Art. 14 (5) (b) relieves that where the effort is disproportionate, on condition
that the controller makes the information available to the public. A public privacy notice is exactly
that measure, so the position holds. **It should say so**, which is the change this review makes.

## 3. What the statute check confirmed

Read against [dsgvo-gesetz.de](https://dsgvo-gesetz.de/), item by item.

| Claim in the notice                                                | Verdict                                                                                                             |
| ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------- |
| Art. 6 (1) (f) for logs, with Recital 49 named                     | Correct, and Recital 49 does name network and information security                                                  |
| Art. 6 (1) (f) for event and artist data, with the interest stated | Correct. Art. 13 (1) (d) requires the interest to be named, and §4 names it                                         |
| No data protection officer, under Art. 37 and § 38 BDSG            | Correct on the stated facts: one person, no large-scale processing, no systematic monitoring                        |
| Rights list: Art. 15, 16, 17, 18, 20                               | Complete for Art. 13 (2) (b), which also requires the objection right — §6 carries it separately                    |
| Art. 21 objection, in its own paragraph with a bold heading        | Satisfies Art. 21 (4), which requires the notice to be explicit and separate from other information                 |
| Art. 77 complaint, naming three fora                               | Correct since the 2026-09-03 pass. Art. 77 (1) names residence, place of work and place of the alleged infringement |
| Art. 22, no automated decisions                                    | Correct. Translation is not a decision about a person                                                               |
| § 25 (2) Nr. 2 TDDDG for `theme` and `locale`                      | Correct. Both are strictly necessary for a setting the user asked for                                               |
| Retention: 14 days for logs, 30 and 35 days for backups            | Correct, and Art. 13 (2) (a) accepts criteria where a period is impossible — §4 uses a criterion for event data     |

## 4. What the generator produced, and what it did not

[GENERATOR_INPUT.md](GENERATOR_INPUT.md) has the inputs and the modules.
[GENERATOR_OUTPUT.de.md](GENERATOR_OUTPUT.de.md) is the document it produced, kept verbatim. Six
differences are worth the comparison, and each one runs the same way: the generator states the
common case, and our notice states this deployment.

| The generator says                                                                           | We say, and why                                                                                                                                   |
| -------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| _"Logfile-Informationen werden für die Dauer von maximal 30 Tagen gespeichert"_              | 14 days, because `ZO_COMPACT_DATA_RETENTION_DAYS` is 14. The generator's number is a ceiling for a site it knows nothing about                    |
| _"verarbeiten wir die IP-Adresse des Nutzers"_, and lists `IP-Adressen` among the log data   | The web server uses a log format that omits the address, so none is written. §7.5 holds that claim to the configuration                           |
| A full `Einsatz von Cookies` section, with consent management and two-year permanent cookies | No cookies at all. Two `localStorage` values under § 25 (2) Nr. 2 TDDDG, which the generator has no module for                                    |
| _"sobald die zugrundeliegenden Einwilligungen widerrufen werden"_ as the deletion trigger    | Nothing here rests on consent. Deletion follows the purpose ending, and §4 states a criterion rather than a period                                |
| An `Internationale Datentransfers` clause asserting DPF **and** standard contractual clauses | We could not make that claim today. It is the shape §1's finding needs once the processor question is decided, and only if it is true             |
| `Kategorien betroffener Personen: Kommunikationspartner. Nutzer.`                            | The largest category here is artists, who never visited the site. No generator module produces them, which is finding §2 seen from the other side |

Two more, unchanged from the 2026-09-03 comparison:

- **The generator always emits a `Sicherheitsmaßnahmen` clause**, and this run is no exception. Art. 32
  obliges the measures. It does not oblige a description of them in the notice, and a wrong
  description is a statement against interest. Still deliberately absent here.
- **Its rights list carries a `Widerrufsrecht bei Einwilligungen`.** There is no consent to withdraw.

**One thing the generator does better, and it is a citation style.** It writes _Art. 6 Abs. 1 S. 1
lit. f) DSGVO_ where the notice writes _Art. 6 Abs. 1 lit. f DSGVO_. Art. 6 (1) has one subparagraph
carrying the letters, so naming the sentence is the more precise German form. It is a refinement
rather than a defect, and it is not applied here.

## 5. What changed as a result

- §4 of both notices now names **Art. 14** and the public-availability measure under Art. 14 (5) (b).
- `LEGAL.md` §7.2 records that the checklist covers Art. 13 and that Art. 14 governs the artist data.
- `LEGAL.md` §7.8 records that the generator was run rather than only compared, and what was entered.
- The processor finding is an issue rather than an edit, for the reason §1 gives.
