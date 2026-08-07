<script lang="ts" setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { EventSummary } from '@/api/types'
import {
  eventLabel,
  formatDate,
  formatEventType,
  formatPrice,
  formatTime,
  todayIso,
} from '@/lib/format'

const props = defineProps<{ event: EventSummary }>()

// `OTHER` is the importers' catch-all — it tells a reader nothing the card doesn't already say,
// so it's dropped rather than spending a pill on it. Every other type earns its place.
const eventType = computed(() =>
  props.event.eventType && props.event.eventType !== 'OTHER'
    ? formatEventType(props.event.eventType)
    : null,
)

// An event happening today gets a pulsing "live" dot — it stands out in the Upcoming feed and on
// venue/artist pages, and reinforces liveness in the Tonight feed. Self-contained, so any caller
// gets it for free.
const isLive = computed(
  () => Boolean(props.event.eventDate) && props.event.eventDate === todayIso(),
)
</script>

<template>
  <RouterLink
    :to="`/events/${event.slug}`"
    class="group flex gap-4 rounded-xl border border-border bg-card p-3 shadow-sm transition-all hover:border-primary/40 hover:shadow-md motion-safe:hover:-translate-y-0.5"
  >
    <img
      v-if="event.imageUrl"
      :alt="event.title ?? ''"
      :src="event.imageUrl"
      class="size-20 shrink-0 rounded-lg object-cover grayscale-[0.5] transition duration-300 group-hover:grayscale-0"
      loading="lazy"
    />
    <div class="min-w-0 flex-1 space-y-1">
      <div class="flex items-start justify-between gap-2">
        <div class="flex min-w-0 items-center gap-2">
          <span v-if="isLive" class="relative flex size-2 shrink-0">
            <span
              class="absolute inline-flex size-full rounded-full bg-primary opacity-75 motion-safe:animate-ping"
            />
            <span class="relative inline-flex size-2 rounded-full bg-primary" />
            <span class="sr-only">Live tonight</span>
          </span>
          <!--
            Both lines are `truncate`d, so a long one is cut off with no way to read the rest.
            The native `title` tooltip spells each out on hover — the same affordance the
            calendar cells got, sharing `eventLabel` so the two can't drift. Scoped to the
            clipped elements rather than the whole card, so hovering the card doesn't pop a
            tooltip over information that is already fully visible.
          -->
          <h3
            :title="eventLabel(event.title, event.venue?.name)"
            class="truncate leading-tight font-semibold"
          >
            {{ event.title }}
          </h3>
        </div>
        <span
          v-if="event.soldOut"
          class="shrink-0 rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive"
        >
          Sold out
        </span>
        <span
          v-else-if="event.free"
          class="shrink-0 rounded-full bg-emerald-500/10 px-2 py-0.5 text-xs font-medium text-emerald-700 dark:text-emerald-400"
        >
          Free
        </span>
      </div>
      <p
        v-if="event.subtitle"
        :title="event.subtitle"
        class="truncate text-sm text-muted-foreground"
      >
        {{ event.subtitle }}
      </p>
      <p class="text-sm text-muted-foreground">
        {{ formatDate(event.eventDate) }}
        <template v-if="event.startTime"> · {{ formatTime(event.startTime) }}</template>
        <template v-if="event.venue?.name"> · {{ event.venue.name }}</template>
      </p>
      <div class="flex flex-wrap items-center gap-1.5 pt-0.5">
        <!--
          Type and genre are different taxonomies — one kind of night, many kinds of music — so
          the type pill is outlined rather than filled. Same row, same size, but the eye can tell
          "Club night" from "Techno" without reading them.
        -->
        <span
          v-if="eventType"
          class="rounded-full border border-border px-2 py-0.5 text-xs font-medium text-foreground/70"
        >
          {{ eventType }}
        </span>
        <span
          v-for="tag in event.genreTags"
          :key="tag"
          class="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground"
        >
          {{ tag }}
        </span>
        <span
          v-if="formatPrice(event.pricePresale, event.priceCurrency)"
          class="ml-auto text-sm font-medium"
        >
          {{ formatPrice(event.pricePresale, event.priceCurrency) }}
        </span>
      </div>
    </div>
  </RouterLink>
</template>
