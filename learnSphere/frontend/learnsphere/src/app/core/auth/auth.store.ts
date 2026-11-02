import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { User } from '@core/models/user.model';

/**
 * AuthStore holds client session state strictly in memory (AUD-15 / W1-06).
 * Access tokens are NEVER written to localStorage. Legacy storage keys
 * 'ls_token' and 'ls_user' are actively cleared on session changes.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  private readonly accessTokenSubject = new BehaviorSubject<string | null>(null);
  private currentUserSubject = new BehaviorSubject<User | null>(null);

  readonly currentUser$: Observable<User | null> = this.currentUserSubject.asObservable();
  /** Emits on every new access token and on sign-out (null); drives SessionKeeper (W7-04). */
  readonly accessToken$: Observable<string | null> = this.accessTokenSubject.asObservable();

  get accessToken(): string | null {
    return this.accessTokenSubject.value;
  }

  get currentUser(): User | null {
    return this.currentUserSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this.accessTokenSubject.value;
  }

  setSession(token: string, user: User): void {
    this.currentUserSubject.next(user);
    this.accessTokenSubject.next(token);
    this.purgeLegacyStorage();
  }

  setAccessToken(token: string): void {
    this.accessTokenSubject.next(token);
    this.purgeLegacyStorage();
  }

  setUser(user: User): void {
    this.currentUserSubject.next(user);
  }

  clearSession(): void {
    this.accessTokenSubject.next(null);
    this.currentUserSubject.next(null);
    this.purgeLegacyStorage();
  }

  private purgeLegacyStorage(): void {
    try {
      localStorage.removeItem('ls_token');
      localStorage.removeItem('ls_user');
    } catch {
      // Ignore security/sandboxed environments where localStorage is restricted
    }
  }
}
