/**
 * Every screen leaves its loading state promptly once its data has arrived.
 *
 * The app runs zoneless (Angular 21, no zone.js): a component that only assigns fields
 * inside a subscription is not re-rendered when its data arrives. It recovers only when some
 * unrelated tick (a poll, a websocket retry) happens to re-render the whole tree, which is late
 * and unpredictable (post-W6 audit: dashboard and bookmarks stuck on "Loading…"). jsdom unit
 * tests hide this because they call detectChanges() by hand, so this check runs in a real
 * browser against the visual-harness fixtures, for every screen in the inventory.
 */
import { test, expect } from '@playwright/test';
import { CAPTURED_CONTRACTS } from './visual/screen-contracts';
import { installApiStubs, seedSession } from './visual/api-stubs';

const LOADING_CSS = '[class*="skeleton"], [class*="spinner"], [class*="loading-state"]';

/** Fixture data a screen must show, so a stale empty state cannot pass as "loaded". */
const MUST_SHOW: Record<string, string> = {
  'SCR-14': 'Definition of learning' // notes: showed "No notes found" while the API returned notes
};

test.describe('Pages finish loading (zoneless re-render)', () => {
  for (const contract of CAPTURED_CONTRACTS) {
    test(`${contract.id} ${contract.title}`, async ({ page }) => {
      await page.setViewportSize({ width: 1280, height: 900 });
      await installApiStubs(page, contract.role);
      await seedSession(page, contract.role, 'light');
      await page.goto(contract.route!, { waitUntil: 'domcontentloaded' });
      await page.locator('app-root > :not(router-outlet)').first().waitFor({ state: 'attached', timeout: 15_000 });

      // Once the stubbed API has answered, content must render promptly, with no user input.
      await page.waitForLoadState('networkidle');
      await expect
        .poll(async () => (await page.getByText(/^\s*Loading/i).or(page.locator(LOADING_CSS)).filter({ visible: true }).count()), { timeout: 1_500 })
        .toBe(0);
      if (MUST_SHOW[contract.id]) {
        await expect(page.getByText(MUST_SHOW[contract.id]).first()).toBeVisible({ timeout: 1_500 });
      }
    });
  }
});
