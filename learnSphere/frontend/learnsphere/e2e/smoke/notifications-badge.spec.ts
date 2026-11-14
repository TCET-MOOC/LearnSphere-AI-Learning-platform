/**
 * Smoke (W7-10): the top bar's unread badge belongs to the signed-in role.
 *
 * Regression: the badge read its role from `router.url`. With lazy role groups (W7-02) the
 * `/notifications` response can land before the first navigation ends, so the count was stored
 * under `student` and the admin/teacher badge stayed empty.
 */
import { test, expect } from '@playwright/test';
import { CAPTURED_CONTRACTS } from '../visual/screen-contracts';
import { openScreen } from '../a11y/open-screen';

test.use({ viewport: { width: 1280, height: 900 } });

// The stubbed `/notifications` list holds two unread items for every role.
for (const id of ['SCR-10', 'SCR-30', 'SCR-50', 'SCR-51']) {
  const contract = CAPTURED_CONTRACTS.find((c) => c.id === id)!;
  test(`${contract.role} — ${contract.title}: the bell shows 2 unread`, async ({ page }) => {
    await openScreen(page, contract, 'light');
    await expect(page.locator('app-topbar .badge--count')).toHaveText('2');
  });
}
