import { Injectable } from '@angular/core';
import { User } from '@core/models/user.model';
import { AuthStore } from '@core/auth/auth.store';

/**
 * StorageService legacy compatibility wrapper.
 * Under AUD-15 (W1-06), access tokens and user snapshots are strictly held in-memory
 * inside AuthStore. Nothing is persisted to localStorage.
 */
@Injectable({
  providedIn: 'root'
})
export class StorageService {
  constructor(private authStore: AuthStore) {}

  getToken(): string | null {
    return this.authStore.accessToken;
  }

  setToken(token: string): void {
    this.authStore.setAccessToken(token);
  }

  getUser(): User | null {
    return this.authStore.currentUser;
  }

  setUser(user: User): void {
    this.authStore.setUser(user);
  }

  clear(): void {
    this.authStore.clearSession();
  }
}
