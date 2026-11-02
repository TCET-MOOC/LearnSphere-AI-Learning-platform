import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { expiredLoginExtras, isExpired } from './session';

/**
 * Blocks access to any route it guards unless the user holds a valid session.
 * An expired access token is renewed once from the refresh cookie before the route opens
 * (W7-04, [G-F5]); if that fails the user goes to login with the page they asked for.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated) {
    return router.createUrlTree(['/login'], { queryParams: { redirectTo: state.url } });
  }
  if (!isExpired(authService.getAccessToken())) {
    return true;
  }

  return authService.refreshToken({ silent: true }).pipe(
    map(() => true),
    catchError(() => {
      authService.logout();
      return of(router.createUrlTree(['/login'], expiredLoginExtras(state.url)));
    })
  );
};
