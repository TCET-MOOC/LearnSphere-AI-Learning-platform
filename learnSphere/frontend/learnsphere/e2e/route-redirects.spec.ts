/**
 * W7-06 (`spec §4`): redirects land on their canonical screen in the browser.
 * The route ↔ SCR matrix itself is unit-tested in `src/app/route-screen-matrix.spec.ts`.
 */
import { test, expect } from '@playwright/test';
import { installApiStubs, seedSession } from './visual/api-stubs';

for (const role of ['student', 'teacher', 'admin'] as const) {
  test(`/${role} opens the ${role} dashboard`, async ({ page }) => {
    await installApiStubs(page, role);
    await seedSession(page, role, 'light');
    await page.goto(`/${role}`);
    await expect(page).toHaveURL(new RegExp(`/${role}/dashboard$`));
  });
}

test('the legacy lecture link opens that lecture, not the resume point', async ({ page }) => {
  await installApiStubs(page, 'student');
  await seedSession(page, 'student', 'light');
  await page.goto('/student/courses/1/lecture/2');
  await expect(page).toHaveURL(/\/student\/courses\/1\/lectures\/2$/);
});

test('the older certificate link redirects to the canonical verification page', async ({ page }) => {
  await installApiStubs(page, 'public');
  await page.goto('/verify/certificate/LS-TEST-0001');
  await expect(page).toHaveURL(/\/verify-certificate\/LS-TEST-0001$/);
});
