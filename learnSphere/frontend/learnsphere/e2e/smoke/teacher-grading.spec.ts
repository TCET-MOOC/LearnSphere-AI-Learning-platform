/**
 * Smoke (W7-16; SCR-35, W4-05, AC-17): a teacher grades a written answer and, after a full
 * reload, the queue and the counts come back from the server without it — the grade went to
 * `PATCH /answers/{id}/grade`, it was not kept in the page. The payload never carries a key.
 */
import { test, expect, Route } from '@playwright/test';
import { PENDING_ANSWERS, TEACHER_TESTS, installApiStubs, seedSession } from '../visual/api-stubs';

test.use({ viewport: { width: 1280, height: 900 } });

const json = (route: Route, body: unknown, status = 200) =>
  route.fulfill({
    status,
    headers: {
      'access-control-allow-origin': route.request().headers()['origin'] ?? 'http://127.0.0.1:4200',
      'access-control-allow-credentials': 'true',
      'content-type': 'application/json',
    },
    body: JSON.stringify(body),
  });

test('teacher grades a TEXT answer; after a reload it is gone from the queue', async ({ page }) => {
  await installApiStubs(page, 'teacher');
  await seedSession(page, 'teacher', 'light');

  // A tiny server: the answer leaves the queue only when PATCH /answers/31/grade succeeds.
  let graded: number | null = null;
  const patches: unknown[] = [];
  await page.route('**/api/teacher/assessments/pending', (route) =>
    route.request().method() === 'OPTIONS' ? route.fallback() : json(route, graded === null ? PENDING_ANSWERS : []));
  await page.route('**/api/teacher/assessments', (route) =>
    route.request().method() === 'OPTIONS'
      ? route.fallback()
      : json(route, TEACHER_TESTS.map((t) => (t.testId === 1 && graded !== null ? { ...t, pendingCount: 0 } : t))));
  await page.route('**/api/answers/31/grade', async (route) => {
    const req = route.request();
    if (req.method() === 'OPTIONS') return route.fallback();
    const body = req.postDataJSON() as { marksAwarded: number };
    patches.push(body);
    if (body.marksAwarded > 10) return json(route, { status: 400, code: 'BAD_REQUEST', detail: 'marksAwarded must be between 0 and 10' }, 400);
    graded = body.marksAwarded;
    return json(route, { studentAnswerId: 31, answerText: PENDING_ANSWERS[0].answerText, marksAwarded: graded });
  });

  await page.goto('/teacher/assessments', { waitUntil: 'domcontentloaded' });
  const card = page.locator('.grading-card');
  await expect(card).toContainText('Aarav Sharma');
  await expect(card).toContainText('out of 10');
  await expect(page.locator('body')).not.toContainText('correctAnswer');

  await card.getByLabel('Score').fill('8');
  await card.getByRole('button', { name: 'Save grade' }).click();
  await expect.poll(() => patches).toEqual([{ marksAwarded: 8 }]);
  await expect(page.getByText('Nothing is waiting for a grade')).toBeVisible();

  await page.reload({ waitUntil: 'domcontentloaded' });
  await expect(page.getByText('Nothing is waiting for a grade')).toBeVisible();
  await expect(page.locator('.grading-card')).toHaveCount(0);
  await expect(page.locator('app-data-table')).toContainText('Supervised learning check');
});

test('the teacher sidebar opens Assessments', async ({ page }) => {
  await installApiStubs(page, 'teacher');
  await seedSession(page, 'teacher', 'light');
  await page.goto('/teacher/dashboard', { waitUntil: 'domcontentloaded' });
  await page.locator('app-teacher-sidebar').getByRole('link', { name: 'Assessments' }).click();
  await expect(page).toHaveURL(/\/teacher\/assessments$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Assessments' })).toBeVisible();
});
