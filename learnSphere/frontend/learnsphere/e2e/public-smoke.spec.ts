import { expect, test } from '@playwright/test';

/**
 * Phase 0 smoke coverage: prove that the built shell serves and that the
 * anonymous auth routes render. Full user journeys belong to W7-10.
 */
test.describe('public shell', () => {
  test('serves the login page', async ({ page }) => {
    const response = await page.goto('/login', { waitUntil: 'domcontentloaded' });

    expect(response?.ok()).toBeTruthy();
    await expect(page).toHaveTitle(/LearnSphere/i);
    await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign In' })).toBeVisible();
  });

  test('navigates from login to registration', async ({ page }) => {
    await page.goto('/login', { waitUntil: 'domcontentloaded' });
    await page.getByRole('link', { name: 'Sign up' }).click();

    await expect(page).toHaveURL(/\/register$/);
    await expect(page.getByRole('heading', { name: 'Create an Account' })).toBeVisible();
    await expect(page.locator('#fullName')).toBeVisible();
  });
});
