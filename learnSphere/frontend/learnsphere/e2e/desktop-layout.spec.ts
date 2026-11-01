/**
 * Desktop (1280 px) layout gate, requested after the post-W6 audit: on every screen nothing
 * is drawn over anything else, nothing is cut off by its container and nothing pushes the
 * page sideways (e2e/layout-audit.ts). Profile and settings also get explicit checks for the
 * shared blocks whose styles were lost in commit 8000ccd (switches, text buttons, user card).
 */
import { test, expect, Page } from '@playwright/test';
import { CAPTURED_CONTRACTS } from './visual/screen-contracts';
import { installApiStubs, seedSession } from './visual/api-stubs';
import { auditLayout } from './layout-audit';

type Role = 'student' | 'teacher' | 'admin';

async function open(page: Page, route: string, role: Role | 'public') {
  await page.setViewportSize({ width: 1280, height: 900 });
  await installApiStubs(page, role);
  await seedSession(page, role, 'light');
  await page.goto(route, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(300);
}

test.describe('Desktop layout at 1280 px: no overlaps, cut-offs or sideways scroll', () => {
  for (const c of CAPTURED_CONTRACTS) {
    test(`${c.id} ${c.title}`, async ({ page }) => {
      await open(page, c.route!, c.role);
      expect(await auditLayout(page)).toEqual([]);
    });
  }
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
