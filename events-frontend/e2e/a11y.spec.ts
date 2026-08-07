import AxeBuilder from '@axe-core/playwright'
import { expect, type Page, test } from '@playwright/test'

/**
 * Automated accessibility sweep — the runtime half of the WCAG 2.1 AA target
 * (docs/FOOTER_AND_LEGAL_PLAN.md §12.3).
 *
 * axe catches what `eslint-plugin-vuejs-accessibility` cannot see from the source: colour
 * contrast against the resolved theme tokens, focus order, landmark structure, and duplicate IDs.
 * It is not a conformance certificate — axe reliably finds roughly a third of WCAG issues — but
 * it is what stops the accessibility already in this codebase from regressing silently.
 *
 * Static routes only, for the same reason as the smoke suite: the BFF is not running during e2e,
 * so data-driven views render their error state. That still exercises the shared chrome (skip
 * link, header, footer), which is where the repeated content lives.
 */

const staticRoutes = [
  '/',
  '/events',
  '/venues',
  '/calendar',
  '/about',
  '/legal/imprint',
  '/legal/privacy',
  '/legal/notices',
] as const

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
