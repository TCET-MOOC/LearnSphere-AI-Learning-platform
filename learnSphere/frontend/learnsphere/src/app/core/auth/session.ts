import { NavigationExtras } from '@angular/router';

/** Refresh the access token this long before it expires (A-09: 15 min tokens). */
export const REFRESH_LEAD_MS = 60_000;

/** Expiry of a JWT access token in epoch milliseconds, or null when it has no readable `exp`. */
export function tokenExpiresAt(token: string | null | undefined): number | null {
  const payload = token?.split('.')[1];
  if (!payload) return null;
  try {
    const b64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const claims = JSON.parse(atob(b64.padEnd(b64.length + ((4 - (b64.length % 4)) % 4), '=')));
    return typeof claims?.exp === 'number' ? claims.exp * 1000 : null;
  } catch {
    return null;
  }
}

/**
 * True when the token's `exp` has passed. A token without a readable `exp` is left to the
 * server: its 401 starts the interceptor's refresh, so the client never guesses.
 */
export function isExpired(token: string | null | undefined, now = Date.now()): boolean {
  const exp = tokenExpiresAt(token);
  return exp !== null && exp <= now;
}

const AUTH_PAGES = /^\/(login|register|forgot-password)(?:[/?#]|$)/;

/**
 * A post-login return path, or null when it is unsafe: only same-app absolute paths are
 * allowed (no scheme, no protocol-relative `//host`, no backslash trick), and never an auth
 * page, which would loop.
 */
export function safeReturnUrl(raw: string | null | undefined): string | null {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || raw.startsWith('/\\')) return null;
  if (AUTH_PAGES.test(raw)) return null;
  return raw;
}

/** Navigation extras for "session expired, sign in again" that bring the user back afterwards. */
export function expiredLoginExtras(currentUrl: string): NavigationExtras {
  const redirectTo = safeReturnUrl(currentUrl);
  return { queryParams: redirectTo ? { redirectTo, reason: 'expired' } : { reason: 'expired' } };
}
