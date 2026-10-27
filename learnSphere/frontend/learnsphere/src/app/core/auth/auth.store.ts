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
  private _accessToken: string | null = null;
  private currentUserSubject = new BehaviorSubject<User | null>(null);

  readonly currentUser$: Observable<User | null> = this.currentUserSubject.asObservable();

  get accessToken(): string | null {
    return this._accessToken;
  }

  get currentUser(): User | null {
    return this.currentUserSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this._accessToken;
  }

  setSession(token: string, user: User): void {
    this._accessToken = token;
    this.currentUserSubject.next(user);
    this.purgeLegacyStorage();
  }

  setAccessToken(token: string): void {
    this._accessToken = token;
    this.purgeLegacyStorage();
  }

  setUser(user: User): void {
    this.currentUserSubject.next(user);
  }

  clearSession(): void {
    this._accessToken = null;
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
