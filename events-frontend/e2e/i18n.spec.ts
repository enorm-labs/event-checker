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
  // `de` is not in LOCALES yet, so `/de/events` must not resolve — a route that renders English
  // under a German URL is worse than no German URL at all. It is prefixed as an ordinary unknown
  // path (`/en/de/events`), matches nothing, and settles on the locale home. Two hops, no loop.
  await page.goto('/de/events')

  await expect(page).toHaveURL(/\/en$/)
  await expect(page.getByRole('main')).toBeVisible()
})

test('navigating within the app keeps the locale prefix', async ({ page }) => {
  await page.goto('/en')

  await page.getByRole('navigation', { name: 'Main' }).getByRole('link', { name: 'Venues' }).click()

  await expect(page).toHaveURL(/\/en\/venues$/)
})
