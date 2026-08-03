import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { UserRole } from '@core/models/user.model';
import { NotificationService } from '@core/services/notification.service';

/**
 * Blocks teacher routes until their college affiliation has been verified by an admin.
 * Students/admins are unaffected (they don't go through affiliation review).
 */
export const verifiedCollegeGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const notificationService = inject(NotificationService);

  const user = authService.currentUser;
  if (!user || user.role !== UserRole.TEACHER) {
    return true;
  }

  if (user.collegeVerificationStatus === 'VERIFIED') {
    return true;
  }

  notificationService.info('Your college affiliation is still pending admin verification.');
  return router.createUrlTree(['/teacher/profile']);
};
