import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { errorInterceptor } from './error.interceptor';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { AuthResponse } from '@core/auth/auth.models';
import { UserRole } from '@core/models/user.model';

describe('errorInterceptor (W1-06 refresh retry & logout)', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: { refreshToken: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  const mockAuthResponse: AuthResponse = {
    token: 'refreshed-jwt-token-999',
    user: {
      id: 1,
      fullName: 'Student',
      email: 'student@example.com',
      role: UserRole.STUDENT,
      createdAt: '2026-01-01',
    },
  };

  beforeEach(() => {
    authServiceSpy = {
      refreshToken: vi.fn(),
      logout: vi.fn(),
    };
    routerSpy = {
      navigate: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: NotificationService, useValue: { error: vi.fn() } },
        ErrorHandlerService,
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('triggers silent refresh on 401 and retries original request with new token', () => {
    authServiceSpy.refreshToken.mockReturnValue(of(mockAuthResponse));

    let responseBody: any = null;
    http.get('/api/protected/data').subscribe((res) => {
      responseBody = res;
    });

    // Initial protected request fails with 401
    const firstReq = httpMock.expectOne('/api/protected/data');
    firstReq.flush({ detail: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    // Expect silent refresh to be called
    expect(authServiceSpy.refreshToken).toHaveBeenCalled();

    // Expect retried request with new Bearer token and retry header
    const retryReq = httpMock.expectOne('/api/protected/data');
    expect(retryReq.request.headers.get('Authorization')).toBe('Bearer refreshed-jwt-token-999');
    expect(retryReq.request.headers.get('X-Auth-Retry')).toBe('true');
    retryReq.flush({ success: true });

    expect(responseBody).toEqual({ success: true });
    expect(authServiceSpy.logout).not.toHaveBeenCalled();
  });

  it('logs out and redirects to /login on 401 from /auth/login without attempting refresh', () => {
    http.post('/api/auth/login', { email: 'bad@credentials.com' }).subscribe({
      error: () => {},
    });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush({ detail: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refreshToken).not.toHaveBeenCalled();
    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('logs out and redirects to /login when silent refresh fails', () => {
    authServiceSpy.refreshToken.mockReturnValue(throwError(() => new Error('Refresh failed')));

    http.get('/api/protected/data').subscribe({
      error: () => {},
    });

    const firstReq = httpMock.expectOne('/api/protected/data');
    firstReq.flush({ detail: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.refreshToken).toHaveBeenCalled();
    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });
});
