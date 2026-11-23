/**
 * Student profile copy (SCR-26; W7-12, AUD-10, `PLAN §7` "Copy"). Moved verbatim from the
 * template, plus the new validation and failure messages (audit.md D1). One file per screen
 * (see W7-07).
 */
export const STUDENT_PROFILE_COPY = {
  eyebrow: 'Student workspace',
  title: 'Profile',
  intro: 'Manage how your identity and activity appear across LearnSphere.',
  savedToast: 'Profile saved ✓',
  edit: 'Edit profile',
  cancel: 'Cancel',
  save: 'Save profile',
  saving: 'Saving…',
  dismiss: 'Dismiss',
  fallbackName: 'Student',
  fallbackDetail: 'LearnSphere Student',
  verifiedBadge: '✓ College verified',
  accountSettings: 'Account settings',
  notificationCentre: 'Notification centre',
  stats: {
    points: 'Leaderboard points',
    attendance: 'Attendance score',
    verified: 'College verified',
    status: 'Account status',
  },
  info: {
    title: 'Personal information',
    intro: 'Your core account and public details.',
    name: 'Display name',
    namePlaceholder: 'Your name',
    email: 'Email address',
    emailPlaceholder: 'Email',
    detail: 'Department · College',
    detailPlaceholder: 'Department info',
    about: 'About',
    aboutPlaceholder: 'Write a short bio…',
  },
  security: {
    title: 'Account security',
    intro: 'Keep your account safe and up to date.',
    manage: 'Manage security',
  },
  saved: 'Profile saved.',
  saveFailed: 'Could not save your profile. Your changes are still here — try again.',
  nameRequired: 'Enter your name before saving.',
  loadFailed: 'Could not load your latest profile. Showing the details saved on this device.',
} as const;
