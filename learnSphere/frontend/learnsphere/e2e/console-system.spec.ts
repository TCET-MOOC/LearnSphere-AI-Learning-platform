/**
 * DS-12 (W7-14; NFR-04, NFR-01): the teacher and admin consoles are on one system.
 *   - numbers that are money, scores, percentages or counts use tabular numerals, so columns
 *     and stat tiles do not jitter as values change;
 *   - every card-like container (cards, panels, tiles, KPI/stat boxes) shares one chrome:
 *     one corner radius and one border width, taken from the tokens.
 * Measured on the rendered page (computed styles), at 1280 px light and dark, on the stubbed API.
 */
import { test, expect, Page } from '@playwright/test';
import { CAPTURED_CONTRACTS } from './visual/screen-contracts';
import { installApiStubs, seedSession } from './visual/api-stubs';

const CONSOLE = CAPTURED_CONTRACTS.filter((c) => c.role === 'teacher' || c.role === 'admin');

/** The card radius every console card uses (`--border-radius-lg`). */
const CARD_RADIUS = '12px';

async function open(page: Page, route: string, role: 'teacher' | 'admin', theme: 'light' | 'dark') {
  await page.setViewportSize({ width: 1280, height: 900 });
  await installApiStubs(page, role);
  await seedSession(page, role, theme);
  await page.goto(route, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(300);
}

/** Text leaves inside the routed screen (not the shell) whose whole text is a number. */
function numbersWithoutTabularFigures(page: Page) {
  return page.evaluate(() => {
    const root = document.querySelector('.main-content router-outlet + *, main router-outlet + *, router-outlet + *:not(app-root)') ?? document.body;
    const NUMBER = /^[₹$]?\s?[+-−]?\d[\d,]*(\.\d+)?\s?(%|k|K|L|Cr|pts)?$/;
    const out: string[] = [];
    for (const el of Array.from(root.querySelectorAll<HTMLElement>('*'))) {
      if (el.children.length > 0) continue;
      const text = (el.textContent ?? '').trim();
      if (!NUMBER.test(text)) continue;
      const r = el.getBoundingClientRect();
      if (r.width === 0 || r.height === 0) continue;
      if (el.closest('input, textarea, select, button.tab, .pager-status, time')) continue;
      const fvn = getComputedStyle(el).fontVariantNumeric;
      if (!fvn.includes('tabular-nums')) out.push(`${el.tagName.toLowerCase()}.${[...el.classList].join('.')} "${text}"`);
    }
    return out;
  });
}

/** Card-like containers whose radius or border differs from the console card. */
function offSystemCards(page: Page, radius: string) {
  return page.evaluate((cardRadius) => {
    const root = document.querySelector('router-outlet + *:not(app-root)') ?? document.body;
    const CARD = /(^|[-_])(card|panel|tile|kpi|stat-card|summary-card|metric)([-_]|$)/;
    const out: string[] = [];
    for (const el of Array.from(root.querySelectorAll<HTMLElement>('[class]'))) {
      const cls = [...el.classList].find((c) => CARD.test(c) && !/__|--(?!$)/.test(c) && !/(label|value|val|icon|head|header|body|title|sub|top|foot|grid|list|row|actions?|meta|text|chip)$/.test(c));
      if (!cls) continue;
      if (el.closest('.modal-backdrop, [role="dialog"]')) continue;
      const cs = getComputedStyle(el);
      const r = el.getBoundingClientRect();
      if (r.width < 120 || r.height < 48) continue;
      const bordered = parseFloat(cs.borderTopWidth) > 0;
      const filled = cs.backgroundColor !== 'rgba(0, 0, 0, 0)' && cs.backgroundColor !== 'transparent';
      if (!bordered || !filled) continue; // only chrome-bearing containers
      const problems: string[] = [];
      if (cs.borderTopLeftRadius !== cardRadius) problems.push(`radius ${cs.borderTopLeftRadius}`);
      if (cs.borderTopWidth !== '1px') problems.push(`border ${cs.borderTopWidth}`);
      if (problems.length) out.push(`.${cls}: ${problems.join(', ')}`);
    }
    return [...new Set(out)];
  }, radius);
}

for (const theme of ['light', 'dark'] as const) {
  test.describe(`Consoles on one system (DS-12) — ${theme}`, () => {
    for (const c of CONSOLE) {
      test(`${c.id} ${c.title}: tabular numerals and one card chrome`, async ({ page }) => {
        await open(page, c.route!, c.role as 'teacher' | 'admin', theme);
        const numbers = await numbersWithoutTabularFigures(page);
        const cards = await offSystemCards(page, CARD_RADIUS);
        expect({ numbers, cards }, 'numbers need tabular figures; cards share one chrome').toEqual({ numbers: [], cards: [] });
      });
    }
  });
}
