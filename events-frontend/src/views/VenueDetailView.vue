<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDetailView from '@/components/BaseDetailView.vue'
import { useEventSearch } from '@/composables/useEvents'
import { useVenue } from '@/composables/useVenue'

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
    kind="Venue"
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
        Website
      </a>
    </template>

    <p v-if="venue?.description" class="whitespace-pre-line text-foreground/90">
      {{ venue.description }}
    </p>
  </BaseDetailView>
</template>
