import { expect, test, type Page, type Route } from '@playwright/test'

/**
 * Events list-page filtering e2e tests with a mocked BFF.
 *
 * The Events view keeps every filter in the URL query and re-fetches
 * `GET /api/events?…` whenever the query changes. We mock that endpoint with a
 * handler that keys its response off the incoming query params: asserting both
 * the rendered result and the resulting URL therefore proves the frontend
 * serialized and sent the right filter, end to end, without a real backend.
 *
 * Results render as an <h3> event title per card, so tests assert on those
 * headings; the empty state and pagination controls are asserted by their copy.
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

/** Build an EventPage payload from a list of event titles. */
function eventPage(
  titles: string[],
  opts: { page?: number; totalPages?: number; totalElements?: number } = {},
) {
  return {
    content: titles.map((title) => ({ slug: slugify(title), title, eventDate: '2026-08-15' })),
    page: opts.page ?? 0,
    size: 20,
    totalElements: opts.totalElements ?? titles.length,
    totalPages: opts.totalPages ?? (titles.length ? 1 : 0),
  }
}

/**
 * Response keyed off the search query params. Each filter maps to a distinct
 * result, so a rendered title uniquely identifies which filter reached the BFF.
 * The unfiltered default spans two pages so pagination can be exercised.
 */
function eventsResponseFor(sp: URLSearchParams) {
  if (sp.get('q') === 'nothing') return eventPage([])
  if (sp.get('q') === 'jazz') return eventPage(['Jazz Night'])
  if (sp.get('eventType') === 'FESTIVAL') return eventPage(['Big Festival'])
  if (sp.get('genre') === 'techno') return eventPage(['Techno Rave'])
  if (sp.get('minPrice') || sp.get('maxPrice')) return eventPage(['Cheap Gig'])

  return Number(sp.get('page') ?? '0') >= 1
    ? eventPage(['Second Page Event'], { page: 1, totalPages: 2, totalElements: 21 })
    : eventPage(['Default Event A', 'Default Event B'], {
        page: 0,
        totalPages: 2,
        totalElements: 21,
      })
}

test.beforeEach(async ({ page }) => {
  // Populate the genre dropdown so its options can be selected.
  await page.route(/\/api\/genres/, (route) =>
    json(route, [
      { slug: 'techno', name: 'Techno' },
      { slug: 'jazz', name: 'Jazz' },
    ]),
  )
  // Serve the search feed based on the query params the frontend sends.
  await page.route(/\/api\/events(\?|$)/, (route) =>
    json(route, eventsResponseFor(new URL(route.request().url()).searchParams)),
  )
})

/** The native <select> that contains the given placeholder option. */
function selectWithOption(page: Page, optionName: string) {
  return page.locator('select', { has: page.getByRole('option', { name: optionName }) })
}

const eventHeading = (page: Page, name: string) =>
  page.getByRole('heading', { level: 3, name })

test('filters by search query', async ({ page }) => {
  const errors = collectPageErrors(page)
  await page.goto('/events')
  await expect(eventHeading(page, 'Default Event A')).toBeVisible()

  await page.getByRole('searchbox').fill('jazz')
  await page.getByRole('button', { name: 'Search' }).click()

  await expect(page).toHaveURL(/[?&]q=jazz\b/)
  await expect(eventHeading(page, 'Jazz Night')).toBeVisible()
  await expect(eventHeading(page, 'Default Event A')).toHaveCount(0)
  expect(errors, 'unexpected uncaught exceptions').toEqual([])
})

test('filters by event type', async ({ page }) => {
  await page.goto('/events')
  await expect(eventHeading(page, 'Default Event A')).toBeVisible()

  await selectWithOption(page, 'All types').selectOption('FESTIVAL')

  await expect(page).toHaveURL(/[?&]eventType=FESTIVAL\b/)
  await expect(eventHeading(page, 'Big Festival')).toBeVisible()
})

test('filters by genre', async ({ page }) => {
  await page.goto('/events')
  await expect(eventHeading(page, 'Default Event A')).toBeVisible()

  await selectWithOption(page, 'All genres').selectOption('techno')

  await expect(page).toHaveURL(/[?&]genre=techno\b/)
  await expect(eventHeading(page, 'Techno Rave')).toBeVisible()
})

test('filters by price range', async ({ page }) => {
  await page.goto('/events')
  await expect(eventHeading(page, 'Default Event A')).toBeVisible()

  await page.getByLabel('Minimum presale price').fill('10')
  await page.getByLabel('Maximum presale price').fill('30')
  await page.getByRole('button', { name: 'Apply' }).click()

  await expect(page).toHaveURL(/[?&]minPrice=10\b/)
  await expect(page).toHaveURL(/[?&]maxPrice=30\b/)
  await expect(eventHeading(page, 'Cheap Gig')).toBeVisible()
})

test('shows the empty state when no events match', async ({ page }) => {
  await page.goto('/events')
  await expect(eventHeading(page, 'Default Event A')).toBeVisible()

  await page.getByRole('searchbox').fill('nothing')
  await page.getByRole('button', { name: 'Search' }).click()

  await expect(page.getByText('No events match these filters.')).toBeVisible()
  await expect(eventHeading(page, 'Default Event A')).toHaveCount(0)
})

test('paginates through results, preserving no filter', async ({ page }) => {
  const errors = collectPageErrors(page)
  await page.goto('/events')

  await expect(eventHeading(page, 'Default Event A')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Previous' })).toBeDisabled()

  await page.getByRole('button', { name: 'Next' }).click()

  await expect(page).toHaveURL(/[?&]page=1\b/)
  await expect(eventHeading(page, 'Second Page Event')).toBeVisible()
  await expect(page.getByText('Page 2 of 2')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Next' })).toBeDisabled()
  expect(errors, 'unexpected uncaught exceptions').toEqual([])
})
