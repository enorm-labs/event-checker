import { expect, test, type Page } from '@playwright/test'

/**
 * Resilient smoke suite.
 *
 * Deliberately shallow: it verifies the app boots, the router mounts each static
 * view, and the shared chrome renders — the cross-cutting breakage that unit tests
 * miss (blank screen, broken lazy-loaded chunk, dead route). It intentionally does
 * NOT assert on data-driven content, so it survives UI churn while the frontend is
 * still in flux.
 *
 * Detail routes (/events/:slug, /venues/:slug, …) are omitted on purpose: they
 * depend on the live BFF and real data. Add those once the API can be mocked
 * (Playwright page.route) or a fixture seeded — see the follow-up note in the PR.
 *
 * The BFF is not running during e2e, so onMounted API calls fail and log
 * console/network errors by design; the views render their error state. We
 * therefore assert on uncaught exceptions (pageerror) only — the true "the app
 * broke" signal — rather than console output.
 */

/** Static routes and the stable <h1> each is expected to mount. */
const staticRoutes = [
  { path: '/', name: 'home', heading: 'Event Junkie' },
  { path: '/events', name: 'events', heading: 'Events' },
  { path: '/calendar', name: 'calendar', heading: 'Calendar' },
  { path: '/about', name: 'about', heading: 'About' },
] as const

/** Attach an uncaught-exception collector before navigation. */
function collectPageErrors(page: Page): string[] {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  return errors
}

for (const route of staticRoutes) {
  test(`mounts the ${route.name} view without crashing`, async ({ page }) => {
    const errors = collectPageErrors(page)

    await page.goto(route.path)

    // View mounted: its landmark and heading are present.
    await expect(page.getByRole('main')).toBeVisible()
    await expect(page.getByRole('heading', { level: 1, name: route.heading })).toBeVisible()

    // Shared app shell rendered.
    await expect(page.getByRole('navigation')).toBeVisible()

    expect(errors, 'unexpected uncaught exceptions').toEqual([])
  })
}

test('navigates between static routes via the nav bar', async ({ page }) => {
  const errors = collectPageErrors(page)

  await page.goto('/')
  const nav = page.getByRole('navigation')

  for (const route of staticRoutes) {
    const linkName = route.name.charAt(0).toUpperCase() + route.name.slice(1)
    await nav.getByRole('link', { name: linkName, exact: true }).click()
    await expect(page).toHaveURL(new RegExp(`${route.path.replace('/', '\\/')}$`))
    await expect(page.getByRole('heading', { level: 1, name: route.heading })).toBeVisible()
  }

  expect(errors, 'unexpected uncaught exceptions').toEqual([])
})

test('app shell exposes a working dark-mode toggle', async ({ page }) => {
  await page.goto('/')

  const toggle = page.getByRole('button', { name: /switch to (dark|light) mode/i })
  await expect(toggle).toBeVisible()

  // New visitors start in dark (the default); toggling switches to light.
  await expect(page.locator('html')).toHaveClass(/dark/)
  await toggle.click()
  await expect(page.locator('html')).not.toHaveClass(/dark/)
})