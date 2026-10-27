import { Injectable } from '@angular/core';
import { Observable, catchError, finalize, of, tap } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AuthStore } from './auth.store';
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

  refreshToken(): Observable<AuthResponse> {
    return this.apiService.post<AuthResponse>('/auth/refresh', {}, { withCredentials: true }).pipe(
      tap((res) => this.authStore.setSession(res.token, res.user))
    );
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
