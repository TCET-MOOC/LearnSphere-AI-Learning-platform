import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { StorageService } from '@core/services/storage.service';
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
 * AuthService owns the authenticated session: login/register against the backend,
 * persisting the JWT + user snapshot, and exposing the current user as an observable
 * so guards/components can react to auth state changes.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<User | null>;
  currentUser$: Observable<User | null>;

  constructor(private apiService: ApiService, private storageService: StorageService) {
    this.currentUserSubject = new BehaviorSubject<User | null>(this.storageService.getUser());
    this.currentUser$ = this.currentUserSubject.asObservable();
  }

  get currentUser(): User | null {
    return this.currentUserSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this.storageService.getToken();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.apiService.post<AuthResponse>('/auth/login', request).pipe(
      tap((res) => this.persistSession(res))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.apiService.post<AuthResponse>('/auth/register', request).pipe(
      tap((res) => this.persistSession(res))
    );
  }

  fetchCurrentUser(): Observable<User> {
    return this.apiService.get<User>('/auth/me').pipe(
      tap((user) => {
        this.storageService.setUser(user);
        this.currentUserSubject.next(user);
      })
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
      tap((user) => {
        this.storageService.setUser(user);
        this.currentUserSubject.next(user);
      })
    );
  }

  logout(): void {
    this.storageService.clear();
    this.currentUserSubject.next(null);
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

  private persistSession(res: AuthResponse): void {
    this.storageService.setToken(res.token);
    this.storageService.setUser(res.user);
    this.currentUserSubject.next(res.user);
  }
}
