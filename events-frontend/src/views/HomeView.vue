<script lang="ts" setup>
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { CalendarDays } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import { useTodayEvents, useUpcomingEvents } from '@/composables/useEvents'
import { todayIso } from '@/lib/format'

const today = useTodayEvents()
const upcoming = useUpcomingEvents(todayIso())

onMounted(() => {
  today.run()
  upcoming.run()
})
</script>

<template>
  <main class="mx-auto max-w-5xl space-y-10 p-8">
    <section class="space-y-3">
      <h1 class="text-3xl font-bold tracking-tight">Event Junkie</h1>
      <p class="text-muted-foreground">
        Concerts, club nights, and festivals across Berlin's venues.
      </p>
      <Button as-child>
        <RouterLink to="/calendar">
          <CalendarDays />
          Browse calendar
        </RouterLink>
      </Button>
    </section>

    <section class="space-y-4">
      <h2 class="text-xl font-semibold tracking-tight">Tonight</h2>
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
      <h2 class="text-xl font-semibold tracking-tight">Upcoming</h2>
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
