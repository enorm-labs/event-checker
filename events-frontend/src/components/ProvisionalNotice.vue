<script lang="ts" setup>
/**
 * Says out loud that the legal pages are not final yet.
 *
 * The contact details are placeholders (§8.3) and the infrastructure the privacy notice describes
 * is proposed rather than built (ADR-012 is still `Proposed`). A legal page that presents either
 * as settled fact is inaccurate — and an inaccurate notice is the defect these pages exist to
 * avoid. Both flags must be `false` before go-live; a unit test keeps them honest.
 */
import { CONTACT_DETAILS_ARE_PROVISIONAL, INFRASTRUCTURE_IS_PROPOSED } from '@/lib/legal'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <aside
    v-if="CONTACT_DETAILS_ARE_PROVISIONAL || INFRASTRUCTURE_IS_PROPOSED"
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
    </ul>
  </aside>
</template>
