/**
 * Smoke (W7-11; AUD-11/AUD-12, BR-24, AC-02): a teacher or admin edits their profile, saves, and
 * the edit is still there after a full reload — because it went to `PUT /users/me` and the page
 * reads `GET /users/me` again, not because it was kept in the page.
 */
import { test, expect, Route } from '@playwright/test';
import { VISUAL_USERS, installApiStubs, seedSession } from '../visual/api-stubs';

test.use({ viewport: { width: 1280, height: 900 } });

const json = (route: Route, body: unknown) =>
  route.fulfill({
    headers: {
      'access-control-allow-origin': route.request().headers()['origin'] ?? 'http://127.0.0.1:4200',
      'access-control-allow-credentials': 'true',
      'content-type': 'application/json',
    },
    body: JSON.stringify(body),
  });

for (const role of ['teacher', 'admin'] as const) {
  test(`${role}: a saved name and bio survive a reload`, async ({ page }) => {
    await installApiStubs(page, role);
    await seedSession(page, role, 'light');

    // A tiny server: the stored profile changes only through PUT /users/me.
    let stored = { ...VISUAL_USERS[role], bio: 'Original bio' } as Record<string, unknown>;
    const puts: unknown[] = [];
    await page.route('**/api/users/me', async (route) => {
      const req = route.request();
      if (req.method() === 'OPTIONS') return route.fallback();
      if (req.method() === 'PUT') {
        const body = req.postDataJSON();
        puts.push(body);
        stored = { ...stored, ...body };
      }
      return json(route, stored);
    });

    await page.goto(`/${role}/profile`, { waitUntil: 'domcontentloaded' });
    await page.getByRole('button', { name: 'Edit profile' }).click();
    await page.getByLabel('Display name').fill('Renamed Person');
    await page.getByLabel('About').fill('A bio that was saved');
    await page.getByRole('button', { name: 'Save profile' }).click();
    await expect(page.getByRole('button', { name: 'Edit profile' })).toBeVisible();
    expect(puts).toEqual([{ fullName: 'Renamed Person', bio: 'A bio that was saved' }]);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await expect(page.getByLabel('Display name')).toHaveValue('Renamed Person');
    await expect(page.getByLabel('About')).toHaveValue('A bio that was saved');
    await expect(page.locator('.identity-card h2')).toHaveText('Renamed Person');
  });
}
