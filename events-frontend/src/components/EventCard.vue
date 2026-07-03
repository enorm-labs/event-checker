<script lang="ts" setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import type { EventSummary } from '@/api/types'
import { formatDate, formatPrice, formatTime, todayIso } from '@/lib/format'

const props = defineProps<{ event: EventSummary }>()

// An event happening today gets a pulsing "live" dot — it stands out in the Upcoming feed and on
// venue/artist pages, and reinforces liveness in the Tonight feed. Self-contained, so any caller
// gets it for free.
const isLive = computed(() => Boolean(props.event.eventDate) && props.event.eventDate === todayIso())
</script>

<template>
  <RouterLink
    :to="`/events/${event.slug}`"
    class="group flex gap-4 rounded-xl border border-border bg-card p-3 shadow-sm transition-all hover:border-primary/40 hover:shadow-md motion-safe:hover:-translate-y-0.5"
  >
    <img
      v-if="event.imageUrl"
      :src="event.imageUrl"
      :alt="event.title ?? ''"
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
          <h3 class="truncate leading-tight font-semibold">{{ event.title }}</h3>
        </div>
        <span
          v-if="event.soldOut"
          class="shrink-0 rounded-full bg-destructive/10 px-2 py-0.5 text-xs font-medium text-destructive"
        >
          Sold out
        </span>
      </div>
      <p v-if="event.subtitle" class="truncate text-sm text-muted-foreground">
        {{ event.subtitle }}
      </p>
      <p class="text-sm text-muted-foreground">
        {{ formatDate(event.eventDate) }}
        <template v-if="event.startTime"> · {{ formatTime(event.startTime) }}</template>
        <template v-if="event.venue?.name"> · {{ event.venue.name }}</template>
      </p>
      <div class="flex flex-wrap items-center gap-1.5 pt-0.5">
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
