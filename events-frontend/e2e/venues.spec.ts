import { expect, type Page, type Route, test } from '@playwright/test'

/**
 * Venues overview e2e tests with a fully mocked BFF.
 *
 * The list route (`/venues`) is data-driven, so like the detail routes it needs the BFF
 * intercepted with Playwright's request routing — happy path, search, empty state, and
 * pagination are all exercised deterministically without a running backend.
 *
 * Endpoint: GET /api/venues?q=&page=&size= → PageResponseVenueSummaryResponse
 */

/** Collect uncaught exceptions — the "the app broke" signal, as in the smoke suite. */
function collectPageErrors(page: Page): string[] {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  return errors
}

/** Fulfill a matched request with a JSON body. */
function json(route: Route, body: unknown, status = 200): Promise<void> {
  return route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) })
}

/** Matches the venue list (`/api/venues?…` or bare `/api/venues`), not `/api/venues/:slug`. */
const venuesList = /\/api\/venues(\?|$)/

function venue(slug: string, name: string) {
  return { slug, name, city: 'Berlin', district: 'friedrichshain-kreuzberg' }
}

function pageBody(content: ReturnType<typeof venue>[], page = 0, totalPages = 1) {
  return { content, page, size: 24, totalElements: content.length, totalPages }
}

test('lists venues returned by the API', async ({ page }) => {
  const errors = collectPageErrors(page)
  await page.route(venuesList, (route) =>
    json(route, pageBody([venue('lido', 'Lido'), venue('astra', 'Astra Kulturhaus')])),
  )

  await page.goto('/venues')

  await expect(page.getByRole('heading', { level: 1, name: 'Venues' })).toBeVisible()
  await expect(page.getByRole('link', { name: /Lido/ })).toHaveAttribute('href', '/venues/lido')
  await expect(page.getByRole('link', { name: /Astra Kulturhaus/ })).toBeVisible()
  expect(errors, 'unexpected uncaught exceptions').toEqual([])
})

test('searching updates the URL query and re-requests', async ({ page }) => {
  await page.route(venuesList, (route) => {
    const q = new URL(route.request().url()).searchParams.get('q')
    json(route, pageBody(q === 'lido' ? [venue('lido', 'Lido')] : [venue('astra', 'Astra')]))
  })

  await page.goto('/venues')
  await page.getByPlaceholder('Search venues…').fill('lido')
  await page.getByRole('button', { name: 'Search' }).click()

  await expect(page).toHaveURL(/\/venues\?q=lido$/)
  await expect(page.getByRole('link', { name: /Lido/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /Astra/ })).toHaveCount(0)
})

test('filtering by district updates the URL query and re-requests', async ({ page }) => {
  await page.route(venuesList, (route) => {
    const district = new URL(route.request().url()).searchParams.get('district')
    json(
      route,
      pageBody(district === 'mitte' ? [venue('berghain', 'Berghain')] : [venue('lido', 'Lido')]),
    )
  })

  await page.goto('/venues')
  await page.getByLabel('Filter by district').selectOption('mitte')

  await expect(page).toHaveURL(/\/venues\?district=mitte$/)
  await expect(page.getByRole('link', { name: /Berghain/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /Lido/ })).toHaveCount(0)
})

test('shows an empty state when no venues match', async ({ page }) => {
  await page.route(venuesList, (route) => json(route, pageBody([])))

  await page.goto('/venues')

  await expect(page.getByText(/no venues match/i)).toBeVisible()
})

test('paginates when there is more than one page', async ({ page }) => {
  await page.route(venuesList, (route) => {
    const pageParam = Number(new URL(route.request().url()).searchParams.get('page') ?? '0')
    const body = pageBody([venue(`v${pageParam}`, `Venue ${pageParam}`)], pageParam, 2)
    // totalElements drives the counter; keep it above one page for realism.
    json(route, { ...body, totalElements: 2 })
  })

  await page.goto('/venues')
  await expect(page.getByText('Page 1 of 2')).toBeVisible()

  await page.getByRole('button', { name: 'Next' }).click()

  await expect(page).toHaveURL(/\/venues\?page=1$/)
  await expect(page.getByText('Page 2 of 2')).toBeVisible()
})
