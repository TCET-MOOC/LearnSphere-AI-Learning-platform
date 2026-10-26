/**
 * Visual baseline harness (DS-01 / W1-12).
 *
 * 46 captured screen contracts × 3 viewports × 2 themes = 276 PNG baselines.
 * The 47th contract (`SCR-35`) has no route and is skipped with a reason.
 *
 * Determinism:
 *   - every `/api/**` response comes from `api-stubs.ts`,
 *   - the clock is fixed at `2026-10-25T09:00:00Z`,
 *   - session and theme are seeded before the app boots,
 *   - CSS animations/transitions are frozen by `toHaveScreenshot`,
 *   - the capture waits for the routed component and for webfonts.
 *
 * Usage: `npm run e2e:visual` (verify) / `npm run e2e:visual:update` (re-baseline).
 */
import { test, expect } from '@playwright/test';
import {
  CAPTURED_CONTRACTS,
  SCREEN_CONTRACTS,
  SKIPPED_CONTRACTS,
  THEMES,
  VIEWPORTS,
} from './screen-contracts';
import { installApiStubs, seedSession } from './api-stubs';

test.describe.configure({ mode: 'parallel' });

test('inventory covers the 47 spec contracts', () => {
  expect(SCREEN_CONTRACTS).toHaveLength(47);
  expect(CAPTURED_CONTRACTS).toHaveLength(46);
  expect(SKIPPED_CONTRACTS.map((c) => c.id)).toEqual(['SCR-35']);
  expect(CAPTURED_CONTRACTS.every((c) => c.route?.startsWith('/'))).toBe(true);
});

for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.width}x${viewport.height}`, () => {
    test.use({ viewport });

    for (const theme of THEMES) {
      for (const contract of CAPTURED_CONTRACTS) {
        test(`${contract.id} ${contract.title} — ${theme}`, async ({ page }) => {
          const misses = await installApiStubs(page, contract.role);
          await seedSession(page, contract.role, theme);
          await page.clock.setFixedTime(new Date('2026-10-25T09:00:00Z'));
          await page.emulateMedia({ colorScheme: theme });

          await page.goto(contract.route!, { waitUntil: 'domcontentloaded' });

          // Wait for the routed component (not just `<router-outlet>`).
          await page
            .locator('app-root > :not(router-outlet)')
            .first()
            .waitFor({ state: 'attached', timeout: 15_000 });

          await page.waitForFunction(() => document.fonts?.status === 'loaded', undefined, {
            timeout: 15_000,
          });
          // Force every declared weight so no late swap can land mid-capture.
          const fontsReady = await page.evaluate(async () => {
            const loads = [
              '400 16px "Inter"',
              '500 16px "Inter"',
              '600 16px "Inter"',
              '700 16px "Inter"',
              '500 16px "Plus Jakarta Sans"',
              '600 16px "Plus Jakarta Sans"',
              '700 16px "Plus Jakarta Sans"',
              '800 16px "Plus Jakarta Sans"',
            ].map((font) => document.fonts.load(font));
            await Promise.all(loads);
            await document.fonts.ready;
            return (
              document.fonts.check('400 16px "Inter"') &&
              document.fonts.check('500 16px "Plus Jakarta Sans"')
            );
          });
          expect(fontsReady, 'webfonts must load, otherwise the baseline is not reproducible').toBe(
            true,
          );

          // Stub responses resolve immediately; this lets Angular flush the
          // last render before the capture.
          await page.waitForTimeout(400);

          await expect(page).toHaveScreenshot(
            `${contract.id}-${contract.slug}-${viewport.width}-${theme}.png`,
            {
              fullPage: true,
              animations: 'disabled',
              caret: 'hide',
              maxDiffPixelRatio: 0.002,
            },
          );

          expect(
            [...misses],
            `${contract.id} requested endpoints the stub table does not model`,
          ).toEqual([]);
        });
      }
    }
  });
}

for (const contract of SKIPPED_CONTRACTS) {
  test.skip(`${contract.id} ${contract.title}`, async () => {
    // Never executed: the contract has no route, so there is nothing to render.
    // The skip reason is recorded in the tracker and printed in the HTML report.
    test.info().annotations.push({ type: 'skip-reason', description: contract.skipReason ?? '' });
  });
}
