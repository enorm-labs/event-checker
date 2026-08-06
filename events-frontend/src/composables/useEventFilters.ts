import { computed } from 'vue'
import { type LocationQueryRaw, useRoute, useRouter } from 'vue-router'
import type { EventFilterValues } from './useEvents'

/**
 * Reads and writes the event filters that live in the URL query, so any view showing the filter
 * bar is shareable and survives back/forward. The bar itself and the views that consume the
 * result both call this — the URL is the single source of truth, so no state is passed between
 * them.
 */
export function useEventFilters() {
  const route = useRoute()
  const router = useRouter()

  /** The raw query value for `key`, or `''` when absent or repeated (`?q=a&q=b`). */
  function queryString(key: string): string {
    const value = route.query[key]
    return typeof value === 'string' ? value : ''
  }

  const filters = computed<EventFilterValues>(() => ({
    q: queryString('q') || undefined,
    eventType: queryString('eventType') || undefined,
    venue: queryString('venue') || undefined,
    district: queryString('district') || undefined,
    genre: queryString('genre') || undefined,
    minPrice: queryString('minPrice') ? Number(queryString('minPrice')) : undefined,
    maxPrice: queryString('maxPrice') ? Number(queryString('maxPrice')) : undefined,
    excludeSoldOut: queryString('excludeSoldOut') === 'true' || undefined,
    free: queryString('free') === 'true' || undefined,
  }))

  function applyFilters(patch: LocationQueryRaw) {
    // Any filter change resets to the first page; empty values drop out of the URL.
    const next: LocationQueryRaw = { ...route.query, ...patch, page: undefined }
    for (const key of Object.keys(next)) {
      if (next[key] === '' || next[key] === undefined) delete next[key]
    }
    router.push({ query: next })
  }

  return { queryString, filters, applyFilters }
}
