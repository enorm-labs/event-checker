<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDetailView from '@/components/BaseDetailView.vue'
import { useEventSearch } from '@/composables/useEvents'
import { usePromoter } from '@/composables/usePromoter'

const route = useRoute()
const slug = computed(() => String(route.params.slug))

const {
  data: promoter,
  error,
  notFound,
  loading,
  run: loadPromoter,
} = usePromoter(() => slug.value)
const {
  data: events,
  error: eventsError,
  loading: eventsLoading,
  run: loadEvents,
} = useEventSearch(() => ({ promoter: slug.value, size: 50 }), 'events from this promoter')

function reload() {
  loadPromoter()
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
    :image-url="promoter?.imageUrl"
    :loading="loading"
    :name="promoter?.name"
    :not-found="notFound"
    :ready="Boolean(promoter)"
    empty-text="Nothing on their calendar yet — check back soon."
    kind="Promoter"
    not-found-text="This promoter isn't in our books."
  >
    <template #meta>
      <a
        v-if="promoter?.websiteUrl"
        :href="promoter.websiteUrl"
        class="text-sm text-primary underline-offset-4 hover:underline"
        rel="noopener noreferrer"
        target="_blank"
      >
        Website
      </a>
    </template>
  </BaseDetailView>
</template>
