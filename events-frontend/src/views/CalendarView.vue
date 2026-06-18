<script setup lang="ts">
import type { EventInput } from '@fullcalendar/core'
import EventCalendar from '@/components/EventCalendar.vue'

// Placeholder data — replace with events fetched from the BFF (e.g. GET /api/events).
const events: EventInput[] = [
  { title: 'Techno Night — Berghain', start: '2026-06-19T23:00:00' },
  { title: 'Indie Live — Lido', start: '2026-06-20T20:00:00' },
  { title: 'Jazz Session — A-Trane', start: '2026-06-21T21:00:00' },
  { title: 'House — Watergate', start: '2026-06-24T22:00:00' },
  { title: 'Open Air Festival — Tempelhof', start: '2026-06-27', end: '2026-06-29' },
]
</script>

<template>
  <main class="mx-auto max-w-5xl space-y-6 p-8">
    <header class="space-y-1">
      <h1 class="text-3xl font-bold tracking-tight">Calendar</h1>
      <p class="text-muted-foreground">Browse upcoming music events across Berlin.</p>
    </header>
    <EventCalendar :events="events" />
  </main>
</template>
