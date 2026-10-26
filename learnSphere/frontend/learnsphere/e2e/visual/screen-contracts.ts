/**
 * The 47 screen contracts from `Docs/source/spec.md` §4 (DS-01 / W1-12).
 *
 * One row per contract ID. Grouped headings in the spec (`SCR-14 / SCR-15`,
 * `SCR-26 / SCR-41 / SCR-61`, …) expand to one row per ID because each ID is a
 * distinct route with its own layout and therefore its own baseline.
 *
 * `route: null` means the contract has no standalone route in `app.routes.ts`.
 * Such a row is skipped explicitly and recorded in the tracker; it is never
 * silently dropped.
 */
export type ScreenRole = 'public' | 'student' | 'teacher' | 'admin';

export interface ScreenContract {
  /** Stable spec ID. */
  id: string;
  /** Short slug used in the snapshot filename. */
  slug: string;
  /** Spec heading, for readable test titles. */
  title: string;
  /** Route to capture, or `null` when the screen is not routed yet. */
  route: string | null;
  /** Session the route needs. */
  role: ScreenRole;
  /** Why the contract cannot be captured (only for `route: null`). */
  skipReason?: string;
}

export const SCREEN_CONTRACTS: readonly ScreenContract[] = [
  { id: 'SCR-01', slug: 'landing', title: 'Landing & public catalogue', route: '/', role: 'public' },
  { id: 'SCR-02', slug: 'login', title: 'Login', route: '/login', role: 'public' },
  { id: 'SCR-03', slug: 'register', title: 'Register', route: '/register', role: 'public' },
  { id: 'SCR-04', slug: 'forgot-password', title: 'Forgot / reset password', route: '/forgot-password', role: 'public' },
  { id: 'SCR-05', slug: 'verify-college', title: 'College verification', route: '/verify-college', role: 'student' },
  { id: 'SCR-06', slug: 'verify-certificate', title: 'Certificate verification (public)', route: '/verify-certificate', role: 'public' },
  { id: 'SCR-07', slug: 'checkout', title: 'Checkout', route: '/payment/checkout/1', role: 'student' },
  { id: 'SCR-08', slug: 'payment-success', title: 'Payment success', route: '/payment/success', role: 'student' },
  { id: 'SCR-10', slug: 'student-dashboard', title: 'Student dashboard', route: '/student/dashboard', role: 'student' },
  { id: 'SCR-11', slug: 'browse-courses', title: 'Browse courses', route: '/student/courses', role: 'student' },
  { id: 'SCR-12', slug: 'course-detail', title: 'Course detail', route: '/student/courses/1', role: 'student' },
  { id: 'SCR-13', slug: 'lecture-player', title: 'Lecture player', route: '/student/courses/1/lectures/1', role: 'student' },
  { id: 'SCR-14', slug: 'notes', title: 'Notes', route: '/student/notes', role: 'student' },
  { id: 'SCR-15', slug: 'bookmarks', title: 'Bookmarks', route: '/student/bookmarks', role: 'student' },
  { id: 'SCR-16', slug: 'assessments', title: 'Assessments', route: '/student/assessments', role: 'student' },
  { id: 'SCR-17', slug: 'quiz-attempt', title: 'Quiz attempt', route: '/student/assessments/1', role: 'student' },
  { id: 'SCR-18', slug: 'leaderboard', title: 'Leaderboard', route: '/student/leaderboard', role: 'student' },
  { id: 'SCR-19', slug: 'discussion', title: 'Discussion', route: '/student/discussion', role: 'student' },
  { id: 'SCR-20', slug: 'live-sessions', title: 'Live sessions', route: '/student/live', role: 'student' },
  { id: 'SCR-21', slug: 'certificates', title: 'Certificates', route: '/student/certificates', role: 'student' },
  { id: 'SCR-22', slug: 'purchases', title: 'Purchases', route: '/student/purchases', role: 'student' },
  { id: 'SCR-23', slug: 'messages', title: 'Messages', route: '/student/messages', role: 'student' },
  { id: 'SCR-24', slug: 'notifications', title: 'Notifications', route: '/student/notifications', role: 'student' },
  { id: 'SCR-25', slug: 'announcements', title: 'Announcements', route: '/student/announcements', role: 'student' },
  { id: 'SCR-26', slug: 'student-profile', title: 'Profile & settings (student)', route: '/student/profile', role: 'student' },
  { id: 'SCR-30', slug: 'teacher-dashboard', title: 'Teacher dashboard', route: '/teacher/dashboard', role: 'teacher' },
  { id: 'SCR-31', slug: 'teacher-courses', title: 'My courses', route: '/teacher/courses', role: 'teacher' },
  { id: 'SCR-32', slug: 'course-editor', title: 'Course editor', route: '/teacher/courses/1/manage', role: 'teacher' },
  { id: 'SCR-33', slug: 'upload', title: 'Upload', route: '/teacher/upload', role: 'teacher' },
  { id: 'SCR-34', slug: 'students-standings', title: 'Students & standings', route: '/teacher/students', role: 'teacher' },
  {
    id: 'SCR-35',
    slug: 'teacher-assessments',
    title: 'Teacher assessments',
    route: null,
    role: 'teacher',
    skipReason:
      'No route exists: assessment authoring is reached from the course editor, and app.routes.ts exposes no teacher assessments screen. DS-01 captures it once the route is defined.',
  },
  { id: 'SCR-36', slug: 'teacher-live', title: 'Live sessions (teacher)', route: '/teacher/live', role: 'teacher' },
  { id: 'SCR-37', slug: 'trending', title: 'Trending courses', route: '/teacher/trending', role: 'teacher' },
  { id: 'SCR-38', slug: 'royalties', title: 'Royalties', route: '/teacher/royalties', role: 'teacher' },
  { id: 'SCR-41', slug: 'teacher-profile', title: 'Profile & settings (teacher)', route: '/teacher/profile', role: 'teacher' },
  { id: 'SCR-50', slug: 'admin-dashboard', title: 'Admin dashboard', route: '/admin/dashboard', role: 'admin' },
  { id: 'SCR-51', slug: 'user-governance', title: 'User governance', route: '/admin/users', role: 'admin' },
  { id: 'SCR-52', slug: 'course-approvals', title: 'Course approvals', route: '/admin/courses', role: 'admin' },
  { id: 'SCR-53', slug: 'revenue', title: 'Revenue', route: '/admin/revenue', role: 'admin' },
  { id: 'SCR-54', slug: 'reports', title: 'Reports', route: '/admin/reports', role: 'admin' },
  { id: 'SCR-55', slug: 'payouts', title: 'Payouts', route: '/admin/payouts', role: 'admin' },
  { id: 'SCR-56', slug: 'moderation', title: 'Moderation', route: '/admin/flagged', role: 'admin' },
  { id: 'SCR-57', slug: 'sentiment', title: 'Sentiment', route: '/admin/sentiment', role: 'admin' },
  { id: 'SCR-58', slug: 'colleges', title: 'Colleges & affiliations', route: '/admin/colleges', role: 'admin' },
  { id: 'SCR-59', slug: 'admin-announcements', title: 'Announcements (admin)', route: '/admin/announcements', role: 'admin' },
  { id: 'SCR-60', slug: 'admin-messages', title: 'Messages (admin)', route: '/admin/messages', role: 'admin' },
  { id: 'SCR-61', slug: 'admin-profile', title: 'Profile & settings (admin)', route: '/admin/profile', role: 'admin' },
] as const;

/** Contracts that are captured (everything with a route). */
export const CAPTURED_CONTRACTS = SCREEN_CONTRACTS.filter((c) => c.route !== null);

/** Contracts that are deliberately skipped, with their reason. */
export const SKIPPED_CONTRACTS = SCREEN_CONTRACTS.filter((c) => c.route === null);

export const VIEWPORTS = [
  { width: 360, height: 800 },
  { width: 768, height: 900 },
  { width: 1280, height: 900 },
] as const;

export const THEMES = ['light', 'dark'] as const;
