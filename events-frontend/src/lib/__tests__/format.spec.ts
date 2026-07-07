import { afterEach, describe, expect, it, vi } from 'vitest'

import { todayIso, tomorrowIso } from '@/lib/format'

describe('date helpers', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('tomorrowIso is the calendar day after todayIso', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-07T12:00:00Z'))

    expect(todayIso()).toBe('2026-07-07')
    expect(tomorrowIso()).toBe('2026-07-08')
  })

  it('rolls over month and year boundaries', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-12-31T12:00:00Z'))

    expect(tomorrowIso()).toBe('2027-01-01')
  })

  it('advances by one calendar day across the spring DST shift', () => {
    // Europe/Berlin springs forward on 2026-03-29; adding a day to the calendar date
    // (not 24h to a timestamp) must still land on the 29th.
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-28T12:00:00Z'))

    expect(tomorrowIso()).toBe('2026-03-29')
  })
})
