<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { type LocationQueryRaw, useRoute, useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import SectionLabel from '@/components/SectionLabel.vue'
import VenueCard from '@/components/VenueCard.vue'
import { useVenueSearch, type VenueSearchParams } from '@/composables/useVenues'
import { DISTRICTS } from '@/lib/districts'

const PAGE_SIZE = 24

const route = useRoute()
const router = useRouter()

// Filters live in the URL query so the list is shareable and survives back/forward.
function queryString(key: string): string {
  const value = route.query[key]
  return typeof value === 'string' ? value : ''
}

const params = computed<VenueSearchParams>(() => ({
  q: queryString('q') || undefined,
  district: queryString('district') || undefined,
  page: queryString('page') ? Number(queryString('page')) : 0,
  size: PAGE_SIZE,
}))

const { data: page, error, loading, run } = useVenueSearch(() => params.value)

// The search box is a local draft applied on submit, kept in sync with the URL.
const search = ref(queryString('q'))
watch(
  () => route.query.q,
  () => {
    search.value = queryString('q')
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
      <SectionLabel as="p">Where it goes down</SectionLabel>
      <h1 class="text-3xl font-bold tracking-tight">Venues</h1>
      <p class="text-muted-foreground">Every stage, club, and hall we track across Berlin.</p>
    </header>

    <div class="flex flex-wrap items-end gap-3 rounded-xl border border-border bg-card p-4">
      <form class="flex gap-2" @submit.prevent="applyFilters({ q: search })">
        <input
          v-model="search"
          class="h-8 rounded-lg border border-border bg-background px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          placeholder="Search venues…"
          type="search"
        />
        <Button type="submit" variant="outline">Search</Button>
      </form>

      <select
        :value="queryString('district')"
        aria-label="Filter by district"
        class="h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        @change="applyFilters({ district: ($event.target as HTMLSelectElement).value })"
      >
        <option value="">All districts</option>
        <option v-for="d in DISTRICTS" :key="d.slug" :value="d.slug">{{ d.label }}</option>
      </select>
    </div>

    <p v-if="loading" class="text-sm text-muted-foreground">Rounding up the rooms…</p>
    <p v-else-if="error" class="text-sm text-destructive">{{ error }}</p>
    <p v-else-if="!page?.content?.length" class="text-sm text-muted-foreground">
      No venues match that search. Try a different name.
    </p>
    <template v-else>
      <p class="text-sm text-muted-foreground">
        {{ page.totalElements }} {{ page.totalElements === 1 ? 'venue' : 'venues' }} found
      </p>
      <div class="grid gap-3 sm:grid-cols-2">
        <VenueCard v-for="venue in page.content" :key="venue.slug" :venue="venue" />
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
