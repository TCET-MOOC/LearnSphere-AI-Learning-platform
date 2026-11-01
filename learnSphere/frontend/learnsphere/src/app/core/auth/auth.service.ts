import { Injectable } from '@angular/core';
import { HttpContext } from '@angular/common/http';
import { Observable, catchError, defer, finalize, firstValueFrom, from, map, of, tap, timeout } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AuthStore } from './auth.store';
import { SILENT_AUTH } from './auth.context';

const REFRESH_LOCK = 'learnsphere-auth-refresh';
const REFRESH_TIMEOUT_MS = 15_000;
const RESTORE_TIMEOUT_MS = 5_000;
import { User, UserRole } from '@core/models/user.model';
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  VerifyCollegeRequest
} from './auth.models';

/**
 * AuthService owns the authenticated session: login/register/refresh/logout against the backend,
 * maintaining the JWT in memory via AuthStore (AUD-15 / W1-06), and exposing reactive auth state.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  readonly currentUser$: Observable<User | null>;
  private refreshQueue: Promise<unknown> = Promise.resolve();

  constructor(
    private apiService: ApiService,
    private authStore: AuthStore
  ) {
    this.currentUser$ = this.authStore.currentUser$;
  }

  get currentUser(): User | null {
    return this.authStore.currentUser;
  }

  get isAuthenticated(): boolean {
    return this.authStore.isAuthenticated;
  }

  getAccessToken(): string | null {
    return this.authStore.accessToken;
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.apiService.post<AuthResponse>('/auth/login', request, { withCredentials: true }).pipe(
      tap((res) => this.authStore.setSession(res.token, res.user))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.apiService.post<AuthResponse>('/auth/register', request, { withCredentials: true }).pipe(
      tap((res) => this.authStore.setSession(res.token, res.user))
    );
  }

  /**
   * Rotates the refresh cookie and stores the new access token.
   * Refreshes are serialised (across tabs where the browser supports Web Locks, otherwise
   * within this tab): W1-05 revokes the whole token family when a rotated refresh token is
   * reused, so two concurrent refreshes carrying the same cookie would sign the user out.
   */
  refreshToken(options: { silent?: boolean; timeoutMs?: number } = {}): Observable<AuthResponse> {
    const { silent = false, timeoutMs = REFRESH_TIMEOUT_MS } = options;
    const request = () => firstValueFrom(
      this.apiService.post<AuthResponse>('/auth/refresh', {}, {
        withCredentials: true,
        context: new HttpContext().set(SILENT_AUTH, silent)
      }).pipe(timeout(timeoutMs))
    );
    return defer(() => from(this.exclusive(request))).pipe(
      tap((res) => this.authStore.setSession(res.token, res.user))
    );
  }

  /** Start-up restore: true when the refresh cookie produced a session; never redirects. */
  restoreSession(): Observable<boolean> {
    return this.refreshToken({ silent: true, timeoutMs: RESTORE_TIMEOUT_MS }).pipe(
      map(() => true),
      catchError(() => of(false))
    );
  }

  private exclusive<T>(work: () => Promise<T>): Promise<T> {
    const locks = typeof navigator !== 'undefined'
      ? (navigator as Navigator & { locks?: { request<R>(name: string, cb: () => Promise<R>): Promise<R> } }).locks
      : undefined;
    if (locks?.request) {
      return locks.request<T>(REFRESH_LOCK, work);
    }
    const run: Promise<T> = this.refreshQueue.then(() => work(), () => work());
    this.refreshQueue = run.catch(() => undefined);
    return run;
  }

  fetchCurrentUser(): Observable<User> {
    return this.apiService.get<User>('/users/me').pipe(
      tap((user) => this.authStore.setUser(user))
    );
  }

  updateProfile(patch: Partial<Pick<User, 'fullName' | 'bio' | 'avatarUrl'>>): Observable<User> {
    return this.apiService.put<User>('/users/me', patch).pipe(
      tap((user) => this.authStore.setUser(user))
    );
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<{ message: string }> {
    return this.apiService.post<{ message: string }>('/auth/forgot-password', request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<{ message: string }> {
    return this.apiService.post<{ message: string }>('/auth/reset-password', request);
  }

  verifyCollege(request: VerifyCollegeRequest): Observable<User> {
    return this.apiService.post<User>('/auth/verify-college', request).pipe(
      tap((user) => this.authStore.setUser(user))
    );
  }

  logout(): void {
    this.apiService.post('/auth/logout', {}, { withCredentials: true }).pipe(
      catchError(() => of(null)),
      finalize(() => this.authStore.clearSession())
    ).subscribe();
    this.authStore.clearSession();
  }

  hasRole(...roles: UserRole[]): boolean {
    const role = this.currentUser?.role;
    return !!role && roles.includes(role);
  }

  dashboardPathForRole(role: UserRole | string | undefined): string {
    switch (role) {
      case UserRole.TEACHER:
        return '/teacher/dashboard';
      case UserRole.ADMIN:
        return '/admin/dashboard';
      default:
        return '/student/dashboard';
    }
  }
}
