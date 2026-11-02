/**
 * Landing top bar (approved 360 px redesign, 2026-11-02).
 * Phones (< 600 px) show the logo, one short "Sign up" button and the menu; "Log in" and
 * the page links live in the menu drawer. Tablet and desktop keep "Log in" and
 * "Sign up free" in the bar.
 */
export interface LandingNavLink {
  label: string;
  /** In-page anchor (e.g. "#courses") or app route (e.g. "/register"). */
  href: string;
  route?: boolean;
}

export const LANDING_HEADER_COPY = {
  links: [
    { label: 'Explore courses', href: '#courses' },
    { label: 'Departments', href: '#departments' },
    { label: 'For colleges', href: '#colleges' },
    { label: 'Teach with us', href: '/register', route: true },
  ] as LandingNavLink[],
  logIn: 'Log in',
  signUpFree: 'Sign up free',
  signUpShort: 'Sign up',
  openMenu: 'Open menu',
  closeMenu: 'Close menu',
  menuLabel: 'Site menu',
} as const;
