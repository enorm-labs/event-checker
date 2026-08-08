<script lang="ts" setup>
/**
 * "About" — English version; `AboutView.de.vue` is its counterpart, written from the concept
 * rather than translated (docs/LOCALISATION_PLAN.md §5). Both anchor the beta section at `#beta`,
 * because the header badge links there regardless of language.
 */
import { RouterLink } from 'vue-router'
import { useLocalePath } from '@/composables/useLocalePath'

const localePath = useLocalePath()
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-4 p-8">
    <h1 class="text-3xl font-bold tracking-tight">About</h1>
    <p class="text-muted-foreground">
      Event Junkie is your guide to what's on across Berlin's venues — a single, always-fresh feed
      of live events, from concerts and club nights to festivals, parties, and the odd quiz night.
    </p>
    <p class="text-muted-foreground">
      Browse everything at a glance, filter by genre or type to home in on your scene, and open the
      calendar to plan your week. Every event links through to its venue, lineup, and artists, so
      you can dig into the details — and never miss the nights worth showing up for.
    </p>
    <p class="text-muted-foreground">
      The focus is music of every stripe — big stages and small back rooms alike, not just techno —
      all across Berlin.
    </p>

    <!-- The header's beta badge links here (Phase 4). `scroll-mt` keeps the heading clear of the
         sticky-ish header when the anchor is followed. -->
    <section id="beta" class="scroll-mt-8 space-y-4 pt-4">
      <h2 class="text-2xl font-bold tracking-tight">Why it says beta</h2>
      <p class="text-muted-foreground">
        Event Junkie is early. It works, we use it, and it is genuinely useful — but it is still
        finding its feet, and you should know what that means before you plan a night around it.
      </p>
      <p class="text-muted-foreground">
        <strong class="font-medium text-foreground">Coverage is incomplete.</strong> New venues are
        added one at a time, so a quiet night on Event Junkie is not always a quiet night in Berlin.
        <strong class="font-medium text-foreground">Details can be wrong or stale.</strong> Events
        are read automatically from venue websites; when a show moves, sells out or is cancelled, we
        find out on our next read, not immediately. Always check with the venue before you go.
        <strong class="font-medium text-foreground">Things change without notice</strong> — pages,
        filters and the data behind them are all still moving.
      </p>
      <p class="text-muted-foreground">
        What beta does <em>not</em> mean: nothing here is a trial you get charged for, no data about
        you is sold or shared, and nothing tracks you — see the
        <RouterLink
          class="text-foreground underline underline-offset-4"
          :to="localePath('/legal/privacy')"
        >
          privacy page
        </RouterLink>
        , which is short for exactly that reason.
      </p>
      <p class="text-muted-foreground">
        Found something wrong? Telling us is the fastest way to fix it —
        <a
          class="text-foreground underline underline-offset-4"
          href="https://github.com/enorm-labs/event-checker/issues/new/choose"
          rel="noopener"
          target="_blank"
        >
          open an issue
        </a>
        . What has changed lately is on the
        <a
          class="text-foreground underline underline-offset-4"
          href="https://github.com/enorm-labs/event-checker/releases"
          rel="noopener"
          target="_blank"
        >
          releases page
        </a>
        .
      </p>
    </section>
  </main>
</template>
