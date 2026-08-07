<script lang="ts" setup>
import PulseMark from '@/components/PulseMark.vue'
// Brand lockup: the pulse mark (soundwave + heartbeat) beside the wordmark.
// Matches the favicon (public/favicon.svg) and the directions in docs/BRANDING.md §4.
// The mark is decorative — the adjacent text carries the accessible name.

withDefaults(
  defineProps<{
    /**
     * Keep the wordmark visible at every width. The header hides it below `sm` to stay inside a
     * ~390px viewport (see the overflow guard in e2e/smoke.spec.ts); the footer stacks its columns
     * and has the room, so a mark on its own there would just read as a stray icon.
     */
    alwaysShowWordmark?: boolean
  }>(),
  { alwaysShowWordmark: false },
)
</script>

<template>
  <span class="inline-flex items-center gap-2">
    <PulseMark class="h-6 shrink-0" />
    <!-- Wordmark collapses to just the mark on narrow screens so the header never overflows;
         kept in the a11y tree (sr-only) so the link's accessible name stays "Event Junkie". -->
    <span
      :class="alwaysShowWordmark ? '' : 'sr-only sm:not-sr-only'"
      class="text-lg font-bold tracking-tight text-foreground"
    >
      Event <span class="text-primary">Junkie</span>
    </span>
  </span>
</template>
