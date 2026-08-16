import { Injectable } from '@angular/core';
import { User } from '@core/models/user.model';

const TOKEN_KEY = 'ls_token';
const USER_KEY = 'ls_user';

/**
 * StorageService wraps localStorage access for the JWT and current user snapshot.
 */
@Injectable({
  providedIn: 'root'
})
export class StorageService {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  }

  getUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as User) : null;
  }

  setUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
}
