/**
 * Settings rows (W7-12, BR-24). A row is one of:
 * - `theme`: the real dark-mode switch (local, non-secret preference; A-09);
 * - `password`: sends a reset link to the account e-mail (`POST /auth/forgot-password`);
 * - `fact`: a read-only value taken from `GET /users/me`;
 * - `link`: opens another real screen;
 * - `unavailable`: a preference the server cannot store yet — shown, disabled, and labelled.
 */
export type AccountFact = 'collegeVerification' | 'role';

export type SettingRowDef =
  | { kind: 'theme'; label: string; hint: string }
  | { kind: 'password'; label: string; hint: string; action: string }
  | { kind: 'fact'; label: string; hint: string; fact: AccountFact }
  | { kind: 'link'; label: string; hint: string; action: string; route: string }
  | { kind: 'unavailable'; label: string; hint: string; value?: string; action?: string; toggle?: boolean };

export interface SettingsTabDef {
  name: string;
  title: string;
  description: string;
  rows: SettingRowDef[];
}

export interface AccountSettingsCopy {
  eyebrow: string;
  title: string;
  intro: string;
  navLabel: string;
  fallbackName: string;
  tabs: SettingsTabDef[];
  unavailable: string;
  signOut: { title: string; body: string; button: string };
  resetFailed: string;
  resetSent: (email: string) => string;
  facts: {
    verified: string;
    pending: string;
    notVerified: string;
    roles: Record<string, string>;
  };
}
