<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { type LocationQueryRaw, useRoute, useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import EventCard from '@/components/EventCard.vue'
import SectionLabel from '@/components/SectionLabel.vue'
import { type EventSearchParams, useEventSearch } from '@/composables/useEvents'
import { useGenres } from '@/composables/useGenres'
import { useAllVenues } from '@/composables/useVenues'
import { DISTRICTS } from '@/lib/districts'

const PAGE_SIZE = 20
const EVENT_TYPES = [
  'CONCERT',
  'FESTIVAL',
  'PARTY',
  'QUIZ',
  'CLUB_NIGHT',
  'SHOW',
  'SCREENING',
  'EXHIBITION',
  'READING',
  'OTHER',
]

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
  venue: queryString('venue') || undefined,
  district: queryString('district') || undefined,
  genre: queryString('genre') || undefined,
  minPrice: queryString('minPrice') ? Number(queryString('minPrice')) : undefined,
  maxPrice: queryString('maxPrice') ? Number(queryString('maxPrice')) : undefined,
  excludeSoldOut: queryString('excludeSoldOut') === 'true' || undefined,
  free: queryString('free') === 'true' || undefined,
  page: queryString('page') ? Number(queryString('page')) : 0,
  size: PAGE_SIZE,
}))

const genres = useGenres()
const venues = useAllVenues()
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
  venues.run()
  run()
})
watch(() => route.query, run, { deep: true })
</script>

<template>
  <main class="mx-auto max-w-5xl space-y-6 p-8">
    <header class="space-y-1">
      <SectionLabel as="p">Pick your poison</SectionLabel>
      <h1 class="text-3xl font-bold tracking-tight">Events</h1>
      <p class="text-muted-foreground">Browse and filter upcoming music events across Berlin.</p>
    </header>

    <div class="flex flex-wrap items-end gap-3 rounded-xl border border-border bg-card p-4">
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
        :value="queryString('venue')"
        aria-label="Filter by venue"
        class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        @change="applyFilters({ venue: ($event.target as HTMLSelectElement).value })"
      >
        <option value="">All venues</option>
        <option v-for="v in venues.data.value ?? []" :key="v.slug" :value="v.slug ?? ''">
          {{ v.name }}
        </option>
      </select>

      <select
        :value="queryString('district')"
        aria-label="Filter by district"
        class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        @change="applyFilters({ district: ($event.target as HTMLSelectElement).value })"
      >
        <option value="">All districts</option>
        <option v-for="d in DISTRICTS" :key="d.slug" :value="d.slug">{{ d.label }}</option>
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

      <form class="flex items-center gap-2" @submit.prevent="applyFilters({ minPrice, maxPrice })">
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

      <label class="flex h-8 items-center gap-2 text-sm text-muted-foreground">
        <input
          :checked="queryString('excludeSoldOut') === 'true'"
          class="size-4 rounded border-border accent-primary outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          type="checkbox"
          @change="
            applyFilters({
              excludeSoldOut: ($event.target as HTMLInputElement).checked ? 'true' : '',
            })
          "
        />
        Hide sold out
      </label>

      <label class="flex h-8 items-center gap-2 text-sm text-muted-foreground">
        <input
          :checked="queryString('free') === 'true'"
          class="size-4 rounded border-border accent-primary outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          type="checkbox"
          @change="
            applyFilters({ free: ($event.target as HTMLInputElement).checked ? 'true' : '' })
          "
        />
        Free only
      </label>
    </div>

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
