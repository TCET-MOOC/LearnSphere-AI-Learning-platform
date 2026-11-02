/**
 * W7-02: role route groups are lazy, and the URL table did not change.
 *
 * The characterisation table below was taken from the eager route file before the split
 * (2026-11-02, `0bf5243`): every URL keeps its component, guards and roles, in order.
 */
import { Route, Routes } from '@angular/router';
import { routes } from './app.routes';

type Row = [string, string] | [string, string, string, string];

const EXPECTED: Row[] = [
  ['', 'LandingComponent', '', ''],
  ['login', 'LoginComponent', '', ''],
  ['register', 'RegisterComponent', '', ''],
  ['forgot-password', 'ForgotPasswordComponent', '', ''],
  ['verify-college', 'VerifyCollegeComponent', 'authGuard', ''],
  ['verify-certificate', 'VerifyCertificateComponent', '', ''],
  ['verify-certificate/:code', 'VerifyCertificateComponent', '', ''],
  ['verify/certificate/:code', 'VerifyCertificateComponent', '', ''],
  ['payment/checkout/:courseId', 'CheckoutComponent', 'authGuard', ''],
  ['payment/success', 'PaymentSuccessComponent', 'authGuard', ''],
  ['student/dashboard', 'DashboardComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/courses', 'CoursesComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/explore', 'ExploreComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/courses/:id', 'CourseDetailComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/courses/:courseId/player', 'PlayerResolverComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/courses/:courseId/lectures/:lectureId', 'LectureComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/courses/:courseId/lecture/:lectureId', '->student/courses/:courseId/player'],
  ['student/lectures', '->student/explore'],
  ['student/notes', 'NotesComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/bookmarks', 'BookmarksComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/messages', 'MessagesComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/leaderboard', 'LeaderboardComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/discussion', 'DiscussionComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/certificates', 'CertificatesComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/purchases', 'PurchasesComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/live', 'StudentLiveComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/live/:id', 'StudentLiveRoomComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/assessments', 'AssessmentsComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/assessments/:id', 'QuizComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/settings', 'SettingsComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/announcements', 'AnnouncementsComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/notifications', 'NotificationsComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['student/profile', 'ProfileComponent', 'authGuard,roleGuard', 'STUDENT'],
  ['teacher/dashboard', 'DashboardComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/courses', 'CoursesComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/students', 'StudentsComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/courses/new', 'CourseManagementComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/courses/:id/manage', 'CourseManagementComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/upload', 'UploadComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/discussion', 'DiscussionComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/messages', 'MessagesComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/live', 'LiveComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/trending', 'TrendingComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/royalties', 'RoyaltiesComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/settings', 'SettingsComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/announcements', 'AnnouncementsComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/notifications', 'NotificationsComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['teacher/profile', 'ProfileComponent', 'authGuard,roleGuard', 'TEACHER'],
  ['admin/dashboard', 'DashboardComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/users', 'UsersComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/messages', 'MessagesComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/payouts', 'PayoutsComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/flagged', 'FlaggedComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/sentiment', 'SentimentComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/settings', 'SettingsComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/announcements', 'AnnouncementsComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/notifications', 'NotificationsComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/profile', 'ProfileComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/colleges', 'CollegesComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/reports', 'ReportsComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/revenue', 'RevenueComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['admin/courses', 'CoursesComponent', 'authGuard,roleGuard', 'ADMIN'],
  ['**', 'NotFoundComponent', '', ''],
];

const join = (prefix: string, path = '') => [prefix, path].filter(Boolean).join('/');
const fnNames = (fns: unknown[] | undefined) => (fns ?? []).map((f) => (f as { name: string }).name);

// The Angular build renames classes to `_Name`; compare the source name.
const className = (c: { name: string }) => c.name.replace(/^_/, '');

async function resolveComponent(route: Route): Promise<string> {
  if (route.component) return className(route.component);
  const loaded = (await route.loadComponent!()) as unknown as { name?: string; default?: { name: string } };
  return className((loaded.default ?? loaded) as { name: string });
}

async function flatten(list: Routes, prefix = '', inherited: string[] = []): Promise<Row[]> {
  const rows: Row[] = [];
  for (const route of list) {
    const path = join(prefix, route.path);
    const guards = [...inherited, ...fnNames(route.canActivate)];
    if (route.redirectTo !== undefined) {
      rows.push([path, `->${prefix && !String(route.redirectTo).startsWith('/') ? join(prefix, String(route.redirectTo)) : String(route.redirectTo).replace(/^\//, '')}`]);
      continue;
    }
    if (route.loadChildren || route.children) {
      const children = route.children ?? ((await route.loadChildren!()) as unknown as Routes | { default: Routes });
      const resolved = Array.isArray(children) ? children : children.default;
      rows.push(...(await flatten(resolved, path, guards)));
      continue;
    }
    const roles = (route.data?.['roles'] as string[] | undefined)?.join(',') ?? '';
    rows.push([path, await resolveComponent(route), guards.join(','), roles]);
  }
  return rows;
}

describe('app routes (W7-02)', () => {
  it('keeps every URL, component, guard and role from the eager table', async () => {
    expect(await flatten(routes)).toEqual(EXPECTED);
  });

  it('loads the student, teacher and admin areas as lazy route groups', () => {
    for (const area of ['student', 'teacher', 'admin']) {
      const group = routes.find((r) => r.path === area);
      expect(group, `${area} group`).toBeDefined();
      expect(group!.loadChildren, `${area} group is lazy`).toBeTypeOf('function');
      expect(group!.component).toBeUndefined();
    }
    const eagerAreaRoutes = routes.filter((r) => /^(student|teacher|admin)\//.test(r.path ?? ''));
    expect(eagerAreaRoutes.map((r) => r.path)).toEqual([]);
  });

  it('loads every public screen except the landing page on demand', () => {
    const eager = routes.filter((r) => r.component).map((r) => r.path);
    expect(eager).toEqual(['']);
  });

  it('never imports a role screen statically (that would pull it into the initial bundle)', async () => {
    // @ts-ignore -- spec runs under Node (vitest); the app tsconfig has no Node types.
    const { readFileSync } = await import('node:fs');
    const routesSource = readFileSync('src/app/app.routes.ts', 'utf8');
    expect(routesSource).not.toMatch(/^import [^;]*from '\.\/features\/(student|teacher|admin)\//m);
  });
});
