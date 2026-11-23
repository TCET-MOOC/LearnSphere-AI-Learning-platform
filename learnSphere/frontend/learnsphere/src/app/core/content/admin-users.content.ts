/**
 * Copy added to User management (SCR-51) by W7-12: server paging, server tab counts, and the
 * Inspect sections for roles, payments and audit history (audit.md D5; AUD-12, BR-03).
 * One file per screen (see W7-07). The screen's older strings move with W7-15.
 */
export const ADMIN_USERS_COPY = {
  tabs: {
    all: (n: number) => `All users (${n})`,
    students: (n: number) => `Students (${n})`,
    teachers: (n: number) => `Teachers (${n})`,
    flagged: (n: number) => `Flagged (${n})`,
    blacklisted: (n: number) => `Blacklisted (${n})`,
  },
  pager: {
    label: 'User pages',
    previous: 'Previous page',
    next: 'Next page',
    status: (page: number, pages: number) => `Page ${page} of ${pages}`,
  },
  roleNames: { STUDENT: 'Student', TEACHER: 'Teacher', ADMIN: 'Admin' } as Record<string, string>,
  inspect: {
    loading: 'Loading account…',
    loadFailed: 'Could not load this account. Close and try again.',
    roles: 'Roles',
    noRoles: 'No roles assigned.',
    revoke: (role: string) => `Revoke ${role}`,
    assignLabel: 'Role to assign',
    assignPlaceholder: 'Choose a role',
    assign: 'Assign role',
    assigned: (role: string) => `${role} role assigned.`,
    revoked: (role: string) => `${role} role revoked.`,
    roleFailed: 'Could not change the role. Try again.',
    payments: 'Recent payments',
    noPayments: 'No payments.',
    audit: 'Account history',
    noAudit: 'No recorded changes.',
    unknownCourse: 'Course removed',
    actions: {
      USER_STATUS_CHANGED: 'Status changed',
      USER_ROLE_ASSIGNED: 'Role assigned',
      USER_ROLE_REVOKED: 'Role revoked',
    } as Record<string, string>,
  },
} as const;
