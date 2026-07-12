<script lang="ts" setup>
import { RouterLink } from 'vue-router'
import type { EventPage } from '@/api/types'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import SectionLabel from '@/components/SectionLabel.vue'
import { usePageTitle } from '@/composables/usePageTitle'

/**
 * Presentational shell shared by the artist, venue, and promoter detail pages. Owns the
 * loading / not-found / error scaffold, the hero header (image + kind label + name), and the
 * "Upcoming events" feed, so each view only wires its data and fills the entity-specific slots.
 *
 * Slots:
 * - `meta` — entity-specific header metadata rendered under the name (links, address, …).
 * - default — optional content between the header and the events feed (e.g. a description).
 */
const props = defineProps<{
  /** Entity kind label, e.g. "Artist" — shown above the name and in the not-found heading. */
  kind: string
  /** Entity fetch state (from `useAsync`). */
  loading: boolean
  error: string | null
  notFound: boolean
  /** Whether the entity has loaded — guards the content branch. */
  ready: boolean
  /** Copy shown under the "<kind> not found" heading. */
  notFoundText: string
  /** Resolved entity display fields. */
  name?: string | null
  imageUrl?: string | null
  /** Upcoming-events feed state (from `useEventSearch`). */
  events: EventPage | null
  eventsLoading: boolean
  eventsError: string | null
  /** Copy shown when the events feed is empty. */
  emptyText: string
}>()

usePageTitle(() => (props.notFound ? `${props.kind} not found` : (props.name ?? props.kind)))
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-8 p-8">
    <p v-if="loading" class="text-sm text-muted-foreground">Cueing it up…</p>

    <div v-else-if="notFound" class="space-y-3">
      <h1 class="text-2xl font-bold tracking-tight">{{ kind }} not found</h1>
      <p class="text-muted-foreground">{{ notFoundText }}</p>
      <Button as-child variant="outline">
        <RouterLink to="/events">Browse events</RouterLink>
      </Button>
    </div>

    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>

    <template v-else-if="ready">
      <header class="flex gap-4">
        <img
          v-if="imageUrl"
          :alt="name ?? ''"
          :src="imageUrl"
          class="size-24 shrink-0 rounded-lg border border-border object-cover"
          loading="lazy"
        />
        <div class="space-y-2">
          <SectionLabel as="p">{{ kind }}</SectionLabel>
          <h1 class="text-3xl font-bold tracking-tight">{{ name }}</h1>
          <slot name="meta" />
        </div>
      </header>

      <slot />

      <section class="space-y-4">
        <SectionLabel>Upcoming events</SectionLabel>
        <p v-if="eventsLoading" class="text-sm text-muted-foreground">Cueing it up…</p>
        <p v-else-if="eventsError" class="text-sm text-destructive">{{ eventsError }}</p>
        <p v-else-if="!events?.content?.length" class="text-sm text-muted-foreground">
          {{ emptyText }}
        </p>
        <div v-else class="grid gap-3 sm:grid-cols-2">
          <EventCard v-for="event in events.content" :key="event.slug" :event="event" />
        </div>
      </section>
    </template>
  </main>
</template>
