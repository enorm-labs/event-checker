import { expect, test, type Page } from '@playwright/test'

/**
 * The dark-mode toggle lives in the app shell (App.vue), which is not remounted by
 * in-app router navigation, so the choice persists as you move between routes. It is
 * also stored in localStorage and re-applied before paint (inline script in index.html),
 * so it survives a full page reload too. Both contracts are asserted below.
 */

const html = (page: Page) => page.locator('html')
const toggle = (page: Page) => page.getByRole('button', { name: /switch to (dark|light) mode/i })
const navLink = (page: Page, name: string) =>
  page.getByRole('navigation').getByRole('link', { name, exact: true })

test('dark-mode choice persists across in-app navigation', async ({ page }) => {
  await page.goto('/')
  await expect(html(page)).not.toHaveClass(/dark/)

  await toggle(page).click()
  await expect(html(page)).toHaveClass(/dark/)

  await navLink(page, 'Events').click()
  await expect(page).toHaveURL(/\/events$/)
  await expect(html(page)).toHaveClass(/dark/)

  await navLink(page, 'About').click()
  await expect(page).toHaveURL(/\/about$/)
  await expect(html(page)).toHaveClass(/dark/)

  // Toggling back off also persists across navigation.
  await toggle(page).click()
  await expect(html(page)).not.toHaveClass(/dark/)

  await navLink(page, 'Home').click()
  await expect(page).toHaveURL(/\/$/)
  await expect(html(page)).not.toHaveClass(/dark/)
})

test('dark-mode choice persists across a full page reload', async ({ page }) => {
  await page.goto('/')
  await toggle(page).click()
  await expect(html(page)).toHaveClass(/dark/)

  await page.reload()

  // Re-applied before paint from localStorage, and the toggle reflects it.
  await expect(html(page)).toHaveClass(/dark/)
  await expect(page.getByRole('button', { name: /switch to light mode/i })).toBeVisible()
})
