import { TestBed } from '@angular/core/testing';
import { HttpContext, HttpErrorResponse, provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { NEVER, Subject, firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';
import { SILENT_AUTH, provideSessionRestore } from './session-restore';
import { ApiService } from '@core/services/api.service';
import { errorInterceptor } from '@core/interceptors/error.interceptor';
import { NotificationService } from '@core/services/notification.service';
import { UserRole } from '@core/models/user.model';
import { appConfig } from '../../app.config';

/**
 * Session restore on page load (pulled forward from W7-04, needed by the visual harness):
 * the in-memory access token (W1-06) is rebuilt from the HttpOnly refresh cookie at startup.
 */
describe('session restore on page load', () => {
  const user = { id: 1, fullName: 'Asha Rao', email: 'asha@example.test', role: UserRole.STUDENT, createdAt: '2026-01-01T00:00:00Z' };

  describe('AuthService.restoreSession', () => {
    let api: { post: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn>; put: ReturnType<typeof vi.fn> };
    let service: AuthService;
    let store: AuthStore;

    beforeEach(() => {
      api = { post: vi.fn(), get: vi.fn(), put: vi.fn() };
      TestBed.configureTestingModule({ providers: [AuthService, AuthStore, { provide: ApiService, useValue: api }] });
      service = TestBed.inject(AuthService);
      store = TestBed.inject(AuthStore);
    });

    afterEach(() => vi.useRealTimers());

    it('restores the session from the refresh cookie, marked silent', async () => {
      api.post.mockReturnValue(of({ token: 'fresh-token', user }));
      await expect(firstValueFrom(service.restoreSession())).resolves.toBe(true);
      expect(store.accessToken).toBe('fresh-token');
      expect(store.currentUser).toEqual(user);
      const [url, , options] = api.post.mock.calls[0];
      expect(url).toBe('/auth/refresh');
      expect(options.withCredentials).toBe(true);
      expect((options.context as HttpContext).get(SILENT_AUTH)).toBe(true);
    });

    it('starts signed out, without calling logout, when there is no valid cookie', async () => {
      api.post.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
      await expect(firstValueFrom(service.restoreSession())).resolves.toBe(false);
      expect(store.accessToken).toBeNull();
      expect(api.post).toHaveBeenCalledTimes(1); // no /auth/logout call
    });

    it('gives up after 5 seconds so a down backend cannot block startup', async () => {
      vi.useFakeTimers();
      api.post.mockReturnValue(NEVER);
      const result = firstValueFrom(service.restoreSession());
      await vi.advanceTimersByTimeAsync(5000);
      await expect(result).resolves.toBe(false);
    });

    it('serialises refreshes so two callers never send the same refresh cookie at once', async () => {
      const first = new Subject<unknown>();
      const second = new Subject<unknown>();
      api.post.mockReturnValueOnce(first).mockReturnValueOnce(second);

      const a = firstValueFrom(service.refreshToken());
      const b = firstValueFrom(service.refreshToken());
      await Promise.resolve();
      await Promise.resolve();
      expect(api.post).toHaveBeenCalledTimes(1);

      first.next({ token: 't1', user });
      first.complete();
      await a;
      await vi.waitFor(() => expect(api.post).toHaveBeenCalledTimes(2));

      second.next({ token: 't2', user });
      second.complete();
      await expect(b).resolves.toMatchObject({ token: 't2' });
      expect(store.accessToken).toBe('t2');
    });
  });

  describe('error interceptor', () => {
    let http: HttpClient;
    let ctrl: HttpTestingController;
    let router: { navigate: ReturnType<typeof vi.fn> };
    let auth: { logout: ReturnType<typeof vi.fn>; refreshToken: ReturnType<typeof vi.fn> };

    beforeEach(() => {
      router = { navigate: vi.fn() };
      auth = { logout: vi.fn(), refreshToken: vi.fn() };
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(withInterceptors([errorInterceptor])),
          provideHttpClientTesting(),
          { provide: Router, useValue: router },
          { provide: AuthService, useValue: auth },
          { provide: NotificationService, useValue: { error: vi.fn(), success: vi.fn(), info: vi.fn() } }
        ]
      });
      http = TestBed.inject(HttpClient);
      ctrl = TestBed.inject(HttpTestingController);
    });

    it('does not log out or redirect when a silent refresh is rejected', async () => {
      const result = firstValueFrom(http.post('/api/auth/refresh', {}, { context: new HttpContext().set(SILENT_AUTH, true) }))
        .catch((e) => e);
      ctrl.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
      expect((await result).status).toBe(401);
      expect(router.navigate).not.toHaveBeenCalled();
      expect(auth.logout).not.toHaveBeenCalled();
    });

    it('still logs out and redirects when an ordinary refresh is rejected', async () => {
      const result = firstValueFrom(http.post('/api/auth/refresh', {})).catch((e) => e);
      ctrl.expectOne('/api/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
      await result;
      expect(auth.logout).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('startup', () => {
    it('provideSessionRestore runs restoreSession before the app starts', async () => {
      const restoreSession = vi.fn(() => of(true));
      TestBed.configureTestingModule({
        providers: [provideSessionRestore(), { provide: AuthService, useValue: { restoreSession } }]
      });
      await TestBed.compileComponents();
      TestBed.inject(AuthService);
      expect(restoreSession).toHaveBeenCalledTimes(1);
    });

    it('is part of the application config', () => {
      const restoreSession = vi.fn(() => of(false));
      TestBed.configureTestingModule({
        providers: [...appConfig.providers, { provide: AuthService, useValue: { restoreSession } }]
      });
      TestBed.inject(Router);
      expect(restoreSession).toHaveBeenCalledTimes(1);
    });
  });
});
