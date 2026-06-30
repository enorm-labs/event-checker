import { api, unwrap } from '@/api/client'
import type { VenueDetail } from '@/api/types'
import { useAsync } from './useAsync'

/** Loads a single venue by slug for the venue detail page. Call `run()` to (re)fetch. */
export function useVenue(slug: () => string) {
  return useAsync<VenueDetail>(
    () => unwrap(api.GET('/venues/{slug}', { params: { path: { slug: slug() } } })),
    'this venue',
  )
}
