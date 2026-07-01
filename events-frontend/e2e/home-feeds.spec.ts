import { expect, test, type Page, type Route } from '@playwright/test'

/**
 * Home page feed e2e tests with a mocked BFF.
 *
 * The home page loads two independent feeds on mount:
 *   Tonight  → GET /api/events/today        (an EventSummary[] array)
 *   Upcoming → GET /api/events?from=&size=12 (an EventPage; the view reads .content)
 *
 * Each feed has its own loading / error / empty / list state, so the tests exercise
 * them independently. The two matchers are non-overlapping (`/events/today` vs
 * `/events?…`) so route registration order does not matter.
 */

function collectPageErrors(page: Page): string[] {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  return errors
}

function json(route: Route, body: unknown, status = 200): Promise<void> {
  return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
}

const slugify = (title: string) => title.toLowerCase().replace(/\s+/g, '-')

/** Build an EventPage payload from a list of event titles (for the Upcoming feed). */
function eventPage(titles: string[]) {
  return {
    content: titles.map((title) => ({ slug: slugify(title), title, eventDate: '2026-08-15' })),
    page: 0,
    size: 12,
    totalElements: titles.length,
    totalPages: titles.length ? 1 : 0,
  }
}

const todayFeed = /\/api\/events\/today/
const upcomingFeed = /\/api\/events(\?|$)/

const todayEvents = [
  { slug: 'tonight-show', title: 'Tonight Show', eventDate: '2026-07-01', startTime: '21:00' },
]

test.beforeEach(async ({ page }) => {
  await page.route(todayFeed, (route) => json(route, todayEvents))
  await page.route(upcomingFeed, (route) => json(route, eventPage(['Upcoming One', 'Upcoming Two'])))
})

const eventHeading = (page: Page, name: string) =>
  page.getByRole('heading', { level: 3, name })

test('renders the tonight and upcoming feeds', async ({ page }) => {
  const errors = collectPageErrors(page)
  await page.goto('/')

  await expect(page.getByRole('heading', { level: 2, name: 'Tonight' })).toBeVisible()
  await expect(page.getByRole('heading', { level: 2, name: 'Upcoming' })).toBeVisible()

  await expect(eventHeading(page, 'Tonight Show')).toBeVisible()
  await expect(eventHeading(page, 'Upcoming One')).toBeVisible()
  await expect(eventHeading(page, 'Upcoming Two')).toBeVisible()
  expect(errors, 'unexpected uncaught exceptions').toEqual([])
})

test('shows empty states when both feeds are empty', async ({ page }) => {
  await page.route(todayFeed, (route) => json(route, []))
  await page.route(upcomingFeed, (route) => json(route, eventPage([])))

  await page.goto('/')

  await expect(page.getByText('Nothing on tonight.')).toBeVisible()
  await expect(page.getByText('No upcoming events found.')).toBeVisible()
})

test('shows an error in one feed without affecting the other', async ({ page }) => {
  // Tonight fails; Upcoming keeps the default (successful) mock.
  await page.route(todayFeed, (route) => json(route, { message: 'boom' }, 500))

  await page.goto('/')

  await expect(page.getByText(/couldn't load tonight's events/i)).toBeVisible()
  await expect(eventHeading(page, 'Upcoming One')).toBeVisible()
})

test('navigates to an event detail from a feed card', async ({ page }) => {
  await page.route(/\/api\/events\/tonight-show/, (route) =>
    json(route, { slug: 'tonight-show', title: 'Tonight Show', eventDate: '2026-07-01' }),
  )

  await page.goto('/')

  await page.getByRole('link', { name: /Tonight Show/ }).click()

  await expect(page).toHaveURL(/\/events\/tonight-show$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Tonight Show' })).toBeVisible()
})
