import { expect, type Page, test } from '@playwright/test'

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
  // `nav` is the accessible name of the nav link — home's is the brand logo, not "Home".
  { path: '/', name: 'home', nav: 'Event Junkie', heading: 'Event Junkie' },
  { path: '/events', name: 'events', nav: 'Events', heading: 'Events' },
  { path: '/venues', name: 'venues', nav: 'Venues', heading: 'Venues' },
  { path: '/calendar', name: 'calendar', nav: 'Calendar', heading: 'Calendar' },
  { path: '/about', name: 'about', nav: 'About', heading: 'About' },
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
    await nav.getByRole('link', { name: route.nav, exact: true }).click()
    await expect(page).toHaveURL(new RegExp(`${route.path.replace('/', '\\/')}$`))
    await expect(page.getByRole('heading', { level: 1, name: route.heading })).toBeVisible()
  }

  expect(errors, 'unexpected uncaught exceptions').toEqual([])
})

test('header nav fits its viewport without overflowing', async ({ page }) => {
  // The header packs a brand lockup, four nav links and two icon controls in; on a ~390px screen
  // one row overflowed and pushed the controls off-screen, so the nav wraps below `sm`. Runs on
  // every project — the two mobile ones are what this actually guards.
  //
  // Scoped to the nav rather than the whole document because this runs without a BFF, so the
  // data-driven routes render their error state. The document-level check lives in
  // home-feeds.spec.ts, where the feeds are mocked and real cards exist to overflow.
  await page.goto('/about')

  const nav = page.getByRole('navigation')
  await expect(nav).toBeVisible()

  const box = await nav.evaluate((el) => ({ scroll: el.scrollWidth, client: el.clientWidth }))
  expect(box.scroll, 'nav content is wider than the nav').toBeLessThanOrEqual(box.client)

  // The right-most control must land inside the viewport, not merely inside a clipped nav.
  const toggle = page.getByRole('button', { name: /switch to (dark|light) mode/i })
  const toggleBox = await toggle.boundingBox()
  const viewport = page.viewportSize()
  expect(toggleBox && viewport && toggleBox.x + toggleBox.width).toBeLessThanOrEqual(
    viewport!.width,
  )
})

test('app shell links to the source repository on GitHub', async ({ page }) => {
  await page.goto('/about')

  const link = page.getByRole('navigation').getByRole('link', { name: 'Source code on GitHub' })
  await expect(link).toBeVisible()
  await expect(link).toHaveAttribute('href', 'https://github.com/enorm-labs/event-checker')
  await expect(link).toHaveAttribute('title', 'Source code on GitHub')
})

test('app shell exposes a working dark-mode toggle', async ({ page }) => {
  await page.goto('/')

  const toggle = page.getByRole('button', { name: /switch to (dark|light) mode/i })
  await expect(toggle).toBeVisible()

  // New visitors start in dark (the default); toggling switches to light.
  await expect(page.locator('html')).toHaveClass(/dark/)
  await expect(toggle).toHaveAttribute('title', 'Switch to light mode')

  await toggle.click()

  await expect(page.locator('html')).not.toHaveClass(/dark/)
  // The tooltip tracks the theme alongside the accessible name — both read from one computed.
  await expect(toggle).toHaveAttribute('title', 'Switch to dark mode')
})
