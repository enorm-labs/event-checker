<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDetailView from '@/components/BaseDetailView.vue'
import { usePageMeta } from '@/composables/usePageMeta'
import { artistPageMeta, placeholderPageMeta } from '@/lib/pageMeta'
import { useArtist } from '@/composables/useArtist'
import { useEventSearch } from '@/composables/useEvents'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

/**
 * Entity label: the eyebrow above the name, the not-found heading, and the placeholder title.
 *
 * A `computed` rather than a constant because it has to follow the active locale — the locale can
 * change without this view remounting, since the switcher only rewrites the URL's locale segment.
 */
const kind = computed(() => t('detail.artist.kind'))

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
    // The social labels are brand names and stay as they are; only "Website" is a word.
    { label: t('common.actions.website'), url: artist.value?.websiteUrl },
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

// Title, description and image for this page — the same values the meta injector will need
// server-side later (ADR-014 §Decision 3). Mirrors the loading / not-found states the view
// itself renders.
usePageMeta(() =>
  artist.value
    ? artistPageMeta(artist.value)
    : placeholderPageMeta(
        notFound.value ? t('detail.notFoundHeading', { kind: kind.value }) : kind.value,
      ),
)
</script>

<template>
  <BaseDetailView
    :empty-text="t('detail.artist.empty')"
    :error="error"
    :events="events"
    :events-error="eventsError"
    :events-loading="eventsLoading"
    :image-url="artist?.imageUrl"
    :kind="kind"
    :loading="loading"
    :name="artist?.name"
    :not-found="notFound"
    :not-found-text="t('detail.artist.notFound')"
    :ready="Boolean(artist)"
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
