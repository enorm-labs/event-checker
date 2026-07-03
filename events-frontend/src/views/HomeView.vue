<script lang="ts" setup>
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { CalendarDays } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import PulseMark from '@/components/PulseMark.vue'
import SectionLabel from '@/components/SectionLabel.vue'
import { useTodayEvents, useUpcomingEvents } from '@/composables/useEvents'
import { TAGLINE } from '@/composables/usePageTitle'
import { todayIso } from '@/lib/format'

const today = useTodayEvents()
const upcoming = useUpcomingEvents(todayIso())

onMounted(() => {
  today.run()
  upcoming.run()
})
</script>

<template>
  <main class="mx-auto max-w-5xl space-y-12 p-8">
    <section class="relative py-20 sm:py-28">
      <div class="relative flex flex-col items-center gap-5 text-center">
        <!-- The glow is centered on the mark (not the whole hero) so the pulse reads as the light
             source. No overflow-clip anywhere, so the radial fades to transparent softly on all
             sides; the extra top padding keeps its upward reach from washing over the nav. -->
        <div class="relative">
          <div
            aria-hidden="true"
            class="pointer-events-none absolute top-1/2 left-1/2 h-[300px] w-[560px] max-w-[92vw] -translate-x-1/2 -translate-y-1/2 rounded-full opacity-35 blur-[80px]"
            style="background: radial-gradient(closest-side, var(--primary), transparent)"
          />
          <PulseMark animate class="relative h-14 sm:h-20" />
        </div>
        <h1 class="text-4xl font-extrabold tracking-tight sm:text-6xl">
          Event <span class="text-primary">Junkie</span>
        </h1>
        <p class="font-mono text-xs tracking-[0.2em] text-muted-foreground uppercase sm:text-sm">
          {{ TAGLINE }}
        </p>
        <Button as-child size="lg">
          <RouterLink to="/calendar">
            <CalendarDays />
            Browse calendar
          </RouterLink>
        </Button>
      </div>
    </section>

    <section class="space-y-4">
      <SectionLabel>Tonight</SectionLabel>
      <p v-if="today.loading.value" class="text-sm text-muted-foreground">Loading…</p>
      <p v-else-if="today.error.value" class="text-sm text-destructive">
        {{ today.error.value }}
      </p>
      <p v-else-if="!today.data.value?.length" class="text-sm text-muted-foreground">
        Nothing on tonight.
      </p>
      <div v-else class="grid gap-3 sm:grid-cols-2">
        <EventCard v-for="event in today.data.value" :key="event.slug" :event="event" />
      </div>
    </section>

    <section class="space-y-4">
      <SectionLabel>Upcoming</SectionLabel>
      <p v-if="upcoming.loading.value" class="text-sm text-muted-foreground">Loading…</p>
      <p v-else-if="upcoming.error.value" class="text-sm text-destructive">
        {{ upcoming.error.value }}
      </p>
      <p v-else-if="!upcoming.data.value?.length" class="text-sm text-muted-foreground">
        No upcoming events found.
      </p>
      <div v-else class="grid gap-3 sm:grid-cols-2">
        <EventCard v-for="event in upcoming.data.value" :key="event.slug" :event="event" />
      </div>
    </section>
  </main>
</template>
