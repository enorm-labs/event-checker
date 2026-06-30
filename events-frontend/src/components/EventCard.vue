<script lang="ts" setup>
import { RouterLink } from 'vue-router'
import type { EventSummary } from '@/api/types'
import { formatDate, formatPrice, formatTime } from '@/lib/format'

defineProps<{ event: EventSummary }>()
</script>

<template>
  <RouterLink
    :to="`/events/${event.slug}`"
    class="group flex gap-4 rounded-lg border border-border bg-card p-3 transition-colors hover:bg-muted"
  >
    <img
      v-if="event.imageUrl"
      :src="event.imageUrl"
      :alt="event.title ?? ''"
      class="size-20 shrink-0 rounded-md object-cover"
      loading="lazy"
    />
    <div class="min-w-0 flex-1 space-y-1">
      <div class="flex items-start justify-between gap-2">
        <h3 class="truncate font-semibold leading-tight">{{ event.title }}</h3>
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
