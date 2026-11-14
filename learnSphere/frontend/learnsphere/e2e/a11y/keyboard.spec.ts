/**
 * Keyboard-only paths (W7-08; `spec §7` "keyboard reachable", `PLAN §7` "keyboard-only path for
 * player, quiz and checkout; modals trap focus"). Each test drives the page with Tab / Enter /
 * Space / Escape only — no mouse clicks — on the same stubbed API as the visual baseline.
 */
import { test, expect, Page } from '@playwright/test';
import { CAPTURED_CONTRACTS } from '../visual/screen-contracts';
import { openScreen } from './open-screen';

const screen = (id: string) => CAPTURED_CONTRACTS.find((c) => c.id === id)!;

/** Presses Tab until the focused element matches `selector`; fails if it is never reached. */
async function tabTo(page: Page, selector: string, max = 120): Promise<void> {
  for (let i = 0; i < max; i++) {
    await page.keyboard.press('Tab');
    if (await page.evaluate((s) => !!document.activeElement?.matches(s), selector)) return;
  }
  throw new Error(`"${selector}" was not reached with ${max} Tab presses`);
}

test.use({ viewport: { width: 1280, height: 900 } });

test('player: the playlist is reachable with Tab and Enter opens another lecture', async ({ page }) => {
  await openScreen(page, screen('SCR-13'), 'light');
  await tabTo(page, '.playlist-item:not(.is-current)');
  const before = page.url();
  await page.keyboard.press('Enter');
  await expect(page).not.toHaveURL(before);
  await expect(page).toHaveURL(/\/student\/courses\/1\/lectures\/\d+$/);
});

test('quiz: begin, answer, move on and leave with the keyboard only', async ({ page }) => {
  await openScreen(page, screen('SCR-17'), 'light');
  await tabTo(page, 'button.btn--primary');
  await page.keyboard.press('Enter');

  await tabTo(page, '.option input[type="radio"]');
  await page.keyboard.press('Space');
  await expect(page.locator('.option.selected')).toHaveCount(1);

  await tabTo(page, '.q-nav .btn--primary');
  await page.keyboard.press('Enter');
  await expect(page.locator('.q-counter')).toContainText('Question 2');
});

test('quiz: "Back to assessments" is reachable and works from the keyboard', async ({ page }) => {
  await openScreen(page, screen('SCR-17'), 'light');
  await tabTo(page, 'a.btn--ghost');
  await page.keyboard.press('Enter');
  await expect(page).toHaveURL(/\/student\/assessments$/);
});

test('checkout: the buy button is reachable and Enter opens the payment gateway', async ({ page }) => {
  await page.addInitScript(() => {
    (window as unknown as Record<string, unknown>)['Razorpay'] = class {
      constructor(readonly options: unknown) {}
      on(): void {}
      open(): void {
        (window as unknown as Record<string, unknown>)['__gatewayOpened'] = true;
      }
    };
  });
  // Course 2 is paid (₹899) and the baseline student is not enrolled in it.
  await openScreen(page, screen('SCR-12'), 'light', {
    route: '/student/courses/2',
    stubs: (p) => p.route('**/api/student/courses/2', (r) => r.fulfill({ status: 404, headers: { 'access-control-allow-origin': 'http://127.0.0.1:4200', 'access-control-allow-credentials': 'true' }, body: '{}' })),
  });
  await expect(page.locator('.enroll-btn')).toContainText('Buy Course');
  await tabTo(page, '.enroll-btn');
  await page.keyboard.press('Enter');
  await expect.poll(() => page.evaluate(() => (window as unknown as Record<string, unknown>)['__gatewayOpened'])).toBe(true);
});

test('modal: certificate preview takes focus, keeps it inside, and Escape hands it back', async ({ page }) => {
  await openScreen(page, screen('SCR-21'), 'light');
  await tabTo(page, 'button.btn-link');
  await page.keyboard.press('Enter');

  const dialog = page.getByRole('dialog', { name: 'Official Credential Document' });
  await expect(dialog).toBeVisible();
  await expect(dialog).toHaveAttribute('aria-modal', 'true');
  for (let i = 0; i < 12; i++) {
    await page.keyboard.press('Tab');
    expect(await dialog.evaluate((d) => d.contains(document.activeElement))).toBe(true);
  }

  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
  expect(await page.evaluate(() => document.activeElement?.matches('button.btn-link'))).toBe(true);
});
