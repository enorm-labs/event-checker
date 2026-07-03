<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import SectionLabel from '@/components/SectionLabel.vue'
import { useArtist } from '@/composables/useArtist'
import { usePageTitle } from '@/composables/usePageTitle'
import { useEventSearch } from '@/composables/useEvents'

const route = useRoute()
const slug = computed(() => String(route.params.slug))

const { data: artist, error, notFound, loading, run: loadArtist } = useArtist(() => slug.value)

usePageTitle(() => (notFound.value ? 'Artist not found' : (artist.value?.name ?? 'Artist')))
const {
  data: events,
  error: eventsError,
  loading: eventsLoading,
  run: loadEvents,
} = useEventSearch(() => ({ artist: slug.value, size: 50 }), 'events for this artist')

const links = computed(() =>
  [
    { label: 'Website', url: artist.value?.websiteUrl },
    { label: 'Facebook', url: artist.value?.facebookUrl },
    { label: 'Instagram', url: artist.value?.instagramUrl },
    { label: 'YouTube', url: artist.value?.youtubeUrl },
  ].filter((link): link is { label: string; url: string } => Boolean(link.url)),
)

function reload() {
  loadArtist()
  loadEvents()
}

onMounted(reload)
watch(slug, reload)
</script>

<template>
  <main class="mx-auto max-w-3xl space-y-8 p-8">
    <p v-if="loading" class="text-sm text-muted-foreground">Cueing it up…</p>

    <div v-else-if="notFound" class="space-y-3">
      <h1 class="text-2xl font-bold tracking-tight">Artist not found</h1>
      <p class="text-muted-foreground">This act isn't on our radar.</p>
      <Button as-child variant="outline">
        <RouterLink to="/events">Browse events</RouterLink>
      </Button>
    </div>

    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>

    <template v-else-if="artist">
      <header class="flex gap-4">
        <img
          v-if="artist.imageUrl"
          :src="artist.imageUrl"
          :alt="artist.name ?? ''"
          class="size-24 shrink-0 rounded-lg border border-border object-cover"
          loading="lazy"
        />
        <div class="space-y-2">
          <SectionLabel as="p">Artist</SectionLabel>
          <h1 class="text-3xl font-bold tracking-tight">{{ artist.name }}</h1>
          <div v-if="links.length" class="flex flex-wrap gap-3 text-sm">
            <a
              v-for="link in links"
              :key="link.label"
              :href="link.url"
              class="text-primary underline-offset-4 hover:underline"
              rel="noopener noreferrer"
              target="_blank"
            >
              {{ link.label }}
            </a>
          </div>
        </div>
      </header>

      <p v-if="artist.description" class="whitespace-pre-line text-foreground/90">
        {{ artist.description }}
      </p>

      <section class="space-y-4">
        <SectionLabel>Upcoming events</SectionLabel>
        <p v-if="eventsLoading" class="text-sm text-muted-foreground">Cueing it up…</p>
        <p v-else-if="eventsError" class="text-sm text-destructive">{{ eventsError }}</p>
        <p v-else-if="!events?.content?.length" class="text-sm text-muted-foreground">
          No dates on the radar yet — check back soon.
        </p>
        <div v-else class="grid gap-3 sm:grid-cols-2">
          <EventCard v-for="event in events.content" :key="event.slug" :event="event" />
        </div>
      </section>
    </template>
  </main>
</template>
