/** Development: the local backend (8080), payments service (8081) and STOMP broker. */
export const environment = {
  production: false,
  endpoints: {
    api: 'http://localhost:8080/api',
    payments: 'http://localhost:8081/api/payments',
    websocket: 'ws://localhost:8080/ws-native',
  },
};
