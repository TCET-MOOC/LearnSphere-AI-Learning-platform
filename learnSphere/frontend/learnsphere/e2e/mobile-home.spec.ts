/**
 * Approved 360 px student Home: greeting, a "pick up where you left off" card, a short
 * "Coming up" list and the student's courses. The KPI grid and desktop panels stay on
 * desktop; desktop at 1280 px is unchanged.
 */
import { test, expect, Page } from '@playwright/test';
import { installApiStubs, seedSession } from './visual/api-stubs';

function json(body: unknown) {
  return {
    status: 200,
    headers: { 'content-type': 'application/json', 'access-control-allow-origin': 'http://127.0.0.1:4200', 'access-control-allow-credentials': 'true' },
    body: JSON.stringify(body),
  };
}

async function openHome(page: Page, width: number, opts: { allComplete?: boolean } = {}) {
  await page.setViewportSize({ width, height: width === 360 ? 800 : 900 });
  await installApiStubs(page, 'student');
  const later = new Date();
  later.setHours(later.getHours() + 2, 0, 0, 0);
  const get = (r: import('@playwright/test').Route, body: unknown) => (r.request().method() === 'OPTIONS' ? r.fallback() : r.fulfill(json(body)));
  await page.route('**/api/student/courses/1/resume', (r) =>
    get(r, { courseId: 1, lectureId: 2, title: 'Features, labels and leakage', contentType: 'VIDEO', position: 1, progressPercent: 62, secondsWatched: 565, completed: false }));
  await page.route('**/api/student/live-sessions', (r) =>
    get(r, [{ id: 7, courseId: 1, courseTitle: 'Applied Machine Learning', title: 'Live revision: model evaluation', status: 'SCHEDULED', scheduledAt: later.toISOString() }]));
  if (opts.allComplete) {
    await page.route('**/api/student/courses', (r) =>
      get(r, [{ id: 2, title: 'Design Systems in Practice', teacherName: 'Meera Iyer', department: 'Design', lectureCount: 8, completedLecturesCount: 8, progressPercent: 100, completed: true }]));
  }
  await seedSession(page, 'student', 'light');
  await page.goto('/student/dashboard');
  await page.waitForLoadState('networkidle');
}

test.describe('Student Home at 360 px', () => {
  test('greeting, resume card, coming up and my courses; no KPI grid; no sideways scroll', async ({ page }) => {
    await openHome(page, 360);
    const home = page.locator('.home-phone');
    await expect(home).toBeVisible();
    await expect(page.locator('.kpi-grid')).toBeHidden();
    await expect(home.locator('.home-phone__greeting')).toHaveText(/^Good (morning|afternoon|evening), Aarav$/);

    const resume = home.locator('.resume-card');
    await expect(resume.getByRole('heading', { name: 'Pick up where you left off' })).toBeVisible();
    await expect(resume).toContainText('Features, labels and leakage');
    await expect(resume).toContainText('Applied Machine Learning, lesson 2 of 3');
    await expect(resume.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '62');
    await expect(resume.getByRole('link', { name: 'Resume lesson' })).toHaveAttribute('href', '/student/courses/1/lectures/2');

    const upcoming = home.locator('.coming-up');
    await expect(upcoming.getByRole('heading', { name: 'Coming up' })).toBeVisible();
    await expect(upcoming).toContainText('Live revision: model evaluation');
    await expect(upcoming.getByRole('link', { name: /Open/ }).first()).toHaveAttribute('href', '/student/live/7');

    const courses = home.locator('.home-courses');
    await expect(courses.getByRole('link', { name: 'See all 3' })).toHaveAttribute('href', '/student/courses');
    await expect(courses.getByRole('link', { name: /Applied Machine Learning/ })).toHaveAttribute('href', '/student/courses/1/player');
    await expect(courses.getByRole('link', { name: /Design Systems in Practice/ })).toContainText('Completed');

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
  });

  test('with every course finished there is no resume card; Explore is offered instead', async ({ page }) => {
    await openHome(page, 360, { allComplete: true });
    await expect(page.locator('.home-phone .resume-card')).toHaveCount(0);
    await expect(page.locator('.home-phone').getByRole('link', { name: 'Explore courses' })).toHaveAttribute('href', '/student/explore');
  });
});

test.describe('Student Home at 1280 px is unchanged', () => {
  test('desktop panels stay; the phone layout is hidden', async ({ page }) => {
    await openHome(page, 1280);
    await expect(page.locator('.home-phone')).toBeHidden();
    await expect(page.locator('.kpi-grid')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Continue Learning' })).toBeVisible();
  });
});
