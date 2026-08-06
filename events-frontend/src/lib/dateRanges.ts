// Shortcut date ranges for the events filter bar — the questions people actually ask
// ("what's on tonight?", "anything this weekend?") rather than two dates they have to pick.
// All arithmetic is on the Berlin calendar date, matching the rest of the app.

import { todayIso } from './format'

/** An inclusive date range as ISO `YYYY-MM-DD` strings, the shape the BFF's from/to expect. */
export interface DateRange {
  from: string
  to: string
}

const FRIDAY = 5
const SUNDAY = 7

/** Adds whole calendar days to an ISO date. UTC arithmetic, so a DST shift can't move it. */
function addDays(isoDate: string, days: number): string {
  const date = new Date(`${isoDate}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + days)
  return date.toISOString().slice(0, 10)
}

/** ISO-8601 weekday for a date: 1 = Monday … 7 = Sunday (the EU convention this app uses). */
function isoWeekday(isoDate: string): number {
  return new Date(`${isoDate}T00:00:00Z`).getUTCDay() || 7
}

/** Today only. */
export function tonight(): DateRange {
  const today = todayIso()
  return { from: today, to: today }
}

/**
 * Friday through Sunday of the current week. From Friday onwards it starts today instead — the
 * past isn't selectable, and a weekend already under way should show what's left of it — so on a
 * Sunday this is Sunday alone.
 */
export function thisWeekend(): DateRange {
  const today = todayIso()
  const weekday = isoWeekday(today)
  return {
    from: weekday < FRIDAY ? addDays(today, FRIDAY - weekday) : today,
    to: addDays(today, SUNDAY - weekday),
  }
}

/** Today plus the following six days — a full week counting today. */
export function nextSevenDays(): DateRange {
  const today = todayIso()
  return { from: today, to: addDays(today, 6) }
}

/**
 * The presets the filter bar offers, in display order. Each range is computed on click rather
 * than up front, so a page left open overnight still resolves "tonight" against the current day.
 */
export const DATE_PRESETS: readonly { label: string; range: () => DateRange }[] = [
  { label: 'Tonight', range: tonight },
  { label: 'This weekend', range: thisWeekend },
  { label: 'Next 7 days', range: nextSevenDays },
]
