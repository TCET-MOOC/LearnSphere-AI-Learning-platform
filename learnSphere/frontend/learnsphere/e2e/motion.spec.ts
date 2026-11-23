/**
 * DS-13 (W7-15; NFR-04(e)): motion restraint on every routed screen.
 *   - no hover-lift (`:hover` → translateY(-n)) on an element that cannot be clicked: lifting a
 *     static card suggests an action that is not there (click-only elements are `role="button"`
 *     since W7-08, so "interactive" is decidable from the DOM);
 *   - at most one orchestrated page-load moment: one distinct finite CSS animation while the page
 *     loads (infinite spinners/pulses are state indicators, not a moment);
 *   - `prefers-reduced-motion: reduce` stops every animation.
 */
import { test, expect, Page } from '@playwright/test';
import { CAPTURED_CONTRACTS } from './visual/screen-contracts';
import { installApiStubs, seedSession } from './visual/api-stubs';

async function open(page: Page, route: string, role: (typeof CAPTURED_CONTRACTS)[number]['role'], reduced = false) {
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.emulateMedia({ reducedMotion: reduced ? 'reduce' : 'no-preference' });
  await page.addInitScript(() => {
    const w = window as unknown as { __anims: { name: string; infinite: boolean; ms: number }[] };
    w.__anims = [];
    document.addEventListener('animationstart', (e) => {
      const cs = getComputedStyle(e.target as Element);
      const names = cs.animationName.split(',').map((s) => s.trim());
      const i = Math.max(0, names.indexOf(e.animationName));
      const iter = cs.animationIterationCount.split(',').map((s) => s.trim());
      const dur = cs.animationDuration.split(',').map((s) => s.trim());
      w.__anims.push({
        name: e.animationName,
        infinite: (iter[i] ?? iter[0]) === 'infinite',
        ms: parseFloat(dur[i] ?? dur[0]) * ((dur[i] ?? dur[0]).endsWith('ms') ? 1 : 1000),
      });
    }, true);
  });
  await installApiStubs(page, role);
  await seedSession(page, role, 'light');
  await page.goto(route, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(400);
}

/** Non-interactive elements that a `:hover` rule lifts with a negative translateY. */
function hoverLiftsOnStaticElements(page: Page) {
  return page.evaluate(() => {
    const INTERACTIVE = 'a[href], button, input, select, textarea, summary, label, [role="button"], [role="link"], [role="tab"], [role="menuitem"], [role="option"], [tabindex]:not([tabindex="-1"])';
    const out = new Set<string>();
    const visit = (rules: CSSRuleList) => {
      for (const rule of Array.from(rules)) {
        if (rule instanceof CSSMediaRule || rule instanceof CSSSupportsRule) { visit(rule.cssRules); continue; }
        if (!(rule instanceof CSSStyleRule) || !rule.selectorText.includes(':hover')) continue;
        if (!/translateY\(\s*-/.test(rule.style.transform)) continue;
        for (const sel of rule.selectorText.split(',')) {
          if (!sel.includes(':hover')) continue;
          const base = sel.replace(/:hover/g, '').replace(/:not\(:disabled\)/g, '').trim();
          let els: Element[] = [];
          try { els = Array.from(document.querySelectorAll(base)); } catch { continue; }
          for (const el of els) {
            if ((el as HTMLElement).offsetParent === null) continue;
            if (el.matches(INTERACTIVE)) continue;
            out.add(`${el.tagName.toLowerCase()}.${[...el.classList].filter((c) => !c.startsWith('ng-')).join('.')}`);
          }
        }
      }
    };
    for (const sheet of Array.from(document.styleSheets)) {
      try { visit(sheet.cssRules); } catch { /* cross-origin font sheet */ }
    }
    return [...out].sort();
  });
}

const loadMoments = (page: Page) =>
  page.evaluate(() => {
    const anims = (window as unknown as { __anims: { name: string; infinite: boolean; ms: number }[] }).__anims;
    return [...new Set(anims.filter((a) => !a.infinite).map((a) => a.name))].sort();
  });

test.describe('Motion restraint (DS-13)', () => {
  for (const c of CAPTURED_CONTRACTS) {
    test(`${c.id} ${c.title}`, async ({ page }) => {
      await open(page, c.route!, c.role);
      const lifts = await hoverLiftsOnStaticElements(page);
      const moments = await loadMoments(page);
      expect({ lifts, moments: moments.length > 1 ? moments : [] },
        'no hover-lift on static elements; at most one page-load animation')
        .toEqual({ lifts: [], moments: [] });
    });
  }

  test('reduced motion: nothing animates for longer than a frame', async ({ page }) => {
    const long: string[] = [];
    for (const c of CAPTURED_CONTRACTS.filter((x) => ['SCR-01', 'SCR-10', 'SCR-20', 'SCR-50'].includes(x.id))) {
      await page.unrouteAll({ behavior: 'ignoreErrors' });
      await open(page, c.route!, c.role, true);
      const anims = await page.evaluate(() => (window as unknown as { __anims: { name: string; ms: number }[] }).__anims);
      long.push(...anims.filter((a) => a.ms > 1).map((a) => `${c.id} ${a.name} ${a.ms}ms`));
    }
    expect(long).toEqual([]);
  });
});
