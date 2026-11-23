import type { AccountSettingsCopy } from '@shared/components/account-settings/account-settings.model';

/**
 * Teacher settings copy (SCR-41; W7-12, AUD-13, BR-24). Rows marked `unavailable` are
 * preferences the server cannot store yet: they stay visible, disabled and labelled. The
 * invented "SBI ••••4821 · Verified" payout account is gone — no bank details are stored.
 * One file per screen (see W7-07).
 */
export const TEACHER_SETTINGS_COPY: AccountSettingsCopy = {
  eyebrow: 'Teacher workspace',
  title: 'Settings',
  intro: 'Control your account, communication, and workspace preferences.',
  navLabel: 'Settings sections',
  fallbackName: 'Teacher',
  unavailable: 'Not available yet',
  tabs: [
    {
      name: 'General',
      title: 'Course defaults',
      description: 'Apply defaults to newly created courses.',
      rows: [
        { kind: 'theme', label: 'Dark mode', hint: 'Use the dark theme on this device' },
        { kind: 'unavailable', label: 'AI question extraction', hint: 'Generate draft questions from uploads', toggle: true },
        { kind: 'unavailable', label: 'Notify students', hint: 'On every new lecture', toggle: true },
        { kind: 'unavailable', label: 'Lecture discussions', hint: 'Allow questions below lectures', toggle: true },
      ],
    },
    {
      name: 'Notifications',
      title: 'Teaching notifications',
      description: 'Choose the teaching activity you hear about.',
      rows: [
        { kind: 'unavailable', label: 'New enrolments', hint: 'When somebody joins a course', toggle: true },
        { kind: 'unavailable', label: 'Student messages', hint: 'In-app and email alerts', toggle: true },
        { kind: 'unavailable', label: 'Monthly payout', hint: 'Payment confirmation by email', toggle: true },
      ],
    },
    {
      name: 'Privacy & security',
      title: 'Privacy & security',
      description: 'Manage your public identity and sign-in.',
      rows: [
        { kind: 'unavailable', label: 'Public teacher profile', hint: 'Show bio, courses, and ratings', toggle: true },
        { kind: 'unavailable', label: 'Direct student messages', hint: 'Allow students to start conversations', toggle: true },
        { kind: 'unavailable', label: 'Two-factor authentication', hint: 'Secure payouts and account access', toggle: true },
      ],
    },
    {
      name: 'Account',
      title: 'Account & payouts',
      description: 'Manage account details and payment preferences.',
      rows: [
        { kind: 'unavailable', label: 'Payout account', hint: 'Bank details for royalty payouts', action: 'Update' },
        { kind: 'unavailable', label: 'Download my data', hint: 'Export courses and account activity', action: 'Request' },
        { kind: 'password', label: 'Password', hint: 'We email you a secure link to set a new password.', action: 'Change' },
      ],
    },
  ],
  signOut: { title: 'Account access', body: 'Securely end this session on the current device.', button: 'Sign out' },
  resetSent: (email: string) => `Password reset link sent to ${email}.`,
  resetFailed: 'Could not send a password reset link. Try again in a moment.',
  facts: {
    verified: 'Verified',
    pending: 'Pending',
    notVerified: 'Not verified',
    roles: { STUDENT: 'Student', TEACHER: 'Teacher', ADMIN: 'Administrator' },
  },
};
