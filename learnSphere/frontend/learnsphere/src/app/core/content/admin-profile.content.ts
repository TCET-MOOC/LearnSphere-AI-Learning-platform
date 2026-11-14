/**
 * Admin profile copy (SCR-61; W7-11, `PLAN §7` "Copy"). Labels for real figures only: every
 * stat comes from the server (audit.md D3, AUD-12). One file per screen (see W7-07).
 */
export const ADMIN_PROFILE_COPY = {
  stats: {
    users: 'Users',
    courses: 'Courses',
    colleges: 'Colleges',
    openFlags: 'Open flags',
  },
  role: 'Administrator · LearnSphere platform',
  saved: 'Profile saved.',
  saveFailed: 'Could not save your profile. Your changes are still here — try again.',
  resetSent: (email: string) => `Password reset link sent to ${email}.`,
  resetFailed: 'Could not send a password reset link. Try again in a moment.',
  securityHint: 'We email you a secure link to set a new password.',
  institutionHint: 'Administrator accounts are not tied to a college.',
} as const;
