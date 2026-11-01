/**
 * Approved 360 px My Courses: filter chips with counts, rows that resume the course (details
 * behind an info button), "Find a new course", and a Continue bar above the bottom bar.
 * Desktop at 1280 px is unchanged.
 */
import { test, expect, Page } from '@playwright/test';
import { installApiStubs, seedSession } from './visual/api-stubs';

async function openCourses(page: Page, width: number, query = '') {
  await page.setViewportSize({ width, height: width === 360 ? 800 : 900 });
  await installApiStubs(page, 'student');
  await page.route('**/api/student/courses/1/resume', (r) =>
    r.request().method() === 'OPTIONS'
      ? r.fallback()
      : r.fulfill({
          status: 200,
          headers: { 'content-type': 'application/json', 'access-control-allow-origin': 'http://127.0.0.1:4200', 'access-control-allow-credentials': 'true' },
          body: JSON.stringify({ courseId: 1, lectureId: 2, title: 'Features, labels and leakage', contentType: 'VIDEO', position: 1, progressPercent: 62, secondsWatched: 565, completed: false }),
        }));
  await seedSession(page, 'student', 'light');
  await page.goto('/student/courses' + query);
  await page.waitForLoadState('networkidle');
}

const phone = (page: Page) => page.locator('.courses-phone');

test.describe('My Courses at 360 px', () => {
  test('chips with counts; rows resume the course; info opens details; no sideways scroll', async ({ page }) => {
    await openCourses(page, 360);
    await expect(page.locator('.student-courses .page-head')).toBeHidden();
    const chips = phone(page).getByRole('group', { name: 'Filter courses' }).getByRole('button');
    await expect(chips).toHaveText(['All 3', 'In progress 2', 'Completed 1']);
    await expect(chips.first()).toHaveAttribute('aria-pressed', 'true');

    const ml = phone(page).getByRole('link', { name: /^Applied Machine Learning/ });
    await expect(ml).toHaveAttribute('href', '/student/courses/1/player');
    await expect(ml).toContainText('5 of 12');
    await expect(ml).toContainText('Resume lesson 6');
    await expect(phone(page).getByRole('link', { name: 'Course details: Applied Machine Learning' })).toHaveAttribute('href', '/student/courses/1');
    await expect(phone(page).getByRole('link', { name: 'Find a new course' })).toHaveAttribute('href', '/student/explore');

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
  });

  test('the Completed chip shows only finished courses', async ({ page }) => {
    await openCourses(page, 360);
    await phone(page).getByRole('button', { name: 'Completed 1' }).click();
    const rows = phone(page).locator('.mc-row');
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText('Design Systems in Practice');
    await expect(rows.first()).toContainText('Completed');
  });

  test('a search from the header filters the list', async ({ page }) => {
    await openCourses(page, 360, '?search=design');
    await expect(phone(page).locator('.mc-row')).toHaveCount(1);
    await expect(phone(page)).toContainText('Design Systems in Practice');
  });

  test('Continue bar sits above the bottom bar and the AI button above it', async ({ page }) => {
    await openCourses(page, 360);
    const bar = page.locator('.continue-bar');
    await expect(bar).toContainText('Features, labels and leakage');
    await expect(bar).toContainText('62% watched');
    await expect(bar.getByRole('link')).toHaveAttribute('href', '/student/courses/1/lectures/2');
    const b = (await bar.boundingBox())!;
    const nav = (await page.locator('nav.mobile-bar').boundingBox())!;
    const ai = (await page.getByRole('button', { name: 'Toggle AI Assistant' }).boundingBox())!;
    expect(b.y + b.height).toBeLessThanOrEqual(nav.y + 1);
    expect(ai.y + ai.height).toBeLessThanOrEqual(b.y);
  });
});

test.describe('My Courses at 1280 px is unchanged', () => {
  test('desktop header and tabs stay; phone layout and Continue bar hidden', async ({ page }) => {
    await openCourses(page, 1280);
    await expect(page.locator('.student-courses .page-head')).toBeVisible();
    await expect(page.locator('nav.tabs')).toBeVisible();
    await expect(phone(page)).toBeHidden();
    await expect(page.locator('.continue-bar')).toBeHidden();
  });
});
