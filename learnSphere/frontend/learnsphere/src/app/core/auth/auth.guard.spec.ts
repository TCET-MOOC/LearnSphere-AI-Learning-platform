import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom, isObservable, of, throwError } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';
import { UserRole } from '@core/models/user.model';
import { fakeJwt } from './fake-jwt.testing';

const user = { id: 1, fullName: 'Aarav Shah', email: 'a@example.com', role: UserRole.STUDENT, createdAt: '2026-01-01' };
const tokenExpiringIn = (ms: number) => fakeJwt({ sub: '1', exp: Math.floor((Date.now() + ms) / 1000) });

describe('authGuard (W7-04)', () => {
  let store: AuthStore;
  let refresh: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    refresh = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        AuthStore,
        { provide: AuthService, useFactory: (s: AuthStore) => ({ get isAuthenticated() { return s.isAuthenticated; }, getAccessToken: () => s.accessToken, refreshToken: refresh, logout: () => s.clearSession() }), deps: [AuthStore] },
      ],
    });
    store = TestBed.inject(AuthStore);
  });

  async function run(url: string) {
    const result = TestBed.runInInjectionContext(() => authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));
    const value = isObservable(result) ? await firstValueFrom(result) : await result;
    return value instanceof UrlTree ? TestBed.inject(Router).serializeUrl(value) : value;
  }

  it('lets a live session through without refreshing', async () => {
    store.setSession(tokenExpiringIn(10 * 60_000), user);
    expect(await run('/student/notes')).toBe(true);
    expect(refresh).not.toHaveBeenCalled();
  });

  it('sends a signed-out visitor to login and keeps the page they asked for', async () => {
    expect(await run('/student/notes')).toBe('/login?redirectTo=%2Fstudent%2Fnotes');
  });

  it('renews an expired access token before letting the user in', async () => {
    store.setSession(tokenExpiringIn(-1_000), user);
    refresh.mockImplementation(() => { store.setSession(tokenExpiringIn(15 * 60_000), user); return of({}); });
    expect(await run('/student/notes')).toBe(true);
    expect(refresh).toHaveBeenCalledWith({ silent: true });
  });

  it('when the expired session cannot be renewed: login with the page and the reason', async () => {
    store.setSession(tokenExpiringIn(-1_000), user);
    refresh.mockReturnValue(throwError(() => new Error('401')));
    expect(await run('/student/notes')).toBe('/login?redirectTo=%2Fstudent%2Fnotes&reason=expired');
    expect(store.isAuthenticated).toBe(false);
  });
});
