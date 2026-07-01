import { expect, test, type Page, type Route } from '@playwright/test'

/**
 * Calendar view e2e tests with a mocked BFF.
 *
 * The calendar fetches `GET /api/events/calendar?from=&to=` whenever FullCalendar's
 * visible range changes — on initial render and on every prev/next/today or view
 * switch. We mock that endpoint and key its response off the requested `from`, so a
 * deterministic event always lands in the visible window regardless of the machine's
 * clock (FullCalendar opens on the real "today").
 *
 * FullCalendar renders events that carry a URL as <a> links; the view intercepts the
 * click and navigates via vue-router instead. Toolbar controls are plain buttons:
 * prev/next/today (aria-label) and month/week/list (text).
 *
 * The feed matcher (`/events/calendar?…`) and the event-detail matcher
 * (`/events/calendar-gig`) are deliberately non-overlapping so they don't collide.
 */

function collectPageErrors(page: Page): string[] {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  return errors
}

function json(route: Route, body: unknown, status = 200): Promise<void> {
  return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
}

const calendarFeed = /\/api\/events\/calendar(\?|$)/

/** Records the `from` param of every calendar feed request, to assert refetches. */
function collectCalendarFroms(page: Page): string[] {
  const froms: string[] = []
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (url.pathname.endsWith('/events/calendar')) {
      const from = url.searchParams.get('from')
      if (from) froms.push(from)
    }
  })
  return froms
}

test.beforeEach(async ({ page }) => {
  // Place a single event on the first visible day of whatever range is requested, so it
  // renders in every view (month/week/list) without depending on the current date.
  await page.route(calendarFeed, (route) => {
    const from = new URL(route.request().url()).searchParams.get('from') ?? '2026-07-01'
    return json(route, [
      { slug: 'calendar-gig', title: 'Calendar Gig', eventDate: from, startTime: '20:00' },
    ])
  })
})

test('renders events and opens the event detail on click', async ({ page }) => {
  const errors = collectPageErrors(page)
  await page.route(/\/api\/events\/calendar-gig/, (route) =>
    json(route, { slug: 'calendar-gig', title: 'Calendar Gig', eventDate: '2026-08-15' }),
  )

  await page.goto('/calendar')

  const eventLink = page.getByRole('link', { name: /Calendar Gig/ })
  await expect(eventLink).toBeVisible()

  await eventLink.click()

  await expect(page).toHaveURL(/\/events\/calendar-gig$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Calendar Gig' })).toBeVisible()
  expect(errors, 'unexpected uncaught exceptions').toEqual([])
})

test('refetches events when navigating to the next month', async ({ page }) => {
  const froms = collectCalendarFroms(page)
  await page.goto('/calendar')
  await expect(page.getByRole('link', { name: /Calendar Gig/ })).toBeVisible()

  const initialCount = froms.length
  const firstFrom = froms.at(-1) ?? ''

  await page.getByRole('button', { name: 'next' }).click()

  await expect.poll(() => froms.length).toBeGreaterThan(initialCount)
  expect(froms.at(-1)! > firstFrom, 'next month should request a later range').toBe(true)
  await expect(page.getByRole('link', { name: /Calendar Gig/ })).toBeVisible()
})

test('refetches when switching the calendar view', async ({ page }) => {
  const froms = collectCalendarFroms(page)
  await page.goto('/calendar')
  await expect(page.getByRole('link', { name: /Calendar Gig/ })).toBeVisible()

  const initialCount = froms.length

  await page.getByRole('button', { name: 'list' }).click()

  await expect.poll(() => froms.length).toBeGreaterThan(initialCount)
})

test('shows an error state when the calendar feed fails', async ({ page }) => {
  await page.route(calendarFeed, (route) => json(route, { message: 'boom' }, 500))

  await page.goto('/calendar')

  // The Calendar heading still renders; the feed failure surfaces the describeError copy.
  await expect(page.getByRole('heading', { level: 1, name: 'Calendar' })).toBeVisible()
  await expect(page.getByText(/couldn't load the calendar/i)).toBeVisible()
})
