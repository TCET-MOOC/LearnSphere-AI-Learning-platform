import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { UserRole } from '@core/models/user.model';

/**
 * Restricts a route to the role(s) listed in its `data.roles` array.
 * Assumes authGuard already confirmed the user is signed in.
 */
export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const allowedRoles = (route.data?.['roles'] as UserRole[] | undefined) ?? [];
  if (allowedRoles.length === 0 || authService.hasRole(...allowedRoles)) {
    return true;
  }

  const fallback = authService.dashboardPathForRole(authService.currentUser?.role);
  return router.createUrlTree([fallback]);
};
