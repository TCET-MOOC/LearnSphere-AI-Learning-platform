import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStore } from './auth.store';

/**
 * Attaches the in-memory JWT (if present) as a Bearer token and
 * ensures withCredentials: true so the browser sends and receives
 * the HttpOnly refresh token cookie on API requests (AUD-15 / W1-06).
 */
export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authStore = inject(AuthStore);
  const token = authStore.accessToken;

  const authReq = req.clone({
    withCredentials: true,
    ...(token ? { setHeaders: { Authorization: `Bearer ${token}` } } : {})
  });

  return next(authReq);
};
