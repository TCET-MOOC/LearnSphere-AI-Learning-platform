import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';

const AUTH_RETRY_HEADER = 'X-Auth-Retry';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

function isAuthEndpoint(url: string): boolean {
  return url.includes('/auth/login') ||
         url.includes('/auth/register') ||
         url.includes('/auth/refresh') ||
         url.includes('/auth/logout');
}

/**
 * Global HTTP error handling (AUD-15 / W1-06):
 * On 401: triggers silent refresh with 1 retry on protected resources.
 * If refresh fails or if on auth endpoints, revokes session and redirects to /login.
 * On 403 / 500: surfaces error messages via NotificationService.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const notificationService = inject(NotificationService);
  const errorHandler = inject(ErrorHandlerService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        // Do not attempt refresh on auth entrypoints or already retried requests
        if (isAuthEndpoint(req.url) || req.headers.has(AUTH_RETRY_HEADER)) {
          authService.logout();
          router.navigate(['/login']);
          return throwError(() => error);
        }

        // Silent refresh with 1 retry
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          return authService.refreshToken().pipe(
            switchMap((res) => {
              isRefreshing = false;
              refreshTokenSubject.next(res.token);
              const retryReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${res.token}`,
                  [AUTH_RETRY_HEADER]: 'true'
                },
                withCredentials: true
              });
              return next(retryReq);
            }),
            catchError((refreshErr) => {
              isRefreshing = false;
              refreshTokenSubject.next(null);
              authService.logout();
              router.navigate(['/login']);
              return throwError(() => refreshErr);
            })
          );
        } else {
          // Another refresh is in flight: wait for the new token and retry once
          return refreshTokenSubject.pipe(
            filter((token): token is string => token !== null),
            take(1),
            switchMap((token) => {
              const retryReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${token}`,
                  [AUTH_RETRY_HEADER]: 'true'
                },
                withCredentials: true
              });
              return next(retryReq);
            })
          );
        }
      } else if (error instanceof HttpErrorResponse && error.status === 403) {
        notificationService.error(
          errorHandler.extractMessage(error, 'Access Denied: You do not have permission for this action.')
        );
      } else if (error instanceof HttpErrorResponse && (error.status === 0 || error.status >= 500)) {
        notificationService.error(errorHandler.extractMessage(error));
      }

      return throwError(() => error);
    })
  );
};
