import { api, unwrap } from '@/api/client'
import type { VenuePage } from '@/api/types'
import { useAsync } from './useAsync'

/** Query parameters accepted by the venue list endpoint (`GET /venues`). */
export interface VenueSearchParams {
  q?: string
  page?: number
  size?: number
  sort?: string[]
}

/**
 * Paged venue search for the venues overview page. `params` is read lazily on each `run()`,
 * so callers re-run after changing the search term or the page.
 */
export function useVenueSearch(params: () => VenueSearchParams, label = 'venues') {
  return useAsync<VenuePage>(
    () => unwrap(api.GET('/venues', { params: { query: params() } })),
    label,
  )
}
