import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';
import { ApiService } from '@core/services/api.service';
import { StorageService } from '@core/services/storage.service';
import { User, UserRole } from '@core/models/user.model';
import { AuthResponse } from './auth.models';

describe('AuthService (W1-06 memory-only session & refresh)', () => {
  let service: AuthService;
  let authStore: AuthStore;
  let apiServiceSpy: { post: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn>; put: ReturnType<typeof vi.fn> };

  const mockUser: User = {
    id: 1,
    fullName: 'Test Student',
    email: 'student@example.com',
    role: UserRole.STUDENT,
    createdAt: '2026-01-01T00:00:00Z',
  };

  const mockAuthResponse: AuthResponse = {
    token: 'jwt-access-token-xyz',
    user: mockUser,
  };

  beforeEach(() => {
    localStorage.clear();
    apiServiceSpy = {
      post: vi.fn(),
      get: vi.fn(),
      put: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        AuthStore,
        StorageService,
        { provide: ApiService, useValue: apiServiceSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    authStore = TestBed.inject(AuthStore);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('stores access token and user in memory and NEVER in localStorage on login', () => {
    apiServiceSpy.post.mockReturnValue(of(mockAuthResponse));

    service.login({ email: 'student@example.com', password: 'Password@123' }).subscribe();

    // In-memory verification
    expect(service.getAccessToken()).toBe('jwt-access-token-xyz');
    expect(service.currentUser).toEqual(mockUser);
    expect(service.isAuthenticated).toBe(true);

    // Critical AUD-15 security verification: no tokens or user data in localStorage
    expect(localStorage.getItem('ls_token')).toBeNull();
    expect(localStorage.getItem('ls_user')).toBeNull();
  });

  it('calls POST /auth/logout, clears in-memory state, and cleans up legacy storage', () => {
    // Seed in-memory session and legacy localStorage keys
    authStore.setSession('temp-token', mockUser);
    localStorage.setItem('ls_token', 'legacy-leak');
    localStorage.setItem('ls_user', JSON.stringify(mockUser));

    apiServiceSpy.post.mockReturnValue(of({ message: 'Logged out successfully' }));

    service.logout();

    expect(apiServiceSpy.post).toHaveBeenCalledWith('/auth/logout', {}, { withCredentials: true });
    expect(service.getAccessToken()).toBeNull();
    expect(service.currentUser).toBeNull();
    expect(service.isAuthenticated).toBe(false);

    expect(localStorage.getItem('ls_token')).toBeNull();
    expect(localStorage.getItem('ls_user')).toBeNull();
  });

  it('rotates access token in memory via POST /auth/refresh without touching localStorage', () => {
    const refreshedResponse: AuthResponse = {
      token: 'new-rotated-token-123',
      user: mockUser,
    };
    apiServiceSpy.post.mockReturnValue(of(refreshedResponse));

    service.refreshToken().subscribe((res) => {
      expect(res.token).toBe('new-rotated-token-123');
    });

    expect(apiServiceSpy.post).toHaveBeenCalledWith('/auth/refresh', {}, { withCredentials: true });
    expect(service.getAccessToken()).toBe('new-rotated-token-123');
    expect(localStorage.getItem('ls_token')).toBeNull();
  });
});
