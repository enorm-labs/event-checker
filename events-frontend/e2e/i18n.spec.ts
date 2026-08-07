import { expect, test } from '@playwright/test'

/**
 * Locale routing — the part of localisation that is infrastructure rather than translation.
 *
 * Only English is published in this phase (`LOCALES = ['en']`), so these cover the URL contract
 * and the redirects rather than any German content. German assertions arrive with Phase 3 of
 * docs/LOCALISATION_PLAN.md.
 */

test('the bare root redirects to a locale', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/en$/)
  await expect(page.getByRole('main')).toBeVisible()
})

test('an unprefixed path keeps its route through the redirect', async ({ page }) => {
  // The redirect must preserve where the visitor was going, not just drop them on the home page.
  await page.goto('/venues')

  await expect(page).toHaveURL(/\/en\/venues$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Venues' })).toBeVisible()
})

test('an unprefixed path preserves its query string and hash', async ({ page }) => {
  await page.goto('/events?type=CONCERT')
  await expect(page).toHaveURL(/\/en\/events\?type=CONCERT$/)

  await page.goto('/about#beta')
  await expect(page).toHaveURL(/\/en\/about#beta$/)
  await expect(page.getByRole('heading', { name: 'Why it says beta' })).toBeVisible()
})

test('html lang matches the locale in the URL', async ({ page }) => {
  // WCAG 3.1.1. It was `lang=""` before the footer work; this is what keeps it from drifting back.
  await page.goto('/en/about')

  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
})

test('an unknown path under a published locale does not loop', async ({ page }) => {
  // The catch-all prefixes unprefixed paths. Without a guard, an unmatched *prefixed* path would
  // be prefixed again — `/en/en/nonsense` — and redirect forever. It lands on the locale home.
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))

  await page.goto('/en/nonsense')

  await expect(page).toHaveURL(/\/en$/)
  await expect(page.getByRole('main')).toBeVisible()
  expect(errors, 'redirect loop or router error').toEqual([])
})

test('an unpublished locale is treated as an unknown path, not as a locale', async ({ page }) => {
  // French is not in LOCALES, so `/fr/events` must not resolve — a route that renders another
  // language under a French URL is worse than no French URL at all. It is prefixed as an ordinary
  // unknown path, matches nothing, and settles on the locale home. Two hops, no loop.
  await page.goto('/fr/events')

  await expect(page).toHaveURL(/\/en$/)
  await expect(page.getByRole('main')).toBeVisible()
})

test('navigating within the app keeps the locale prefix', async ({ page }) => {
  await page.goto('/en')

  await page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'Venues' }).click()

  await expect(page).toHaveURL(/\/en\/venues$/)
})

test('a German URL renders German', async ({ page }) => {
  await page.goto('/de/venues')

  await expect(page.locator('html')).toHaveAttribute('lang', 'de')
  await expect(page.getByRole('heading', { level: 1, name: 'Locations' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Haupt' })).toBeVisible()
  await expect(page.getByRole('contentinfo')).toContainText('Von Berlin kriegst du nie genug')
})

test('the locale switcher keeps you on the same page', async ({ page }) => {
  // Switching language on the venues list must not dump you on the home page — the whole reason
  // the switcher rewrites only the locale segment.
  await page.goto('/en/venues')

  await page.getByRole('navigation', { name: 'Language' }).getByRole('link', { name: 'Deutsch' }).click()

  await expect(page).toHaveURL(/\/de\/venues$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Locations' })).toBeVisible()
})

test('the switcher marks the active language and offers real links', async ({ page }) => {
  await page.goto('/de/about')
  const switcher = page.getByRole('navigation', { name: 'Sprache' })

  // Real hrefs, not JS handlers: middle-click, "copy link" and crawlers all depend on them.
  await expect(switcher.getByRole('link', { name: 'English' })).toHaveAttribute('href', '/en/about')
  await expect(switcher.getByRole('link', { name: 'Deutsch' })).toHaveAttribute('aria-current', 'true')
})

test('dates render in the locale format', async ({ page }) => {
  // The most visible difference between the two locales, and the reason formatDate takes a locale.
  await page.route('**/api/events/today', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([
        { slug: 'x', title: 'Test Event', eventDate: '2026-06-12', venue: { slug: 'v', name: 'V' } },
      ]),
    }),
  )

  await page.goto('/en')
  await expect(page.getByText(/12 Jun 2026/)).toBeVisible()

  await page.goto('/de')
  await expect(page.getByText(/12\. Juni 2026/)).toBeVisible()
})

test('the legal pages say they are English-only while German is pending', async ({ page }) => {
  // German UI shipped before the German legal pages (Phase 4). Until then the gap is disclosed on
  // the page rather than left for a German reader to discover — FOOTER_AND_LEGAL_PLAN §6.1.
  await page.goto('/de/legal/privacy')

  await expect(page.getByRole('main')).toContainText('nur auf Englisch')
})
