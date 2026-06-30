<script lang="ts" setup>
import type { EventInput } from '@fullcalendar/core'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import EventCalendar from '@/components/EventCalendar.vue'
import { fetchCalendarEvents } from '@/composables/useEvents'

const router = useRouter()
const events = ref<EventInput[]>([])
const error = ref<string | null>(null)

/** Clamps a range to the BFF's 92-day maximum (no built-in view exceeds it, but be safe). */
function clampTo(from: string, to: string): string {
  const max = new Date(from)
  max.setDate(max.getDate() + 92)
  const maxIso = max.toISOString().slice(0, 10)
  return to > maxIso ? maxIso : to
}

// Driven by EventCalendar's `datesSet`, which also fires on initial render.
async function loadRange({ from, to }: { from: string; to: string }) {
  try {
    const data = await fetchCalendarEvents(from, clampTo(from, to))
    events.value = data.map((event) => ({
      title: event.title ?? '',
      start: event.startTime ? `${event.eventDate}T${event.startTime}` : event.eventDate,
      url: `/events/${event.slug}`,
      extendedProps: { slug: event.slug },
    }))
    error.value = null
  } catch {
    error.value = 'Could not load calendar events.'
  }
}

function openEvent(slug: string) {
  router.push(`/events/${slug}`)
}
</script>

<template>
  <main class="mx-auto max-w-5xl space-y-6 p-8">
    <header class="space-y-1">
      <h1 class="text-3xl font-bold tracking-tight">Calendar</h1>
      <p class="text-muted-foreground">Browse upcoming music events across Berlin.</p>
    </header>
    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
    <EventCalendar :events="events" @dates-set="loadRange" @event-click="openEvent" />
  </main>
</template>
