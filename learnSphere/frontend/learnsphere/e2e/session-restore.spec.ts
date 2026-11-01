/**
 * Session restore on page load: the in-memory access token (W1-06) is rebuilt from the
 * HttpOnly refresh cookie at start-up, so a reload keeps the user signed in, and a
 * signed-out visitor on a public page is never pushed to /login.
 */
import { test, expect, Page } from '@playwright/test';

const student = { id: 1, fullName: 'Aarav Sharma', email: 'student@learnsphere.test', role: 'STUDENT', status: 'ACTIVE' };

async function stubApi(page: Page, opts: { cookieValid: boolean }) {
  const refreshCalls: string[] = [];
  await page.route(/\/api\//, async (route) => {
    const req = route.request();
    const headers = {
      'access-control-allow-origin': req.headers()['origin'] ?? 'http://127.0.0.1:4200',
      'access-control-allow-credentials': 'true',
      'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS',
      'access-control-allow-headers': 'authorization,content-type,x-request-id'
    };
    if (req.method() === 'OPTIONS') return route.fulfill({ status: 204, headers });
    const path = new URL(req.url()).pathname;
    if (path.endsWith('/auth/refresh')) {
      refreshCalls.push(path);
      return opts.cookieValid
        ? route.fulfill({ json: { token: 'restored-token', user: student }, headers })
        : route.fulfill({ status: 401, json: { code: 'UNAUTHORIZED' }, headers });
    }
    if (path.endsWith('/users/me')) return route.fulfill({ json: student, headers });
    return route.fulfill({ json: [], headers });
  });
  return refreshCalls;
}

test.describe('Session restore on page load', () => {
  test('a valid refresh cookie keeps the user signed in across a reload', async ({ page }) => {
    const refreshCalls = await stubApi(page, { cookieValid: true });
    await page.goto('/student/purchases');
    await expect(page).toHaveURL(/\/student\/purchases$/);
    await expect(page.getByRole('heading', { name: 'Order History & Purchases' })).toBeVisible();

    await page.reload();
    await expect(page).toHaveURL(/\/student\/purchases$/);
    await expect(page.getByRole('heading', { name: 'Order History & Purchases' })).toBeVisible();
    expect(refreshCalls.length).toBe(2); // one restore per page load
  });

  test('a signed-out visitor stays on the public landing page', async ({ page }) => {
    const refreshCalls = await stubApi(page, { cookieValid: false });
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await expect(page).toHaveURL(/\/$/);
    expect(refreshCalls.length).toBe(1);
  });

  test('a signed-out visitor opening a protected page is sent to login', async ({ page }) => {
    await stubApi(page, { cookieValid: false });
    await page.goto('/student/purchases');
    await expect(page).toHaveURL(/\/login/);
  });
});
