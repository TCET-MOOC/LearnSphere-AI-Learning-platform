import type { AccountSettingsCopy } from '@shared/components/account-settings/account-settings.model';

/**
 * Admin settings copy (SCR-61; W7-12, AUD-13, BR-24). Platform switches the server cannot
 * change from here stay visible, disabled and labelled. "Super Admin" was invented: the role
 * shown is the account's real role. Audit history and role changes live in User management
 * (Inspect). One file per screen (see W7-07).
 */
export const ADMIN_SETTINGS_COPY: AccountSettingsCopy = {
  eyebrow: 'Administration',
  title: 'Admin settings',
  intro: 'Control your account, communication, and workspace preferences.',
  navLabel: 'Settings sections',
  fallbackName: 'Admin',
  unavailable: 'Not available yet',
  tabs: [
    {
      name: 'General',
      title: 'Platform controls',
      description: 'Configure publishing and automated safety rules.',
      rows: [
        { kind: 'theme', label: 'Dark mode', hint: 'Use the dark theme on this device' },
        { kind: 'unavailable', label: 'AI abuse detection', hint: 'Auto-flag posts, comments, and messages', toggle: true },
        { kind: 'unavailable', label: 'Sentiment analysis', hint: 'Analyse comments and reviews', toggle: true },
        { kind: 'unavailable', label: 'Course approval required', hint: 'Review courses before publishing', toggle: true },
      ],
    },
    {
      name: 'Notifications',
      title: 'Admin alerts',
      description: 'Choose which operational events need attention.',
      rows: [
        { kind: 'unavailable', label: 'High-risk flag alerts', hint: 'Immediate trust and safety alert', toggle: true },
        { kind: 'unavailable', label: 'College applications', hint: 'When verification documents arrive', toggle: true },
        { kind: 'unavailable', label: 'Weekly platform digest', hint: 'Users, revenue, and moderation summary', toggle: true },
      ],
    },
    {
      name: 'Privacy & security',
      title: 'Privacy & security',
      description: 'Enforce administrative protection.',
      rows: [
        { kind: 'unavailable', label: 'Two-factor authentication', hint: 'Required for every administrator', toggle: true },
        { kind: 'unavailable', label: 'Session timeout', hint: 'Sign out inactive admins automatically', toggle: true },
        { kind: 'unavailable', label: 'Login audit alerts', hint: 'Alert on a new device or location', toggle: true },
      ],
    },
    {
      name: 'Account',
      title: 'Administration account',
      description: 'Manage access, exports, and administrator controls.',
      rows: [
        { kind: 'fact', label: 'Admin role', hint: 'Your role on this account', fact: 'role' },
        { kind: 'link', label: 'Audit log', hint: 'Each account’s history is under Inspect', action: 'View', route: '/admin/users' },
        { kind: 'link', label: 'Manage admins', hint: 'Assign or revoke roles under Inspect', action: 'Open', route: '/admin/users' },
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
