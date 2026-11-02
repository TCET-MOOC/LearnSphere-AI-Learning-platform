import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { APP_ENDPOINTS, apiOrigin, resolveEndpoints } from './app-endpoints';
import { ApiService } from '@core/services/api.service';
import { PaymentsService } from '../../features/student/services/payments.service';
import { environment } from '../../../environments/environment';
import { environment as prodEnvironment } from '../../../environments/environment.prod';

describe('app endpoints (W7-03)', () => {
  describe('resolveEndpoints', () => {
    it('turns a same-origin websocket path into wss:// on an https page', () => {
      const e = resolveEndpoints({ api: '/api', payments: '/api/payments', websocket: '/ws-native' }, { protocol: 'https:', host: 'learn.example.edu' });
      expect(e).toEqual({ api: '/api', payments: '/api/payments', websocket: 'wss://learn.example.edu/ws-native' });
    });

    it('uses ws:// on an http page', () => {
      expect(resolveEndpoints({ api: '/api', payments: '/api/payments', websocket: '/ws-native' }, { protocol: 'http:', host: 'localhost:4200' }).websocket)
        .toBe('ws://localhost:4200/ws-native');
    });

    it('keeps absolute URLs as configured', () => {
      const dev = { api: 'http://localhost:8080/api', payments: 'http://localhost:8081/api/payments', websocket: 'ws://localhost:8080/ws-native' };
      expect(resolveEndpoints(dev, { protocol: 'http:', host: 'localhost:4200' })).toEqual(dev);
    });
  });

  it('derives the media origin from the API base', () => {
    expect(apiOrigin('/api')).toBe('');
    expect(apiOrigin('http://localhost:8080/api')).toBe('http://localhost:8080');
    expect(apiOrigin('https://learn.example.edu/api/')).toBe('https://learn.example.edu');
  });

  it('production environment uses same-origin paths only', () => {
    expect(prodEnvironment.production).toBe(true);
    expect(prodEnvironment.endpoints).toEqual({ api: '/api', payments: '/api/payments', websocket: '/ws-native' });
  });

  it('development environment keeps the local services', () => {
    expect(environment.endpoints).toEqual({
      api: 'http://localhost:8080/api',
      payments: 'http://localhost:8081/api/payments',
      websocket: 'ws://localhost:8080/ws-native',
    });
  });

  describe('services read the injected endpoints', () => {
    let http: HttpTestingController;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: APP_ENDPOINTS, useValue: { api: 'https://main.test/api', payments: 'https://pay.test/api/payments', websocket: 'wss://main.test/ws-native' } },
        ],
      });
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('ApiService prefixes the API base', () => {
      TestBed.inject(ApiService).get('/courses').subscribe();
      http.expectOne('https://main.test/api/courses').flush([]);
    });

    it('PaymentsService sends orders to the payments service and history to the main API', () => {
      const payments = TestBed.inject(PaymentsService);
      payments.getMyOrders().subscribe();
      http.expectOne('https://pay.test/api/payments/orders/me').flush([]);
      payments.getPaymentHistory().subscribe();
      http.expectOne('https://main.test/api/payments/history').flush([]);
    });
  });
});
