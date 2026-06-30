<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import { useEventSearch, type EventSearchParams } from '@/composables/useEvents'
import { useGenres } from '@/composables/useGenres'

const PAGE_SIZE = 20
const EVENT_TYPES = ['CONCERT', 'FESTIVAL', 'PARTY', 'QUIZ', 'CLUB_NIGHT', 'SHOW', 'OTHER']

const route = useRoute()
const router = useRouter()

// Filters live in the URL query so list views are shareable and survive back/forward.
function queryString(key: string): string {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

const params = computed<EventSearchParams>(() => ({
  q: queryString('q') || undefined,
  eventType: queryString('eventType') || undefined,
  genre: queryString('genre') || undefined,
  minPrice: queryString('minPrice') ? Number(queryString('minPrice')) : undefined,
  maxPrice: queryString('maxPrice') ? Number(queryString('maxPrice')) : undefined,
  page: queryString('page') ? Number(queryString('page')) : 0,
  size: PAGE_SIZE,
}))

const genres = useGenres()
const { data: page, error, loading, run } = useEventSearch(() => params.value)

// Search box and price range are local drafts applied on submit; selects apply immediately.
const search = ref(queryString('q'))
watch(
  () => route.query.q,
  () => {
    search.value = queryString('q')
  },
)

const minPrice = ref(queryString('minPrice'))
const maxPrice = ref(queryString('maxPrice'))
watch(
  () => [route.query.minPrice, route.query.maxPrice],
  () => {
    minPrice.value = queryString('minPrice')
    maxPrice.value = queryString('maxPrice')
  },
)

const currentPage = computed(() => page.value?.page ?? 0)
const totalPages = computed(() => page.value?.totalPages ?? 0)

function applyFilters(patch: LocationQueryRaw) {
  // Any filter change resets to the first page; empty values drop out of the URL.
  const next: LocationQueryRaw = { ...route.query, ...patch, page: undefined }
  for (const key of Object.keys(next)) {
    if (next[key] === '' || next[key] === undefined) delete next[key]
  }
  router.push({ query: next })
}

function goToPage(target: number) {
  // Unlike filter changes, paging keeps the current filters and only moves the page.
  const next: LocationQueryRaw = { ...route.query, page: target > 0 ? String(target) : undefined }
  if (next.page === undefined) delete next.page
  router.push({ query: next })
}

onMounted(() => {
  genres.run()
  run()
})
watch(() => route.query, run, { deep: true })
</script>

<template>
  <main class="mx-auto max-w-5xl space-y-6 p-8">
    <header class="space-y-1">
      <h1 class="text-3xl font-bold tracking-tight">Events</h1>
      <p class="text-muted-foreground">Browse and filter upcoming music events across Berlin.</p>
    </header>

    <div class="flex flex-wrap items-end gap-3">
      <form class="flex gap-2" @submit.prevent="applyFilters({ q: search })">
        <input
          v-model="search"
          class="h-8 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          placeholder="Search events…"
          type="search"
        />
        <Button type="submit" variant="outline">Search</Button>
      </form>

      <select
        :value="queryString('eventType')"
        class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        @change="applyFilters({ eventType: ($event.target as HTMLSelectElement).value })"
      >
        <option value="">All types</option>
        <option v-for="type in EVENT_TYPES" :key="type" :value="type">{{ type }}</option>
      </select>

      <select
        :value="queryString('genre')"
        class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        @change="applyFilters({ genre: ($event.target as HTMLSelectElement).value })"
      >
        <option value="">All genres</option>
        <option v-for="tag in genres.data.value ?? []" :key="tag.slug" :value="tag.slug ?? ''">
          {{ tag.name }}
        </option>
      </select>

      <form
        class="flex items-center gap-2"
        @submit.prevent="applyFilters({ minPrice, maxPrice })"
      >
        <input
          v-model="minPrice"
          aria-label="Minimum presale price"
          class="h-8 w-20 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          inputmode="decimal"
          min="0"
          placeholder="Min €"
          step="0.01"
          type="number"
        />
        <span class="text-sm text-muted-foreground">–</span>
        <input
          v-model="maxPrice"
          aria-label="Maximum presale price"
          class="h-8 w-20 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          inputmode="decimal"
          min="0"
          placeholder="Max €"
          step="0.01"
          type="number"
        />
        <Button type="submit" variant="outline">Apply</Button>
      </form>
    </div>

    <p v-if="loading" class="text-sm text-muted-foreground">Loading…</p>
    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>
    <p v-else-if="!page?.content?.length" class="text-sm text-muted-foreground">
      No events match these filters.
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
