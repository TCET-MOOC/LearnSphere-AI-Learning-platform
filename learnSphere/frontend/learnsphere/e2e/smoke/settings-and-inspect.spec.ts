/**
 * Smoke (W7-12; AUD-10/12/13, BR-24, BR-03/AC-03): the settings journeys moved here from W7-10,
 * plus admin Inspect. Settings: the theme choice survives a reload, Change password sends the
 * reset request, Sign out calls the logout API, and nothing pretends to save a preference the
 * server cannot store. Inspect: the account's roles, payments and history come from the server,
 * and an assigned role is there when Inspect loads the account again.
 */
import { test, expect, Page, Route } from '@playwright/test';
import { ADMIN_USERS, ROLES, VISUAL_USERS, adminUserDetail, installApiStubs } from '../visual/api-stubs';

test.use({ viewport: { width: 1280, height: 900 } });

const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({
    status,
    headers: {
      'access-control-allow-origin': route.request().headers()['origin'] ?? 'http://127.0.0.1:4200',
      'access-control-allow-credentials': 'true',
      'content-type': 'application/json',
    },
    body: JSON.stringify(body),
  });

/** Like `seedSession`, but keeps a theme the page saved, so a reload shows the saved choice. */
async function boot(page: Page, role: 'student' | 'teacher' | 'admin') {
  await installApiStubs(page, role);
  await page.addInitScript(() => {
    try {
      if (!localStorage.getItem('ls_theme_mode')) localStorage.setItem('ls_theme_mode', 'light');
    } catch {
      // about:blank has no storage origin
    }
  });
}

for (const role of ['student', 'teacher', 'admin'] as const) {
  test(`${role} settings: theme persists, password reset and sign-out are real, nothing fakes a save`, async ({ page }) => {
    await boot(page, role);
    const calls: { path: string; body: unknown }[] = [];
    await page.route('**/api/auth/**', async (route) => {
      const req = route.request();
      if (req.method() === 'OPTIONS') return route.fallback();
      const path = new URL(req.url()).pathname;
      if (path.endsWith('/auth/forgot-password')) {
        calls.push({ path, body: req.postDataJSON() });
        return json(route, { message: 'Reset link sent.' });
      }
      if (path.endsWith('/auth/logout')) {
        calls.push({ path, body: null });
        return json(route, {});
      }
      return route.fallback();
    });

    await page.goto(`/${role}/settings`, { waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('button', { name: 'Save changes' })).toHaveCount(0);

    // Theme: a real switch, saved on this device, still on after a reload.
    const darkMode = page.getByRole('switch', { name: 'Dark mode' });
    await expect(darkMode).toHaveAttribute('aria-checked', 'false');
    await darkMode.click();
    await expect(darkMode).toHaveAttribute('aria-checked', 'true');
    await page.reload({ waitUntil: 'domcontentloaded' });
    await expect(page.getByRole('switch', { name: 'Dark mode' })).toHaveAttribute('aria-checked', 'true');
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    // A preference the server cannot store is disabled and says so.
    await page.getByRole('button', { name: 'Notifications', exact: true }).click();
    const firstPref = page.locator('.setting-row').first();
    await expect(firstPref).toContainText('Not available yet');
    await expect(firstPref.getByRole('switch')).toBeDisabled();

    // Change password sends the reset link to the account e-mail.
    await page.getByRole('button', { name: 'Account', exact: true }).click();
    await page.locator('.setting-row', { hasText: 'Password' }).getByRole('button', { name: 'Change' }).click();
    await expect.poll(() => calls.find((c) => c.path.endsWith('/auth/forgot-password'))?.body)
      .toEqual({ email: VISUAL_USERS[role].email });

    // Sign out ends the session on the server and leaves the app.
    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page).toHaveURL(/\/login/);
    await expect.poll(() => calls.some((c) => c.path.endsWith('/auth/logout'))).toBe(true);
  });
}

test('admin Inspect: roles, payments and history come from the server; an assigned role sticks', async ({ page }) => {
  await boot(page, 'admin');
  const priya = ADMIN_USERS.find((u) => u.name === 'Priya Nair')!;
  const detail = adminUserDetail(priya);
  const assigned: unknown[] = [];

  await page.route(`**/api/admin/users/${priya.id}`, (route) =>
    route.request().method() === 'OPTIONS' ? route.fallback() : json(route, detail));
  await page.route(`**/api/users/${priya.id}/roles`, async (route) => {
    const req = route.request();
    if (req.method() !== 'POST') return route.fallback();
    const body = req.postDataJSON() as { roleId: number };
    assigned.push(body);
    const role = ROLES.find((r) => r.roleId === body.roleId)!;
    const row = { id: 300, userId: priya.id, roleId: role.roleId, roleName: role.name };
    detail.roles = [...detail.roles, row];
    return json(route, row);
  });

  await page.goto('/admin/users', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.tab').first()).toHaveText('All users (3)');
  await page.locator('.user-row', { hasText: 'Priya Nair' }).getByRole('button', { name: 'Inspect' }).click();

  const modal = page.locator('.user-inspect-modal');
  await expect(modal.locator('.inspect-roles')).toContainText('Student');
  await expect(modal.locator('.inspect-payments')).toContainText('pay_visual4');
  await expect(modal.locator('.inspect-audit')).toContainText('Status changed');

  await modal.getByLabel('Role to assign').selectOption({ label: 'Teacher' });
  await modal.getByRole('button', { name: 'Assign role' }).click();
  await expect.poll(() => assigned).toEqual([{ roleId: 2 }]);
  await expect(modal.locator('.inspect-roles')).toContainText('Teacher');

  // Close and reopen: the role is read back from the server, not kept in the page.
  await modal.getByRole('button', { name: 'Close' }).first().click();
  await page.locator('.user-row', { hasText: 'Priya Nair' }).getByRole('button', { name: 'Inspect' }).click();
  await expect(page.locator('.user-inspect-modal .inspect-roles')).toContainText('Teacher');
});
