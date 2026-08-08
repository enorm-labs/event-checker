import { type Component, computed, defineAsyncComponent, defineComponent, h } from 'vue'
import { useI18n } from 'vue-i18n'

import { DEFAULT_LOCALE, type Locale } from '@/i18n/locales'

/**
 * Routes a page to a **separate component per language** rather than to one component that swaps
 * its prose through the message catalogue.
 *
 * Everywhere else in this app the opposite is right: UI labels belong in `src/i18n/messages`, and
 * the key-parity test exists to keep them honest. This helper is for the four pages where that
 * breaks down — the About page and the three legal pages, ~1,600 words carrying inline links,
 * `<strong>` and `<code>` *inside* their paragraphs. Putting
 * that in JSON means either HTML inside message strings or sentences shattered into fragments.
 *
 * For the legal pages there is a second reason, and it is the stronger one: an imprint and a
 * privacy notice are **documents**, reviewed as documents — possibly by someone who does not read
 * Vue. `ImprintView.de.vue` can be read start to finish as the German imprint. The same page
 * assembled from interpolated fragments cannot be reviewed at all without running it.
 *
 * The cost is real and worth naming: the two versions can drift, and no test can tell you that a
 * German sentence no longer means what the English one does. What *is* testable is that both carry
 * the mandatory elements, which is what `views/legal/__tests__/legalViews.spec.ts` checks per
 * locale, and that the facts they cite come from one place — {@link module:@/lib/legal}.
 *
 * Each locale stays in its own lazy chunk, so a German visitor never downloads the English
 * imprint.
 *
 * @param loaders one dynamic `import()` per published locale
 */
export function localisedView(loaders: Record<Locale, () => Promise<Component>>): Component {
  // Wrapped once at module scope rather than per render: `defineAsyncComponent` returns a new
  // component identity each call, and a fresh identity on every render would remount the page —
  // losing scroll position and re-running its setup on every reactive tick.
  const versions = Object.fromEntries(
    Object.entries(loaders).map(([locale, loader]) => [
      locale,
      defineAsyncComponent(loader as () => Promise<Component>),
    ]),
  ) as Record<Locale, Component>

  return defineComponent({
    name: 'LocalisedView',
    setup() {
      const { locale } = useI18n()
      // The router applies the URL's locale before this renders (`beforeEach` → `setI18nLocale`),
      // so this is already correct on first paint. The fallback covers nothing in the app and
      // everything in a unit test that mounts without a router.
      const version = computed(() => versions[locale.value as Locale] ?? versions[DEFAULT_LOCALE])
      return () => h(version.value)
    },
  })
}
