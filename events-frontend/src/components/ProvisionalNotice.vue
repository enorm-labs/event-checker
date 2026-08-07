<script lang="ts" setup>
/**
 * Says out loud that the legal pages are not final yet.
 *
 * The contact details are placeholders (§8.3) and the infrastructure the privacy notice describes
 * is proposed rather than built (ADR-012 is still `Proposed`). A legal page that presents either
 * as settled fact is inaccurate — and an inaccurate notice is the defect these pages exist to
 * avoid. Both flags must be `false` before go-live; a unit test keeps them honest.
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { CONTACT_DETAILS_ARE_PROVISIONAL, INFRASTRUCTURE_IS_PROPOSED } from '@/lib/legal'
import { DEFAULT_LOCALE } from '@/i18n/locales'

const { t, locale } = useI18n()

/**
 * The legal pages are still English-only while the rest of the site speaks German (Phase 4 of
 * docs/LOCALISATION_PLAN.md writes them).
 *
 * Saying so is not decoration. FOOTER_AND_LEGAL_PLAN §6.1 rules out shipping a site that presents
 * itself in German with an English-only imprint and privacy notice — so until Phase 4 lands, the
 * gap is disclosed on the page rather than left for a German reader to discover. It disappears by
 * itself once German versions exist.
 */
const showEnglishOnly = computed(() => locale.value !== DEFAULT_LOCALE)
</script>

<template>
  <aside
    v-if="CONTACT_DETAILS_ARE_PROVISIONAL || INFRASTRUCTURE_IS_PROPOSED || showEnglishOnly"
    class="rounded-lg border border-border bg-muted/50 p-4 text-sm"
  >
    <p class="font-medium text-foreground">{{ t('legal.notFinal') }}</p>
    <ul class="mt-2 list-disc space-y-1 pl-5">
      <li v-if="CONTACT_DETAILS_ARE_PROVISIONAL">
        The postal address and email address below are placeholders. Real contact details are added
        before the site goes live.
      </li>
      <li v-if="INFRASTRUCTURE_IS_PROPOSED">
        Event Junkie is not deployed yet. The hosting and content-delivery providers named here are
        the ones we intend to use; this page is re-checked against what actually runs before launch.
      </li>
      <!-- Rendered in German for a German reader, which is the whole point of saying it. -->
      <li v-if="showEnglishOnly" :lang="DEFAULT_LOCALE === 'en' ? 'de' : 'en'">
        {{ t('legal.englishOnly') }}
      </li>
    </ul>
  </aside>
</template>
