import type { EventDetail } from '@/api/types'
import type { Locale } from '@/i18n/locales'

/**
 * Which description to show, and what language to declare for it.
 *
 * The chrome is localised and the venue's prose is not. A German text under English chrome is not
 * a defect we can fix — it is what the venue published — but claiming it is English is. So the
 * page shows the visitor's locale where a text exists in it, and marks the language either way
 * (ADR-026).
 *
 * **This has to be one function shared by the view, the page meta and the structured data**, and
 * through those by the injector sidecar. `injector/__tests__/parity.spec.ts` asserts that the two
 * heads agree, and a second copy of this rule is how they stop agreeing.
 */
export interface ChosenDescription {
  /** The text to render. */
  text: string
  /** BCP 47 language for `lang` and `inLanguage`, or null when the language is unknown. */
  lang: Locale | null
  /** True when a machine produced this text, which the page must disclose. */
  machine: boolean
}

const isLocale = (value: string | null | undefined): value is Locale =>
  value === 'de' || value === 'en'

/**
 * The description for [locale], or null when the event has none.
 *
 * The locale's own text wins. Failing that the original is shown in its own language, because a
 * description a visitor cannot read still says who is playing and what kind of night it is.
 */
export function descriptionFor(event: EventDetail, locale: Locale): ChosenDescription | null {
  const original = event.description
  const originalLang = isLocale(event.descriptionLanguage) ? event.descriptionLanguage : null

  const alt = event.descriptionAlt
  const altLang = isLocale(event.descriptionAltLanguage) ? event.descriptionAltLanguage : null
  const altIsMachine = event.descriptionAltOrigin === 'MACHINE'

  if (original && originalLang === locale) return { text: original, lang: locale, machine: false }
  if (alt && altLang === locale) return { text: alt, lang: locale, machine: altIsMachine }
  if (original) return { text: original, lang: originalLang, machine: false }
  return null
}
