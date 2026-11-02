/**
 * Production: same-origin paths only (W7-03). nginx proxies /api and /ws-native to the
 * backend and /api/payments/orders|webhook to the payments service (nginx.conf).
 */
export const environment = {
  production: true,
  endpoints: {
    api: '/api',
    payments: '/api/payments',
    websocket: '/ws-native',
  },
};
