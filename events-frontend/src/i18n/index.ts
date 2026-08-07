import { createI18n } from 'vue-i18n'

import en from './messages/en.json'
import { DEFAULT_LOCALE, type Locale } from './locales'

/**
 * The i18n instance.
 *
 * `legacy: false` selects the Composition API (`useI18n`). The Legacy API is deprecated in
 * vue-i18n v11 and removed in v12, so this is not a style preference — writing against it now
 * makes v12 a version bump rather than a migration (ADR-013 §Decision 1).
 *
 * German messages arrive in Phase 3 of docs/LOCALISATION_PLAN.md. Until then only `en` is
 * registered, which is deliberate: this phase is plumbing, and a half-translated locale in the
 * catalogue would look like progress while silently falling back to English.
 */
type MessageSchema = typeof en

// The generics matter: without them vue-i18n infers the locale type from the keys of `messages`,
// which is `'en'` alone until German lands — so `setI18nLocale('de')` would not compile even
// though switching is the whole point. Declaring `Locale` up front keeps the type honest about
// what the site publishes rather than about what is currently loaded.
export const i18n = createI18n<[MessageSchema], Locale, false>({
  legacy: false,
  locale: DEFAULT_LOCALE,
  fallbackLocale: DEFAULT_LOCALE,
  messages: { en },
  // These two silence the console warnings for keys that exist in `en` but not yet elsewhere.
  // Keep them OFF once `de` exists — a missing translation should be noisy, and the key-parity
  // test in Phase 2 is what stops one shipping.
  missingWarn: true,
  fallbackWarn: true,
})

/**
 * Switches the active locale and reflects it on `<html lang>`.
 *
 * The `lang` attribute is not decoration: it tells assistive technology which language to
 * pronounce the page in, and an empty or wrong value is a WCAG 3.1.1 failure. It was `lang=""`
 * until the footer work fixed it — do not let it drift back.
 */
export function setI18nLocale(locale: Locale): void {
  i18n.global.locale.value = locale
  document.documentElement.setAttribute('lang', locale)
}
