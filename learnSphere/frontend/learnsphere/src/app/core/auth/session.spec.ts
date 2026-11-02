import { expiredLoginExtras, isExpired, safeReturnUrl, tokenExpiresAt } from './session';
import { fakeJwt } from './fake-jwt.testing';


describe('session helpers (W7-04)', () => {
  describe('tokenExpiresAt', () => {
    it('reads the exp claim as epoch milliseconds', () => {
      expect(tokenExpiresAt(fakeJwt({ sub: '1', exp: 1_900_000_000 }))).toBe(1_900_000_000_000);
    });

    it('decodes base64url payloads (- and _)', () => {
      // "?>?" style bytes produce + and / in plain base64; base64url swaps them.
      const token = fakeJwt({ exp: 1_900_000_123, name: '??>>??' });
      expect(tokenExpiresAt(token)).toBe(1_900_000_123_000);
    });

    it('returns null for tokens without a readable exp', () => {
      expect(tokenExpiresAt('not-a-jwt')).toBeNull();
      expect(tokenExpiresAt(fakeJwt({ sub: '1' }))).toBeNull();
      expect(tokenExpiresAt('a.%%%.c')).toBeNull();
    });
  });

  describe('isExpired', () => {
    it('compares the exp claim with the clock', () => {
      const exp = 1_900_000_000;
      expect(isExpired(fakeJwt({ exp }), exp * 1000 - 1)).toBe(false);
      expect(isExpired(fakeJwt({ exp }), exp * 1000)).toBe(true);
    });

    it('leaves an unreadable token to the server (its 401 starts the refresh)', () => {
      expect(isExpired('opaque-token')).toBe(false);
      expect(isExpired(null)).toBe(false);
    });
  });

  describe('safeReturnUrl', () => {
    it('accepts in-app paths', () => {
      expect(safeReturnUrl('/student/courses/4?tab=notes')).toBe('/student/courses/4?tab=notes');
    });

    it('rejects anything that could leave the app or loop back to login', () => {
      for (const bad of [null, undefined, '', 'student/courses', '//evil.example', '/\\evil.example', 'https://evil.example', 'javascript:alert(1)', '/login', '/login?redirectTo=/x', '/register']) {
        expect(safeReturnUrl(bad as string | null | undefined), String(bad)).toBeNull();
      }
    });
  });

  it('builds the login redirect for an expired session', () => {
    expect(expiredLoginExtras('/teacher/courses/3/manage')).toEqual({
      queryParams: { redirectTo: '/teacher/courses/3/manage', reason: 'expired' },
    });
    expect(expiredLoginExtras('/login')).toEqual({ queryParams: { reason: 'expired' } });
  });
});
