/**
 * Approved 360 px landing top bar (2026-11-02): logo, one short "Sign up" button and the
 * menu icon on one row. "Log in" moves into the menu drawer, which also carries the page
 * links and full-width "Log in" / "Sign up free" buttons. 768 and 1280 px are unchanged.
 */
import { test, expect, Page, Locator } from '@playwright/test';

const errors = new WeakMap<Page, string[]>();

async function openLanding(page: Page, width: number) {
  const seen: string[] = [];
  errors.set(page, seen);
  page.on('console', (m) => { if (m.type() === 'error') seen.push(m.text()); });
  page.on('pageerror', (e) => seen.push(e.message));
  // Public page: answer every API call so no server is needed and no error toast covers the bar.
  await page.route('**/api/**', (r) => r.fulfill({
    status: 200, contentType: 'application/json', body: '[]',
    headers: { 'access-control-allow-origin': 'http://127.0.0.1:4200', 'access-control-allow-credentials': 'true' },
  }));
  await page.setViewportSize({ width, height: width === 360 ? 800 : 900 });
  await page.goto('/', { waitUntil: 'networkidle' });
  await expect(page.locator('.landing-bar')).toBeVisible();
}

async function box(l: Locator) {
  const b = await l.boundingBox();
  expect(b, 'element has a box').not.toBeNull();
  return b!;
}

test.describe('Landing top bar at 360 px', () => {
  test('one row: logo, Sign up and menu, all inside the viewport; no Log in in the bar', async ({ page }) => {
    await openLanding(page, 360);
    const bar = page.locator('.landing-bar');

    const signUp = bar.getByRole('link', { name: 'Sign up', exact: true });
    const menu = bar.getByRole('button', { name: 'Open menu' });
    const logo = bar.getByRole('link', { name: /LearnSphere/ });
    await expect(signUp).toBeVisible();
    await expect(menu).toBeVisible();
    await expect(signUp).toHaveAttribute('href', '/register');
    await expect(bar.getByRole('link', { name: 'Log in' })).toBeHidden();
    await expect(bar.getByRole('link', { name: 'Sign up free' })).toBeHidden();

    const [l, s, m] = [await box(logo), await box(signUp), await box(menu)];
    // Same row, single-line button, nothing past the right edge.
    for (const b of [l, s, m]) expect(b.y + b.height).toBeLessThanOrEqual(60);
    expect(s.height).toBeLessThanOrEqual(40);
    expect(m.x + m.width).toBeLessThanOrEqual(360 - 12);
    // Breathing room between logo, Sign up and menu.
    expect(s.x - (l.x + l.width)).toBeGreaterThanOrEqual(12);
    expect(m.x - (s.x + s.width)).toBeGreaterThanOrEqual(8);
    // Tap targets.
    expect(m.width).toBeGreaterThanOrEqual(40);
    expect(m.height).toBeGreaterThanOrEqual(40);

    const scroll = await page.evaluate(() => document.documentElement.scrollWidth);
    expect(scroll).toBeLessThanOrEqual(360);
  });

  test('menu drawer lists the page links plus full-width Log in and Sign up free', async ({ page }) => {
    await openLanding(page, 360);
    const menu = page.locator('.landing-bar').getByRole('button', { name: 'Open menu' });
    await expect(menu).toHaveAttribute('aria-expanded', 'false');
    await menu.click();

    const close = page.locator('.landing-bar').getByRole('button', { name: 'Close menu' });
    await expect(close).toHaveAttribute('aria-expanded', 'true');
    const drawer = page.locator('#landing-menu');
    await expect(drawer).toBeVisible();
    await expect(close).toHaveAttribute('aria-controls', 'landing-menu');
    for (const name of ['Explore courses', 'Departments', 'For colleges', 'Teach with us']) {
      await expect(drawer.getByRole('link', { name })).toBeVisible();
    }
    const login = drawer.getByRole('link', { name: 'Log in' });
    const signupFree = drawer.getByRole('link', { name: 'Sign up free' });
    await expect(login).toHaveAttribute('href', '/login');
    await expect(signupFree).toHaveAttribute('href', '/register');
    const [a, b] = [await box(login), await box(signupFree)];
    expect(a.width).toBeGreaterThanOrEqual(140);
    expect(b.width).toBeGreaterThanOrEqual(140);
    expect(Math.abs(a.y - b.y)).toBeLessThanOrEqual(1);
    // Opening the drawer must not push the page sideways.
    const sideways = await page.evaluate(() => ({ scrollX: window.scrollX, width: document.documentElement.scrollWidth }));
    expect(sideways).toEqual({ scrollX: 0, width: 360 });
    expect(b.x + b.width).toBeLessThanOrEqual(360 - 12);

    await close.click();
    await expect(drawer).toBeHidden();
  });
});

for (const width of [768, 1280]) {
  test(`landing top bar at ${width} px keeps Log in and Sign up free in the bar`, async ({ page }) => {
    await openLanding(page, width);
    const bar = page.locator('.landing-bar');
    await expect(bar.getByRole('link', { name: 'Log in' })).toBeVisible();
    await expect(bar.getByRole('link', { name: 'Sign up free' })).toBeVisible();
    await expect(bar.getByRole('link', { name: 'Sign up', exact: true })).toBeHidden();
    const login = await box(bar.getByRole('link', { name: 'Log in' }));
    expect(login.height).toBeLessThanOrEqual(40);
    if (width === 1280) {
      for (const name of ['Explore courses', 'Departments', 'For colleges', 'Teach with us']) {
        await expect(bar.getByRole('link', { name })).toBeVisible();
      }
      await expect(bar.getByRole('button', { name: 'Open menu' })).toBeHidden();
    }
    // A missing icon throws during Angular's first render and leaves the bar half-built.
    expect(errors.get(page)).toEqual([]);
  });
}
