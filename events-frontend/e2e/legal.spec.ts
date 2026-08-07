import { expect, test } from '@playwright/test'

/**
 * The legal pages have to be *reachable*, not merely present: German practice expects the imprint
 * within a couple of clicks from any page, which for this site means the footer on every route.
 * See docs/FOOTER_AND_LEGAL_PLAN.md §6.
 */

test('reaches the imprint in one click from the footer, on any route', async ({ page }) => {
  await page.goto('/venues')

  await page.getByRole('contentinfo').getByRole('link', { name: 'Imprint' }).click()

  await expect(page).toHaveURL(/\/legal\/imprint$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Imprint' })).toBeVisible()
})

test('reaches the privacy notice in one click from the footer', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('contentinfo').getByRole('link', { name: 'Privacy' }).click()

  await expect(page).toHaveURL(/\/legal\/privacy$/)
  await expect(page.getByRole('heading', { level: 1, name: 'Privacy' })).toBeVisible()
})

test('scrolls to the top when opening a legal page from a scrolled position', async ({ page }) => {
  // Without a scrollBehavior the router keeps the previous offset, so a footer link opens the
  // imprint somewhere in its middle — which reads as a broken page.
  await page.goto('/about')
  // `window.scrollTo` rather than `mouse.wheel`: the latter is a no-op on touch-emulating projects
  // (Mobile Safari), so the page was never scrolled and the test passed vacuously there.
  await page.evaluate(() => window.scrollTo(0, 2000))
  await expect.poll(() => page.evaluate(() => window.scrollY)).toBeGreaterThan(100)

  await page.getByRole('contentinfo').getByRole('link', { name: 'Imprint' }).click()
  await expect(page.getByRole('heading', { level: 1, name: 'Imprint' })).toBeVisible()

  expect(await page.evaluate(() => window.scrollY)).toBeLessThan(50)
})

test('the imprint carries the § 5 DDG essentials', async ({ page }) => {
  await page.goto('/legal/imprint')
  const main = page.getByRole('main')

  await expect(main).toContainText('Norman Lange')
  await expect(main).toContainText('§ 18 (2) MStV')
  await expect(main).toContainText('without warranty as to accuracy')
  await expect(main.getByRole('link', { name: /@/ })).toBeVisible()
})

test('the privacy notice states its legal basis, rights and supervisory authority', async ({
  page,
}) => {
  await page.goto('/legal/privacy')
  const main = page.getByRole('main')

  await expect(main).toContainText('Art. 6 (1) (f) GDPR')
  await expect(main).toContainText('Right to object (Art. 21 GDPR)')
  await expect(main).toContainText('Berliner Beauftragte')
  await expect(main).toContainText('§ 25 (2) 2 TDDDG')
})

test('legal pages say they are provisional while the details are placeholders', async ({
  page,
}) => {
  for (const path of ['/legal/imprint', '/legal/privacy']) {
    await page.goto(path)
    await expect(page.getByRole('main')).toContainText('This page is not final')
  }
})

test('sets a document title for each legal route', async ({ page }) => {
  const titles = [
    ['/legal/imprint', 'Imprint · Event Junkie'],
    ['/legal/privacy', 'Privacy · Event Junkie'],
    ['/legal/notices', 'Open-source notices · Event Junkie'],
  ] as const

  for (const [path, title] of titles) {
    await page.goto(path)
    await expect(page).toHaveTitle(title)
  }
})

test('the notices route resolves even though the footer does not link it yet', async ({ page }) => {
  // Phase 5 fills this page and adds the footer link; until then it must at least not 404.
  await page.goto('/legal/notices')

  await expect(page.getByRole('heading', { level: 1, name: 'Open-source notices' })).toBeVisible()
  await expect(page.getByRole('contentinfo').getByRole('link', { name: /notices/i })).toHaveCount(0)
})
