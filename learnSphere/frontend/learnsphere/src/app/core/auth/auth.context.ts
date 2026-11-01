import { HttpContextToken } from '@angular/common/http';

/**
 * Marks an auth request whose 401 is an expected answer, not a lost session:
 * the error interceptor must neither log out nor redirect to /login for it.
 * Used by the start-up session restore, which runs on public pages too.
 */
export const SILENT_AUTH = new HttpContextToken<boolean>(() => false);
