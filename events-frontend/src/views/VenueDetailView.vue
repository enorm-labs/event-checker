<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDetailView from '@/components/BaseDetailView.vue'
import { usePageMeta } from '@/composables/usePageMeta'
import { placeholderPageMeta, venuePageMeta } from '@/lib/pageMeta'
import { useEventSearch } from '@/composables/useEvents'
import { useVenue } from '@/composables/useVenue'
import { useI18n } from 'vue-i18n'
import { APP_NAME } from '@/lib/pageMeta'
import { useStructuredData } from '@/composables/useStructuredData'
import { breadcrumbJsonLd, venueJsonLd, type JsonLd } from '@/lib/structuredData'
import type { Locale } from '@/i18n/locales'

/** Entity label: the eyebrow above the name, the not-found heading, and the placeholder title. */
const KIND = 'Venue'

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

const { t, locale } = useI18n()

// A MusicVenue carries the address and coordinates the page already displays. No rich result rides
// on it the way it does for events, but it is accurate and it is what ties an event's `location`
// to a real place. See lib/structuredData.ts.
useStructuredData((): JsonLd[] => {
  const current = venue.value
  if (!current?.slug || !current.name) return []

  return [
    venueJsonLd(current, locale.value as Locale),
    breadcrumbJsonLd(
      [
        [APP_NAME, ''],
        [t('common.nav.venues'), '/venues'],
        [current.name, `/venues/${current.slug}`],
      ],
      locale.value as Locale,
    ),
  ].filter((document): document is JsonLd => document !== null)
})

// Title, description and image for this page — the same values the meta injector will need
// server-side later (ADR-014 §Decision 3). Mirrors the loading / not-found states the view
// itself renders.
usePageMeta(() =>
  venue.value ? venuePageMeta(venue.value) : placeholderPageMeta(notFound.value ? `${KIND} not found` : KIND),
)
</script>

<template>
  <BaseDetailView
    :error="error"
    :events="events"
    :events-error="eventsError"
    :events-loading="eventsLoading"
    :image-url="venue?.imageUrl"
    :loading="loading"
    :name="venue?.name"
    :not-found="notFound"
    :ready="Boolean(venue)"
    empty-text="No upcoming nights here yet — check back soon."
    :kind="KIND"
    not-found-text="That venue isn't in our little black book."
  >
    <template #meta>
      <p v-if="addressLine" class="text-muted-foreground">{{ addressLine }}</p>
      <a
        v-if="venue?.websiteUrl"
        :href="venue.websiteUrl"
        class="text-sm text-primary underline-offset-4 hover:underline"
        rel="noopener noreferrer"
        target="_blank"
      >
        {{ t('common.actions.website') }}
      </a>
    </template>

    <p v-if="venue?.description" class="whitespace-pre-line text-foreground/90">
      {{ venue.description }}
    </p>
  </BaseDetailView>
</template>
