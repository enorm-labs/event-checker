<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDetailView from '@/components/BaseDetailView.vue'
import { usePageMeta } from '@/composables/usePageMeta'
import { placeholderPageMeta, promoterPageMeta } from '@/lib/pageMeta'
import { useEventSearch } from '@/composables/useEvents'
import { usePromoter } from '@/composables/usePromoter'
import { useI18n } from 'vue-i18n'

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

/**
 * Entity label: the eyebrow above the name, the not-found heading, and the placeholder title.
 *
 * A `computed` rather than a constant because it has to follow the active locale — the locale can
 * change without this view remounting, since the switcher only rewrites the URL's locale segment.
 */
const kind = computed(() => t('detail.promoter.kind'))

// Title, description and image for this page — the same values the meta injector will need
// server-side later (ADR-014 §Decision 3). Mirrors the loading / not-found states the view
// itself renders.
usePageMeta(() =>
  promoter.value
    ? promoterPageMeta(promoter.value)
    : placeholderPageMeta(
        notFound.value ? t('detail.notFoundHeading', { kind: kind.value }) : kind.value,
      ),
)
</script>

<template>
  <BaseDetailView
    :empty-text="t('detail.promoter.empty')"
    :error="error"
    :events="events"
    :events-error="eventsError"
    :events-loading="eventsLoading"
    :image-url="promoter?.imageUrl"
    :kind="kind"
    :loading="loading"
    :name="promoter?.name"
    :not-found="notFound"
    :not-found-text="t('detail.promoter.notFound')"
    :ready="Boolean(promoter)"
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
