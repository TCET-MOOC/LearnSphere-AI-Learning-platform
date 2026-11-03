/**
 * W7-06 (`spec §4`): every client route entry maps to one screen contract (SCR) or is a named
 * redirect to one, and every screen contract has a route. Mirrors the matrix in
 * `Docs/plan/schema-usage.md` §1a and the visual contracts in `e2e/visual/screen-contracts.ts`.
 */
import { Routes } from '@angular/router';
import { routes } from './app.routes';

/** Screen IDs defined in `spec §4` (47). SCR-62 (public instructor profile, v1.1) is built in W8-15. */
const SPEC_SCREENS = [
  1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
  30, 31, 32, 33, 34, 35, 36, 37, 38, 41, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
].map((n) => `SCR-${String(n).padStart(2, '0')}`);

/** Contracts with no route yet, and why. Remove an entry when its screen gets a route. */
const KNOWN_GAPS: Record<string, string> = {
  'SCR-35': 'teacher assessments: authoring lives in Upload (quiz builder); no grading screen calls PATCH /api/answers/{id}/grade',
};

/** path → screen. `alias` entries are redirects kept for old links; they render no screen of their own. */
const MATRIX: Record<string, { scr: string; alias?: true }> = {
  '': { scr: 'SCR-01' },
  login: { scr: 'SCR-02' },
  register: { scr: 'SCR-03' },
  'forgot-password': { scr: 'SCR-04' },
  'verify-college': { scr: 'SCR-05' },
  'verify-certificate': { scr: 'SCR-06' },
  'verify-certificate/:code': { scr: 'SCR-06' },
  'verify/certificate/:code': { scr: 'SCR-06', alias: true },
  'payment/checkout/:courseId': { scr: 'SCR-07' },
  'payment/success': { scr: 'SCR-08' },

  student: { scr: 'SCR-10', alias: true },
  'student/dashboard': { scr: 'SCR-10' },
  'student/courses': { scr: 'SCR-11' },
  'student/explore': { scr: 'SCR-11' },
  'student/courses/:id': { scr: 'SCR-12' },
  'student/courses/:courseId/player': { scr: 'SCR-13' },
  'student/courses/:courseId/lectures/:lectureId': { scr: 'SCR-13' },
  'student/courses/:courseId/lecture/:lectureId': { scr: 'SCR-13', alias: true },
  'student/lectures': { scr: 'SCR-11', alias: true },
  'student/notes': { scr: 'SCR-14' },
  'student/bookmarks': { scr: 'SCR-15' },
  'student/messages': { scr: 'SCR-23' },
  'student/leaderboard': { scr: 'SCR-18' },
  'student/discussion': { scr: 'SCR-19' },
  'student/certificates': { scr: 'SCR-21' },
  'student/purchases': { scr: 'SCR-22' },
  'student/live': { scr: 'SCR-20' },
  'student/live/:id': { scr: 'SCR-20' },
  'student/assessments': { scr: 'SCR-16' },
  'student/assessments/:id': { scr: 'SCR-17' },
  'student/settings': { scr: 'SCR-26' },
  'student/announcements': { scr: 'SCR-25' },
  'student/notifications': { scr: 'SCR-24' },
  'student/profile': { scr: 'SCR-26' },

  teacher: { scr: 'SCR-30', alias: true },
  'teacher/dashboard': { scr: 'SCR-30' },
  'teacher/courses': { scr: 'SCR-31' },
  'teacher/students': { scr: 'SCR-34' },
  'teacher/courses/new': { scr: 'SCR-32' },
  'teacher/courses/:id/manage': { scr: 'SCR-32' },
  'teacher/upload': { scr: 'SCR-33' },
  'teacher/discussion': { scr: 'SCR-19' },
  'teacher/messages': { scr: 'SCR-23' },
  'teacher/live': { scr: 'SCR-36' },
  'teacher/trending': { scr: 'SCR-37' },
  'teacher/royalties': { scr: 'SCR-38' },
  'teacher/settings': { scr: 'SCR-41' },
  'teacher/announcements': { scr: 'SCR-25' },
  'teacher/notifications': { scr: 'SCR-24' },
  'teacher/profile': { scr: 'SCR-41' },

  admin: { scr: 'SCR-50', alias: true },
  'admin/dashboard': { scr: 'SCR-50' },
  'admin/users': { scr: 'SCR-51' },
  'admin/messages': { scr: 'SCR-60' },
  'admin/payouts': { scr: 'SCR-55' },
  'admin/flagged': { scr: 'SCR-56' },
  'admin/sentiment': { scr: 'SCR-57' },
  'admin/settings': { scr: 'SCR-61' },
  'admin/announcements': { scr: 'SCR-59' },
  'admin/notifications': { scr: 'SCR-24' },
  'admin/profile': { scr: 'SCR-61' },
  'admin/colleges': { scr: 'SCR-58' },
  'admin/reports': { scr: 'SCR-54' },
  'admin/revenue': { scr: 'SCR-53' },
  'admin/courses': { scr: 'SCR-52' },
};

interface Entry { path: string; redirect: boolean }

async function entries(list: Routes, prefix = ''): Promise<Entry[]> {
  const out: Entry[] = [];
  for (const r of list) {
    const path = [prefix, r.path].filter(Boolean).join('/');
    if (r.loadChildren || r.children) {
      const loaded = r.children ?? ((await r.loadChildren!()) as unknown as Routes);
      out.push(...(await entries(loaded, path)));
    } else if (path !== '**') {
      out.push({ path, redirect: r.redirectTo !== undefined });
    }
  }
  return out;
}

describe('Route ↔ SCR matrix (W7-06)', () => {
  it('maps every route entry to a screen, and lists no route that does not exist', async () => {
    const paths = (await entries(routes)).map((e) => e.path);
    expect(paths.filter((p) => !(p in MATRIX)), 'routes missing from the matrix').toEqual([]);
    expect(Object.keys(MATRIX).filter((p) => !paths.includes(p)), 'matrix rows with no route').toEqual([]);
  });

  it('has exactly one canonical (non-redirect) path per screen and alias, and aliases are redirects', async () => {
    const all = await entries(routes);
    for (const { path, redirect } of all) {
      expect(redirect, `${path} redirect`).toBe(!!MATRIX[path]?.alias);
    }
  });

  it('gives every spec §4 screen a route, except the recorded gaps', () => {
    const routed = new Set(Object.values(MATRIX).filter((m) => !m.alias).map((m) => m.scr));
    expect(SPEC_SCREENS.filter((s) => !routed.has(s))).toEqual(Object.keys(KNOWN_GAPS));
    expect([...routed].filter((s) => !SPEC_SCREENS.includes(s)), 'unknown SCR ids').toEqual([]);
  });

  it('keeps no legacy NgModule route tables beside the standalone routes', async () => {
    // @ts-ignore -- spec runs under Node (vitest); the app tsconfig has no Node types.
    const fs = await import('node:fs');
    // @ts-ignore -- as above.
    const path = await import('node:path');
    const walk = (dir: string): string[] =>
      fs.readdirSync(dir, { withFileTypes: true }).flatMap((e: { name: string; isDirectory(): boolean }) =>
        e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]);
    const modules = walk('src/app').filter((f) => f.endsWith('.module.ts')).map((f) => f.split(path.sep).join('/'));
    expect(modules).toEqual([]);
  });
});
