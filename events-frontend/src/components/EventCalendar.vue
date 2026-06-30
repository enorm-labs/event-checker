<script lang="ts" setup>
import type { CalendarOptions, DatesSetArg, EventClickArg, EventInput } from '@fullcalendar/core'
import { computed } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import listPlugin from '@fullcalendar/list'

interface Props {
  events: EventInput[]
  initialView?: string
}

const props = withDefaults(defineProps<Props>(), {
  initialView: 'dayGridMonth',
})

const emit = defineEmits<{
  // Emitted whenever the visible range changes (initial render + navigation), so the parent
  // can fetch events for the new window. Dates are inclusive ISO date strings.
  datesSet: [range: { from: string; to: string }]
  // Emitted when an event is clicked, carrying its slug for router navigation.
  eventClick: [slug: string]
}>()

function toIsoDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function handleDatesSet(arg: DatesSetArg) {
  // FullCalendar's `end` is exclusive; the BFF's `to` is inclusive, so step back a day.
  const inclusiveEnd = new Date(arg.end)
  inclusiveEnd.setDate(inclusiveEnd.getDate() - 1)
  emit('datesSet', { from: toIsoDate(arg.start), to: toIsoDate(inclusiveEnd) })
}

function handleEventClick(arg: EventClickArg) {
  arg.jsEvent.preventDefault() // navigate via vue-router instead of following event.url
  const slug = arg.event.extendedProps.slug as string | undefined
  if (slug) emit('eventClick', slug)
}

// FullCalendar is encapsulated here so the rest of the app sees a single, on-theme
// component (see ADR-011). The CSS-variable bridge to our shadcn tokens lives in <style> below.
const options = computed<CalendarOptions>(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, listPlugin],
  initialView: props.initialView,
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,listWeek',
  },
  height: 'auto',
  firstDay: 1, // Monday (Berlin / EU convention)
  nowIndicator: true,
  events: props.events,
  datesSet: handleDatesSet,
  eventClick: handleEventClick,
}))
</script>

<template>
  <div class="event-calendar">
    <FullCalendar :options="options" />
  </div>
</template>

<style scoped>
/* Bridge FullCalendar's CSS variables to our shadcn/Tailwind design tokens. */
/* These custom properties inherit into FullCalendar's DOM below this wrapper, */
/* so the calendar follows light/dark mode automatically. */
.event-calendar {
  --fc-border-color: var(--border);
  --fc-page-bg-color: var(--background);
  --fc-neutral-bg-color: var(--muted);
  --fc-neutral-text-color: var(--muted-foreground);
  --fc-today-bg-color: color-mix(in oklch, var(--primary) 8%, transparent);
  --fc-highlight-color: color-mix(in oklch, var(--primary) 12%, transparent);
  --fc-event-bg-color: var(--primary);
  --fc-event-border-color: var(--primary);
  --fc-event-text-color: var(--primary-foreground);
  --fc-button-bg-color: var(--primary);
  --fc-button-border-color: var(--primary);
  --fc-button-text-color: var(--primary-foreground);
  --fc-button-hover-bg-color: color-mix(in oklch, var(--primary) 90%, black);
  --fc-button-hover-border-color: color-mix(in oklch, var(--primary) 90%, black);
  --fc-button-active-bg-color: color-mix(in oklch, var(--primary) 80%, black);
  --fc-button-active-border-color: color-mix(in oklch, var(--primary) 80%, black);
  --fc-list-event-hover-bg-color: var(--muted);

  color: var(--foreground);
}
</style>
