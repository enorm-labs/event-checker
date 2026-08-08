<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDetailView from '@/components/BaseDetailView.vue'
import { usePageMeta } from '@/composables/usePageMeta'
import { placeholderPageMeta, promoterPageMeta } from '@/lib/pageMeta'
import { useEventSearch } from '@/composables/useEvents'
import { usePromoter } from '@/composables/usePromoter'
import { useI18n } from 'vue-i18n'

/** Entity label: the eyebrow above the name, the not-found heading, and the placeholder title. */
const KIND = 'Promoter'

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

const { t } = useI18n()

// Title, description and image for this page — the same values the meta injector will need
// server-side later (ADR-014 §Decision 3). Mirrors the loading / not-found states the view
// itself renders.
usePageMeta(() =>
  promoter.value ? promoterPageMeta(promoter.value) : placeholderPageMeta(notFound.value ? `${KIND} not found` : KIND),
)
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
    :kind="KIND"
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
        {{ t('common.actions.website') }}
      </a>
    </template>
  </BaseDetailView>
</template>
