<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDetailView from '@/components/BaseDetailView.vue'
import { useArtist } from '@/composables/useArtist'
import { useEventSearch } from '@/composables/useEvents'

const route = useRoute()
const slug = computed(() => String(route.params.slug))

const { data: artist, error, notFound, loading, run: loadArtist } = useArtist(() => slug.value)
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
  <BaseDetailView
    :error="error"
    :events="events"
    :events-error="eventsError"
    :events-loading="eventsLoading"
    :image-url="artist?.imageUrl"
    :loading="loading"
    :name="artist?.name"
    :not-found="notFound"
    :ready="Boolean(artist)"
    empty-text="No dates on the radar yet — check back soon."
    kind="Artist"
    not-found-text="This act isn't on our radar."
  >
    <template #meta>
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
    </template>

    <p v-if="artist?.description" class="whitespace-pre-line text-foreground/90">
      {{ artist.description }}
    </p>
  </BaseDetailView>
</template>
