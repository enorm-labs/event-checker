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

import { DEFAULT_LOCALE, isLocale, type Locale, LOCALES, stripLocale } from '@/i18n/locales'

const props = withDefaults(
  defineProps<{
    /**
     * Renders `EN · DE` instead of `English · Deutsch`, and **without a `nav` landmark**.
     *
     * Both matter for the header. The full names do not fit a ~390px viewport, which the header
     * has already been reworked for once (see the overflow guard in e2e/smoke.spec.ts). And a
     * second landmark named "Language" would collide with the footer's — identically-named
     * landmarks are indistinguishable in a screen reader's landmark list, and ambiguous to any
     * selector addressing them by name. The compact form lives *inside* the header's existing
     * "Main" navigation instead, where it needs no landmark of its own.
     */
    compact?: boolean
  }>(),
  { compact: false },
)

// `useRoute()` yields undefined when no router is installed — component tests mount the footer
// without one. Degrading to the default locale keeps those tests router-free, the same way
// `useLocalePath` does; in the app a route is always present.
const route = useRoute() as ReturnType<typeof useRoute> | undefined

/** Native language names — a German speaker looks for "Deutsch", not "German". */
const LOCALE_NAMES: Record<Locale, string> = {
  en: 'English',
  de: 'Deutsch',
}

/**
 * The compact label. `EN`/`DE` is the conventional shorthand and stays legible at `text-xs`, but
 * it is a poor *accessible* name on its own — so the link keeps the full native name as its
 * `aria-label` and `title`, the same pattern the header's icon controls use.
 */
const shortName = (locale: Locale) => locale.toUpperCase()

const current = computed<Locale>(() => {
  const fromUrl = route?.params?.locale
  return isLocale(fromUrl) ? fromUrl : DEFAULT_LOCALE
})

/** The current path with its locale segment swapped, so the switch stays on this page. */
function pathIn(locale: Locale): string {
  const rest = stripLocale(route?.path ?? '/')
  return `${`/${locale}${rest}`.replace(/\/$/, '')}${route?.hash ?? ''}`
}
</script>

<template>
  <!-- `nav` only in the full form: the footer contributes several landmarks and each must be
       distinguishable. The compact form is a plain wrapper inside the header's own nav, so it adds
       no landmark. `aria-current` marks the active language in both. -->
  <component
    :is="props.compact ? 'span' : 'nav'"
    :class="['flex items-center', props.compact ? 'gap-1 text-xs' : 'gap-2 text-sm']"
    v-bind="props.compact ? {} : { 'aria-label': $t('common.locale.label') }"
  >
    <template v-for="(locale, index) in LOCALES" :key="locale">
      <span v-if="index > 0" aria-hidden="true" class="text-muted-foreground">·</span>
      <a
        :aria-current="locale === current ? 'true' : undefined"
        :aria-label="props.compact ? LOCALE_NAMES[locale] : undefined"
        :class="
          locale === current
            ? 'font-medium text-foreground'
            : 'text-muted-foreground hover:text-foreground'
        "
        :href="pathIn(locale)"
        :hreflang="locale"
        :lang="locale"
        :title="props.compact ? LOCALE_NAMES[locale] : undefined"
      >
        {{ props.compact ? shortName(locale) : LOCALE_NAMES[locale] }}
      </a>
    </template>
  </component>
</template>
