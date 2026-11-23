/**
 * Smoke journeys (W7-10; `audit.md` D-series, `[G-F8]`, `PLAN §10`): the paths a user takes end to
 * end, driven through the real UI on the stubbed API used by the visual baseline. Each test layers
 * its own stateful routes on top of `installApiStubs` (later routes win), so the server stays the
 * authority: the page only shows what the stubbed server said.
 *
 * Profile save and settings persistence are not here: those screens are rebuilt in W7-11/W7-12,
 * which write their journeys red-first (decided with the user, TRACKER §4).
 */
import { test, expect, Page, Route } from '@playwright/test';
import { COURSES, VISUAL_USERS, installApiStubs, seedSession } from '../visual/api-stubs';

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

async function boot(page: Page, role: 'student' | 'public', opts: { fixedClock?: boolean } = {}) {
  await installApiStubs(page, role);
  await seedSession(page, role, 'light');
  // The fixed clock also freezes timers, so a debounced input (catalogue search) needs the real one.
  if (opts.fixedClock !== false) await page.clock.setFixedTime(new Date('2026-10-25T09:00:00Z'));
}

test('session: sign in, use the app, sign out — the session is gone after sign-out', async ({ page }) => {
  await boot(page, 'public');
  let signedIn = false;
  const calls: string[] = [];
  await page.route('**/api/auth/**', async (route) => {
    if (route.request().method() === 'OPTIONS') return route.fallback();
    const path = new URL(route.request().url()).pathname;
    calls.push(path);
    if (path.endsWith('/auth/login')) {
      signedIn = true;
      return json(route, { token: 'smoke-token', user: VISUAL_USERS.student });
    }
    if (path.endsWith('/auth/refresh')) {
      return signedIn ? json(route, { token: 'smoke-token', user: VISUAL_USERS.student }) : json(route, { code: 'UNAUTHORIZED' }, 401);
    }
    if (path.endsWith('/auth/logout')) {
      signedIn = false; // the server revokes the refresh cookie
      return json(route, {});
    }
    return route.fallback();
  });

  await page.goto('/login');
  await page.locator('#email').fill('student@learnsphere.test');
  await page.locator('#password').fill('Smoke@2026'); // meets the form rule: 8–16, upper, digit, special
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/student\/dashboard$/);
  expect(calls).toContain('/api/auth/login');

  await page.locator('app-topbar .avatar').click();
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL(/\/login/);
  await expect.poll(() => calls.includes('/api/auth/logout')).toBe(true);

  // A protected page after sign-out: the refresh cookie is gone, so the guard sends the visitor to login.
  await page.goto('/student/purchases');
  await expect(page).toHaveURL(/\/login\?redirectTo=%2Fstudent%2Fpurchases/);
});

test('purchase: buy a paid course, the server verifies it, and the receipt carries the gateway payment', async ({ page }) => {
  await boot(page, 'student');
  let paid = false;
  const orderBodies: Record<string, unknown>[] = [];
  const verifyBodies: Record<string, unknown>[] = [];

  // Razorpay checkout: report a successful payment as soon as the gateway opens.
  await page.addInitScript(() => {
    (window as unknown as Record<string, unknown>)['Razorpay'] = class {
      constructor(private readonly options: { order_id: string; handler: (r: unknown) => void }) {}
      on(): void {}
      open(): void {
        this.options.handler({ razorpay_order_id: this.options.order_id, razorpay_payment_id: 'pay_SMOKE0001', razorpay_signature: 'sig' });
      }
    };
  });
  await page.route('**/api/payments/orders', async (route) => {
    if (route.request().method() !== 'POST') return route.fallback();
    orderBodies.push(route.request().postDataJSON());
    return json(route, { key_id: 'rzp_test_smoke', amount: 89900, currency: 'INR', razorpay_order_id: 'order_SMOKE0001' });
  });
  await page.route('**/api/payments/orders/verify', async (route) => {
    verifyBodies.push(route.request().postDataJSON());
    paid = true;
    return json(route, { status: 'PAID' });
  });
  await page.route('**/api/student/courses/2', (route) =>
    paid ? json(route, { ...COURSES[1], completed: false, progressPercent: 0 }) : json(route, {}, 404),
  );
  await page.route('**/api/payments/history', (route) =>
    json(route, paid ? [{
      id: 7, courseId: 2, courseTitle: 'Design Systems in Practice', department: 'Design', amount: 899, currency: 'INR',
      status: 'PAID', gatewayOrderId: 'order_SMOKE0001', gatewayPaymentId: 'pay_SMOKE0001',
      createdAt: '2026-10-25T08:58:00', paidAt: '2026-10-25T09:00:00',
    }] : []),
  );

  await page.goto('/student/courses/2', { waitUntil: 'domcontentloaded' });
  await page.locator('.enroll-btn').click();
  await expect(page.getByRole('link', { name: 'Continue Learning' })).toBeVisible();

  // The client asks for an order for this course; the price is the server's, never the client's.
  expect(orderBodies).toHaveLength(1);
  expect(orderBodies[0]).toMatchObject({ courseId: 2 });
  expect(Object.keys(orderBodies[0])).not.toEqual(expect.arrayContaining(['amount']));
  expect(Object.keys(orderBodies[0])).not.toEqual(expect.arrayContaining(['price']));
  expect(verifyBodies).toEqual([{ razorpay_order_id: 'order_SMOKE0001', razorpay_payment_id: 'pay_SMOKE0001', razorpay_signature: 'sig' }]);

  await page.goto('/student/purchases', { waitUntil: 'domcontentloaded' });
  await expect(page.getByText('pay_SMOKE0001')).toBeVisible();
  await page.getByRole('button', { name: /receipt/i }).first().click();
  const receipt = page.getByRole('dialog');
  await expect(receipt).toContainText('pay_SMOKE0001');
  await expect(receipt).toContainText('Design Systems in Practice');
});

test('catalogue: search the catalogue and open the course that matches', async ({ page }) => {
  await boot(page, 'student', { fixedClock: false });
  // The catalogue searches on the server (`GET /courses?search=`); model that filter here.
  const searches: string[] = [];
  await page.route(/\/api\/courses(\?.*)?$/, (route) => {
    const q = new URL(route.request().url()).searchParams.get('search');
    if (q) searches.push(q);
    return json(route, q ? COURSES.filter((c) => c.title.toLowerCase().includes(q.toLowerCase())) : COURSES);
  });
  await page.route('**/api/courses/3', (route) => json(route, COURSES[2]));

  await page.goto('/student/explore', { waitUntil: 'domcontentloaded' });
  await expect(page.locator('.explore-card')).not.toHaveCount(0);
  await page.getByRole('textbox', { name: 'Search course catalog' }).fill('Databases');
  await expect.poll(() => searches).toContain('Databases');
  await expect(page).toHaveURL(/\/student\/explore\?search=Databases$/);
  const cards = page.locator('.explore-card');
  await expect(cards).toHaveCount(1);
  await expect(cards.first()).toContainText('Databases for Engineers');

  await cards.first().click();
  await expect(page).toHaveURL(/\/student\/courses\/3$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Databases for Engineers' })).toBeVisible();
});

test('resume: Continue Learning opens the lecture the server says to resume, not the first one', async ({ page }) => {
  await boot(page, 'student');
  await page.route('**/api/student/courses/1/resume', (route) =>
    json(route, { courseId: 1, contentId: 2, lectureId: 2, moduleId: 1, title: 'Features, labels and leakage', contentType: 'VIDEO', position: 2, progressPercent: 40, secondsWatched: 300, completed: false }),
  );

  await page.goto('/student/courses/1', { waitUntil: 'domcontentloaded' });
  await page.getByRole('link', { name: 'Continue Learning' }).click();
  await expect(page).toHaveURL(/\/student\/courses\/1\/lectures\/2$/);
});
