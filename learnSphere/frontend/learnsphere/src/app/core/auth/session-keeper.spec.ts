import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { AuthStore } from './auth.store';
import { AuthService } from './auth.service';
import { SessionKeeper } from './session-keeper';
import { NotificationService } from '@core/services/notification.service';
import { SESSION_COPY } from '@core/content/session.content';
import { UserRole } from '@core/models/user.model';
import { fakeJwt } from './fake-jwt.testing';

const NOW = new Date('2026-11-02T10:00:00Z').getTime();
const user = { id: 1, fullName: 'Aarav Shah', email: 'a@example.com', role: UserRole.STUDENT, createdAt: '2026-01-01' };
/** Token expiring `ms` from the (fake) current time. */
const tokenExpiringIn = (ms: number) => fakeJwt({ sub: '1', exp: Math.floor((Date.now() + ms) / 1000) });

describe('SessionKeeper (W7-04)', () => {
  let store: AuthStore;
  let refresh: ReturnType<typeof vi.fn>;
  let logout: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.fn>;
  let info: ReturnType<typeof vi.fn>;
  let router: { url: string; navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(NOW);
    refresh = vi.fn();
    logout = vi.fn(() => store.clearSession());
    navigate = vi.fn();
    info = vi.fn();
    router = { url: '/student/notes', navigate };
    TestBed.configureTestingModule({
      providers: [
        AuthStore,
        { provide: AuthService, useValue: { refreshToken: refresh, logout } },
        { provide: Router, useValue: router },
        { provide: NotificationService, useValue: { info, error: vi.fn(), success: vi.fn() } },
      ],
    });
    store = TestBed.inject(AuthStore);
    TestBed.inject(SessionKeeper).start();
  });

  afterEach(() => {
    TestBed.inject(SessionKeeper).stop();
    vi.useRealTimers();
  });

  it('refreshes silently one minute before the access token expires', () => {
    refresh.mockImplementation(() => { store.setSession(tokenExpiringIn(30 * 60_000), user); return of({}); });
    store.setSession(tokenExpiringIn(15 * 60_000), user);

    vi.advanceTimersByTime(14 * 60_000 - 1);
    expect(refresh).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(refresh).toHaveBeenCalledWith({ silent: true });
  });

  it('keeps renewing: the next refresh is scheduled from the new token', () => {
    refresh.mockImplementation(() => { store.setSession(tokenExpiringIn(15 * 60_000), user); return of({}); });
    store.setSession(tokenExpiringIn(15 * 60_000), user);
    vi.advanceTimersByTime(14 * 60_000);
    vi.advanceTimersByTime(14 * 60_000);
    expect(refresh).toHaveBeenCalledTimes(2);
  });

  it('refreshes at once when the token is already inside the last minute', () => {
    refresh.mockReturnValue(new Subject());
    store.setSession(tokenExpiringIn(20_000), user);
    vi.advanceTimersByTime(0);
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it('when the refresh fails: signs out, says the session expired and returns to login with the current page', () => {
    refresh.mockReturnValue(throwError(() => new Error('401')));
    store.setSession(tokenExpiringIn(15 * 60_000), user);
    vi.advanceTimersByTime(14 * 60_000);

    expect(logout).toHaveBeenCalled();
    expect(info).toHaveBeenCalledWith(SESSION_COPY.expiredToast);
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { redirectTo: '/student/notes', reason: 'expired' } });
  });

  it('stops the timer on sign-out', () => {
    store.setSession(tokenExpiringIn(15 * 60_000), user);
    store.clearSession();
    vi.advanceTimersByTime(60 * 60_000);
    expect(refresh).not.toHaveBeenCalled();
  });

  it('catches up when a background tab becomes visible after the refresh time passed', () => {
    refresh.mockReturnValue(new Subject());
    store.setSession(tokenExpiringIn(15 * 60_000), user);
    // Background tabs throttle timers: jump the clock without running them.
    vi.setSystemTime(NOW + 14 * 60_000 + 5_000);
    document.dispatchEvent(new Event('visibilitychange'));
    expect(refresh).toHaveBeenCalledTimes(1);
  });
});
