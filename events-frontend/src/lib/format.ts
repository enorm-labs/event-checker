// Formatting helpers for BFF values. Dates/times arrive as ISO strings (`2026-06-12`, `19:00`)
// and prices as plain numbers; these render them for a Berlin/EU audience.

const DATE_FORMAT = new Intl.DateTimeFormat('en-GB', {
  weekday: 'short',
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

/** Formats an ISO date (`YYYY-MM-DD`) as e.g. "Fri, 12 Jun 2026". Parses parts to avoid UTC shift. */
export function formatDate(isoDate?: string | null): string {
  if (!isoDate) return ''
  const [year, month, day] = isoDate.split('-').map(Number)
  if (!year || !month || !day) return isoDate
  return DATE_FORMAT.format(new Date(year, month - 1, day))
}

/** Trims an ISO time (`HH:mm[:ss]`) down to `HH:mm`. */
export function formatTime(isoTime?: string | null): string {
  if (!isoTime) return ''
  return isoTime.slice(0, 5)
}

/** Formats a numeric amount with its ISO currency code, e.g. "38,00 €". Returns null when unknown. */
export function formatPrice(amount?: number | null, currency?: string | null): string | null {
  if (amount == null) return null
  return new Intl.NumberFormat('de-DE', {
    style: 'currency',
    currency: currency ?? 'EUR',
  }).format(amount)
}

/**
 * The full one-line label for an event — `"<title> @ <venue>"`. Used as the hover tooltip
 * wherever the visible title is clipped (calendar cells, event cards), so those surfaces read
 * the same. Falls back to the bare title when the venue is unknown, rather than leaving a
 * dangling "@".
 */
export function eventLabel(title?: string | null, venueName?: string | null): string {
  if (!title) return venueName ?? ''
  return venueName ? `${title} @ ${venueName}` : title
}

/**
 * Turns an event-type constant into a human label — `CLUB_NIGHT` → "Club night". Deliberately a
 * generic transform rather than a lookup table, so a type added to the BFF enum reads correctly
 * without a matching change here.
 */
export function formatEventType(eventType?: string | null): string {
  if (!eventType) return ''
  const words = eventType.replace(/_/g, ' ').toLowerCase()
  return words.charAt(0).toUpperCase() + words.slice(1)
}

/** Today's date in Berlin as an ISO date string (`YYYY-MM-DD`), for default date filters. */
export function todayIso(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Berlin' }).format(new Date())
}

/**
 * Tomorrow's date in Berlin as an ISO date string (`YYYY-MM-DD`). Used by the Home "Upcoming"
 * feed so it starts the day after today — today's events live in the separate "Tonight" section.
 * Adds a day to the Berlin calendar date (not to `now`), so it's correct across the DST shift.
 */
export function tomorrowIso(): string {
  const next = new Date(`${todayIso()}T00:00:00Z`)
  next.setUTCDate(next.getUTCDate() + 1)
  return next.toISOString().slice(0, 10)
}
