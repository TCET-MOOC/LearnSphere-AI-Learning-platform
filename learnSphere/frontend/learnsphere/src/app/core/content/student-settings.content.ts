import type { AccountSettingsCopy } from '@shared/components/account-settings/account-settings.model';

/**
 * Student settings copy (SCR-26; W7-12, AUD-13, BR-24). Rows marked `unavailable` are
 * preferences the server cannot store yet: they stay visible, disabled and labelled.
 * One file per screen (see W7-07).
 */
export const STUDENT_SETTINGS_COPY: AccountSettingsCopy = {
  eyebrow: 'Student workspace',
  title: 'Settings',
  intro: 'Control your account, communication, and workspace preferences.',
  navLabel: 'Settings sections',
  fallbackName: 'Student',
  unavailable: 'Not available yet',
  tabs: [
    {
      name: 'General',
      title: 'Learning preferences',
      description: 'Choose defaults for your classroom experience.',
      rows: [
        { kind: 'theme', label: 'Dark mode', hint: 'Use the dark theme on this device' },
        { kind: 'unavailable', label: 'Video quality', hint: 'Default streaming quality', value: 'Auto', action: 'Change' },
        { kind: 'unavailable', label: 'Playback speed', hint: 'Default lecture speed', value: '1×', action: 'Change' },
        { kind: 'unavailable', label: 'Language', hint: 'Interface and captions', value: 'English', action: 'Change' },
      ],
    },
    {
      name: 'Notifications',
      title: 'Notification preferences',
      description: 'Choose which learning updates reach you.',
      rows: [
        { kind: 'unavailable', label: 'New lecture uploaded', hint: 'When an enrolled teacher uploads', toggle: true },
        { kind: 'unavailable', label: 'Assessment reminders', hint: '48 and 24 hours before a deadline', toggle: true },
        { kind: 'unavailable', label: 'Leaderboard changes', hint: 'When your rank moves', toggle: true },
      ],
    },
    {
      name: 'Privacy & security',
      title: 'Privacy & security',
      description: 'Control visibility and protect your account.',
      rows: [
        { kind: 'unavailable', label: 'Leaderboard visibility', hint: 'Visible to batchmates', toggle: true },
        { kind: 'unavailable', label: 'Profile discoverability', hint: 'Allow classmates to find your profile', toggle: true },
        { kind: 'unavailable', label: 'Two-factor authentication', hint: 'Add an extra sign-in step', toggle: true },
      ],
    },
    {
      name: 'Account',
      title: 'Account management',
      description: 'Manage data and account access.',
      rows: [
        { kind: 'fact', label: 'College email', hint: 'Verified academic identity', fact: 'collegeVerification' },
        { kind: 'unavailable', label: 'Download my data', hint: 'Receive a copy of your LearnSphere data', action: 'Request' },
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
