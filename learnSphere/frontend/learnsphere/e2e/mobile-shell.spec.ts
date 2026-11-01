/**
 * Approved mobile redesign (360 px): slim header (page title, search, notifications), a role
 * bottom bar plus a More sheet, focus mode on the lesson player, and desktop left as it was.
 */
import { test, expect, Page } from '@playwright/test';
import { installApiStubs, seedSession } from './visual/api-stubs';

async function open(page: Page, route: string, role: 'student' | 'teacher' | 'admin', width: number) {
  await page.setViewportSize({ width, height: width === 360 ? 800 : 900 });
  await installApiStubs(page, role);
  await seedSession(page, role, 'light');
  await page.goto(route);
  await page.locator('app-root > :not(router-outlet)').first().waitFor({ state: 'attached' });
  await page.waitForLoadState('networkidle');
}

const bar = (page: Page) => page.locator('nav.mobile-bar');

test.describe('Mobile shell at 360 px', () => {
  test('student: bottom bar, slim header, no menu button, no sideways scroll', async ({ page }) => {
    await open(page, '/student/dashboard', 'student', 360);
    await expect(bar(page)).toBeVisible();
    await expect(bar(page).locator(':scope > *')).toHaveText(['Home', 'My Courses', 'Live', 'Tests', 'More']);
    await expect(bar(page).getByRole('link', { name: 'Home' })).toHaveAttribute('aria-current', 'page');

    const header = page.locator('header.topbar');
    await expect(header.getByRole('heading', { level: 1 })).toHaveText('Home');
    await expect(page.getByRole('button', { name: 'Toggle navigation menu' })).toBeHidden();
    const controls = await header.locator('button, a, input').filter({ visible: true }).count();
    expect(controls).toBeLessThanOrEqual(2); // search + notifications

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
  });

  test('More opens a sheet with the other destinations and closes with Escape', async ({ page }) => {
    await open(page, '/student/dashboard', 'student', 360);
    await bar(page).getByRole('button', { name: 'More' }).click();
    const sheet = page.getByRole('dialog', { name: 'More' });
    await expect(sheet).toBeVisible();
    await expect(sheet.getByRole('link', { name: 'Notes' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(sheet).toBeHidden();
  });

  test('choosing a destination in More navigates and closes the sheet', async ({ page }) => {
    await open(page, '/student/dashboard', 'student', 360);
    await bar(page).getByRole('button', { name: 'More' }).click();
    await page.getByRole('dialog', { name: 'More' }).getByRole('link', { name: 'Notes' }).click();
    await expect(page).toHaveURL(/\/student\/notes$/);
    await expect(page.getByRole('dialog', { name: 'More' })).toBeHidden();
    await expect(page.locator('header.topbar').getByRole('heading', { level: 1 })).toHaveText('Notes');
  });

  test('the browser back gesture closes the sheet before leaving the page', async ({ page }) => {
    await open(page, '/student/dashboard', 'student', 360);
    await bar(page).getByRole('button', { name: 'More' }).click();
    await expect(page.getByRole('dialog', { name: 'More' })).toBeVisible();
    await page.goBack();
    await expect(page.getByRole('dialog', { name: 'More' })).toBeHidden();
    await expect(page).toHaveURL(/\/student\/dashboard$/);
  });

  test('the lesson player is focus mode: no bottom bar', async ({ page }) => {
    await open(page, '/student/courses/1/lectures/1', 'student', 360);
    await expect(bar(page)).toBeHidden();
  });

  test('the AI assistant button sits above the bottom bar', async ({ page }) => {
    await open(page, '/student/dashboard', 'student', 360);
    const ai = await page.getByRole('button', { name: 'Toggle AI Assistant' }).boundingBox();
    const nav = await bar(page).boundingBox();
    expect(ai!.y + ai!.height).toBeLessThanOrEqual(nav!.y);
  });

  test('teacher and admin get their own bars', async ({ page }) => {
    await open(page, '/teacher/dashboard', 'teacher', 360);
    await expect(bar(page).locator(':scope > *')).toHaveText(['Home', 'Courses', 'Live', 'Messages', 'More']);
    await open(page, '/admin/dashboard', 'admin', 360);
    await expect(bar(page).locator(':scope > *')).toHaveText(['Home', 'Users', 'Approvals', 'Reports', 'More']);
  });
});

test.describe('Desktop at 1280 px is unchanged', () => {
  test('no bottom bar; the sidebar and full header stay', async ({ page }) => {
    await open(page, '/student/dashboard', 'student', 1280);
    await expect(bar(page)).toBeHidden();
    await expect(page.locator('app-sidebar aside, app-sidebar nav').first()).toBeVisible();
    await expect(page.getByTitle('Messages')).toBeVisible();
  });
});

test.describe('Phone pages scroll clear of the floating AI button', () => {
  test('at the bottom of Home the last row is not under the AI button', async ({ page }) => {
    await open(page, '/student/dashboard', 'student', 360);
    await page.locator('.home-courses__row').last().waitFor();
    await page.evaluate(() => {
      for (const sel of ['.content-area', '.scroll-container']) {
        const el = document.querySelector(sel) as HTMLElement;
        el.scrollTo(0, el.scrollHeight);
      }
    });
    const last = await page.locator('.home-courses__row').last().boundingBox();
    const ai = await page.getByRole('button', { name: 'Toggle AI Assistant' }).boundingBox();
    expect(last!.y + last!.height).toBeLessThanOrEqual(ai!.y);
  });
});
