import { EnvironmentProviders, Injectable, inject, provideEnvironmentInitializer } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from './auth.service';
import { AuthStore } from './auth.store';
import { REFRESH_LEAD_MS, expiredLoginExtras, tokenExpiresAt } from './session';
import { NotificationService } from '@core/services/notification.service';
import { SESSION_COPY } from '@core/content/session.content';

/**
 * Keeps a signed-in session alive (W7-04, A-09): the 15-minute access token is renewed
 * silently one minute before it expires, from the HttpOnly refresh cookie. When a tab comes
 * back from the background (where timers are throttled) an overdue renewal runs at once.
 * If renewal fails the user is signed out, told why, and sent to login with a return path.
 */
@Injectable({ providedIn: 'root' })
export class SessionKeeper {
  private readonly store = inject(AuthStore);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  private tokenSub?: Subscription;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private refreshAt: number | null = null;
  private refreshing = false;
  private readonly onVisible = () => {
    if (document.visibilityState !== 'hidden' && this.refreshAt !== null && Date.now() >= this.refreshAt) {
      this.renew();
    }
  };

  start(): void {
    if (this.tokenSub) return;
    this.tokenSub = this.store.accessToken$.subscribe((token) => this.schedule(token));
    document.addEventListener('visibilitychange', this.onVisible);
  }

  stop(): void {
    this.tokenSub?.unsubscribe();
    this.tokenSub = undefined;
    this.clearTimer();
    document.removeEventListener('visibilitychange', this.onVisible);
  }

  private schedule(token: string | null): void {
    this.clearTimer();
    const exp = tokenExpiresAt(token);
    if (!token || exp === null) return;
    this.refreshAt = exp - REFRESH_LEAD_MS;
    this.timer = setTimeout(() => this.renew(), Math.max(0, this.refreshAt - Date.now()));
  }

  private renew(): void {
    if (this.refreshing) return;
    this.refreshing = true;
    this.clearTimer();
    this.auth.refreshToken({ silent: true }).subscribe({
      // The new token arrives through accessToken$, which schedules the next renewal.
      next: () => { this.refreshing = false; },
      error: () => {
        this.refreshing = false;
        this.expire();
      },
    });
  }

  private expire(): void {
    const extras = expiredLoginExtras(this.router.url);
    this.auth.logout();
    this.notifications.info(SESSION_COPY.expiredToast);
    this.router.navigate(['/login'], extras);
  }

  private clearTimer(): void {
    if (this.timer !== null) clearTimeout(this.timer);
    this.timer = null;
    this.refreshAt = null;
  }
}

/** Starts the SessionKeeper with the app (after the start-up session restore). */
export function provideSessionKeeper(): EnvironmentProviders {
  return provideEnvironmentInitializer(() => inject(SessionKeeper).start());
}
