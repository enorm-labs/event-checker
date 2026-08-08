import AxeBuilder from '@axe-core/playwright'
import { expect, type Page, type Route, test } from '@playwright/test'

/**
 * Automated accessibility sweep — the runtime half of the WCAG 2.1 AA target
 * (docs/LEGAL.md §12). Run it on its own with `npm run test:a11y`.
 *
 * axe catches what `eslint-plugin-vuejs-accessibility` cannot see from the source: colour
 * contrast against the resolved theme tokens, focus order, landmark structure, and duplicate IDs.
 * It is not a conformance certificate — axe reliably finds roughly a third of WCAG issues — but
 * it is what stops the accessibility already in this codebase from regressing silently.
 *
 * Two passes, because they fail for different reasons:
 *
 *   1. **Static routes**, with no BFF. Data-driven views render their error state, which still
 *      exercises the shared chrome — skip link, header, footer — where the repeated content lives.
 *   2. **Data-driven routes, with the BFF mocked.** Without this pass the components that carry
 *      almost all of the interactive markup — the event and venue cards, the filter bar's selects
 *      and checkboxes, pagination, the detail layout — are never scanned at all, because an error
 *      state renders none of them. The mocks are deliberately small; axe needs the elements to
 *      exist, not the data to be realistic.
 */

// Both locales. German is reliably longer than English, so it is where a layout overflow or a
// contrast regression actually shows up — sweeping only `/en` would miss exactly the cases the
// translation introduces (see AGENTS.md §Testing — locale strategy).
const PATHS = ['', '/events', '/venues', '/calendar', '/about', '/legal/imprint', '/legal/privacy', '/legal/notices']
const staticRoutes = ['en', 'de'].flatMap((locale) => PATHS.map((path) => `/${locale}${path}`))

/** The conformance target. `best-practice` is deliberately excluded: useful, but not the bar. */
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']

/**
 * Locally the suite runs against the Vite dev server, which injects the `vite-plugin-vue-devtools`
 * floating panel; its button carries an ARIA attribute axe rejects. It is not our markup and never
 * reaches a build — CI already runs against `npm run preview`, where the anchor does not exist, so
 * excluding it costs no coverage and keeps the local and CI results identical.
 */
function buildScan(page: Page): AxeBuilder {
  return new AxeBuilder({ page }).exclude('#__vue-devtools-container__').withTags(TAGS)
}

for (const path of staticRoutes) {
  test(`${path} has no detectable accessibility violations`, async ({ page }) => {
    await page.goto(path)
    await expect(page.getByRole('main')).toBeVisible()

    const results = await buildScan(page).analyze()

    // Name the offending rules and elements in the failure message — a bare count is unactionable.
    expect(
      results.violations.map((v) => ({
        rule: v.id,
        impact: v.impact,
        help: v.help,
        nodes: v.nodes.map((n) => n.target.join(' ')),
      })),
    ).toEqual([])
  })
}

test('both themes pass contrast, not just the default', async ({ page }) => {
  // New visitors get dark; the toggle is the only way into light, and its palette is a separate
  // set of tokens that no other test exercises for contrast.
  await page.goto('/about')
  await page.getByRole('button', { name: /switch to light mode/i }).click()
  await expect(page.locator('html')).not.toHaveClass(/dark/)

  const results = await buildScan(page).analyze()

  expect(results.violations.map((v) => ({ rule: v.id, nodes: v.nodes.length }))).toEqual([])
})

test('the skip link is hidden until focused and moves focus to the content', async ({ page }) => {
  await page.goto('/about')

  const skipLink = page.getByRole('link', { name: 'Skip to content' })

  // Present but not occupying layout for sighted users who never tab.
  await expect(skipLink).toBeAttached()
  await expect(skipLink).not.toBeInViewport()

  await skipLink.focus()
  await expect(skipLink).toBeVisible()

  await skipLink.press('Enter')

  await expect(page.locator('#main-content')).toBeFocused()
})

/**
 * Pass 2 — the data-driven routes, with the BFF mocked.
 *
 * Payloads are shaped after the ones in detail-routes.spec.ts / events-filters.spec.ts, trimmed to
 * what makes the components render. English only: this pass is about markup that the static pass
 * never reaches, and the German locale's contribution — longer strings — is already covered above
 * on the routes where the layout is shared.
 */
function json(route: Route, body: unknown): Promise<void> {
  return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
}

const eventSummaries = [
  {
    slug: 'tonight-show',
    title: 'Tonight Show',
    subtitle: 'With a support act',
    eventDate: '2026-08-15',
    startTime: '21:00',
    venue: { slug: 'mock-venue', name: 'Mock Venue' },
  },
  { slug: 'second-show', title: 'Second Show', eventDate: '2026-08-16' },
]

const page1 = <T,>(content: T[], size: number) => ({
  content,
  page: 0,
  size,
  totalElements: content.length,
  // Two pages, so the pagination controls render and get scanned rather than being hidden by a
  // single-page short-circuit.
  totalPages: 2,
})

/** Every read endpoint the data-driven views touch, answered with something renderable. */
async function mockBff(page: Page): Promise<void> {
  await page.route(/\/api\/events\/today/, (route) => json(route, eventSummaries))
  await page.route(/\/api\/events\/[^/?]+/, (route) =>
    json(route, {
      slug: 'tonight-show',
      title: 'Tonight Show',
      eventDate: '2026-08-15',
      startTime: '21:00',
      status: 'SCHEDULED',
      venue: { slug: 'mock-venue', name: 'Mock Venue', address: 'Test Str. 1', city: 'Berlin' },
      lineup: [
        { artist: { slug: 'mock-artist', name: 'Mock Artist' }, role: 'HEADLINER', billingOrder: 1 },
      ],
      promoters: [{ slug: 'mock-promoter', name: 'Mock Promoter' }],
    }),
  )
  await page.route(/\/api\/events(\?|$)/, (route) => json(route, page1(eventSummaries, 20)))
  await page.route(/\/api\/venues(\?|$)/, (route) =>
    json(
      route,
      page1(
        [
          { slug: 'mock-venue', name: 'Mock Venue', city: 'Berlin', district: 'friedrichshain-kreuzberg' },
          { slug: 'other-venue', name: 'Other Venue', city: 'Berlin' },
        ],
        24,
      ),
    ),
  )
  await page.route(/\/api\/genres/, (route) =>
    json(route, [
      { slug: 'techno', name: 'Techno' },
      { slug: 'jazz', name: 'Jazz' },
    ]),
  )
}

const dataRoutes = [
  { name: 'home, with both feeds populated', path: '/en' },
  { name: 'events list, with results and the filter bar', path: '/en/events' },
  { name: 'venues list, with results', path: '/en/venues' },
  { name: 'an event detail page', path: '/en/events/tonight-show' },
]

for (const route of dataRoutes) {
  test(`${route.name} has no detectable accessibility violations`, async ({ page }) => {
    await mockBff(page)
    await page.goto(route.path)

    await expect(page.getByRole('main')).toBeVisible()
    // The scan must not race the fetch: an empty list renders no cards, and axe would pass on
    // markup that was never there. Waiting on real content is what makes this pass meaningful.
    await expect(page.getByRole('heading', { name: /Tonight Show|Mock Venue/ }).first()).toBeVisible()

    const results = await buildScan(page).analyze()

    expect(
      results.violations.map((v) => ({
        rule: v.id,
        impact: v.impact,
        help: v.help,
        nodes: v.nodes.map((n) => n.target.join(' ')),
      })),
    ).toEqual([])
  })
}

test('the skip link is the first thing Tab reaches', async ({ page, browserName }) => {
  // WebKit does not move focus to links on Tab unless macOS "Full Keyboard Access" is enabled —
  // a platform default, not an app defect, and not something a page can or should override. The
  // functional behaviour is covered for every browser by the test above; this one only pins the
  // tab *order*, so it runs where Tab reaches links.
  // This is a conditional skip, not a disabled test.
  // eslint-disable-next-line playwright/no-skipped-test
  test.skip(browserName === 'webkit', 'WebKit excludes links from the Tab order by default')

  await page.goto('/about')
  await page.keyboard.press('Tab')

  await expect(page.getByRole('link', { name: 'Skip to content' })).toBeFocused()
})
