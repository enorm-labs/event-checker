# The Datenschutz-Generator run

What was entered into [datenschutz-generator.de](https://datenschutz-generator.de/?dsgo=free) for the
review in [README.md](README.md), so the run can be repeated and the output compared.

**The free version carries an attribution duty.** Its own terms require a link to the generator
wherever the produced text is used. Nothing produced there is used on the site. This file exists so
that the second opinion is reproducible rather than remembered.

**The eligibility declaration was made on the maintainer's instruction, and it is true.** The free
version is for private persons, or for under 5.000 Euro of revenue a year. Event Junkie is
non-commercial and takes no money.

## What was entered

Only what the imprint already publishes.

| Field                           | Value                                                                                    |
| ------------------------------- | ---------------------------------------------------------------------------------------- |
| Verantwortlicher                | Norman Lange, c/o POSTFLEX PFX-665-382, Emsdettener Straße 10, 48268 Greven, Deutschland |
| E-Mail                          | `hello@event-junkie.de`                                                                  |
| URL des Impressums              | `https://event-junkie.de/de/legal/imprint`                                               |
| Geltungsbereich                 | `event-junkie.de`                                                                        |
| Vertretungsberechtigte Personen | empty — a sole controller has none                                                       |
| Telefonnummer                   | empty — the imprint publishes none                                                       |
| Datenschutzbeauftragte(r)       | none, for the reason §1 of the notice gives                                              |

## Which modules were selected

The quick selection, with every deviation from the generator's own default:

| Module                                              | State   | Why                                                                                                 |
| --------------------------------------------------- | ------- | --------------------------------------------------------------------------------------------------- |
| (Wohn)Sitz in Deutschland                           | **on**  | Off by default. The controller is in Germany                                                        |
| Hinweise Betroffenenrechte und Rechtsgrundlagen     | **on**  | Off by default, and it is the Art. 13 core                                                          |
| Einsatz von Dienstleistern und Datentransfers       | on      | Default. Hetzner, and the finding in README §1                                                      |
| Einhaltung Datensicherheit und Sicherheitsmaßnahmen | on      | Default. Kept so the clause can be compared                                                         |
| Betrieb einer Website                               | on      | Default                                                                                             |
| Einsatz von Cookies                                 | **off** | Default on. This site sets none                                                                     |
| Einholung Cookie-Opt-In                             | **off** | Default on. Nothing here rests on consent                                                           |
| Kontaktformular                                     | **off** | Default on. There is no form, only a `mailto:` address                                              |
| Everything else                                     | off     | Newsletter, shop, analytics, social media, fonts, maps, affiliate, reviews. None of it happens here |

## The output, and how it was obtained

[GENERATOR_OUTPUT.de.md](GENERATOR_OUTPUT.de.md) beside this file holds the document, verbatim.

**The run does not complete in an automated browser session.** After the inputs above it sits at _"Ihr
Dokument wird generiert"_ and never fills the output field. That is a property of the tool, not of
the inputs. The maintainer therefore pressed the button and pasted the result, which took about two
minutes.

To repeat it:

1. Open <https://datenschutz-generator.de/?dsgo=free> and enter the table above.
2. Set the modules as listed.
3. Confirm the eligibility declaration and press **Meine Datenschutzerklärung generieren**.
4. Copy the German text into `GENERATOR_OUTPUT.de.md`, in a fenced block, with the attribution line
   the free version requires.

**The stored run differs from the table above in two ways**, and the output file says so at the top.
The controller fields came back as the generator's placeholders, so no address of ours is in it. The
cookie and contact modules stayed at the generator's defaults. Both make the artefact more useful
rather than less. They show what a default run asserts about a site that sets no cookies and holds no
contact form.
