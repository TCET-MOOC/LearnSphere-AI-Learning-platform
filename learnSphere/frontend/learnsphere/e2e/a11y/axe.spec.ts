/**
 * Accessibility gate (W7-08; `spec §7`, `PLAN §7` Accessibility, `[G-F15]`).
 *
 * axe-core (WCAG 2.1 A/AA) on every routed screen contract at 360/768/1280 px in both themes,
 * rendered from the same stubbed API and seeded session as the visual baseline. Any violation
 * fails: labels, names, contrast, ARIA structure and focusable scroll regions — except the one
 * recorded exception below.
 */
import { test, expect, Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { CAPTURED_CONTRACTS, THEMES, VIEWPORTS } from '../visual/screen-contracts';
import { openScreen } from './open-screen';

test.describe.configure({ mode: 'parallel' });

/**
 * Recorded exception (user decision 2026-11-14, TRACKER §4): the light-theme `--text-muted` token
 * stays #94a3b8 to preserve the design (NFR-01), although it is 2.6:1 on white. Only light-theme
 * `color-contrast` findings on text painted with exactly that token (computed `color`, so faded rows
 * that blend it are covered too) are excused. Every other violation — including any other
 * low-contrast colour, and every finding in the dark theme — still fails.
 */
const LIGHT_MUTED_RGB = 'rgb(148, 163, 184)'; // #94a3b8

async function paintedWithMutedToken(page: Page, target: unknown[]): Promise<boolean> {
  const selector = target[target.length - 1];
  if (typeof selector !== 'string') return false;
  return page.evaluate(
    ([s, rgb]) => {
      const el = document.querySelector(s);
      return !!el && getComputedStyle(el).color === rgb;
    },
    [selector, LIGHT_MUTED_RGB] as const,
  );
}

for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.width}px`, () => {
    test.use({ viewport });
    for (const theme of THEMES) {
      for (const contract of CAPTURED_CONTRACTS) {
        test(`${contract.id} ${contract.title} — ${theme} is axe clean`, async ({ page }) => {
          await openScreen(page, contract, theme);
          const { violations } = await new AxeBuilder({ page })
            .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
            .analyze();

          const summary: string[] = [];
          for (const v of violations) {
            const nodes = [];
            for (const n of v.nodes) {
              const excused =
                theme === 'light' &&
                v.id === 'color-contrast' &&
                (await paintedWithMutedToken(page, n.target as unknown[]));
              if (!excused) nodes.push(n);
            }
            if (nodes.length) {
              summary.push(`${v.id} (${nodes.length}): ${nodes.slice(0, 3).map((n) => n.target.join(' ')).join(' | ')}`);
            }
          }
          expect(summary).toEqual([]);
        });
      }
    }
  });
}
