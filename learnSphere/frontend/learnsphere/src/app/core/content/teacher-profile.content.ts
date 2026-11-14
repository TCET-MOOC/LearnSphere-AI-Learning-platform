/**
 * Teacher profile copy (SCR-41; W7-11, `PLAN §7` "Copy"). Labels for real figures only: every
 * stat comes from the server (audit.md D2, AUD-11). One file per screen (see W7-07).
 */
export const TEACHER_PROFILE_COPY = {
  stats: {
    liveCourses: 'Live courses',
    students: 'Students',
    lectures: 'Lectures',
    royaltyBalance: 'Royalty balance',
  },
  fallbackRole: 'Teacher',
  saved: 'Profile saved.',
  saveFailed: 'Could not save your profile. Your changes are still here — try again.',
  resetSent: (email: string) => `Password reset link sent to ${email}.`,
  resetFailed: 'Could not send a password reset link. Try again in a moment.',
  securityHint: 'We email you a secure link to set a new password.',
  institutionHint: 'Set by your college affiliation; contact an administrator to change it.',
} as const;
