<script lang="ts" setup>
import { computed, onMounted, watch } from 'vue'
import { type LocationQueryRaw, useRoute, useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import EventFilterBar from '@/components/EventFilterBar.vue'
import SectionLabel from '@/components/SectionLabel.vue'
import { type EventSearchParams, useEventSearch } from '@/composables/useEvents'
import { useEventFilters } from '@/composables/useEventFilters'

const PAGE_SIZE = 20

const route = useRoute()
const router = useRouter()

// Filters live in the URL query so list views are shareable and survive back/forward; the
// filter bar writes them and `useEventFilters` reads them back (see EventFilterBar.vue).
const { queryString, filters } = useEventFilters()

const params = computed<EventSearchParams>(() => ({
  ...filters.value,
  page: queryString('page') ? Number(queryString('page')) : 0,
  size: PAGE_SIZE,
}))

const { data: page, error, loading, run } = useEventSearch(() => params.value)

const currentPage = computed(() => page.value?.page ?? 0)
const totalPages = computed(() => page.value?.totalPages ?? 0)

function goToPage(target: number) {
  // Unlike filter changes, paging keeps the current filters and only moves the page.
  const next: LocationQueryRaw = { ...route.query, page: target > 0 ? String(target) : undefined }
  if (next.page === undefined) delete next.page
  router.push({ query: next })
}

onMounted(run)
watch(() => route.query, run, { deep: true })
</script>

<template>
  <main class="mx-auto max-w-5xl space-y-6 p-8">
    <header class="space-y-1">
      <SectionLabel as="p">Pick your poison</SectionLabel>
      <h1 class="text-3xl font-bold tracking-tight">Events</h1>
      <p class="text-muted-foreground">Browse and filter upcoming music events across Berlin.</p>
    </header>

    <EventFilterBar />

    <p v-if="loading" class="text-sm text-muted-foreground">Cueing it up…</p>
    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>
    <p v-else-if="!page?.content?.length" class="text-sm text-muted-foreground">
      Nothing matches those filters. Ease up and try again.
    </p>
    <template v-else>
      <p class="text-sm text-muted-foreground">
        {{ page.totalElements }} {{ page.totalElements === 1 ? 'event' : 'events' }} found
      </p>
      <div class="grid gap-3 sm:grid-cols-2">
        <EventCard v-for="event in page.content" :key="event.slug" :event="event" />
      </div>

      <div v-if="totalPages > 1" class="flex items-center justify-between gap-3 pt-2">
        <Button :disabled="currentPage <= 0" variant="outline" @click="goToPage(currentPage - 1)">
          Previous
        </Button>
        <span class="text-sm text-muted-foreground">
          Page {{ currentPage + 1 }} of {{ totalPages }}
        </span>
        <Button
          :disabled="currentPage >= totalPages - 1"
          variant="outline"
          @click="goToPage(currentPage + 1)"
        >
          Next
        </Button>
      </div>
    </template>
  </main>
</template>
