import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { Course } from '@core/models/course.model';
import { PaymentService } from '../../features/payment/payment.service';

/**
 * Blocks access to a paid course route until the current user has either (a) enrolled in a
 * free course, or (b) a SUCCESS payment on record for it — otherwise redirects to checkout.
 * Reads the course id from the `courseId` route param, falling back to `id` since different
 * course-detail routes in this app use either name.
 */
export const paymentGuard: CanActivateFn = (route) => {
  const apiService = inject(ApiService);
  const paymentService = inject(PaymentService);
  const router = inject(Router);

  const courseIdParam = route.paramMap.get('courseId') ?? route.paramMap.get('id');
  const courseId = courseIdParam ? Number(courseIdParam) : null;

  if (!courseId) {
    return true;
  }

  return apiService.get<Course>(`/courses/${courseId}`).pipe(
    switchMap((course) => {
      if (!course.price || course.price <= 0) {
        return of(true);
      }
      return paymentService.getStatus(courseId).pipe(
        map((status) => status.paid || router.createUrlTree(['/payment/checkout', courseId]))
      );
    }),
    catchError(() => of(router.createUrlTree(['/payment/checkout', courseId])))
  );
};
