<script lang="ts" setup>
/**
 * Switches between the published locales.
 *
 * Renders links rather than a `<select>` or a button: the locale lives in the URL (ADR-013
 * §Decision 2), so each option genuinely *is* a different address. That means middle-click and
 * "copy link" work, and a crawler can follow them — a JavaScript-only switcher would be invisible
 * to both.
 *
 * The current page is preserved across the switch, so changing language on the venues list keeps
 * you on the venues list rather than dumping you on the home page.
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { DEFAULT_LOCALE, isLocale, type Locale, LOCALES } from '@/i18n/locales'

// `useRoute()` yields undefined when no router is installed — component tests mount the footer
// without one. Degrading to the default locale keeps those tests router-free, the same way
// `useLocalePath` does; in the app a route is always present.
const route = useRoute() as ReturnType<typeof useRoute> | undefined

/** Native language names — a German speaker looks for "Deutsch", not "German". */
const LOCALE_NAMES: Record<Locale, string> = {
  en: 'English',
  de: 'Deutsch',
}

const current = computed<Locale>(() => {
  const fromUrl = route?.params?.locale
  return isLocale(fromUrl) ? fromUrl : DEFAULT_LOCALE
})

/** The current path with its locale segment swapped, so the switch stays on this page. */
function pathIn(locale: Locale): string {
  const rest = (route?.path ?? '/').replace(/^\/[^/]+/, '')
  return `${`/${locale}${rest}`.replace(/\/$/, '')}${route?.hash ?? ''}`
}
</script>

<template>
  <!-- `nav` with a name, because the footer already contributes landmarks and each must be
       distinguishable; `aria-current` marks the active language for screen-reader users. -->
  <nav :aria-label="$t('common.locale.label')" class="flex items-center gap-2 text-sm">
    <template v-for="(locale, index) in LOCALES" :key="locale">
      <span v-if="index > 0" aria-hidden="true" class="text-muted-foreground">·</span>
      <a
        :aria-current="locale === current ? 'true' : undefined"
        :class="
          locale === current
            ? 'font-medium text-foreground'
            : 'text-muted-foreground hover:text-foreground'
        "
        :href="pathIn(locale)"
        :hreflang="locale"
        :lang="locale"
      >
        {{ LOCALE_NAMES[locale] }}
      </a>
    </template>
  </nav>
</template>
