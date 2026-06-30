import { api, unwrap } from '@/api/client'
import type { EventSummary } from '@/api/types'
import { useAsync } from './useAsync'

/** Today's events for the Home "Tonight" section. */
export function useTodayEvents() {
  return useAsync<EventSummary[]>(() => unwrap(api.GET('/events/today')))
}

/** First page of upcoming events from a given start date (inclusive), for the Home feed. */
export function useUpcomingEvents(from: string, size = 12) {
  return useAsync<EventSummary[]>(async () => {
    const page = await unwrap(api.GET('/events', { params: { query: { from, size } } }))
    return page.content ?? []
  })
}

/**
 * Fetches events within an inclusive date range for the calendar. The BFF caps the range at
 * 92 days; standard month/week views stay well within that.
 */
export function fetchCalendarEvents(from: string, to: string): Promise<EventSummary[]> {
  return unwrap(api.GET('/events/calendar', { params: { query: { from, to } } }))
}
