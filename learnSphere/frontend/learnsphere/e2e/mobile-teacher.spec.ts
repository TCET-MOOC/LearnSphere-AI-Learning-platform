/**
 * Approved 360 px teacher Home: greeting, the next class, students waiting for a reply and
 * the teacher's courses with their status, all from real data. Desktop at 1280 px is unchanged.
 */
import { test, expect, Page, Route } from '@playwright/test';
import { installApiStubs, seedSession } from './visual/api-stubs';

const get = (r: Route, body: unknown) =>
  r.request().method() === 'OPTIONS'
    ? r.fallback()
    : r.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json', 'access-control-allow-origin': 'http://127.0.0.1:4200', 'access-control-allow-credentials': 'true' },
        body: JSON.stringify(body),
      });

async function openTeacherHome(page: Page, width: number) {
  await page.setViewportSize({ width, height: width === 360 ? 800 : 900 });
  await installApiStubs(page, 'teacher');
  // Fixed midday clock: "two hours from now" must stay on the same day whenever the suite runs.
  const now = new Date();
  now.setHours(12, 0, 0, 0);
  await page.clock.setFixedTime(now);
  const later = new Date(now);
  later.setHours(14, 0, 0, 0);
  const recent = new Date(now.getTime() - 10 * 60 * 1000).toISOString();
  await page.route('**/api/teacher/live-sessions', (r) =>
    get(r, [{ id: 4, courseId: 1, courseTitle: 'Engineering Mathematics III', title: 'Live revision: Laplace transforms', status: 'SCHEDULED', scheduledAt: later.toISOString(), joinUrl: null }]));
  await page.route('**/api/conversations', (r) =>
    get(r, [
      { id: 11, otherUserId: 7, otherUserName: 'Raj Shah', otherUserRole: 'student', lastMessage: 'Can you clarify Laplace lecture 8?', lastMessageAt: recent, createdAt: recent, unreadCount: 2 },
      { id: 12, otherUserId: 8, otherUserName: 'Read Already', otherUserRole: 'student', lastMessage: 'Thanks!', lastMessageAt: recent, createdAt: recent, unreadCount: 0 },
    ]));
  await page.route('**/api/teacher/courses', (r) =>
    get(r, [
      { id: 21, title: 'Statistics for Engineers', status: 'DRAFT', teacherId: 2, department: 'Maths', description: '', thumbnail: '', price: 0 },
      { id: 22, title: 'Numerical Methods', status: 'PENDING', teacherId: 2, department: 'Maths', description: '', thumbnail: '', price: 0 },
    ]));
  await seedSession(page, 'teacher', 'light');
  await page.goto('/teacher/dashboard');
  await page.waitForLoadState('networkidle');
}

const home = (page: Page) => page.locator('.teacher-phone');

test.describe('Teacher Home at 360 px', () => {
  test('next class, waiting replies and courses from real data; no invented KPIs; no sideways scroll', async ({ page }) => {
    await openTeacherHome(page, 360);
    await expect(home(page)).toBeVisible();
    await expect(page.locator('.dashboard-container .kpi-row')).toBeHidden();
    await expect(home(page).locator('.teacher-phone__greeting')).toHaveText(/^Good (morning|afternoon|evening), Meera$/);

    const next = home(page).locator('.next-class');
    await expect(next).toContainText(/Your next class, today at \d{1,2}:00/);
    await expect(next).toContainText('Live revision: Laplace transforms');
    await expect(next).toContainText('Engineering Mathematics III');
    await expect(next.getByRole('link', { name: 'Open live classes' })).toHaveAttribute('href', '/teacher/live');

    const waiting = home(page).locator('.awaiting');
    await expect(waiting.getByRole('heading', { name: 'Waiting for your reply' })).toBeVisible();
    await expect(waiting.getByRole('link', { name: 'All 1' })).toHaveAttribute('href', '/teacher/messages');
    const raj = waiting.getByRole('link', { name: /Raj Shah/ });
    await expect(raj).toHaveAttribute('href', '/teacher/messages?c=11');
    await expect(raj).toContainText('Can you clarify Laplace lecture 8?');
    await expect(waiting).not.toContainText('Read Already');

    const courses = home(page).locator('.teacher-courses');
    await expect(courses.getByRole('link', { name: /Statistics for Engineers/ })).toContainText('Draft');
    await expect(courses.getByRole('link', { name: /Numerical Methods/ })).toContainText('In review');

    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
  });

  test('a waiting reply opens that conversation', async ({ page }) => {
    await openTeacherHome(page, 360);
    await home(page).locator('.awaiting').getByRole('link', { name: /Raj Shah/ }).click();
    await expect(page).toHaveURL(/\/teacher\/messages\?c=11$/);
    await expect(page.locator('app-teacher-messages')).toContainText('Raj Shah');
  });
});

test.describe('Teacher Home at 1280 px is unchanged', () => {
  test('desktop layout stays; the phone layout is hidden', async ({ page }) => {
    await openTeacherHome(page, 1280);
    await expect(home(page)).toBeHidden();
    await expect(page.getByText('Welcome back, Meera Iyer')).toBeVisible();
  });
});
