import { Injectable, inject } from '@angular/core';
import { PreloadingStrategy, Route } from '@angular/router';
import { Observable, of } from 'rxjs';
import { AuthService } from './auth.service';
import { UserRole } from '@core/models/user.model';

/**
 * W7-02: after sign-in, preload only the signed-in user's own role group
 * (`data.preloadFor`). Other roles' code is never downloaded ahead of use, and nothing
 * preloads while signed out. The router re-runs preloading after every navigation, so the
 * group loads right after login without a page refresh.
 */
@Injectable({ providedIn: 'root' })
export class RolePreloadingStrategy implements PreloadingStrategy {
  private readonly auth = inject(AuthService);

  preload(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    const target = route.data?.['preloadFor'] as UserRole | undefined;
    const role = this.auth.currentUser?.role;
    return target && role === target ? load() : of(null);
  }
}
