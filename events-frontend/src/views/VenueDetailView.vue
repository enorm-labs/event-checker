<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import { useVenue } from '@/composables/useVenue'
import { useEventSearch } from '@/composables/useEvents'

const route = useRoute()
const slug = computed(() => String(route.params.slug))

const { data: venue, error, notFound, loading, run: loadVenue } = useVenue(() => slug.value)
const {
  data: events,
  error: eventsError,
  loading: eventsLoading,
  run: loadEvents,
} = useEventSearch(() => ({ venue: slug.value, size: 50 }), 'events at this venue')

// Composed in script to avoid fragile template whitespace around the comma/space separators.
const addressLine = computed(() => {
  const v = venue.value
  if (!v?.address) return ''
  const cityLine = [v.postalCode, v.city].filter(Boolean).join(' ')
  return cityLine ? `${v.address}, ${cityLine}` : v.address
})

function reload() {
  loadVenue()
  loadEvents()
}

onMounted(reload)
watch(slug, reload)
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-8 p-8">
    <p v-if="loading" class="text-sm text-muted-foreground">Loading…</p>

    <div v-else-if="notFound" class="space-y-3">
      <h1 class="text-2xl font-bold tracking-tight">Venue not found</h1>
      <Button as-child variant="outline">
        <RouterLink to="/events">Browse events</RouterLink>
      </Button>
    </div>

    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>

    <template v-else-if="venue">
      <header class="flex gap-4">
        <img
          v-if="venue.imageUrl"
          :src="venue.imageUrl"
          :alt="venue.name ?? ''"
          class="size-24 shrink-0 rounded-lg border border-border object-cover"
          loading="lazy"
        />
        <div class="space-y-1">
          <h1 class="text-3xl font-bold tracking-tight">{{ venue.name }}</h1>
          <p v-if="addressLine" class="text-muted-foreground">{{ addressLine }}</p>
          <a
            v-if="venue.websiteUrl"
            :href="venue.websiteUrl"
            class="text-sm text-primary underline-offset-4 hover:underline"
            rel="noopener noreferrer"
            target="_blank"
          >
            Website
          </a>
        </div>
      </header>

      <section class="space-y-4">
        <h2 class="text-xl font-semibold tracking-tight">Upcoming events</h2>
        <p v-if="eventsLoading" class="text-sm text-muted-foreground">Loading…</p>
        <p v-else-if="eventsError" class="text-sm text-destructive">{{ eventsError }}</p>
        <p v-else-if="!events?.content?.length" class="text-sm text-muted-foreground">
          No upcoming events at this venue.
        </p>
        <div v-else class="grid gap-3 sm:grid-cols-2">
          <EventCard v-for="event in events.content" :key="event.slug" :event="event" />
        </div>
      </section>
    </template>
  </main>
</template>
