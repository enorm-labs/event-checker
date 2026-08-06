<script lang="ts" setup>
/**
 * The shared event filter bar, used by both the events list and the calendar.
 *
 * Every control writes straight to the URL query via `useEventFilters`, and reads its current
 * value back from there — so the component holds no filter state of its own and the two views
 * stay in sync with the address bar without prop or event plumbing. The only local state is the
 * two free-text drafts (search box, price range) that are applied on submit rather than on every
 * keystroke; selects and checkboxes apply immediately.
 */
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Button } from '@/components/ui/button'
import { useEventFilters } from '@/composables/useEventFilters'
import { useGenres } from '@/composables/useGenres'
import { useAllVenues } from '@/composables/useVenues'
import { DISTRICTS } from '@/lib/districts'

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
const { queryString, applyFilters } = useEventFilters()

const genres = useGenres()
const venues = useAllVenues()

// Drafts are seeded from the URL and re-synced whenever it changes elsewhere (back/forward,
// a link with filters, another control resetting the query).
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

onMounted(() => {
  genres.run()
  venues.run()
})
</script>

<template>
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
      aria-label="Filter by event type"
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
      aria-label="Filter by genre"
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
        @change="applyFilters({ free: ($event.target as HTMLInputElement).checked ? 'true' : '' })"
      />
      Free only
    </label>
  </div>
</template>
