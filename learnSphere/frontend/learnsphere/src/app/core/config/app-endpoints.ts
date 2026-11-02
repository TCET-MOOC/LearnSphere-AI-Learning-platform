import { InjectionToken } from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * W7-03: where the browser reaches the backend, the payments service and the
 * websocket broker. Development points at the local services; production uses
 * same-origin paths that nginx proxies (nginx.conf), so no build carries a host name.
 */
export interface AppEndpoints {
  /** Main API base, e.g. `/api` or `http://localhost:8080/api`. */
  api: string;
  /** Payments service base, e.g. `/api/payments` or `http://localhost:8081/api/payments`. */
  payments: string;
  /** STOMP broker; a path (`/ws-native`) is resolved against the page origin. */
  websocket: string;
}

/** Resolve a same-origin websocket path to `ws(s)://host/path`; absolute URLs pass through. */
export function resolveEndpoints(
  configured: AppEndpoints,
  page: { protocol: string; host: string } = window.location,
): AppEndpoints {
  const websocket = configured.websocket.startsWith('/')
    ? `${page.protocol === 'https:' ? 'wss:' : 'ws:'}//${page.host}${configured.websocket}`
    : configured.websocket;
  return { ...configured, websocket };
}

/** Origin that serves API-relative media paths (`''` when the API is same-origin). */
export function apiOrigin(api: string): string {
  return api.replace(/\/api\/?$/, '');
}

export const APP_ENDPOINTS = new InjectionToken<AppEndpoints>('APP_ENDPOINTS', {
  providedIn: 'root',
  factory: () => resolveEndpoints(environment.endpoints),
});
