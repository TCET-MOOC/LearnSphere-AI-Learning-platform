/**
 * Approved 360 px lesson player: focus mode with its own header, recorded progress with the
 * 90 % completion marker, three actions, Up next / Notes / Discussion tabs, and a sticky
 * Mark complete / Next lesson bar. Desktop keeps its layout. Full screen comes from the
 * browser's (or YouTube's) own controls, so those controls must stay on.
 */
import { test, expect, Page } from '@playwright/test';
import { installApiStubs, seedSession } from './visual/api-stubs';

const LECTURES = [
  { id: 1, courseId: 1, title: 'What machine learning actually solves', number: 1, videoUrl: 'http://127.0.0.1:4200/missing-1.mp4', duration: 754, status: 'PUBLISHED' },
  { id: 2, courseId: 1, title: 'Features, labels and leakage', number: 2, videoUrl: 'http://127.0.0.1:4200/missing-2.mp4', duration: 912, status: 'PUBLISHED' },
  { id: 3, courseId: 1, title: 'Evaluating a classifier', number: 3, videoUrl: 'http://127.0.0.1:4200/missing-3.mp4', duration: 1105, status: 'PUBLISHED' },
];

async function openPlayer(page: Page, width: number, opts: { quiz?: boolean; percent?: number; assignments?: boolean } = {}) {
  await page.setViewportSize({ width, height: width === 360 ? 800 : 900 });
  await installApiStubs(page, 'student');
  const json = (body: unknown) => ({
    status: 200,
    headers: { 'content-type': 'application/json', 'access-control-allow-origin': 'http://127.0.0.1:4200', 'access-control-allow-credentials': 'true' },
    body: JSON.stringify(body),
  });
  await page.route('**/api/courses/1/lectures', (r) => r.request().method() === 'OPTIONS' ? r.fallback() : r.fulfill(json(LECTURES)));
  await page.route('**/api/assessments/lecture/*', (r) => r.request().method() === 'OPTIONS' ? r.fallback() : r.fulfill(json(opts.quiz ? [{ id: 5, title: 'Check' }] : [])));
  await page.route('**/api/student/lectures/*/progress', (r) => r.request().method() === 'OPTIONS' ? r.fallback() : r.fulfill(json({ id: 1, lectureId: 2, progressPercent: opts.percent ?? 62, secondsWatched: 565 })));
  await page.route('**/api/courses/1/assignments', (r) => r.request().method() === 'OPTIONS' ? r.fallback() : r.fulfill(json(opts.assignments ? [{ assignmentId: 4, title: 'Leakage write-up', maxMarks: 10 }] : [])));
  await seedSession(page, 'student', 'light');
  await page.goto('/student/courses/1/lectures/2');
  await page.locator('.workspace-grid').waitFor();
  await page.waitForLoadState('networkidle');
}

const phoneHeader = (page: Page) => page.locator('.player-phonebar');
const stickyBar = (page: Page) => page.locator('.player-actionbar');

test.describe('Lesson player at 360 px', () => {
  test('focus mode: own header with back, course and lesson position; no app header or bottom bar', async ({ page }) => {
    await openPlayer(page, 360);
    await expect(page.locator('header.topbar')).toBeHidden();
    await expect(page.locator('nav.mobile-bar')).toBeHidden();
    await expect(phoneHeader(page)).toBeVisible();
    await expect(phoneHeader(page).getByRole('link', { name: 'Back to course' })).toHaveAttribute('href', '/student/courses/1');
    await expect(phoneHeader(page)).toContainText('Applied Machine Learning');
    await expect(phoneHeader(page)).toContainText('Lesson 2 of 3');
  });

  test('shows recorded progress against the 90 % completion mark', async ({ page }) => {
    await openPlayer(page, 360);
    const progress = page.locator('.player-progress');
    await expect(progress).toContainText('62% watched. Completes at 90%.');
    await expect(progress.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '62');
    await expect(progress.locator('.player-progress__mark')).toBeVisible();
  });

  test('three actions, three tabs, no placeholder Resources tab', async ({ page }) => {
    await openPlayer(page, 360, { quiz: true });
    const actions = page.locator('.player-quick-actions');
    await expect(actions.getByRole('button')).toHaveText(['Bookmark', 'Add note', 'Quiz']);
    const tabs = page.getByRole('tablist');
    await expect(tabs.getByRole('tab').filter({ visible: true })).toHaveText(['Up next', 'Notes', 'Discussion']);
    await expect(tabs.getByRole('tab', { name: 'Up next' })).toHaveAttribute('aria-selected', 'true');
    await expect(page.getByRole('tabpanel')).toContainText('Evaluating a classifier');
  });

  test('a course with assignments keeps its Assignments tab on phones', async ({ page }) => {
    await openPlayer(page, 360, { assignments: true });
    await expect(page.getByRole('tablist').getByRole('tab').filter({ visible: true })).toHaveText(['Up next', 'Notes', 'Discussion', 'Assignments']);
  });

  test('sticky bar: completion locked below 90 %, Next lesson opens lesson 3', async ({ page }) => {
    await openPlayer(page, 360);
    await expect(stickyBar(page).getByRole('button', { name: 'Mark complete at 90%' })).toBeDisabled();
    const box = await stickyBar(page).boundingBox();
    expect(box!.y + box!.height).toBeGreaterThanOrEqual(799); // pinned to the bottom of an 800 px screen
    await stickyBar(page).getByRole('link', { name: 'Next lesson' }).click();
    await expect(page).toHaveURL(/\/student\/courses\/1\/lectures\/3$/);
  });

  test('completion unlocks at 90 % recorded', async ({ page }) => {
    await openPlayer(page, 360, { percent: 90 });
    await expect(stickyBar(page).getByRole('button', { name: 'Mark complete' })).toBeEnabled();
  });

  test('a lecture with a quiz completes through the quiz', async ({ page }) => {
    await openPlayer(page, 360, { quiz: true });
    await expect(stickyBar(page).getByRole('button', { name: 'Take quiz' })).toBeEnabled();
  });

  test('no sideways scroll, and the video keeps its built-in controls (full screen)', async ({ page }) => {
    await openPlayer(page, 360);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
    await expect(page.locator('video')).toHaveAttribute('controls', '');
  });
});

test.describe('Lesson player at 1280 px keeps its layout', () => {
  test('no phone header or sticky bar; desktop playlist and app header stay; controls stay on', async ({ page }) => {
    await openPlayer(page, 1280);
    await expect(phoneHeader(page)).toBeHidden();
    await expect(stickyBar(page)).toBeHidden();
    await expect(page.locator('header.topbar')).toBeVisible();
    await expect(page.locator('.sidebar-column')).toBeVisible();
    await expect(page.locator('video')).toHaveAttribute('controls', '');
  });
});

test.describe('Lesson player at 1280 px: meta card actions', () => {
  test('every action button sits inside the card, on one line, without overlapping', async ({ page }) => {
    await openPlayer(page, 1280, { quiz: true });
    const card = (await page.locator('.lecture-meta-card').boundingBox())!;
    const buttons = page.locator('.lecture-meta-card .meta-actions .action-btn');
    const count = await buttons.count();
    expect(count).toBeGreaterThanOrEqual(3);
    const boxes = [];
    for (let i = 0; i < count; i++) {
      const b = (await buttons.nth(i).boundingBox())!;
      expect(b.x).toBeGreaterThanOrEqual(card.x);
      expect(b.x + b.width).toBeLessThanOrEqual(card.x + card.width);
      expect(b.height).toBeLessThanOrEqual(40); // label on one line
      boxes.push(b);
    }
    for (let i = 0; i < boxes.length; i++) {
      for (let j = i + 1; j < boxes.length; j++) {
        const a = boxes[i], b = boxes[j];
        const overlap = a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height;
        expect(overlap).toBe(false);
      }
    }
  });
});
