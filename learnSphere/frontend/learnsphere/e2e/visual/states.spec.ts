/**
 * State baselines (W7-13; NFR-01 "Playwright screenshot baseline at 360/768/1280 px for every
 * changed screen"). The 46 screen contracts in `visual.spec.ts` capture each route in its
 * default state. This file adds the states those captures cannot reach:
 *   - the in-player quiz (W4-11) opened over the lecture player;
 *   - the live room scheduled / ended / live-but-connecting states (W5-10);
 *   - the student, teacher and admin settings screens rebuilt in W7-12 (no route contract).
 *
 * Same determinism as `visual.spec.ts`: stubbed API, fixed clock, seeded session and theme,
 * webfonts loaded, network idle, animations disabled.
 */
import { test, expect, Page, Route } from '@playwright/test';
import { THEMES, VIEWPORTS } from './screen-contracts';
import { LIVE_ROOM_STATES, installApiStubs, seedSession, stubLiveSession, VISUAL_USERS } from './api-stubs';

type Role = keyof typeof VISUAL_USERS;

interface StateContract {
  id: string;
  title: string;
  role: Role;
  route: string;
  /** Extra stubs layered over `installApiStubs` (later routes win). */
  stub?: (page: Page) => Promise<void>;
  /** Interaction that reaches the state after the route has rendered. */
  act?: (page: Page) => Promise<void>;
  /** Element that must be visible before the capture. */
  ready: string;
}

const json = (route: Route, body: unknown) =>
  route.fulfill({
    headers: {
      'access-control-allow-origin': route.request().headers()['origin'] ?? 'http://127.0.0.1:4200',
      'access-control-allow-credentials': 'true',
      'content-type': 'application/json',
    },
    body: JSON.stringify(body),
  });

const liveSession = (overrides: Record<string, unknown>) => (page: Page) => stubLiveSession(page, overrides);

export const STATE_CONTRACTS: readonly StateContract[] = [
  {
    id: 'STATE-player-quiz',
    title: 'In-player quiz open over the lecture player (W4-11)',
    role: 'student',
    route: '/student/courses/1/lectures/1',
    stub: async (page) => {
      // No content-bound quiz: the overlay falls back to the lecture's quiz
      // (`/assessments/lecture/{id}` → `/assessments/{id}` → `/assessments/{id}/start`).
      await page.route(/\/api\/assessments\/content\/\d+$/, (route) =>
        route.request().method() === 'OPTIONS'
          ? route.fallback()
          : route.fulfill({
              status: 404,
              headers: {
                'access-control-allow-origin': route.request().headers()['origin'] ?? 'http://127.0.0.1:4200',
                'access-control-allow-credentials': 'true',
                'content-type': 'application/problem+json',
              },
              body: JSON.stringify({ status: 404, code: 'NOT_FOUND' }),
            }));
    },
    act: async (page) => {
      await page.getByRole('button', { name: /quiz/i }).filter({ visible: true }).first().click();
    },
    ready: '.post-quiz-overlay .quiz-modal-card',
  },
  {
    id: 'STATE-live-scheduled',
    title: 'Live room — scheduled (W5-10)',
    role: 'student',
    route: '/student/live/1',
    stub: liveSession(LIVE_ROOM_STATES.scheduled),
    ready: '.session-state-card.waiting-card',
  },
  {
    id: 'STATE-live-ended',
    title: 'Live room — ended (W5-10)',
    role: 'student',
    route: '/student/live/1',
    stub: liveSession(LIVE_ROOM_STATES.ended),
    ready: '.session-state-card.ended-card',
  },
  {
    id: 'STATE-live-connecting',
    title: 'Live room — live, stream not ready yet (W5-10)',
    role: 'student',
    route: '/student/live/1',
    stub: liveSession(LIVE_ROOM_STATES.connecting),
    ready: '.session-state-card.connecting-card',
  },
  { id: 'STATE-settings-student', title: 'Student settings (W7-12)', role: 'student', route: '/student/settings', ready: '.settings-card' },
  { id: 'STATE-settings-teacher', title: 'Teacher settings (W7-12)', role: 'teacher', route: '/teacher/settings', ready: '.settings-card' },
  { id: 'STATE-settings-admin', title: 'Admin settings (W7-12)', role: 'admin', route: '/admin/settings', ready: '.settings-card' },
];

test.describe.configure({ mode: 'parallel' });

for (const viewport of VIEWPORTS) {
  test.describe(`${viewport.width}x${viewport.height}`, () => {
    test.use({ viewport });

    for (const theme of THEMES) {
      for (const state of STATE_CONTRACTS) {
        test(`${state.id} ${state.title} — ${theme}`, async ({ page }) => {
          const misses = await installApiStubs(page, state.role);
          if (state.stub) await state.stub(page);
          await seedSession(page, state.role, theme);
          await page.clock.setFixedTime(new Date('2026-10-25T09:00:00Z'));
          await page.emulateMedia({ colorScheme: theme });

          await page.goto(state.route, { waitUntil: 'domcontentloaded' });
          await page.waitForLoadState('networkidle');
          if (state.act) await state.act(page);
          await page.locator(state.ready).first().waitFor({ state: 'visible', timeout: 15_000 });

          await page.evaluate(async () => {
            await Promise.all(
              ['400 16px "Inter"', '500 16px "Inter"', '600 16px "Inter"', '700 16px "Inter"',
                '500 16px "Plus Jakarta Sans"', '600 16px "Plus Jakarta Sans"', '700 16px "Plus Jakarta Sans"',
                '800 16px "Plus Jakarta Sans"'].map((f) => document.fonts.load(f)),
            );
            await document.fonts.ready;
          });
          await page.waitForLoadState('networkidle');
          await page.waitForTimeout(400);

          await expect(page).toHaveScreenshot(`${state.id}-${viewport.width}-${theme}.png`, {
            fullPage: true,
            animations: 'disabled',
            caret: 'hide',
            maxDiffPixelRatio: 0.002,
          });

          expect([...misses], `${state.id} requested endpoints the stub table does not model`).toEqual([]);
        });
      }
    }
  });
}
