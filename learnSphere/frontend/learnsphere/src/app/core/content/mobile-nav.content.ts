/**
 * Mobile navigation (approved 360 px redesign, 2026-11-01).
 * The bottom bar carries each role's four most-used destinations; the More sheet carries
 * every other destination, grouped by what the person is doing. Every desktop sidebar
 * destination stays reachable (mobile-nav.component.spec.ts checks it).
 */
export type MobileRole = 'STUDENT' | 'TEACHER' | 'ADMIN';

export interface MobileNavItem {
  label: string;
  route: string;
  /** lucide icon name (registered in app.config.ts) */
  icon: string;
}

export interface MobileNavGroup {
  title: string;
  /** "tiles" for a 4-across grid, "rows" for a list */
  layout: 'tiles' | 'rows';
  items: MobileNavItem[];
}

export interface MobileNavConfig {
  bar: MobileNavItem[];
  profileRoute: string;
  roleLabel: string;
  groups: MobileNavGroup[];
}

export const MOBILE_NAV_COPY = {
  more: 'More',
  close: 'Close',
  profile: 'Profile',
  darkTheme: 'Dark theme',
  signOut: 'Sign out',
  barLabel: 'Main'
} as const;

export const MOBILE_NAV: Record<MobileRole, MobileNavConfig> = {
  STUDENT: {
    roleLabel: 'Student',
    profileRoute: '/student/profile',
    bar: [
      { label: 'Home', route: '/student/dashboard', icon: 'house' },
      { label: 'My Courses', route: '/student/courses', icon: 'book-open' },
      { label: 'Live', route: '/student/live', icon: 'radio' },
      { label: 'Tests', route: '/student/assessments', icon: 'clipboard-list' }
    ],
    groups: [
      { title: 'Study', layout: 'tiles', items: [
        { label: 'Explore', route: '/student/explore', icon: 'compass' },
        { label: 'Notes', route: '/student/notes', icon: 'file-text' },
        { label: 'Bookmarks', route: '/student/bookmarks', icon: 'bookmark' },
        { label: 'Certificates', route: '/student/certificates', icon: 'award' }
      ] },
      { title: 'Class', layout: 'tiles', items: [
        { label: 'Discussion', route: '/student/discussion', icon: 'message-circle' },
        { label: 'Messages', route: '/student/messages', icon: 'mail' },
        { label: 'Notices', route: '/student/announcements', icon: 'megaphone' },
        { label: 'Leaderboard', route: '/student/leaderboard', icon: 'trophy' }
      ] },
      { title: 'Account', layout: 'rows', items: [
        { label: 'My purchases', route: '/student/purchases', icon: 'credit-card' },
        { label: 'Settings', route: '/student/settings', icon: 'settings' }
      ] }
    ]
  },
  TEACHER: {
    roleLabel: 'Teacher',
    profileRoute: '/teacher/profile',
    bar: [
      { label: 'Home', route: '/teacher/dashboard', icon: 'house' },
      { label: 'Courses', route: '/teacher/courses', icon: 'book-open' },
      { label: 'Live', route: '/teacher/live', icon: 'radio' },
      { label: 'Messages', route: '/teacher/messages', icon: 'mail' }
    ],
    groups: [
      { title: 'Teach', layout: 'tiles', items: [
        { label: 'Upload', route: '/teacher/upload', icon: 'upload' },
        { label: 'Students', route: '/teacher/students', icon: 'users' },
        { label: 'Discussion', route: '/teacher/discussion', icon: 'message-circle' },
        { label: 'Trending', route: '/teacher/trending', icon: 'trending-up' }
      ] },
      { title: 'Account', layout: 'rows', items: [
        { label: 'Royalties', route: '/teacher/royalties', icon: 'banknote' },
        { label: 'Notices', route: '/teacher/announcements', icon: 'megaphone' },
        { label: 'Settings', route: '/teacher/settings', icon: 'settings' }
      ] }
    ]
  },
  ADMIN: {
    roleLabel: 'Administrator',
    profileRoute: '/admin/profile',
    bar: [
      { label: 'Home', route: '/admin/dashboard', icon: 'house' },
      { label: 'Users', route: '/admin/users', icon: 'users' },
      { label: 'Approvals', route: '/admin/courses', icon: 'clipboard-list' },
      { label: 'Reports', route: '/admin/reports', icon: 'bar-chart-2' }
    ],
    groups: [
      { title: 'Platform', layout: 'tiles', items: [
        { label: 'Colleges', route: '/admin/colleges', icon: 'building-2' },
        { label: 'Moderation', route: '/admin/flagged', icon: 'flag' },
        { label: 'Sentiment', route: '/admin/sentiment', icon: 'activity' },
        { label: 'Notices', route: '/admin/announcements', icon: 'megaphone' }
      ] },
      { title: 'Money', layout: 'tiles', items: [
        { label: 'Revenue', route: '/admin/revenue', icon: 'trending-up' },
        { label: 'Payouts', route: '/admin/payouts', icon: 'banknote' }
      ] },
      { title: 'Account', layout: 'rows', items: [
        { label: 'Messages', route: '/admin/messages', icon: 'mail' },
        { label: 'Settings', route: '/admin/settings', icon: 'settings' }
      ] }
    ]
  }
};

/** Page title for the mobile header: the label of the longest matching destination. */
export function mobilePageTitle(role: MobileRole, url: string): string {
  const path = url.split(/[?#]/)[0];
  const config = MOBILE_NAV[role];
  const all = [...config.bar, ...config.groups.flatMap((g) => g.items), { label: MOBILE_NAV_COPY.profile, route: config.profileRoute, icon: '' }];
  const match = all
    .filter((i) => path === i.route || path.startsWith(i.route + '/'))
    .sort((a, b) => b.route.length - a.route.length)[0];
  return match ? match.label : 'LearnSphere';
}
