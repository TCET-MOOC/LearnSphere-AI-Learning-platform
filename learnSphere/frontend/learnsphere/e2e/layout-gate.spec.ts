/**
 * Layout gate at 360, 768 and 1280 px (post-W6 audit, requested by the user): on every
 * screen nothing is drawn over anything else, nothing is cut off by its container and
 * nothing pushes the page sideways (e2e/layout-audit.ts). Some defects the detector cannot
 * see (squeezed rows, crowded headers, wrong links) get explicit checks below.
 */
import { test, expect, Page } from '@playwright/test';
import { CAPTURED_CONTRACTS } from './visual/screen-contracts';
import { installApiStubs, seedSession } from './visual/api-stubs';
import { auditLayout } from './layout-audit';

type Role = 'student' | 'teacher' | 'admin';

async function open(page: Page, route: string, role: Role | 'public', width = 1280) {
  await page.setViewportSize({ width, height: width === 360 ? 800 : width === 768 ? 1024 : 900 });
  await installApiStubs(page, role);
  await seedSession(page, role, 'light');
  await page.goto(route, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(300);
}

for (const width of [360, 768, 1280]) {
  test.describe(`Layout at ${width} px: no overlaps, cut-offs or sideways scroll`, () => {
    for (const c of CAPTURED_CONTRACTS) {
      test(`${c.id} ${c.title}`, async ({ page }) => {
        await open(page, c.route!, c.role, width);
        expect(await auditLayout(page)).toEqual([]);
      });
    }
  });
}

test.describe('Phone layouts the detector cannot judge (360 px)', () => {
  test('bookmarks: the title gets the row width and Jump to lecture opens that lecture at the time', async ({ page }) => {
    await open(page, '/student/bookmarks', 'student', 360);
    const row = page.locator('.bookmark-row').first();
    const main = (await row.locator('.bookmark-main').boundingBox())!;
    expect(main.width).toBeGreaterThanOrEqual(150); // not squeezed to one word per line (was ~55 px)
    const actions = (await row.locator('.bookmark-actions').boundingBox())!;
    expect(actions.y).toBeGreaterThanOrEqual(main.y + main.height - 1); // buttons below the text, not on it
    await expect(row.getByRole('link', { name: 'Jump to lecture' })).toHaveAttribute('href', /^\/student\/courses\/\d+\/lectures\/\d+\?t=\d+$/);
  });

  test('course editor: secondary actions live behind a ⋮ menu on phones', async ({ page }) => {
    await open(page, '/teacher/courses/1/manage', 'teacher', 360);
    const head = page.locator('.course-management .page-head');
    await expect(head.getByRole('button', { name: /Import YouTube/ })).toBeHidden();
    await head.getByRole('button', { name: 'More course actions' }).click();
    const menu = page.getByRole('menu');
    await expect(menu.getByRole('menuitem')).toHaveText([/Import YouTube video or playlist/, /Back to courses/, /Delete course/]);
  });

  test('course editor keeps its buttons on desktop', async ({ page }) => {
    await open(page, '/teacher/courses/1/manage', 'teacher', 1280);
    const head = page.locator('.course-management .page-head');
    await expect(head.getByRole('button', { name: /Import YouTube/ })).toBeVisible();
    await expect(head.getByRole('button', { name: 'More course actions' })).toBeHidden();
  });

  test('payouts: each teacher row keeps name, amount and Disburse inside the card', async ({ page }) => {
    await open(page, '/admin/payouts', 'admin', 360);
    const card = page.locator('.payout-item').first();
    const box = (await card.boundingBox())!;
    const btn = (await card.getByRole('button', { name: /Disburse/ }).boundingBox())!;
    expect(btn.x + btn.width).toBeLessThanOrEqual(box.x + box.width + 1);
    const name = (await card.locator('.payout-item__name').boundingBox())!;
    expect(name.width).toBeGreaterThanOrEqual(120); // name not squeezed to one word per line
  });
});

test.describe('Profile and settings blocks are styled (1280 px)', () => {
  for (const role of ['student', 'teacher', 'admin'] as const) {
    test(`${role} settings: user card, text buttons and switches`, async ({ page }) => {
      await open(page, `/${role}/settings`, role);
      const user = page.locator('.settings-user');
      const name = (await user.locator('b').boundingBox())!;
      const email = (await user.locator('small').boundingBox())!;
      expect(email.y).toBeGreaterThanOrEqual(name.y + name.height - 1); // name above e-mail, not run together

      for (const tab of ['General', 'Notifications', 'Privacy & security', 'Account']) {
        await page.locator('.settings-nav').getByRole('button', { name: tab, exact: true }).click();
        // Measured in one pass: the rows re-render, so element handles would go stale.
        const found = await page.evaluate(() => ({
          textButtons: Array.from(document.querySelectorAll('.settings-card .text-btn')).map((el) => {
            const cs = getComputedStyle(el);
            return { border: cs.borderTopWidth, bg: cs.backgroundColor };
          }),
          switches: Array.from(document.querySelectorAll('.settings-card .toggle')).map((el) => {
            const r = el.getBoundingClientRect();
            return { w: r.width, h: r.height };
          })
        }));
        for (const style of found.textButtons) expect(style).toEqual({ border: '0px', bg: 'rgba(0, 0, 0, 0)' }); // not a native grey button
        for (const b of found.switches) {
          expect(b.w).toBeGreaterThanOrEqual(36);
          expect(b.h).toBeGreaterThanOrEqual(20);
        }
      }
    });

    test(`${role} profile: information and security panels are cards`, async ({ page }) => {
      await open(page, `/${role}/profile`, role);
      for (const sel of ['.info-card', '.security-card']) {
        const pad = await page.locator(sel).first().evaluate((el) => parseFloat(getComputedStyle(el).paddingLeft));
        expect(pad, sel).toBeGreaterThanOrEqual(16);
      }
      for (const btn of await page.locator('a.primary-btn, a.secondary-btn, a.danger-btn').all()) {
        expect(await btn.evaluate((el) => getComputedStyle(el).textDecorationLine)).toBe('none'); // buttons, not underlined links
      }
      const links = page.locator('.profile-links a');
      expect(await links.count()).toBeGreaterThanOrEqual(2);
      const first = (await links.nth(0).boundingBox())!;
      const second = (await links.nth(1).boundingBox())!;
      expect(second.y).toBeGreaterThanOrEqual(first.y + first.height - 1); // stacked, not overlapping
    });
  }

  test('teacher settings show the teacher’s own initials', async ({ page }) => {
    await open(page, '/teacher/settings', 'teacher');
    await expect(page.locator('.settings-user > div')).toHaveText('MI');
  });
});
